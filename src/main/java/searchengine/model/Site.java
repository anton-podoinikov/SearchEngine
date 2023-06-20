package searchengine.model;

import com.sun.istack.NotNull;r;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @NotNull
    private int id;

    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL")
    @Enumerated(EnumType.STRING)
    @NotNull
    private Status status;

    @Column(columnDefinition = "DATETIME")
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
}
