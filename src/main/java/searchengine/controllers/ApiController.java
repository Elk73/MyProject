package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.parsers.ConditionStopIndexing;
import searchengine.parsers.ControllerThread;
import searchengine.repository.ObjectSearchRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteModelRepository;
import searchengine.response.searching.StatisticsResponseFromSearchingDto;
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
    public String url;
    private final Indexing indexing;
    private final Searching searching;

    public ApiController(StatisticsService statisticsService, Indexing indexing, Searching searching) {
        this.statisticsService = statisticsService;
        this.indexing = indexing;
        this.searching = searching;
    }
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing(){
        if(ConditionStopIndexing.isAfterStop()){
            ControllerThread.setIsRun(false);
        }
        return ResponseEntity.ok(indexing.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public String stopIndexing(){
        try {
            ConditionStopIndexing.setAfterStop(true);
            Thread.sleep(600);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ConditionStopIndexing.setIsStop(true);
            if (!ControllerThread.isIsRun()) {
            return "'result': false,\t'error': 400 Bad Request \nИндексация не была запущена\n" ;
        }
        return "'result': true";


    }
    @PostMapping("/indexPage")
    public String addUrl(String url){
        if (!Indexing.isValidURL(url)) {
            return  "'result': false 'error':417 Expectation Failed \nДанная страница находится за пределами сайтов, \n указанных в конфигурационном файле" ;
        }
       indexing.indexingPage(url);
       return "'result': true";
    }
    @GetMapping("/search")
    public ResponseEntity<StatisticsResponseFromSearchingDto> search(String query, String site, int offset, int limit) throws IOException {
        if ( query==null) {
            return new ResponseEntity("'result': false,\n Задан пустой поисковый запрос", HttpStatus.BAD_REQUEST);
        }else
        if (site==null){
            return ResponseEntity.ok(searching.getSearchSiteMap(query,offset,limit));
        } else
         return ResponseEntity.ok(searching.getSearch(query,site,offset,limit));
    }
}
