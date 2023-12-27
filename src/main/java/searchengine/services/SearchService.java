package searchengine.services;

import searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse findByLemmaInDatabase(String query, int offset, int limit);

    SearchResponse findByLemmaInDatabase(String query, String site, int offset, int limit);
}
