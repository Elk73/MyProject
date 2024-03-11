package searchengine.model;

import javax.persistence.*;

@Table(name="page",schema = "search_engine")
//@Table(indexes = @Index (name = "p_index",columnList = "path"))
@Entity
public class Page {
    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "pageGen")
    @TableGenerator(name = "pageGen", table = "JPAGEN_GENERATORS",pkColumnName = "p_index",
            pkColumnValue = "JPAGEN_PAGE_GEN",
            valueColumnName = "path")
    private int id;
    @Column  (name="site_id",nullable = false)
    private  int siteId;
    @Column(unique = true,nullable = false,columnDefinition = "TEXT")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(nullable = false,columnDefinition = "MEDIUMTEXT")
    private String content;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }
}

