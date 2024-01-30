package searchengine.utils;
import lombok.Data;
import org.springframework.stereotype.Component;
import searchengine.parsers.ControllerThread;

@Component
@Data
public class Response {
    private String result;
    public  Response() {
        if (!ControllerThread.isIsRun()) {
            result = "'result': true";
        }
        else{
            result ="{\n  'result': false,\n  'error': \"Индексация уже запущена\"\n}";
        }
    }
}
