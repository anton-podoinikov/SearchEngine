package searchengine.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;

@Entity
@Table(name = "page")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "siteId")
    SiteTable siteId;

    @Column(columnDefinition = "TEXT, INDEX(path(255))")
    String path;

    int code;

    @Column(columnDefinition = "MEDIUMTEXT")
    String content;
}