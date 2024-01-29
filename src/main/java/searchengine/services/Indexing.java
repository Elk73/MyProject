package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.parsers.*;
import searchengine.repository.*;
import searchengine.utils.Response;
import searchengine.utils.supportServises.CustomComparator;
import searchengine.utils.supportServises.LemmaFinder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
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
    private final Response response;



    public String url;
    public String comment;
    static public Map listSideMap=new HashMap<>();
    public static int frequency=0;
    public Site site;

    public Indexing(SitesList sites, LemmaFinder lemmaFinder, CustomComparator customComparator,Response response) {
        this.sites = sites;
        this.lemmaFinder = lemmaFinder;
        this.customComparator=customComparator;
        this.response=response;
    }
    public  Response startIndexing(){
        ControllerThread.setIsRun(true);
        ConditionStopIndexing.setIsStop(false);
        // Run a task specified by a Runnable Object asynchronously.
        CompletableFuture<Response> future = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            System.out.println("I'll run in a separate thread than the main thread.!!!!!!!!!     !!!!!!!!!    !!!!!!!!");
            siteModelRepository.deleteAll();
            pageRepository.deleteAll();
            lemmaRepository.deleteAll();
            indexRepository.deleteAll();
            for (int i = 0; i < sites.getSites().size(); i++) {
                site = sites.getSites().get(i);
                url = site.getUrl();
                LinkExecutor.outHTML.clear();
                int numThreads = 5;
                LinkExecutor linkExecutor = new LinkExecutor(url, url);
                String siteMap = numThreads == 0 ? new ForkJoinPool().invoke(linkExecutor) : new ForkJoinPool(numThreads).invoke(linkExecutor);
                SiteModel siteModel = new SiteModel();
                listSideMap.clear();
                getFinalSiteMap(siteMap);
                if (ConditionStopIndexing.isAfterStop() == true) {
                    comment = "Индексация остановлена пользователем";
                    saveSiteModelRepository(url, comment, siteModel, StatusType.FAILED);
                } else if (listSideMap.size() <= 1) {
                    comment = "503 Service Unavailable";
                    saveSiteModelRepository(url, comment, siteModel, StatusType.FAILED);
                } else {
                    comment = "200 Ok";
                    saveSiteModelRepository(url, comment, siteModel, StatusType.INDEXING);
                }
                for (int j = 0; j < listSideMap.size(); j++) {
                    Page page = new Page();
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
            ControllerThread.setIsRun(false);
            return response;
        });
//        try {
//
//            future.get();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }
//        System.out.println("Enter resultNow()  ?????????????????????????????????????????????????????????????????????????????");
//
//        future.resultNow();
//
////        System.out.println(future.complete("{\n  'result': true\n}"));
//        System.out.println(future.resultNow());
////        System.out.println(future.complete("{\n  'result': true\n}"));
//        System.out.println("I had run over in a separate thread than the main thread.");

//            siteModelRepository.deleteAll();
//            pageRepository.deleteAll();
//            lemmaRepository.deleteAll();
//            indexRepository.deleteAll();
//            for (int i = 0; i < sites.getSites().size(); i++) {
//                site = sites.getSites().get(i);
//                url = site.getUrl();
//                LinkExecutor.outHTML.clear();
//                int numThreads = 5;
//                LinkExecutor linkExecutor = new LinkExecutor(url, url);
//                String siteMap = numThreads == 0 ? new ForkJoinPool().invoke(linkExecutor) : new ForkJoinPool(numThreads).invoke(linkExecutor);
//                SiteModel siteModel = new SiteModel();
//                listSideMap.clear();
//                getFinalSiteMap(siteMap);
//                if (ConditionStopIndexing.isAfterStop() == true) {
//                    comment = "Индексация остановлена пользователем";
//                    saveSiteModelRepository(url, comment, siteModel, StatusType.FAILED);
//                } else if (listSideMap.size() <= 1) {
//                    comment = "503 Service Unavailable";
//                    saveSiteModelRepository(url, comment, siteModel, StatusType.FAILED);
//                } else {
//                    comment = "200 Ok";
//                    saveSiteModelRepository(url, comment, siteModel, StatusType.INDEXING);
//                }
//                for (int j = 0; j < listSideMap.size(); j++) {
//                    Page page = new Page();
//                    page.setSiteId(siteModel.getId());
//                    page.setCode(200);
//                    page.setPath((String) listSideMap.get(j));
//                    page.setContent((String) LinkExecutor.outHTML.get(j));
//                    pageRepository.save(page);
//                    lemmaFinder.collectLemmas(lemmaFinder.htmlCleaner(page.getContent()));
//                    for (String key : lemmaFinder.lemmas.keySet()) {
//                        Lemma lemma = new Lemma();
//                        Index index = new Index();
//                        lemma.setLemma(String.valueOf(key));
//                        lemma.setFrequency(frequency + lemmaFinder.lemmas.get(key));
//                        lemma.setSiteId(page.getSiteId());
//                        lemmaRepository.save(lemma);
//                        index.setPageId(page.getId());
//                        index.setLemmaId(lemma.getId());
//                        index.setRank(lemma.getFrequency());
//                        indexRepository.save(index);
//
//                    }
//                }
//            }
//            ControllerThread.setIsRun(false);
//            return "{\n  'result': true\n}";

        return response;

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
            comment = "200 Ok";
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
            return "'result': true";
    }

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
