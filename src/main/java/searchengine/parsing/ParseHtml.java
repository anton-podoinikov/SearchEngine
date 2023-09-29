package searchengine.parsing;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.indexing.Link;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;
import searchengine.model.Status;

import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class ParseHtml extends RecursiveAction {
    private final SiteTable siteTable;
    private static final Set<PageTable> pageTablesUnique = new HashSet<>();
    private static final Set<Link> links = new HashSet<>();
    private final String url;
    private static int sum = 0;

    public ParseHtml(String url, SiteTable siteTable) {
        this.url = url;
        this.siteTable = siteTable;
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

            int statusCode = doc.connection().execute().statusCode();

            Elements linkElements = doc.select("a[href]");

            List<ParseHtml> subtasks = new ArrayList<>();

            for (Element element : linkElements) {
                String linkUrl = element.attr("abs:href");

                Link link = new Link(linkUrl);
                PageTable pageTable = new PageTable();

                if (linkUrl.startsWith(url)
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

                    ParseHtml subtask = new ParseHtml(link.getLink(), siteTable);
                    subtasks.add(subtask);
                }
            }
            invokeAll(subtasks);
        } catch (Exception exception) {
            log.error(exception.getMessage());
        }
    }

    public List<PageTable> getPageTable() {
        siteTable.setStatus(Status.INDEXED);
        return new ArrayList<>(pageTablesUnique);
    }
}