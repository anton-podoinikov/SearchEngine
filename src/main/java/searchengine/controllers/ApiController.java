package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse response = indexingService.startIndexing();
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new IndexingResponse(false, "Индексация уже запущена"));
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse response = indexingService.stopIndexing();
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new IndexingResponse(false, "Индексация не запущена"));
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        IndexingResponse response = indexingService.startIndexingUrl(url);
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new IndexingResponse(false, "Данная страница находится за пределами сайтов, " +
                            "указанных в конфигурационном файле"));
        }
    }

    //TODO Написать контроллер поиска.
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(String query) {
        if (query.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new SearchResponse(false, "Задан пустой поисковый запрос"));
        }
        SearchResponse response = searchService.findByLemmaInDatabase(query);
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new SearchResponse(false, "Указанная страница не найдена"));
        }
    }
}