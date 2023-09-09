package searchengine.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
public class Link {
    private String link;
    private final List<Link> subLinks = new ArrayList<>();
}
