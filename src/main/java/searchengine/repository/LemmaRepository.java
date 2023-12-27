package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaTable;
import searchengine.model.SiteTable;

import java.util.List;
import java.util.Set;

@Repository

public interface LemmaRepository extends JpaRepository<LemmaTable, Integer> {
    int countBySiteId(SiteTable siteTable);
   List<LemmaTable> findByLemmaInAndFrequencyLessThanOrderByFrequencyAsc(Set<String> lemmas, int frequency);
}
