package searchengine.services.morphology;

import java.util.HashMap;
import java.util.List;

public interface LemmaFinder {
    HashMap<String, Integer> collectLemmas(String text);
    List<String> getLemma(String word);
}
