package searchengine.model;

import javax.persistence.*;

@Table(name="lemma",schema = "search_engine")
@Entity
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column  (name="site_id",nullable = false)
    private  int siteId;
    @Column(name = "lemma", nullable = false, columnDefinition = "varchar(255)")
    private String lemma;
    @Column  (name="frequency",nullable = false)
    private  int frequency;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
