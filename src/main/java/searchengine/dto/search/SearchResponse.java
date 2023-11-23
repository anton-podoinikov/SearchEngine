package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private String error;
    private int count;
    private List<SearchData> searchData;

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
