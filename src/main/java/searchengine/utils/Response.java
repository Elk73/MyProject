package searchengine.utils;
import com.google.gson.Gson;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class Response {
    private boolean result;
// private String result;
 //    private String res ="`result`: true";

//   private Gson g = new Gson();

    public  Response() {
        result = true;
 //       result = g.toJson(true);
//        g.toJson(true);
//         g.toJson(res);
//        System.out.println("RESULT -"+g);
    }
}
