package searchengine.dto.indexing;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IndexingResponse {
    boolean result;
    String error;

    public IndexingResponse(boolean result) {
        this.result = result;
    }

    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
