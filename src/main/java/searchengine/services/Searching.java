package searchengine.services;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.parsers.*;
import searchengine.repository.*;
import searchengine.utils.supportServises.CustomComparator;
import searchengine.utils.supportServises.LemmaFinder;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
@Service
public class Searching {
    @Autowired
    private SiteModelRepository siteModelRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private ObjectSearchRepository objectSearchRepository;
    private final SitesList sites;
    private final LemmaFinder lemmaFinder;
    private final CustomComparator customComparator;
    private final Indexing indexing;

    public String url;
    public String comment;
    static public Map listSideMap=new HashMap<>();

    public Site site;
    public static Map<String,Integer> removeKeys=new HashMap<>();

    public Searching(SiteModelRepository siteModelRepository, SitesList sites, LemmaFinder lemmaFinder, CustomComparator customComparator,Indexing indexing) {
        this.siteModelRepository = siteModelRepository;
        this.sites = sites;
        this.lemmaFinder = lemmaFinder;
        this.customComparator=customComparator;
        this.indexing=indexing;
    }
    public String getSearchSiteMap(String query) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        result.add( "\n 'count': " +sites.getSites().size());
        for (int i = 0; i < sites.getSites().size(); i++) {
            site = sites.getSites().get(i);
            url = site.getUrl();
            getSearch(query,url);
            result.add(indexing.toString(0,20,i+1));
        }
        return result.toString();
    }
    public String getSearch(String query,String site) throws IOException {
        siteModelRepository.deleteAll();
        pageRepository.deleteAll();
        objectSearchRepository.deleteAll();
        LinkExecutor.outHTML.clear();
        int numThreads = 5;
        LinkExecutor linkExecutor = new LinkExecutor(site, site);
        String siteMap = numThreads == 0 ? new ForkJoinPool().invoke(linkExecutor) : new ForkJoinPool(numThreads).invoke(linkExecutor);
        SiteModel siteModel = new SiteModel();
        listSideMap.clear();
        indexing.listSideMap.clear();
        indexing.getFinalSiteMap(siteMap);
        listSideMap.putAll(indexing.listSideMap);
        comment = "200 Ok";
        String name = site.substring(12);
        siteModel.setUrl(site);
        siteModel.setName(name);
        siteModel.setStatus(StatusType.INDEXED);
        siteModel.setLastError(comment);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModelRepository.save(siteModel);
        lemmaRepository.deleteAll();
        indexRepository.deleteAll();
        Map<String, Integer> lemmasSearch=new HashMap<>();
        lemmasSearch.putAll(lemmaFinder.collectLemmas(query));
        lemmaFinder.lemmas.clear();
        for (int j = 0; j < listSideMap.size(); j++) {
            Page page = new Page();
                page.setSiteId(siteModel.getId());
                page.setCode(200);
                page.setPath((String) listSideMap.get(j));
                page.setContent((String) LinkExecutor.outHTML.get(j));
                pageRepository.save(page);
                TreeMap<String, Integer> unsortedMap=new TreeMap<>();
                unsortedMap.putAll(lemmaFinder.collectLemmas(lemmaFinder.htmlCleaner(page.getContent())));
                Map<String, Integer> sortedMap = customComparator.valueSort(unsortedMap);
                LinkedHashMap<String, Integer> collectLemmas=new LinkedHashMap<>();
                collectLemmas.putAll(sortedMap);
                for (String key : collectLemmas.keySet()) {
                    if (collectLemmas.get(key)>8) {
                        removeKeys.put(key,collectLemmas.get(key));
                    }
                }
                for (String removeKey : removeKeys.keySet()) {
                    collectLemmas.remove(removeKey);
                }
                //Beginning stage №4
                for (String key : collectLemmas.keySet()) {
                    Lemma lemma = new Lemma();
                    Index index = new Index();
                    for (String keyLemmasSearch : lemmasSearch.keySet()) {
                        if (key.equals(keyLemmasSearch)) {
                            lemma.setLemma(keyLemmasSearch);
                            lemma.setFrequency(lemmaFinder.lemmas.get(key).intValue());
                            lemma.setSiteId(page.getSiteId());
                            lemmaRepository.save(lemma);
                            index.setPageId(page.getId());
                            index.setLemmaId(lemma.getId());
                            index.setRank(lemma.getFrequency());
                            indexRepository.save(index);
                        }
                    }
                }
                //Delete Lemma where  Frequency const.
                for (String key : lemmasSearch.keySet()) {
                    List<Lemma> byKeysFromLemma = lemmaRepository.findByLemma(key);
                    int frequencyBefore=0;
                    for (Lemma byKeyFromLemma :byKeysFromLemma) {
                        if (byKeyFromLemma.getFrequency()==frequencyBefore){
                            lemmaRepository.deleteById(byKeyFromLemma.getId());
                            continue;
                        }
                        //Setting Index equals Lemma
                        Iterable<Index> unRemovedLemmas = indexRepository.findAll();
                        for (Index unRemovedLemma :unRemovedLemmas) {
                            if (!lemmaRepository.existsById(unRemovedLemma.getLemmaId())){
                                indexRepository.delete(unRemovedLemma);
                            }
                        }
                        frequencyBefore++;
                    }
                }
        }
        //Creating and filling objectSearch
        Iterable<Index> byLemmaGetId = indexRepository.findAll();
        Iterable<Page> pagesFromRep=pageRepository.findAll();
        List<Page> pages = new ArrayList<>();
        for(Page pageFromRep:pagesFromRep) {
            pages.add( pageFromRep);
        }
        // Query divide on words and writing on  list
        LinkedHashMap<Integer, String> substringIndices =new LinkedHashMap<>();
        LinkedList<String> substrings = new LinkedList<>();
        String[] words = query.split("\\s+");
        substrings.addAll(List.of(words));


        // Sort out Index and filling objectSearch (without Snippet)
        for (Index byLemmaId :byLemmaGetId) {
            ObjectSearch objectSearch=new ObjectSearch();
            Document doc = null;
            try {
                doc = Jsoup.connect(site+pageRepository.findById(byLemmaId.getPageId()).get().getPath()).ignoreContentType(true).get();
            } catch (NullPointerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (HttpStatusException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String text="";
            if (doc ==null){
             text="406 Not Acceptable";
            }else {
                text = htmlCleaner(String.valueOf(doc.body())).toLowerCase();
            }
            String[] textList = text.split("\\s+");
            LinkedList<String> wordsFromText = new LinkedList<>();
            wordsFromText.addAll(List.of(textList));
            LinkedList<String> substringLemmas=new LinkedList<>();
            lemmaFinder.lemmas.clear();
            substringLemmas.addAll(lemmaFinder.collectLemmas(Arrays.toString(words)).keySet());
            for (String substring : substringLemmas) {
                for (String wordFromText : wordsFromText){
                    if (lemmaFinder.collectLemmas(wordFromText).containsKey(substring)) {
                        int index = text.indexOf(wordFromText);
                        if (index != -1 && index != 0) {
                            substringIndices.put(index, substring);
                        }
                    }
                    lemmaFinder.lemmas.clear();
                }
            }
            //Seek Index  nearby Indexes for couple Lemma
            LinkedHashMap<Integer, String> lemmaFirst =new LinkedHashMap<>();
            LinkedHashMap<Integer, String> lemmaSecond =new LinkedHashMap<>();
            String compareValue="";
            String indexSecondValue="";
            for (Integer index : substringIndices.keySet()) {
                compareValue=substringIndices.get(index);
            }
            for (Integer index : substringIndices.keySet()) {
                if (substringIndices.get(index).equals(compareValue)){
                    lemmaFirst.put(index,substringIndices.get(index));
                    continue;
                }
                lemmaSecond.put(index,substringIndices.get(index));
            }
            if (!lemmaSecond.isEmpty()||!lemmaFirst.isEmpty()) {
                for (Integer indexLemmaFirst : lemmaFirst.keySet()) {
                    for (Integer indexLemmaSecond : lemmaSecond.keySet()) {
                        if (Math.abs(indexLemmaFirst - indexLemmaSecond) < 15) {
                            substringIndices.clear();
                            substringIndices.put(indexLemmaFirst, lemmaFirst.get(indexLemmaFirst));
                            indexSecondValue=lemmaSecond.get(indexLemmaSecond);
                        }
                    }
                }
            }

            //Cut substring and writing on snippet
            Optional<Integer> max=substringIndices.keySet().stream().max(Comparator.comparing(i->i.intValue()));
            for (Integer index : substringIndices.keySet()) {
                int start = index - 97;
                int end = index + 87;
                if (start <=0){
                    start=2;
                } else if (end>max.get()) {
                    end=end-2;
                }
                String cutText="";
                try {
                     cutText = text.substring(start, end);
                }catch (StringIndexOutOfBoundsException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                String[] cutTextMassive = cutText.split("\\s+");
                String snippetText="";
                for (int i=1;i<cutTextMassive.length-1;i++){
                    lemmaFinder.lemmas.clear();
                    if(!lemmaFinder.collectLemmas(cutTextMassive[i]).containsKey(indexSecondValue)
                            &&!lemmaFinder.collectLemmas(cutTextMassive[i]).containsKey(substringIndices.get(index))) {
                        snippetText = snippetText + " " + cutTextMassive[i];
                    } else {
                    snippetText=snippetText+" <b>"+cutTextMassive[i]+"</b>";
                    }
                }
                objectSearch.setSnippet(snippetText);
                break;
            }
            objectSearch.setUri(pageRepository.findById(byLemmaId.getPageId()).get().getPath());
            objectSearch.setTitle(htmlCleaner(pageRepository.findById(byLemmaId.getPageId()).get().getContent()));
            objectSearch.setRelevance(byLemmaId.getRank());
            objectSearchRepository.save(objectSearch);
        }
        //Creating copy of list from objectSearchRepository
        Iterable<ObjectSearch> objectSearchesRep = objectSearchRepository.findAll();
        List<ObjectSearch> objectSearches = new ArrayList<>();
        for(ObjectSearch objectSearch:objectSearchesRep) {
            objectSearches.add(objectSearch);
        }
        //Searching  equals pages for  adding Relevance and  recording on list
        double relevance=0;
        for (int i=0;i<objectSearches.size();i++) {
            Object objectOne = objectSearches.get(i).getUri();
            relevance +=objectSearches.get(i).getRelevance();
            for (int j = 0; j < objectSearches.size(); j++) {
                if(!(i==j)) {
                    Object objectTwo = objectSearches.get(j).getUri();
                    if ( objectOne.equals(objectTwo)) {
                        relevance += objectSearches.get(j).getRelevance();
                        objectSearches.get(j).setUri("");
                    }
                }
            }
            if (relevance !=0){
                objectSearches.get(i).setRelevance(relevance);
            }
            relevance = 0;
        }
        //****удаление страниц с одним и тем же адресом , после суммирования Relevance, сиквестирование списка
//        for (int y=0;y<objectSearches.size();y++) {
//            if (objectSearches.get(y).getUri().equals("")){
//                objectSearches.remove(y);
//            }
 //       }
        //Searching maximum from  absolut Relevance, calculation and recording relative Relevance
        Optional<ObjectSearch> max=objectSearches.stream().max(Comparator.comparing(o -> o.getRelevance()));
        if (max.isPresent()) {
            double maxRelevance = max.get().getRelevance();
            for (ObjectSearch objectSearch : objectSearches) {
                objectSearch.setRelevance(objectSearch.getRelevance() / maxRelevance);
            }
        }
        //Sort out list increasing Relevance
        List<ObjectSearch> sortedList = objectSearches.stream()
                .sorted((o1, o2) -> {
                    if(o1.getRelevance() == o2.getRelevance())
                        return o1.getUri().compareTo(o2.getUri());
                    else if(o1.getRelevance() > o2.getRelevance())
                        return 1;
                    else return -1;
                })
                .collect(Collectors.toList());
        objectSearches.clear();
        objectSearches.addAll(sortedList);
        //Recording on objectSearchRepository final objectSearches
        objectSearchRepository.deleteAll();
        if (!objectSearches.isEmpty()) {
            for (ObjectSearch objectSearchList : objectSearches) {
                if (!objectSearchList.getSnippet().isEmpty()) {
                    ObjectSearch objectSearch = new ObjectSearch();
                    objectSearch.setUri(objectSearchList.getUri());
                    objectSearch.setTitle(objectSearchList.getTitle());
                    objectSearch.setSnippet(objectSearchList.getSnippet());
                    objectSearch.setRelevance(objectSearchList.getRelevance());
                    objectSearchRepository.save(objectSearch);
                }
            }
        }
        else {
            ObjectSearch objectSearchFalse=new ObjectSearch();
            objectSearchFalse.setUri(site);
            objectSearchFalse.setTitle(name);
            objectSearchFalse.setSnippet("'result': false,\n" +
                    "\t        'error':404 Not Found");
            objectSearchFalse.setRelevance(0);
            objectSearchRepository.save(objectSearchFalse);}
        return "'result': true";
    }
    public String htmlCleaner(String html) {
        return Jsoup.parse(html).text();
    }
}
