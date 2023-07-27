import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.List;

public class Main {
    public static String text="Каждый водитель мог бы быть сдержаннее и внимательнее, что увеличит его значимость у окружающих и повысит самооценку водителя";
//    public static List<String>cleanFromPartOffSpeech=new ArrayList<>();
    public static String textHtml="<a href=\"/catalog/1308.html\">Чехлы для смартфонов Samsung</a>";
    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> wordBaseForms = luceneMorph.getNormalForms("белка");
        wordBaseForms.forEach(System.out::println);

        List<String> wordBaseForms2 = luceneMorph.getMorphInfo("в");
        wordBaseForms2.forEach(System.out::println);
        List<String> wordBaseForms3 = luceneMorph.getMorphInfo("что");
        wordBaseForms3.forEach(System.out::println);
        List<String> wordBaseForms4 = luceneMorph.getMorphInfo("бы");
        wordBaseForms4.forEach(System.out::println);
        List<String> wordBaseForms5 = luceneMorph.getMorphInfo("и");
        wordBaseForms5.forEach(System.out::println);

         LemmaFinder lemmaFinder=LemmaFinder.getInstance();
 //       System.out.println(lemmaFinder.getLemmaSet(text));
        System.out.println(lemmaFinder.collectLemmas(text));
        System.out.println(lemmaFinder.htmlCleaner(textHtml));
 //       getCleanFromPartOffSpeech(text);
    }
//    public static List<String> getCleanFromPartOffSpeech (String text) throws IOException {
//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
//
//        String encoding;
//        String encoding2;
//        String regex = "[А-Яа-я]+";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(text);
//        int y=0;
//        while (matcher.find()) {
//            int start = matcher.start();
//            int end = matcher.end();
//            encoding = text.substring(start, end);
//            HashMap<Integer, String> map = new HashMap<>();
//
//            map.put(y,encoding);
//            y++;
//            System.out.println(map);
//
//            List<String> wordBaseForms = luceneMorph.getMorphInfo(encoding);
//            System.out.println(wordBaseForms);
//            String regex2 = "СОЮЗ";
//            Pattern pattern2 = Pattern.compile(regex2);
//            Matcher matcher2 = pattern2.matcher(wordBaseForms.toString());
//            while (matcher2.find()) {
//                System.out.println(matcher.group());
//               }
//            }
//            cleanFromPartOffSpeech = List.of(text.split("\\s+"));
//
//            return cleanFromPartOffSpeech;
//    }

}