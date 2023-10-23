package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.repository.LemmaRepository;

import searchengine.repository.PageRepository;
import searchengine.repository.SiteModelRepository;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private SiteModelRepository siteModelRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;

 //   private final Random random = new Random();
    private final SitesList sites;
    private final Indexing indexing;

    @Override
    public StatisticsResponse getStatistics() {

//        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
//        String[] errors = {"Ошибка индексации: главная страница сайта не доступна","Ошибка индексации: сайт не доступен",""};

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            indexing.startIndexing();
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
 //           int pages = random.nextInt(1_000);
            int pages = (int) pageRepository.count();
 //           int lemmas = pages * random.nextInt(1_000);
            int lemmas = (int) lemmaRepository.count();
            item.setPages(pages);
            item.setLemmas(lemmas);
//            item.setStatus(statuses[i % 3]);
            Iterable<SiteModel> siteResult=siteModelRepository.findAll();
            for(SiteModel s:siteResult) {
                item.setStatus(s.getStatus().toString());
//                item.setError(errors[i % 3]);
                item.setError(s.getLastError());
//                item.setStatusTime(System.currentTimeMillis() - (random.nextInt(10_000)));
                item.setStatusTime(s.getStatusTime());
            }
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
