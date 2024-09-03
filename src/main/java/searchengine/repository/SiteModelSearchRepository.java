package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModelSearch;
@Repository
public interface SiteModelSearchRepository extends CrudRepository<SiteModelSearch,Integer> {
}
