package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.ObjectSearch;

import java.util.List;

@Repository
public interface ObjectSearchRepository extends CrudRepository<ObjectSearch,Integer> {
    List<ObjectSearch> findByUri(String uri);
    List<ObjectSearch> getRelevanceByUri(String uri);
}
