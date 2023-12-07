package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {
    private List<Site> sites;
    private Site site;



    public SitesList(Site site, List<Site> sites) {
        this.site = site;
        this.sites=sites;
    }
    public void setSites( String url, String name) {
        site.setUrl(url);
        site.setName(name);
        sites.add(site);
    }

}
