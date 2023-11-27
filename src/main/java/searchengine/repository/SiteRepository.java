package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteTable;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<SiteTable, Integer> {

    SiteTable findByUrl(String url);

    void deleteByUrl(String url);
}