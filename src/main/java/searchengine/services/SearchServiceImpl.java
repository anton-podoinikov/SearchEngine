package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;

@Service
public class SearchServiceImpl implements SearchService{

    @Override
    public SearchResponse findByLemmaInDatabase(String query) {
        return null;
    }
}
