package searchengine.model;

import javax.persistence.*;

@Table(name="dataForSearch",schema = "search_engine")

@Entity
public class DataForSearch {
    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "site_name",nullable = false, columnDefinition = "varchar(255)")
    private String siteName;
    @Column(name = "side_map", nullable = false, columnDefinition = "TEXT")
    private String sideMap;
    @Column(name = "outHTML", nullable = false, columnDefinition = "TEXT")
    private String outHTML;

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSideMap() {
        return sideMap;
    }

    public void setSideMap(String sideMap) {
        this.sideMap = sideMap;
    }

    public String getOutHTML() {
        return outHTML;
    }

    public void setOutHTML(String outHTML) {
        this.outHTML = outHTML;
    }
}
