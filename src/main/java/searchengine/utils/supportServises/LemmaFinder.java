package searchengine.utils.supportServises;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LemmaFinder {
    public static LuceneMorphology luceneMorphology;
    public static LuceneMorphology luceneMorphologyEnglish;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ","ЧАСТ"};
    public HashMap<String, Integer> lemmas = new HashMap<>();
    static {
        try {
            luceneMorphology = new RussianLuceneMorphology();
            luceneMorphologyEnglish = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static LemmaFinder getInstance() throws IOException {
        return new LemmaFinder();
    }
    /**
     * Метод разделяет текст на слова, находит все леммы и считает их количество.
     *
     * @param text текст из которого будут выбираться леммы
     * @return ключ является леммой, а значение количеством найденных лемм
     */
    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        String regexWordRussian="^[a-zA-Z\\s]*";
 //       String regex = "[а-яёА-ЯЁ\\s]*";
        Pattern pattern = Pattern.compile(regexWordRussian);
        Matcher m = pattern.matcher(text);
        if (m.find()){
            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }

                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }

                List<String> normalForms = luceneMorphology.getNormalForms(word);
                if (normalForms.isEmpty()) {
                    continue;
                }

                String normalWord = normalForms.get(0);

                if (lemmas.containsKey(normalWord)) {
                    lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                } else {
                    lemmas.put(normalWord, 1);
                }
            }
        }
            String[] wordsAng = arrayContainsAngWords(text);
            String regexWordAng = "^[а-яёА-ЯЁ\\s]*";

            Pattern patternAng = Pattern.compile(regexWordAng);
            Matcher mAng = patternAng.matcher(text);
            if (mAng.find()) {
                for (String word : wordsAng) {
                    if (word.isBlank()) {
                        continue;
                    }

                    List<String> wordBaseForms = luceneMorphologyEnglish.getMorphInfo(word);
                    if (anyWordBaseBelongToParticle(wordBaseForms)) {
                        continue;
                    }

                    List<String> normalForms = luceneMorphologyEnglish.getNormalForms(word);
                    if (normalForms.isEmpty()) {
                        continue;
                    }

                    String normalWord = normalForms.get(0);

                    if (lemmas.containsKey(normalWord)) {
                        lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                    } else {
                        lemmas.put(normalWord, 1);
                    }
                }
            }
        return lemmas;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
    private String[] arrayContainsAngWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^a-z\\s])", " ")
                .trim()
                .split("\\s+");
    }
    /**
     * Метод отделяет текст очищая от HTML тегов.
     */
    public String htmlCleaner(String html) {
        return Jsoup.parse(html).text();
    }
}
