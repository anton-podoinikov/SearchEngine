package searchengine.services.parsing;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.SiteTable;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ParseHtml extends RecursiveAction {
    private final String url;
    private final SiteTable siteTable;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private static final Set<Link> links = new CopyOnWriteArraySet<>();
    private static final Set<String> pages = new CopyOnWriteArraySet<>();

    public ParseHtml(String url, SiteTable siteTable, SiteRepository siteRepository, PageRepository pageRepository) {
        this.url = url;
        this.siteTable = siteTable;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(300);
            Document documentParent = connection(url);

            Elements linkElements = documentParent.select("a[href]");
            List<ParseHtml> subtasks = new CopyOnWriteArrayList<>();

            for (Element element : linkElements) {
                Link link = new Link();
                String linkUrl = element.attr("abs:href");
                if (linkUrl.startsWith(url)
                        && !isFileLink(linkUrl)
                        && !linkUrl.contains("#")
                        && !pages.contains(linkUrl)
                ) {
                    log.info(linkUrl);

                    Document documentChildren = connection(linkUrl);

                    link.setPath(linkUrl);
                    link.setContent(documentChildren.html());
                    link.setCode(documentChildren.connection().response().statusCode());

                    links.add(link);
                    pages.add(linkUrl);

                    setSiteTableStatusTime();

                    ParseHtml subtask = new ParseHtml(linkUrl, siteTable, siteRepository, pageRepository);
                    subtasks.add(subtask);
                }
            }
            invokeAll(subtasks);
        } catch (Exception exception) {
            setSiteTableError(exception);
            log.error(exception.getMessage());
        }
    }

    public Set<Link> getLinks() {
        return new HashSet<>(links);
    }

    private void setSiteTableStatusTime() {
        siteTable.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteTable);
    }

    private void setSiteTableError(Exception exception) {
        siteTable.setLastError(exception.getMessage());
        siteTable.setStatus(Status.FAILED);
        siteRepository.save(siteTable);
    }

    private boolean isFileLink(String link) {
        String filePattern = ".*\\.(?i)(jpg|jpeg|png|gif|pdf|doc|docx|xls|xlsx|zip)$";
        Pattern pattern = Pattern.compile(filePattern);
        Matcher matcher = pattern.matcher(link);
        return matcher.matches();
    }

    private Document connection(String url) throws IOException {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                        "AppleWebKit/537.36 (HTML, like Gecko)" +
                        "Chrome/58.0.3029.110 Safari/537.3")
                .get();
    }
}