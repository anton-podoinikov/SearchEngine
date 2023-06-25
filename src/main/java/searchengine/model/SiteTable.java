package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Table(name = "site")
@Entity
@Getter
@Setter
public class SiteTable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @NotNull
    private int id;

    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    @NotNull
    private Date statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)")
    @NotNull
    private String url;

    @Column(columnDefinition = "VARCHAR(255)")
    @NotNull
    private String name;

    @OneToMany(mappedBy = "siteId", fetch = FetchType.EAGER)
    private List<PageTable> pages;
}
