package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;

import java.util.Collection;

@Repository
public interface PageRepository extends JpaRepository<PageTable, Integer> {
    PageTable findByPath(String url);
    int countBySiteId(SiteTable siteTable);
}
