package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageTable;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<PageTable, Integer> {
}
