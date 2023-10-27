package searchengine.splitter;

import java.util.HashMap;
import java.util.List;

public interface LemmaFinder {
    HashMap<String, Integer> collectLemmas(String text);
    List<String> getLemma(String word);
    List<Integer> findLemmaIndexInText(String content, String lemma);
}
