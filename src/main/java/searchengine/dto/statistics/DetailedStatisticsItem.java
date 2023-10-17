package searchengine.dto.statistics;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DetailedStatisticsItem {
    String url;
    String name;
    String status;
    long statusTime;
    String error;
    int pages;
    int lemmas;
}
