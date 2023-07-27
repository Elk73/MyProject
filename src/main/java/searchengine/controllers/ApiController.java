package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.parsers.ConditionStopIndexing;
import searchengine.parsers.ControllerThread;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteModelRepository;
import searchengine.services.Indexing;
import searchengine.services.StatisticsService;

import java.util.*;
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
    private final Indexing indexing;

    public Site site;
    public ApiController(StatisticsService statisticsService, SitesList sites, Indexing indexing) {
        this.statisticsService = statisticsService;
        this.sites = sites;
        this.indexing = indexing;
    }
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public String startIndexing(){
        indexing.startIndexing();
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
    @PostMapping("/indexPage")
    public String addUrl(String url){
       indexing.indexingPage(url);
       return "'result': true";
    }
}
