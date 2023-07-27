package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.parsers.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteModelRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

    private final SitesList sites;
    private final LemmaFinder lemmaFinder;

    public String url;
    public String comment;
    static public Map listSideMap=new HashMap<>();

    public Site site;
    public Indexing(SitesList sites, LemmaFinder lemmaFinder) {
        this.sites = sites;
        this.lemmaFinder = lemmaFinder;
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
            System.out.println("Карта Сайта -" + siteMap + "Размер списка siteMap... " + siteMap.length());
            Page page = new Page();
            SiteModel siteModel = new SiteModel();
            listSideMap.clear();
            getFinalSiteMap(siteMap);
            if (ConditionStopIndexing.isAfterStop() == true) {
                comment="Индексация остановлена пользователем";
                saveSiteModelRepository(url,comment,siteModel,StatusType.FAILED);
            } else if (listSideMap.size() <= 1) {
                comment="Отказано в индексации";
                saveSiteModelRepository(url,comment,siteModel,StatusType.FAILED);
            }
            else {
                comment="no false";
                saveSiteModelRepository(url,comment,siteModel,StatusType.INDEXING);
                page.setId(siteModel.getId());
                if (listSideMap.size() != LinkExecutor.outHTML.size() && listSideMap.size() < 1) {
                    siteModel.setStatus(StatusType.FAILED);
                    siteModel.setLastError("Ошибка  индексации");
                    siteModelRepository.save(siteModel);
                    comment="Парсинг HTML некорректный";
                    savePageRepository(listSideMap,siteModel,500,page,comment);
                }
                else {
                    for (int j = 0; j < listSideMap.size(); j++) {
                        page.setSiteId(siteModel.getId());
                        page.setCode(200);
                        page.setPath((String) listSideMap.get(j));
                        page.setContent((String) LinkExecutor.outHTML.get(j));
                        pageRepository.save(page);
                    }
                }
            }
            ControllerThread.setIsRun(false);
        }
        return "'result': true\n"+"Пройдено сайтов- "+sites.getSites().size();
    }
    public String indexingPage(String url){
        siteModelRepository.deleteAll();
        pageRepository.deleteAll();
            LinkExecutor.outHTML.clear();
            int numThreads = 5;
            LinkExecutor linkExecutor = new LinkExecutor(url, url);
            String siteMap = numThreads == 0 ? new ForkJoinPool().invoke(linkExecutor) : new ForkJoinPool(numThreads).invoke(linkExecutor);
            System.out.println("Карта Сайта -" + siteMap + "Размер списка siteMap... " + siteMap.length());
            Page page = new Page();
            SiteModel siteModel = new SiteModel();
            listSideMap.clear();
            getFinalSiteMap(siteMap);
            comment="no false";
            String name = url.substring(12);
        siteModel.setUrl(url);
        siteModel.setName(name);
        siteModel.setStatus(StatusType.INDEXED);
        siteModel.setLastError(comment);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModelRepository.save(siteModel);
                page.setId(siteModel.getId());
                if (listSideMap.size() != LinkExecutor.outHTML.size() && listSideMap.size() < 1) {
                    siteModel.setStatus(StatusType.FAILED);
                    siteModel.setLastError("Ошибка  индексации");
                    siteModelRepository.save(siteModel);
                    comment="Парсинг HTML некорректный";
                    savePageRepository(listSideMap,siteModel,500,page,comment);
                }
                else {
                    for (int j = 0; j < listSideMap.size(); j++) {
                        page.setSiteId(siteModel.getId());
                        page.setCode(200);
                        page.setPath((String) listSideMap.get(j));
                        page.setContent((String) LinkExecutor.outHTML.get(j));
                        pageRepository.save(page);
                    }
                }
                lemmaRepository.deleteAll();
                Lemma lemma=new Lemma();
                int frequency=0;
                for (int y=0;y< pageRepository.count();y++){
                    lemmaFinder.collectLemmas(lemmaFinder.htmlCleaner(page.getContent()));

                    lemma.setLemma(lemmaFinder.lemmas.toString());
                    lemma.setFrequency(frequency+1);
                    lemmaRepository.save(lemma);
                }
        return "'result': true";
    }
    public static String getFinalSiteMap(String text) {
        String encoding;
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
}
