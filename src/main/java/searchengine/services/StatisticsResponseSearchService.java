package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.ObjectSearch;
import searchengine.model.SiteModel;
import searchengine.repository.ObjectSearchRepository;
import searchengine.repository.SiteModelRepository;
import searchengine.response.searching.StatisticsResponseFromSearchingDto;
import searchengine.response.searching.TotalSearchingDto;
import searchengine.utils.StatisticsServiceSearch;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsResponseSearchService implements StatisticsServiceSearch {
    @Autowired
    private ObjectSearchRepository objectSearchRepository;
    @Autowired
    private SiteModelRepository siteModelRepository;
    @Override
    public StatisticsResponseFromSearchingDto getStatisticsSearch() {
        StatisticsResponseFromSearchingDto statisticsResponseFromSearchingDto=new StatisticsResponseFromSearchingDto();
        Iterable<SiteModel> siteModels = siteModelRepository.findAll();
        Iterable<ObjectSearch> objectSearchesRep = objectSearchRepository.findAll();
        List<TotalSearchingDto> totalSearchingDtos=new ArrayList<>();
        for (SiteModel siteModel:siteModels) {
            for (ObjectSearch objectSearch : objectSearchesRep) {
                TotalSearchingDto totalSearchingDto=new TotalSearchingDto();
                totalSearchingDto.setSites(siteModel.getUrl());
                totalSearchingDto.setSiteName(siteModel.getName());
                totalSearchingDto.setUri(objectSearch.getUri());
                totalSearchingDto.setTitle(objectSearch.getTitle());
                totalSearchingDto.setSnippet(objectSearch.getSnippet());
                totalSearchingDto.setRelevance(objectSearch.getRelevance());
                totalSearchingDtos.add(totalSearchingDto);
            }
        }
            statisticsResponseFromSearchingDto.setResult(true);
            statisticsResponseFromSearchingDto.setCount(totalSearchingDtos.size());
            statisticsResponseFromSearchingDto.setData(totalSearchingDtos);

        return statisticsResponseFromSearchingDto;
    }
}
