package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

import java.util.concurrent.CompletableFuture;

public interface IndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();

    IndexingResponse indexPage(String url);
}
