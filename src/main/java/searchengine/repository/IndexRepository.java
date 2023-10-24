package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.IndexTable;

public interface IndexRepository extends JpaRepository<IndexTable, Integer> {
}
