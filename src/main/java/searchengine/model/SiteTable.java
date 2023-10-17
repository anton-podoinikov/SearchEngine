package searchengine.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "site")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SiteTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    Status status;

    LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT")
    String lastError;

    @Column(columnDefinition = "VARCHAR(255)")
    String url;

    @Column(columnDefinition = "VARCHAR(255)")
    String name;

    @OneToMany(mappedBy = "siteId", fetch = FetchType.EAGER)
    List<PageTable> pages;


}
