package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.LemmaTable;

public interface LemmaRepository extends JpaRepository<LemmaTable, Integer> {
}
