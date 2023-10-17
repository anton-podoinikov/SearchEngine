package searchengine.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "indexing-settings")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SitesList {
    List<Site> sites;
}
