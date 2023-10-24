package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "index_table")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexTable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private int id;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "pageId", nullable = false)
    private PageTable page;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemmaId", nullable = false)
    private LemmaTable lemma;

    @Column(name = "index_rank", nullable = false)
    private float rank;
}
