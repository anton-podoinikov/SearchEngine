package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaTable;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaTable, Integer> {

    LemmaTable findByLemma(String lemmaText);

    void deleteBySiteId(int siteId);

    int countBySiteId(SiteTable siteTable);
}
