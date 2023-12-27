package searchengine.dto.search;

import java.util.List;

@lombok.Data
public class SearchResponse {
    private boolean result;
    private String error;
    private int count;
    private List<Data> data; // Обновлено с searchData на data

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public void setResults(List<Data> results) {
        this.data = results;
        this.count = results.size();
    }
}
