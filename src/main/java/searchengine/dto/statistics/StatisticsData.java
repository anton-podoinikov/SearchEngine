package searchengine.dto.statistics;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticsData {
    TotalStatistics total;
    List<DetailedStatisticsItem> detailed;
}
