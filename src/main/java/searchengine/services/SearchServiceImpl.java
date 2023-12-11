package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.pool.TypePool;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.morphology.LemmaFinder;
import searchengine.repository.LemmaRepository;

import java.util.HashMap;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService{
    private LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;

    @Override
    public SearchResponse findByLemmaInDatabase(String query) {
        HashMap<String, Integer> lemmaList = lemmaFinder.collectLemmas(query);
        return null;
    }
}
