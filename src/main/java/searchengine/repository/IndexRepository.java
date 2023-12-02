package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexTable;
import searchengine.model.PageTable;

@Repository
public interface IndexRepository extends JpaRepository<IndexTable, Integer> {
    void deleteByPageId(int pageId);
}
