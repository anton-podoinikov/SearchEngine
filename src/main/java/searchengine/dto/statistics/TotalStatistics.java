package searchengine.dto.statistics;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TotalStatistics {
    int sites;
    int pages;
    int lemmas;
    boolean indexing;
}
