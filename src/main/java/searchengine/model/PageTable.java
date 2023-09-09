package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "page")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @JoinColumn(name = "siteId")
    private SiteTable siteId;

    @Column(columnDefinition = "TEXT, INDEX(path(255))")
    private String path;

    private int code;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
}