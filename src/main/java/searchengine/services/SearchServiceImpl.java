package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.search.Data;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexTable;
import searchengine.model.LemmaTable;
import searchengine.model.PageTable;
import searchengine.model.Status;
import searchengine.morphology.LemmaFinder;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService{
    private LemmaFinder lemmaFinder;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;


    @Override
    public SearchResponse findByLemmaInDatabase(String query, int offset, int limit) {
        return performSearch(query, null, offset, limit);
    }

    @Override
    public SearchResponse findByLemmaInDatabase(String query, String site, int offset, int limit) {
        if (!isIndexReady(site)) {
            return new SearchResponse(false, "Индекс для сайта " + site + " не готов или отсутствует");
        }
        return performSearch(query, site, offset, limit);
    }

    private SearchResponse performSearch(String query, String site, int offset, int limit) {
        HashMap<String, Integer> lemmaList = lemmaFinder.collectLemmas(query);
        List<LemmaTable> filteredLemmas = filterLemmasByFrequency(lemmaList);
        List<PageTable> relevantPages = findRelevantPages(filteredLemmas, site);

        Map<PageTable, Double> relevanceScores = calculateRelevanceScores(relevantPages, filteredLemmas);
        List<Map.Entry<PageTable, Double>> sortedPages = new ArrayList<>(relevanceScores.entrySet());
        sortedPages.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        int totalResults = sortedPages.size();

        offset = Math.max(offset, 0);
        limit = Math.max(limit, 1);

        int start = Math.min(offset, totalResults);
        int end = Math.min(start + limit, totalResults);

        List<Map.Entry<PageTable, Double>> paginatedPages = sortedPages.subList(start, end);

        List<Data> results = new ArrayList<>();
        for (Map.Entry<PageTable, Double> entry : paginatedPages) {
            PageTable page = entry.getKey();
            double relevance = entry.getValue();
            String snippet = generateSnippet(page.getContent(), query);

            Data searchData = new Data();
            searchData.setUri(page.getPath());
            searchData.setTitle(extractTitle(page.getContent()));
            searchData.setSnippet(snippet);
            searchData.setRelevance(relevance);
            searchData.setSite(page.getSiteId().getUrl());
            searchData.setSiteName(page.getSiteId().getName());

            results.add(searchData);
        }

        SearchResponse response = new SearchResponse(true, "Поиск выполнен успешно");
        response.setResults(results);
        response.setCount(totalResults);
        return response;
    }

    private boolean isIndexReady(String site) {
        return siteRepository.findByUrl(site).getStatus().equals(Status.INDEXED);
    }

    private List<LemmaTable> filterLemmasByFrequency(HashMap<String, Integer> lemmaList) {
        Set<String> allLemmas = lemmaList.keySet();
        int frequencyThreshold = 1000;
        return lemmaRepository.findByLemmaInAndFrequencyLessThanOrderByFrequencyAsc(allLemmas, frequencyThreshold);
    }

    private List<PageTable> findRelevantPages(List<LemmaTable> filteredLemmas, String site) {
        Set<Integer> relevantPageIds = new HashSet<>();

        for (LemmaTable lemma : filteredLemmas) {
            Set<Integer> currentPageIds = lemma.getIndex().stream()
                    .map(index -> index.getPage().getId())
                    .collect(Collectors.toSet());
            relevantPageIds.addAll(currentPageIds);
        }

        if (relevantPageIds.isEmpty()) {
            log.info("No relevant pages found for the given lemmas.");
            return Collections.emptyList();
        }

        List<PageTable> pages = pageRepository.findAllById(relevantPageIds);

        if (site != null && !site.isEmpty()) {
            pages = pages.stream()
                    .filter(page -> page.getSiteId().getUrl().equalsIgnoreCase(site))
                    .collect(Collectors.toList());
        }
        return pages;
    }

    private Map<PageTable, Double> calculateRelevanceScores(List<PageTable> relevantPages, List<LemmaTable> filteredLemmas) {
        Map<PageTable, Double> relevanceScores = new LinkedHashMap<>();

        for (PageTable page : relevantPages) {
            double absoluteRelevance = filteredLemmas.stream()
                    .flatMap(lemma -> lemma.getIndex().stream())
                    .filter(index -> index.getPage().equals(page))
                    .mapToDouble(IndexTable::getRank)
                    .sum();

            relevanceScores.put(page, absoluteRelevance);
        }

        if (relevanceScores.isEmpty()) {
            log.info("No relevant pages found or no relevancies calculated.");
            return relevanceScores;
        }

        double maxRelevance = Collections.max(relevanceScores.values());

        if (maxRelevance == 0) {
            log.info("Maximum relevance is 0, possibly due to no relevant pages or ranks.");
            return relevanceScores;
        }

        Map<PageTable, Double> relativeRelevanceScores = new LinkedHashMap<>();

        for (Map.Entry<PageTable, Double> entry : relevanceScores.entrySet()) {
            double relativeRelevance = entry.getValue() / maxRelevance;
            BigDecimal roundedRelevance = BigDecimal.valueOf(relativeRelevance)
                    .setScale(4, RoundingMode.HALF_UP);
            relativeRelevanceScores.put(entry.getKey(), roundedRelevance.doubleValue());
        }

        return relativeRelevanceScores;
    }

    private String generateSnippet(String pageContent, String query) {
        int pos = pageContent.toLowerCase().indexOf(query.toLowerCase());

        if (pos == -1) {
            return pageContent.substring(0, Math.min(pageContent.length(), 150)) + "...";
        }

        int start = Math.max(pos - 70, 0);
        int end = Math.min(pos + 70 + query.length(), pageContent.length());
        String snippet = pageContent.substring(start, end) + "...";

        snippet = snippet.replaceAll(query, "<b>" + query + "</b>");

        return snippet;
    }

    private String extractTitle(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.title();
    }
}
