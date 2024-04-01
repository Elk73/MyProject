package searchengine.response.searching;
import lombok.Data;
import java.util.List;

@Data
public class StatisticsResponseFromSearchingDto {
    private boolean result;
        private int count;
        private List<TotalSearchingDto> data;
}
