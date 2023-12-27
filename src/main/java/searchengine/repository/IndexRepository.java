package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexTable;
import searchengine.model.LemmaTable;

import java.util.Arrays;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexTable, Integer> {

    List<IndexTable> findAllByPageId(int page_id);

    @Transactional
    void deleteByPageId(int page_id);

    List<IndexTable> findByLemmaId(LemmaTable lemma);
}
