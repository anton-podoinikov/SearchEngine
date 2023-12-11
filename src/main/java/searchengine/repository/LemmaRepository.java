package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaTable;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;

@Repository

public interface LemmaRepository extends JpaRepository<LemmaTable, Integer> {

    @Query("SELECT l FROM LemmaTable l WHERE l.lemma = :lemmaText AND l.siteId = :siteId")
    LemmaTable findByLemmaAAndSiteId(@Param("lemmaText") String lemmaText, @Param("siteId") SiteTable siteTable);


    @Transactional
    void deleteBySiteId(SiteTable siteId);

    int countBySiteId(SiteTable siteTable);
}
