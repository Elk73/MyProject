package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import searchengine.repository.ObjectSearchRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteModelRepository;
import searchengine.services.Indexing;
import searchengine.services.Searching;
import searchengine.utils.Response;
import searchengine.utils.StatisticsService;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private SiteModelRepository siteModelRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private ObjectSearchRepository objectSearchRepository;
    private final StatisticsService statisticsService;
    private final SitesList sites;
    public String url;
    private final Indexing indexing;
    private final Searching searching;
    public Site site;
    public ApiController(StatisticsService statisticsService, SitesList sites, Indexing indexing,Searching searching) {
        this.statisticsService = statisticsService;
        this.sites = sites;
        this.indexing = indexing;
        this.searching = searching;
    }
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing(){
        return ResponseEntity.ok(indexing.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public String stopIndexing(){
        if ( ControllerThread.isIsRun()==false) {
            return "'result': false,\n" + "\t'error': 400 Bad Request \nИндексация не была запущена\n" ;
        }
        ConditionStopIndexing.setIsStop(true);
        return "'result': true";
    }
    @PostMapping("/indexPage")
    public String addUrl(String url){
        if (Indexing.isValidURL(url)==false) {
            return  "'result': false 'error':417 Expectation Failed \nДанная страница находится за пределами сайтов, \n" +
                    "            указанных в конфигурационном файле" ;
        }
       indexing.indexingPage(url);
       return "'result': true";
    }
    @GetMapping("/search")
    public String search(String query,String site,int offset,int limit) throws IOException {
        if ( query==null) {
            return "'result': false,\n" + "400 Bad Request \nЗадан пустой поисковый запрос";
        }
        if (site==null){
            return searching.getSearchSiteMap(query);
        }
        searching.getSearch(query,site);
         return indexing.toString(offset,limit,1);
    }

}
