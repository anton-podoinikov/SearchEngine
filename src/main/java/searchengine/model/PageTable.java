package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;
import searchengine.config.Site;

import javax.persistence.*;

@Table(name = "page")
@Entity
@Getter
@Setter
public class PageTable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @NotNull
    private int id;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "siteId")
    private SiteTable siteId;

    @Column(columnDefinition = "TEXT, INDEX(path(255))")
    @NotNull
    private String path;

    @NotNull
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT")
    @NotNull
    private String content;
}