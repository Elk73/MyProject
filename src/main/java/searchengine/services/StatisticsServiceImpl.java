package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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


    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites((int) siteModelRepository.count());
        total.setIndexing(true);

       List<DetailedStatisticsItem> detailed = new ArrayList<>();
        StatisticsData data = new StatisticsData();
            Iterable<SiteModel> siteResult=siteModelRepository.findAll();
            Iterable<Page> pageResult=pageRepository.findAll();
            Iterable<Lemma> lemmaResult=lemmaRepository.findAll();
            for(SiteModel s:siteResult) {
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(s.getName());
                item.setUrl(s.getUrl());
                int pages=0;
                int lemmas =0;
                for(Page p:pageResult) {
                    if (p.getSiteId()==s.getId()){
                     pages++;
                    }
                }
                for(Lemma l:lemmaResult) {
                    if (l.getSiteId()==s.getId()){
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
