package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.ObjectSearch;
import searchengine.model.SiteModelSearch;
import searchengine.repository.ObjectSearchRepository;
import searchengine.repository.SiteModelSearchRepository;
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
    private SiteModelSearchRepository siteModelSearchRepository;
    static int limit;
    static int offset;
    @Override
    public StatisticsResponseFromSearchingDto getStatisticsSearch() {
        StatisticsResponseFromSearchingDto statisticsResponseFromSearchingDto=new StatisticsResponseFromSearchingDto();
        Iterable<SiteModelSearch> siteModels = siteModelSearchRepository.findAll();
        Iterable<ObjectSearch> objectSearchesRep = objectSearchRepository.findAll();
        List<TotalSearchingDto> totalSearchingDtos=new ArrayList<>();
        List<ObjectSearch> objectSearches = new ArrayList<>((Collection<? extends ObjectSearch>) objectSearchesRep);
        offset=Searching.offsetIn;
        if (Searching.limitIn==0||Searching.limitIn==1){
            Searching.limitIn=20;
            try {
            limit= (int) Math.abs(Searching.limitIn/(Searching.copySiteModel.size()));
        }catch(ArithmeticException e) {
            e.printStackTrace();
            return (StatisticsResponseFromSearchingDto) ResponseEntity.noContent();
        }
        }
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
        if(siteModelSearchRepository.count()>1||Searching.copySiteModel.size()>1){
            System.out.println("siteModelRepository.count()>1");
            for(SiteModelSearch siteModel:siteModels) {
                            for (String m:Searching.mapResponse.keySet()) {
                                List<ObjectSearch> listResponse = new ArrayList<>(Searching.mapResponse.get(m));
                                if (m.equals(siteModel.getName())) {
                                    int limitToString=1;
                                        for (int i=offset;i<listResponse.size();i++) {
                                            if (limitToString <= limit ) {
                                                TotalSearchingDto totalSearchingDto = new TotalSearchingDto();
                                                totalSearchingDto.setSites(siteModel.getUrl());
                                                totalSearchingDto.setSiteName(siteModel.getName());
                                                System.out.println("list.getUri() - " + listResponse.get(i).getUri());
                                                totalSearchingDto.setUri(listResponse.get(i).getUri());
                                                totalSearchingDto.setTitle(listResponse.get(i).getTitle());
                                                totalSearchingDto.setSnippet(listResponse.get(i).getSnippet());
                                                totalSearchingDto.setRelevance(listResponse.get(i).getRelevance());
                                                totalSearchingDtos.add(totalSearchingDto);
                                            }
                                            limitToString=limitToString+1;
                                        }
                                }
                            }
            }
        }else {
            System.out.println("siteModelRepository.count()<<<<<1");
            for (SiteModelSearch siteModel : siteModels) {
                int limitToString = 1;
                for (int j = offset; j < objectSearches.size(); j++) {
                    if (limitToString <= limit ) {
                        TotalSearchingDto totalSearchingDto = new TotalSearchingDto();
                        totalSearchingDto.setSites(siteModel.getUrl());
                        totalSearchingDto.setSiteName(siteModel.getName());
                        totalSearchingDto.setUri(objectSearches.get(j).getUri());
                        totalSearchingDto.setTitle(objectSearches.get(j).getTitle());
                        totalSearchingDto.setSnippet(objectSearches.get(j).getSnippet());
                        totalSearchingDto.setRelevance(objectSearches.get(j).getRelevance());
                        totalSearchingDtos.add(totalSearchingDto);
                    }
                    limitToString = limitToString + 1;
                }
            }
        }
        statisticsResponseFromSearchingDto.setResult(true);
        statisticsResponseFromSearchingDto.setCount(objectSearches.size());
        statisticsResponseFromSearchingDto.setData(totalSearchingDtos);
        return statisticsResponseFromSearchingDto;
    }
}
