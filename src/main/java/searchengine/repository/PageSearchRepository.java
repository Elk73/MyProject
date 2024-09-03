package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageSearch;

@Repository
public interface PageSearchRepository extends CrudRepository<PageSearch,Integer> {
}
