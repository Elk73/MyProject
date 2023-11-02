package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.parsers.*;
import searchengine.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Indexing {
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

    public String url;
    public String comment;
    static public Map listSideMap=new HashMap<>();
    public static int frequency=0;
    public Site site;
    public static Map<String,Integer> removeKeys=new HashMap<>();

    public Indexing(SitesList sites, LemmaFinder lemmaFinder,CustomComparator customComparator) {
        this.sites = sites;
        this.lemmaFinder = lemmaFinder;
        this.customComparator=customComparator;
    }
    public  String startIndexing(){
        ConditionStopIndexing.setIsStop(false);
        if (ControllerThread.isIsRun()==true) {
            return "'result': false,\n" +
                    "'error': \"Индексация уже запущена\"";
        }
        siteModelRepository.deleteAll();
        pageRepository.deleteAll();
        for (int i = 0; i < sites.getSites().size(); i++) {
            site = sites.getSites().get(i);
            url = site.getUrl();
            LinkExecutor.outHTML.clear();
            int numThreads = 5;
            LinkExecutor linkExecutor = new LinkExecutor(url, url);
            String siteMap = numThreads == 0 ? new ForkJoinPool().invoke(linkExecutor) : new ForkJoinPool(numThreads).invoke(linkExecutor);
//            System.out.println("Карта Сайта -" + siteMap + "Размер списка siteMap... " + siteMap.length());
            SiteModel siteModel = new SiteModel();
            listSideMap.clear();
            getFinalSiteMap(siteMap);
            if (ConditionStopIndexing.isAfterStop() == true) {
                comment="Индексация остановлена пользователем";
                saveSiteModelRepository(url,comment,siteModel,StatusType.FAILED);
            } else if (listSideMap.size() <= 1) {
                comment="503 Service Unavailable";
                saveSiteModelRepository(url,comment,siteModel,StatusType.FAILED);
            }
            else {
                comment="200 Ok";
                saveSiteModelRepository(url,comment,siteModel,StatusType.INDEXING);
                lemmaRepository.deleteAll();
                indexRepository.deleteAll();
                for (int j = 0; j < listSideMap.size(); j++) {
                    Page page = new Page();
                    if (listSideMap.size() != LinkExecutor.outHTML.size() && listSideMap.size() < 1) {
                        siteModel.setStatus(StatusType.FAILED);
                        siteModel.setLastError("506 Variant Also Negotiates");
                        siteModelRepository.save(siteModel);
                        comment="Парсинг HTML некорректный по причине конфигурации сервера";
                        savePageRepository(listSideMap,siteModel,500,page,comment);
                    }
                    else {
                        page.setSiteId(siteModel.getId());
                        page.setCode(200);
                        page.setPath((String) listSideMap.get(j));
                        page.setContent((String) LinkExecutor.outHTML.get(j));
                        pageRepository.save(page);
                        lemmaFinder.collectLemmas(lemmaFinder.htmlCleaner(page.getContent()));
                        for (String key : lemmaFinder.lemmas.keySet()) {
                            Lemma lemma=new Lemma();
                            Index index=new Index();
                            lemma.setLemma(String.valueOf(key));
                            lemma.setFrequency(frequency +lemmaFinder.lemmas.get(key) );
                            lemma.setSiteId(page.getSiteId());
                            lemmaRepository.save(lemma);
                            index.setPageId(page.getId());
                            index.setLemmaId(lemma.getId());
                            index.setRank(lemma.getFrequency());
                            indexRepository.save(index);

                        }
                    }
                }
            }
            ControllerThread.setIsRun(false);
        }
        return "'result': true\n"+"Count- "+sites.getSites().size();
    }
    public String indexingPage(String url){
            siteModelRepository.deleteAll();
            pageRepository.deleteAll();
            LinkExecutor.outHTML.clear();
            int numThreads = 5;
            LinkExecutor linkExecutor = new LinkExecutor(url, url);
            String siteMap = numThreads == 0 ? new ForkJoinPool().invoke(linkExecutor) : new ForkJoinPool(numThreads).invoke(linkExecutor);
            System.out.println("Карта Сайта -" + siteMap + "Размер списка siteMap... " + siteMap.length());
            SiteModel siteModel = new SiteModel();
            listSideMap.clear();
            getFinalSiteMap(siteMap);
            comment = "no false";
            String name = url.substring(12);
            siteModel.setUrl(url);
            siteModel.setName(name);
            siteModel.setStatus(StatusType.INDEXED);
            siteModel.setLastError(comment);
            siteModel.setStatusTime(LocalDateTime.now());
            siteModelRepository.save(siteModel);
            lemmaRepository.deleteAll();
            indexRepository.deleteAll();
            for (int j = 0; j < listSideMap.size(); j++) {
                Page page = new Page();
                if (listSideMap.size() != LinkExecutor.outHTML.size() && listSideMap.size() < 1) {
                    siteModel.setStatus(StatusType.FAILED);
                    siteModel.setLastError("506 Variant Also Negotiates");
                    siteModelRepository.save(siteModel);
                    comment = "Парсинг HTML некорректный по причине конфигурации сервера";
                    savePageRepository(listSideMap, siteModel, 500, page, comment);
                } else {
                    page.setSiteId(siteModel.getId());
                    page.setCode(200);
                    page.setPath((String) listSideMap.get(j));
                    page.setContent((String) LinkExecutor.outHTML.get(j));
                    pageRepository.save(page);
                    lemmaFinder.collectLemmas(lemmaFinder.htmlCleaner(page.getContent()));
                    for (String key : lemmaFinder.lemmas.keySet()) {
                        Lemma lemma = new Lemma();
                        Index index = new Index();
                        lemma.setLemma(String.valueOf(key));
                        lemma.setFrequency(frequency + lemmaFinder.lemmas.get(key));
                        lemma.setSiteId(page.getSiteId());
                        lemmaRepository.save(lemma);
                        index.setPageId(page.getId());
                        index.setLemmaId(lemma.getId());
                        index.setRank(lemma.getFrequency());
                        indexRepository.save(index);

                    }
                }
            }
            return "'result': true";
    }
//    public String getSearchSiteMap(String query) throws IOException {
//        ArrayList<String> result = new ArrayList<>();
//        result.add( "\n 'count': " +sites.getSites().size());
//        for (int i = 0; i < sites.getSites().size(); i++) {
//            site = sites.getSites().get(i);
//            url = site.getUrl();
//            getSearch(query,url);
//            result.add(toString(0,20,i+1));
//        }
//        return result.toString();
//
//    }
//    public String getSearch(String query,String site) throws IOException {
//        siteModelRepository.deleteAll();
//        pageRepository.deleteAll();
//        objectSearchRepository.deleteAll();
//        LinkExecutor.outHTML.clear();
//        int numThreads = 5;
//        LinkExecutor linkExecutor = new LinkExecutor(site, site);
//        String siteMap = numThreads == 0 ? new ForkJoinPool().invoke(linkExecutor) : new ForkJoinPool(numThreads).invoke(linkExecutor);
////        System.out.println("Карта Сайта -" + siteMap + "Размер списка siteMap... " + siteMap.length());
//        SiteModel siteModel = new SiteModel();
//        listSideMap.clear();
//        getFinalSiteMap(siteMap);
//        comment = "200 Ok";
//        String name = site.substring(12);
//        siteModel.setUrl(site);
//        siteModel.setName(name);
//        siteModel.setStatus(StatusType.INDEXED);
//        siteModel.setLastError(comment);
//        siteModel.setStatusTime(LocalDateTime.now());
//        siteModelRepository.save(siteModel);
//        lemmaRepository.deleteAll();
//        indexRepository.deleteAll();
//        Map<String, Integer> lemmasSearch=new HashMap<>();
//        lemmasSearch.putAll(lemmaFinder.collectLemmas(query));
//        lemmaFinder.lemmas.clear();
//        for (int j = 0; j < listSideMap.size(); j++) {
//            Page page = new Page();
//            if (listSideMap.size() != LinkExecutor.outHTML.size() && listSideMap.size() < 1) {
//                siteModel.setStatus(StatusType.FAILED);
//                siteModel.setLastError("506 Variant Also Negotiates");
//                siteModelRepository.save(siteModel);
//                comment = "Парсинг HTML некорректный по причине конфигурации сервера";
//                savePageRepository(listSideMap, siteModel, 500, page, comment);
//            } else {
//                page.setSiteId(siteModel.getId());
//                page.setCode(200);
//                page.setPath((String) listSideMap.get(j));
//                page.setContent((String) LinkExecutor.outHTML.get(j));
//                pageRepository.save(page);
//                TreeMap<String, Integer> unsortedMap=new TreeMap<>();
//                unsortedMap.putAll(lemmaFinder.collectLemmas(lemmaFinder.htmlCleaner(page.getContent())));
//                Map<String, Integer> sortedMap = customComparator.valueSort(unsortedMap);
////                System.out.println("sortedMap : "+sortedMap);
//                LinkedHashMap<String, Integer> collectLemmas=new LinkedHashMap<>();
//                collectLemmas.putAll(sortedMap);
////                System.out.println("collectLemmas : "+collectLemmas);
////                System.out.println("sortedMap.keySet() & sortedMap.values() : "+sortedMap.keySet() +"    -"+sortedMap.values());
////                for ( String key : sortedMap.keySet()) {
////                    System.out.println("sortedMap-key : "+key);
////                }
//                for (String key : collectLemmas.keySet()) {
//                    if (collectLemmas.get(key)>8) {
//                        removeKeys.put(key,collectLemmas.get(key));
////                        System.out.println("key: "+key);
////                        System.out.println("collectLemmas.get(key): "+collectLemmas.get(key));
//                    }
//                }
////                System.out.println("removeKeys: "+removeKeys);
//                for (String removeKey : removeKeys.keySet()) {
//                    collectLemmas.remove(removeKey);
//                }
////                System.out.println("collectLemmas after remove: "+collectLemmas);
////                System.out.println("lemmaFinder.lemmas : "+lemmaFinder.lemmas);
////                System.out.println("lemmasSearch : "+lemmasSearch);
//
//                //НАЧАЛО******** Этап №4
//                for (String key : collectLemmas.keySet()) {
//                    Lemma lemma = new Lemma();
//                    Index index = new Index();
//                    for (String keyLemmasSearch : lemmasSearch.keySet()) {
//                    if (key.equals(keyLemmasSearch)) {
////                        System.out.println("LemmasSearch.get(keyLemmasSearch) : "+lemmasSearch.get(keyLemmasSearch));
////                        System.out.println("KeyLemmasSearch : "+keyLemmasSearch);
////                        System.out.println("Key : "+key);
////                        System.out.println("LemmaFinder.lemmas.get(key) : "+lemmaFinder.lemmas.get(key));
//                        lemma.setLemma(keyLemmasSearch);
//                        lemma.setFrequency(lemmaFinder.lemmas.get(key).intValue());
//                        lemma.setSiteId(page.getSiteId());
//                        lemmaRepository.save(lemma);
//                        index.setPageId(page.getId());
//                        index.setLemmaId(lemma.getId());
//                        index.setRank(lemma.getFrequency());
//                        indexRepository.save(index);
//                        }
//                    }
//                }
//                for (String key : lemmasSearch.keySet()) {
//                    List<Lemma> byKeysFromLemma = lemmaRepository.findByLemma(key);
//                    //удаление Lemma где Frequency постоянна(lemma там не встречается)
//                    int frequencyBefore=0;
//                    for (Lemma byKeyFromLemma :byKeysFromLemma) {
//                        if (byKeyFromLemma.getFrequency()==frequencyBefore){
//                            lemmaRepository.deleteById(byKeyFromLemma.getId());
//                            continue;
//                        }
//                        //приведение Index в соответствие с Lemma
//                        Iterable<Index> unRemovedLemmas = indexRepository.findAll();
//                        for (Index unRemovedLemma :unRemovedLemmas) {
//                            if (!lemmaRepository.existsById(unRemovedLemma.getLemmaId())){
// //                               System.out.println("* unRemovedLemmaId *"+unRemovedLemma.getId());
//                                indexRepository.delete(unRemovedLemma);
//                            }
//                        }
//                        frequencyBefore++;
// //                       System.out.println("lemma :"+byKeyFromLemma.getLemma()+"\n Id :"+byKeyFromLemma.getId()+"\n frequency :"+byKeyFromLemma.getFrequency());
//                    }
//                }
//            }
//
//        }
//        //****создание и заполнение objectSearch
//        Iterable<Index> byLemmaGetId = indexRepository.findAll();
//        Iterable<Page> pagesFromRep=pageRepository.findAll();
//        List<Page> pages = new ArrayList<>();
//        for(Page pageFromRep:pagesFromRep) {
//            pages.add( pageFromRep);
//        }
//        //Запрос Query разделяем на слова и записываем в список
//        LinkedHashMap<Integer, String> substringIndices =new LinkedHashMap<>();
//        LinkedList<String> substrings = new LinkedList<>();
//        String[] words = query.split("\\s+");
//        substrings.addAll(List.of(words));
//
//        // Перебираем Index и заполняем objectSearch пока без Snippet
//        for (Index byLemmaId :byLemmaGetId) {
//            ObjectSearch objectSearch=new ObjectSearch();
//            Document doc = null;
//            try {
//            doc = Jsoup.connect(site+pageRepository.findById(byLemmaId.getPageId()).get().getPath()).ignoreContentType(true).get();
//            } catch (NullPointerException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (HttpStatusException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//            String text=htmlCleaner(String.valueOf(doc.body())).toLowerCase();
//            String[] textList = text.split("\\s+");
//            LinkedList<String> wordsFromText = new LinkedList<>();
//            wordsFromText.addAll(List.of(textList));
//            LinkedList<String> substringLemmas=new LinkedList<>();
//            lemmaFinder.lemmas.clear();
//            substringLemmas.addAll(lemmaFinder.collectLemmas(Arrays.toString(words)).keySet());
//            for (String substring : substringLemmas) {System.out.println("substringLemmas- "+substring);}
//            for (String substring : substringLemmas) {
//                for (String wordFromText : wordsFromText){
//                    if (lemmaFinder.collectLemmas(wordFromText).containsKey(substring)) {
//                        int index = text.indexOf(wordFromText);
//                        if (index != -1 && index != 0) {
//                            substringIndices.put(index, substring);
//                        }
//                    }
//                    lemmaFinder.lemmas.clear();
//                }
//            }
//                for (Integer index : substringIndices.keySet()) {
//                    System.out.println("index- "+index+"   substring- "+substringIndices.get(index));
//                }
//                //Ищем Index по близости индексов двух лемм
//            LinkedHashMap<Integer, String> lemmaFirst =new LinkedHashMap<>();
//            LinkedHashMap<Integer, String> lemmaSecond =new LinkedHashMap<>();
//            String compareValue="";
//            for (Integer index : substringIndices.keySet()) {
//             compareValue=substringIndices.get(index);
//            }
//            for (Integer index : substringIndices.keySet()) {
//                System.out.println("compareValue- "+compareValue);
//
//                if (substringIndices.get(index).equals(compareValue)){
//                    lemmaFirst.put(index,substringIndices.get(index));
//                    System.out.println(" lemmaFirst- "+index+"   substring- "+ lemmaFirst.get(index));
//                    continue;
//                }
//                lemmaSecond.put(index,substringIndices.get(index));
//                System.out.println("lemmaSecond- "+index+"   substring- "+lemmaSecond.get(index));
//            }
//            if (!lemmaSecond.isEmpty()||!lemmaFirst.isEmpty()) {
//                System.out.println("!lemmaSecond.isEmpty()- "+!lemmaSecond.isEmpty());
//                for (Integer indexLemmaFirst : lemmaFirst.keySet()) {
//                    for (Integer indexLemmaSecond : lemmaSecond.keySet()) {
//                                  if (Math.abs(indexLemmaFirst - indexLemmaSecond) < 15) {
//                                      substringIndices.clear();
//                                      substringIndices.put(indexLemmaFirst, lemmaFirst.get(indexLemmaFirst));
//                                      System.out.println("indexLemmaFirst- " + indexLemmaFirst + "   indexLemmaSecond- " + indexLemmaSecond +"   substring- " + lemmaFirst.get(indexLemmaFirst));
//                                  }
//                    }
//                }
//            }
//
//                //Вырезаем подстроку и записываем в snippet
//                Optional<Integer> max=substringIndices.keySet().stream().max(Comparator.comparing(i->i.intValue()));
//                for (Integer index : substringIndices.keySet()) {
//                    int start = index - 100;
//                    int end = index + 90;
//                    if (start <=0){
//                        start=0;
//                    } else if (end>max.get()) {
//                        end=end+1;
//                    }
//                    String cutText = text.substring(start, end);
//                    String[] cutTextMassive=cutText.split("\\s+");
//                    String snippetText="";
//                    for (int i=1;i<cutTextMassive.length-1;i++){
//                        snippetText=snippetText+" "+cutTextMassive[i];
//                    }
//                    System.out.println("cutText - " + snippetText + "\n text -" + text + "\n pageRep -"
//                            + pageRepository.findById(byLemmaId.getPageId()).get().getPath());
//                    objectSearch.setSnippet(snippetText);
//
//                    break;
//                }
//                objectSearch.setUri(pageRepository.findById(byLemmaId.getPageId()).get().getPath());
//                objectSearch.setTitle(htmlCleaner(pageRepository.findById(byLemmaId.getPageId()).get().getContent()));
//                objectSearch.setRelevance(byLemmaId.getRank());
//                objectSearchRepository.save(objectSearch);
//        }
//        //****создание копии списка objectSearchRepository
//        Iterable<ObjectSearch> objectSearchesRep = objectSearchRepository.findAll();
//        List<ObjectSearch> objectSearches = new ArrayList<>();
//        for(ObjectSearch objectSearch:objectSearchesRep) {
//            objectSearches.add(objectSearch);
//        }
//        //****поиск одинаковых страниц для суммирования Relevance и его запись в список
//        double relevance=0;
//            for (int i=0;i<objectSearches.size();i++) {
//                Object objectOne = objectSearches.get(i).getUri();
//                relevance +=objectSearches.get(i).getRelevance();
//                for (int j = 0; j < objectSearches.size(); j++) {
//                    if(!(i==j)) {
//                        Object objectTwo = objectSearches.get(j).getUri();
//                        if ( objectOne.equals(objectTwo)) {
//                            relevance += objectSearches.get(j).getRelevance();
//                            objectSearches.get(j).setUri("");
//                        }
//                    }
//            }
//                if (relevance !=0){
//                    objectSearches.get(i).setRelevance(relevance);
////                    System.out.println(" objectSearches.get(i).getId() : " + objectSearches.get(i).getId());
//                    System.out.println(" relevance : " + relevance);}
//                relevance = 0;
//        }
//            //****удаление страниц с одним и тем же адресом , после суммирования Relevance, сиквестирование списка
//        for (int y=0;y<objectSearches.size();y++) {
//            if (objectSearches.get(y).getUri().equals("")){
//                objectSearches.remove(y);
//            }
//        }
//        //****поиск МАХ среди абсолютных Relevance, расчет и запись относительной Relevance
//        Optional<ObjectSearch> max=objectSearches.stream().max(Comparator.comparing(o -> o.getRelevance()));
//        if (max.isPresent()) {
//            double maxRelevance = max.get().getRelevance();
//            for (ObjectSearch objectSearch : objectSearches) {
//                objectSearch.setRelevance(objectSearch.getRelevance() / maxRelevance);
//            }
//        }
//            //****сортировка списка по возрастанию Relevance
//        List<ObjectSearch> sortedList = objectSearches.stream()
//                .sorted((o1, o2) -> {
//                    if(o1.getRelevance() == o2.getRelevance())
//                        return o1.getUri().compareTo(o2.getUri());
//                    else if(o1.getRelevance() > o2.getRelevance())
//                        return 1;
//                    else return -1;
//                })
//                .collect(Collectors.toList());
//            objectSearches.clear();
//            objectSearches.addAll(sortedList);
//        for (ObjectSearch objectSearch:objectSearches){
//        System.out.println("sortedList.getRelevance(): " + objectSearch.getRelevance());
//        }
//        //Запись в objectSearchRepository итогового objectSearches
//        objectSearchRepository.deleteAll();
//        if (!objectSearches.isEmpty()) {
//            for (ObjectSearch objectSearchList : objectSearches) {
//                ObjectSearch objectSearch = new ObjectSearch();
//                objectSearch.setUri(objectSearchList.getUri());
//                objectSearch.setTitle(objectSearchList.getTitle());
//                objectSearch.setSnippet(objectSearchList.getSnippet());
//                objectSearch.setRelevance(objectSearchList.getRelevance());
//                objectSearchRepository.save(objectSearch);
//            }
//        }
//        else {
//               ObjectSearch objectSearchFalse=new ObjectSearch();
//               objectSearchFalse.setUri(site);
//               objectSearchFalse.setTitle(name);
//               objectSearchFalse.setSnippet("'result': false,\n" +
//                       "\t        'error':404 Not Found");
//               objectSearchFalse.setRelevance(0);
//               objectSearchRepository.save(objectSearchFalse);}
//        return "'result': true";
//    }
    public static String getFinalSiteMap(String text) {
        String encoding;
 //       String regex = "\\/[a-zA-Z_]*[_a-z]*\\/[0-9]+.html\\/?[A-Za-z-0-9/]*|\\/[a-z_]*[_a-z]*.html|\\/\\n|\\/[a-zа-я-?=]+\\/[A-Za-zа-я0-9-_.]*\\/?[A-Za-z-]*\\/?[0-9.a-z?]*|\\/[a-zA-Z-?=]+\\n";
        String regex = "\\/[a-zA-Z_]*[_a-z]*\\/[0-9]+.html\\/?[A-Za-z-0-9/]*|\\/[a-z_]*[_a-z]*.html|\\/\\n|\\/[a-zа-я-?=]+\\/[A-Za-zа-я0-9-_.]*\\/?[A-Za-z-]*\\/?[0-9%.a-z?]*|\\/[a-zA-Z-?=]+\\n";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        int i=0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            encoding = text.substring(start, end);
            if (!listSideMap.containsValue(encoding))
            {
                listSideMap.put(i,encoding+"\n");
            }
            i++;
        }
        System.out.println("ListSideMap.size.()- "+listSideMap.size());
        System.out.println("getFinalSiteMap - "+listSideMap);
        return String.valueOf(listSideMap);
    }
    public void saveSiteModelRepository(String url,String comment,SiteModel siteModel,StatusType statusType){
        siteModel.setUrl(url);
        siteModel.setName(site.getName());
        siteModel.setStatus(statusType);
        siteModel.setLastError(comment);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModelRepository.save(siteModel);
    }
    public void savePageRepository(Map listSideMap, SiteModel siteModel, int code, Page page, String comment){
        for (int j = 0; j < listSideMap.size(); j++) {
            page.setSiteId(siteModel.getId());
            page.setCode(code);
            page.setPath((String) listSideMap.get(j));
            page.setContent(comment);
            pageRepository.save(page);
        }
    }
    public static boolean isValidURL(String url) {
        final String URL_REGEX =
                "^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))" +
                        "(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)" +
                        "([).!';/?:,][[:blank:]])?$";
         final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
            if (url == null) {
                return false;
            }
            Matcher matcher = URL_PATTERN.matcher(url);
            return matcher.matches();
        }
