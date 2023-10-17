package searchengine.parsing;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.indexing.Link;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParseHtml extends RecursiveAction {
    final SiteRepository siteRepository;
    final SiteTable siteTable;
    static final Set<PageTable> pageTablesUnique = new HashSet<>();
    static final Set<Link> links = new HashSet<>();
    final String url;
    static int sum = 0;

    public ParseHtml(String url, SiteTable siteTable, SiteRepository siteRepository) {
        this.url = url;
        this.siteTable = siteTable;
        this.siteRepository = siteRepository;
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
                    sum += 1;
                    log.info(sum + " - " + link.getLink());
                    links.add(link);

                    pageTable.setPath(link.getLink());
                    pageTable.setContent(element.html());
                    pageTable.setSiteId(siteTable);
                    pageTable.setCode(statusCode);
                    pageTablesUnique.add(pageTable);

                    updateStatusTime();

                    ParseHtml subtask = new ParseHtml(link.getLink(), siteTable, siteRepository);
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

    public boolean isFileLink(String link) {
        String filePattern = ".*\\.(?i)(jpg|jpeg|png|gif|pdf|doc|docx|xls|xlsx|zip)$";

        Pattern pattern = Pattern.compile(filePattern);
        Matcher matcher = pattern.matcher(link);

        return matcher.matches();
    }

    private void updateStatusTime() {
        siteTable.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(siteTable);
    }
}