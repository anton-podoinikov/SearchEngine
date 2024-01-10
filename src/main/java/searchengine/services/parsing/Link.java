package searchengine.services.parsing;

import lombok.Data;

@Data
public class Link {
    private String path;
    private int code;
    private String content;
}
