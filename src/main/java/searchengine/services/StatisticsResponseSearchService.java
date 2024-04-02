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
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsResponseSearchService implements StatisticsServiceSearch {
    @Autowired
    private ObjectSearchRepository objectSearchRepository;
    @Autowired
    private SiteModelRepository siteModelRepository;
    static int limit;
    static int offset;
    @Override

    public StatisticsResponseFromSearchingDto getStatisticsSearch() {

        StatisticsResponseFromSearchingDto statisticsResponseFromSearchingDto=new StatisticsResponseFromSearchingDto();
        Iterable<SiteModel> siteModels = siteModelRepository.findAll();
        Iterable<ObjectSearch> objectSearchesRep = objectSearchRepository.findAll();
        List<TotalSearchingDto> totalSearchingDtos=new ArrayList<>();

        List<ObjectSearch>objectSearches=new ArrayList<>();
        objectSearches.addAll((Collection<? extends ObjectSearch>) objectSearchesRep);
        for(SiteModel siteModel:siteModels) {
            offset=Searching.offsetIn;
            limit=Searching.limitIn;
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
            for (int j=offset;j<objectSearches.size();j++) {
                if (limitToString<=limit) {
  //                  for (SiteModel siteModel:siteModels) {
  //                      for (ObjectSearch objectSearch : objectSearchesRep) {
                            TotalSearchingDto totalSearchingDto=new TotalSearchingDto();
                            totalSearchingDto.setSites(siteModel.getUrl());
                            totalSearchingDto.setSiteName(siteModel.getName());
                            totalSearchingDto.setUri(objectSearches.get(j).getUri());
                            totalSearchingDto.setTitle(objectSearches.get(j).getTitle());
                            totalSearchingDto.setSnippet(objectSearches.get(j).getSnippet());
                            totalSearchingDto.setRelevance(objectSearches.get(j).getRelevance());
                            totalSearchingDtos.add(totalSearchingDto);
 //                       }
 //                   }
                }
                limitToString=limitToString+1;
            }
        }
        statisticsResponseFromSearchingDto.setResult(true);
        statisticsResponseFromSearchingDto.setCount(totalSearchingDtos.size());
        statisticsResponseFromSearchingDto.setData(totalSearchingDtos);


//        for (SiteModel siteModel:siteModels) {
//            for (ObjectSearch objectSearch : objectSearchesRep) {
//                TotalSearchingDto totalSearchingDto=new TotalSearchingDto();
//                totalSearchingDto.setSites(siteModel.getUrl());
//                totalSearchingDto.setSiteName(siteModel.getName());
//                totalSearchingDto.setUri(objectSearch.getUri());
//                totalSearchingDto.setTitle(objectSearch.getTitle());
//                totalSearchingDto.setSnippet(objectSearch.getSnippet());
//                totalSearchingDto.setRelevance(objectSearch.getRelevance());
//                totalSearchingDtos.add(totalSearchingDto);
//            }
//        }
//            statisticsResponseFromSearchingDto.setResult(true);
//            statisticsResponseFromSearchingDto.setCount(totalSearchingDtos.size());
//            statisticsResponseFromSearchingDto.setData(totalSearchingDtos);

        return statisticsResponseFromSearchingDto;
    }
}
