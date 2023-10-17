package searchengine.controllers;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiController {

    StatisticsService statisticsService;

    IndexingService indexingService;

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
                 .status(HttpStatus.BAD_REQUEST)
                 .body(new IndexingResponse(false,"Индексация уже запущена"));
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing(){
        IndexingResponse response = indexingService.stopIndexing();
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new IndexingResponse(false,"Индексация не запущена"));
        }
    }
}