package searchengine.model;

import javax.persistence.*;

@Table(name="`index_search`",schema = "search_engine")
@Entity
public class IndexSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column  (name="page_id",nullable = false)
    private  int pageId;

    @Column  (name="lemma_id",nullable = false)
    private  int lemmaId;
    @Column  (name="`rank`",nullable = false)
    private  float rank;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getLemmaId() {
        return lemmaId;
    }

    public void setLemmaId(int lemmaId) {
        this.lemmaId = lemmaId;
    }

    public float getRank() {
        return rank;
    }

    public void setRank(float rank) {
        this.rank = rank;
    }
}
