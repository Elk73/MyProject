package searchengine.model;

import javax.persistence.*;

@Table(name="search",schema = "search_engine")
@Entity
public class ObjectSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(unique = true,nullable = false,columnDefinition = "TEXT")
    private String uri;
    @Column(name = "title", nullable = false, columnDefinition = "varchar(255)")
    private String title;
    @Column(name = "snippet", nullable = false, columnDefinition = "varchar(255)")
    private String snippet;
    @Column  (name="relevance",nullable = false)
    private  double relevance;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }
}
