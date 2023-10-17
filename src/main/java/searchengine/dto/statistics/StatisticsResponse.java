package searchengine.dto.statistics;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticsResponse {
    boolean result;
    StatisticsData statistics;
}
