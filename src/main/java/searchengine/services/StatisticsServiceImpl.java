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
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteModelRepository;
import searchengine.utils.StatisticsService;

import java.time.LocalDateTime;
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
    private final SitesList sites;


    @Override
    public StatisticsResponse getStatistics() {
        if (sites.getSites().size() == 0) {
            TotalStatistics total = new TotalStatistics();
                StatisticsResponse response = new StatisticsResponse();
                List<DetailedStatisticsItem> detailed = new ArrayList<>();
                StatisticsData data = new StatisticsData();
                total.setSites(0);
                total.setPages(0);
                total.setLemmas(0);
                data.setDetailed(detailed);
                data.setTotal(total);
                response.setStatistics(data);
                System.out.println("DATA 1-" + data);
                response.setResult(true);
                return response;
        }

             else  if (siteModelRepository.count() == 0) {
                 TotalStatistics total = new TotalStatistics();
                total.setSites(sites.getSites().size());
                //           }
                total.setIndexing(true);
                List<DetailedStatisticsItem> detailed = new ArrayList<>();
                StatisticsData data = new StatisticsData();
                List<Site> sitesList = sites.getSites();
                for (int i = 0; i < sitesList.size(); i++) {
                    Site site = sitesList.get(i);
                    DetailedStatisticsItem item = new DetailedStatisticsItem();
                    item.setName(site.getName());
                    item.setUrl(site.getUrl());
                    item.setPages(0);
                    item.setLemmas(0);
                    item.setStatus("");
                    item.setError("");
                    item.setStatusTime(LocalDateTime.now());
                    total.setPages(0);
                    total.setLemmas(0);
                    detailed.add(item);
                    data.setDetailed(detailed);
                    data.setTotal(total);
                }
                StatisticsResponse response = new StatisticsResponse();
                response.setStatistics(data);
                System.out.println("DATA 2 -" + data);
                response.setResult(true);
                return response;
        } else {
            TotalStatistics total = new TotalStatistics();
            total.setSites((int) siteModelRepository.count());
            total.setIndexing(true);
            List<DetailedStatisticsItem> detailed = new ArrayList<>();
            StatisticsData data = new StatisticsData();
            Iterable<SiteModel> siteResult = siteModelRepository.findAll();
            Iterable<Page> pageResult = pageRepository.findAll();
            Iterable<Lemma> lemmaResult = lemmaRepository.findAll();
            for (SiteModel s : siteResult) {
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(s.getName());
                item.setUrl(s.getUrl());
                int pages = 0;
                int lemmas = 0;
                for (Page p : pageResult) {
                    if (p.getSiteId() == s.getId()) {
                        pages++;
                    }
                }
                for (Lemma l : lemmaResult) {
                    if (l.getSiteId() == s.getId()) {
                        lemmas++;
                    }
                }
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(s.getStatus().toString());
                item.setError(s.getLastError());
                item.setStatusTime(s.getStatusTime());

                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
                detailed.add(item);
                data.setDetailed(detailed);
                data.setTotal(total);
            }

            StatisticsResponse response = new StatisticsResponse();
            response.setStatistics(data);
            response.setResult(true);

            return response;
        }
    }
}
