package searchengine.parsing;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
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
    private final PageRepository pageRepository;
    private final LemmaFinder lemmaFinder;
    private static final Set<PageTable> pageTablesUnique = new HashSet<>();
    private static final Set<String> links = new HashSet<>();
    private final String url;
    private static int count = 0;

    public ParseHtml(String url
            , SiteTable siteTable
            , SiteRepository siteRepository
            , LemmaRepository lemmaRepository
            , IndexRepository indexRepository
            , PageRepository pageRepository, LemmaFinder lemmaFinder) {
        this.url = url;
        this.siteTable = siteTable;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.lemmaFinder = lemmaFinder;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(300);
            Document doc = Jsoup.connect(url)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                            "AppleWebKit/537.36 (KHTML, like Gecko)" +
                            "Chrome/58.0.3029.110 Safari/537.3")
                    .get();

            int statusCode = doc.connection().response().statusCode();

            Elements linkElements = doc.select("a[href]");

            List<ParseHtml> subtasks = new ArrayList<>();

            for (Element element : linkElements) {
                String linkUrl = element.attr("abs:href");
                PageTable pageTable = new PageTable();
                if (linkUrl.startsWith(url)
                        && !isFileLink(linkUrl)
                        && !linkUrl.contains("#")
                        && !links.contains(linkUrl)
                ) {
                    count();
                    log.info(count + " - " + linkUrl);
                    links.add(linkUrl);

                    pageTable.setPath(linkUrl);
                    pageTable.setContent(element.html());
                    pageTable.setSiteId(siteTable);
                    pageTable.setCode(statusCode);
                    pageTablesUnique.add(pageTable);



                    updateStatusTime();

                    ParseHtml subtask = new ParseHtml(linkUrl
                            , siteTable
                            , siteRepository
                            , lemmaRepository
                            , indexRepository
                            , pageRepository
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