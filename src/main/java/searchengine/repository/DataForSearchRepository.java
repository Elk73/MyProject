package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.DataForSearch;
@Repository
public interface DataForSearchRepository extends CrudRepository<DataForSearch,Integer> {
}
