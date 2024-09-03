package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.LemmaSearch;

import java.util.List;
@Repository
public interface LemmaSearchRepository extends CrudRepository<LemmaSearch,Integer> {
    List<LemmaSearch> findByLemma(String lemma);
}
