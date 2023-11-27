package searchengine.morphology;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class LemmaFinderImpl implements LemmaFinder {
    private static RussianLuceneMorphology russianLuceneMorphology;
    private static final String REGEX = "\\p{Punct}|[0-9]|№|©|◄|«|»|—|-|@|…";
    private static final Marker INVALID_SYMBOL_MARKER = MarkerManager.getMarker("INVALID_SYMBOL");
    private static final Logger LOGGER = LogManager.getLogger(LuceneMorphology.class);

    static {
        try {
            russianLuceneMorphology = new RussianLuceneMorphology();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public HashMap<String, Integer> collectLemmas(String content) {
        content = content.toLowerCase(Locale.ROOT)
                .replaceAll(REGEX, " ");
        HashMap<String, Integer> lemmaList = new HashMap<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\s+");
        for (String element : elements) {
            List<String> wordsList = getLemma(element);
            for (String word : wordsList) {
                int count = lemmaList.getOrDefault(word, 0);
                lemmaList.put(word, count + 1);
            }
        }
        return lemmaList;
    }

    public List<String> getLemma(String word) {
        List<String> lemmaList = new ArrayList<>();
        try {
            List<String> baseRusForm = russianLuceneMorphology.getNormalForms(word);
            if (!isServiceWord(word)) {
                lemmaList.addAll(baseRusForm);
            }
        } catch (Exception e) {
            LOGGER.debug(INVALID_SYMBOL_MARKER, "Символ не найден - " + word);
        }
        return lemmaList;
    }

    public List<Integer> findLemmaIndexInText(String content, String lemma) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String el : elements) {
            List<String> lemmas = getLemma(el);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }

    private boolean isServiceWord(String word) {
        List<String> morphForm = russianLuceneMorphology.getMorphInfo(word);
        for (String l : morphForm) {
            if (l.contains("ПРЕДЛ")
                    || l.contains("СОЮЗ")
                    || l.contains("МЕЖД")
                    || l.contains("МС")
                    || l.contains("ЧАСТ")
                    || l.length() <= 3) {
                return true;
            }
        }
        return false;
    }
}