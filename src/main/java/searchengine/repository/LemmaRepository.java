package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaTable;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaTable, Integer> {

    LemmaTable findByLemma(String lemmaText);
}
