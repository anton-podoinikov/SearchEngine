package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteTable;

@Repository
public interface SiteRepository extends JpaRepository<SiteTable, Integer> {

    @Modifying
    @Query("DELETE FROM SiteTable s WHERE s.url = ?1")
    void deleteByUrl(String url);

    SiteTable findByUrl(String url);
}