//    public static String URLReader(URL url) throws IOException {
//        StringBuilder sb = new StringBuilder();
//        String line;
//        InputStream in = url.openStream();
//        try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//            while ((line = reader.readLine()) != null) {
//                sb.append(line).append(System.lineSeparator());
//            }
//        } finally {
//            in.close();
//        }
//        return sb.toString();
//    }
//    public String htmlCleaner(String html) {
//        return Jsoup.parse(html).text();
//    }

    public String toString(int offset,int limit,int count) {
        ArrayList<String> result = new ArrayList<>();
        Iterable<ObjectSearch> objectSearchesRep = objectSearchRepository.findAll();
        List<ObjectSearch> objectSearches = new ArrayList<>();
        for(ObjectSearch objectSearch:objectSearchesRep) {
            objectSearches.add(objectSearch);
        }
        Iterable<SiteModel> siteModelRep = siteModelRepository.findAll();
        for(SiteModel siteModel:siteModelRep) {
            if (limit==0){
                limit=20;
            }
            if (limit>objectSearches.size()){
                limit=objectSearches.size();
            }
            if (offset>limit){
                offset=0;
            }
            if (offset>0){
                limit=limit-offset;
            }
            int limitToString=1;
            for (int j=offset;j<=objectSearches.size();j++) {
                if (limitToString<=limit) {
                    result.add(
                            "\n'count': " + count + "," +
                                    "\n    'data':[ " +
                                    "\n            {" +
                                    "\n            'site': " + siteModel.getUrl() + "," +
                                    "\n            'siteName': " + siteModel.getName() + "," +
                                    "\n            'uri': " + objectSearches.get(j).getUri() +
                                    "\n            'title' :" + objectSearches.get(j).getTitle() + "," +
                                    "\n            'snippet' :" + objectSearches.get(j).getSnippet() + "," +
                                    "\n            'relevance' :" + objectSearches.get(j).getRelevance() +
                                    "\n            }");

                }
                 limitToString=limitToString+1;
            }
        }
        return "\n'result': true\n" +result;
    }
}
