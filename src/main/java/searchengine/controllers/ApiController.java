package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.Application;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.*;
import searchengine.services.StatisticsService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private SiteModelRepository siteModelRepository;
    @Autowired
    private PageRepository pageRepository;
    private final StatisticsService statisticsService;
    private final SitesList sites;
    public String url;
    static public Map listSideMap=new HashMap<>();

    public Site site;
    public ApiController(StatisticsService statisticsService, SitesList sites) {
        this.statisticsService = statisticsService;
        this.sites = sites;

    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public String startIndexing(){
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
                siteModel.setUrl(url);
                siteModel.setName(site.getName());
                siteModel.setStatus(StatusType.FAILED);
                siteModel.setLastError("Индексация остановлена пользователем");
                siteModel.setStatusTime(LocalDateTime.now());
                siteModelRepository.save(siteModel);

            } else if (listSideMap.size() <= 1) {
                siteModel.setUrl(url);
                siteModel.setName(site.getName());
                siteModel.setStatus(StatusType.FAILED);
                siteModel.setLastError("Отказано в индексации");
                siteModel.setStatusTime(LocalDateTime.now());
                siteModelRepository.save(siteModel);
            }
            else {
                siteModel.setUrl(url);
                siteModel.setName(site.getName());
                siteModel.setStatus(StatusType.INDEXING);
                siteModel.setLastError("no false");
                siteModel.setStatusTime(LocalDateTime.now());
                siteModelRepository.save(siteModel);
                page.setId(siteModel.getId());
                if (listSideMap.size() != LinkExecutor.outHTML.size() && listSideMap.size() < 1) {
                    siteModel.setStatus(StatusType.FAILED);
                    siteModel.setLastError("Ошибка  индексации");
                    siteModelRepository.save(siteModel);
                    for (int j = 0; j < listSideMap.size(); j++) {
                        page.setSiteId(siteModel.getId());
                        page.setCode(500);
                        page.setPath((String) listSideMap.get(j));
                        page.setContent("Парсинг HTML некорректный");
                        pageRepository.save(page);
                    }
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
    @GetMapping("/stopIndexing")
    public String stopIndexing(){
        if ( ControllerThread.isIsRun()==false) {
            return "'result': false,\n" + "\t'error': \"Индексация не запущена\"\n" ;
        }
        ConditionStopIndexing.setIsStop(true);
        return "'result': true";
    }
    public static String getFinalSiteMap(String text) {
        String encoding;
//        String regex = "\\/[a-zA-Z_]*[_a-z]*\\/[0-9]+.html|\\/[a-z_]*[_a-z]*.html|\\/\\n|\\/[a-z]+\\/[A-Za-z-]*\\/?[A-Za-z-]*\\/?[0-9%.a-z?]*";
        //       String regex = "\\/[a-zA-Z_]*[_a-z]*\\/[0-9]+.html|\\/[a-z_]*[_a-z]*.html|\\/\\n|\\/[a-zA-Z_]*\\/[0-9]+\\/[0-9]+\\/[a-zA-Z_]*\\/[0-9]+";
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
}
