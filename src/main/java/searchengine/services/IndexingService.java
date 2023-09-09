package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

import java.util.concurrent.CompletableFuture;

public interface IndexingService {
    CompletableFuture<IndexingResponse> startIndexing();
    IndexingResponse stopIndexing();
}
