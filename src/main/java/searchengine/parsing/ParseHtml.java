package searchengine.parsing;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;
import searchengine.splitter.LemmaFinder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ParseHtml extends RecursiveAction {
    private final SiteRepository siteRepository;
    private final SiteTable siteTable;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinder lemmaFinder;
    private static final Set<PageTable> pageTablesUnique = new HashSet<>();
    private static final Set<Link> links = new HashSet<>();

    private final String url;
    private static int count = 0;

    public ParseHtml(String url, SiteTable siteTable, SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, LemmaFinder lemmaFinder) {
        this.url = url;
        this.siteTable = siteTable;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaFinder = lemmaFinder;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(200);
            Document doc = Jsoup.connect(url)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                            "AppleWebKit/537.36 (KHTML, like Gecko)" +
                            "Chrome/58.0.3029.110 Safari/537.3")
                    .get();

            int statusCode = doc.connection().response().statusCode();

            Elements linkElements = doc.select("a[href]");

            HashMap<String, Integer> lemma = lemmaFinder.collectLemmas(doc.html());

            for (Map.Entry<String, Integer> entry : lemma.entrySet()) {
                String lemmaText = entry.getKey();
                int frequency = entry.getValue();

                // Попытка найти лемму в базе данных
                LemmaTable existingLemma = lemmaRepository.findByLemma(lemmaText);

                if (existingLemma != null) {
                    // Лемма уже существует - увеличиваем частоту
                    existingLemma.setFrequency(existingLemma.getFrequency() + frequency);
                } else {
                    LemmaTable lemmaTable = new LemmaTable();
                    lemmaTable.setSiteId(siteTable);
                    lemmaTable.setLemma(lemmaText);
                    lemmaTable.setFrequency(frequency);
                    lemmaRepository.saveAndFlush(lemmaTable);
                }

                IndexTable indexTable = new IndexTable();
                indexTable.setLemma(existingLemma); // Установите существующую лемму
                indexTable.setPage(pageTable);   // Установите существующую страницу
                indexTable.setRank(frequency);      // Установите количество леммы на странице
                indexRepository.saveAndFlush(indexTable);
            }

            List<ParseHtml> subtasks = new ArrayList<>();

            for (Element element : linkElements) {
                String linkUrl = element.attr("abs:href");
                Link link = new Link(linkUrl);
                PageTable pageTable = new PageTable();
                if (linkUrl.startsWith(url)
                        && !isFileLink(url)
                        && !linkUrl.contains("#")
                        && !links.contains(link)
                ) {
                    count();
                    log.info(count + " - " + link.getLink());
                    links.add(link);
                    pageTable.setPath(link.getLink());
                    pageTable.setContent(element.html());
                    pageTable.setSiteId(siteTable);
                    pageTable.setCode(statusCode);
                    pageTablesUnique.add(pageTable);

                    updateStatusTime();
                    ParseHtml subtask = new ParseHtml(link.getLink()
                            , siteTable
                            , siteRepository
                            , lemmaRepository
                            , indexRepository
                            , lemmaFinder);
                    subtasks.add(subtask);
                }
            }

            invokeAll(subtasks);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            siteTable.setStatus(Status.FAILED);
            siteTable.setLastError("Ошибка индексации: " + exception.getMessage());
            siteRepository.saveAndFlush(siteTable);
        }
    }

    public List<PageTable> getPageTable() {
        siteTable.setStatus(Status.INDEXED);
        return new ArrayList<>(pageTablesUnique);
    }

    private boolean isFileLink(String link) {
        String filePattern = ".*\\.(?i)(jpg|jpeg|png|gif|pdf|doc|docx|xls|xlsx|zip)$";
        Pattern pattern = Pattern.compile(filePattern);
        Matcher matcher = pattern.matcher(link);
        return matcher.matches();
    }

    private void updateStatusTime() {
        siteTable.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteTable);
    }

    private static void count() {
        count++;
    }
}