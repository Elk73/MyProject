package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexSearch;

import java.util.List;
@Repository
public interface IndexSearchRepository extends CrudRepository<IndexSearch,Integer> {
    List<IndexSearch> findByLemmaId(Integer lemmaId);
}
