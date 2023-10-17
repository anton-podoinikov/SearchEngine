package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageTable;

@Repository
public interface PageRepository extends JpaRepository<PageTable, Integer> {
    @Modifying
    @Query("DELETE FROM PageTable p WHERE p.siteId.id = ?1")
    void deleteBySiteId(int siteId);
}
