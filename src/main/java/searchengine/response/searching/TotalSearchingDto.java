package searchengine.response.searching;

import lombok.Data;

@Data
public class TotalSearchingDto {
    String sites;
    String siteName;
    String uri;
    String title;
    String snippet;
    Double relevance;
}
