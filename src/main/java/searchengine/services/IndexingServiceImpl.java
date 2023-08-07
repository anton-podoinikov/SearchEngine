package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;
import searchengine.model.Status;
import searchengine.repository.PageRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import static java.lang.Thread.sleep;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;

    private final PageRepository pageRepository;

    @Override
    public IndexingResponse startIndexing() {
        return null;
    }






//    @Override
//    protected void compute() {
//
//        List<Site> siteList = sites.getSites();
//
//        for (Site link : siteList) {
//            try {
//                sleep(150);
//                Connection connection = Jsoup.connect(link.getUrl())
//                        .ignoreHttpErrors(true)
//                        .ignoreContentType(true)
//                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
//                                "AppleWebKit/537.36 (KHTML, like Gecko)" +
//                                "Chrome/58.0.3029.110 Safari/537.3");
//
//                Document doc = connection.get();
//                Elements linkElements = doc.select("a[href]");
//
//                SiteTable siteTable = new SiteTable();
//                siteTable.setName(link.getName());
//                siteTable.setStatus(Status.INDEXING);
//                siteTable.setUrl(link.getUrl());
//
//
//                for (Element element : linkElements) {
//                    String linkUrl = element.attr("abs:href");
//                    PageTable pageTable = new PageTable();
//                    pageTable.setPath(linkUrl);
//                    pageTable.setContent(element.html());
//                    pageTable.setCode(200);
//                    pageTable.setSiteId(siteTable);
//
//                    if (linkUrl.startsWith(link.getUrl())
//                            && !linkUrl.contains("#")
//                    ) {
//                        System.out.println(pageTable.getPath());
//                        links.add(pageTable);
//                    }
//                }
//                compute();
//
//
//            } catch (Exception exception) {
//                exception.printStackTrace();
//            }
//        }
//    }




    //    protected void compute() {
//    @Override
//    public void startIndexing (SitesList sites) {
//        this.sites = sites;
//
//        List<Site> sitesList = sites.getSites();
//
//        try {
//            Thread.sleep(150);
//
//
//        } catch (Exception exception) {
//            exception.printStackTrace();
//        }
//    }


//        for (Site site : sitesList) {
//            try {
//                Thread.sleep(150);
//                Connection connection = Jsoup.connect(site.getUrl())
//                        .ignoreHttpErrors(true)
//                        .ignoreContentType(true)
//                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
//                                "AppleWebKit/537.36 (KHTML, like Gecko)" +
//                                "Chrome/58.0.3029.110 Safari/537.3");
//
//                Document doc = connection.get();
//                Elements linkElements = doc.select("a[href]");
//
//                List<IndexingServiceImpl> subtasks = new ArrayList<>();
//
//                SiteTable siteTable = new SiteTable();
//                siteTable.setName(site.getName());
//                siteTable.setStatus(Status.INDEXING);
//                siteTable.setUrl(site.getUrl());
//
//                for (Element element : linkElements) {
//                    String linkUrl = element.attr("abs:href");
//                    PageTable pageTable = new PageTable();
//
//                    if (linkUrl.startsWith(site.getUrl())
//                            && linkUrl.endsWith("/")
//                            && !linkUrl.contains("#")
//                    ) {
//
//                        pageTable.setPath(linkUrl.replace(siteTable.getUrl(), "/"));
//                        pageTable.setContent(element.html());
//                        pageTable.setCode(200);
//                        pageTable.setSiteId(siteTable);
//                        siteTable.setStatusTime(LocalDateTime.now());
//                        linksList.add(pageTable);
//
//
//
//                    }
//                }
//
//
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

//    @Override
//    public IndexingResponse startIndexing() {
//        compute();
//        return null;
//    }


//    @Override
//    public IndexingResponse startIndexing() throws Exception {
//
//        List<Site> sitesList = sites.getSites();
//
//        for (Site site : sitesList) {
//            Thread.sleep(150);
//            Connection connection = Jsoup.connect(site.getUrl())
//                    .ignoreHttpErrors(true)
//                    .ignoreContentType(true)
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
//                            "AppleWebKit/537.36 (KHTML, like Gecko)" +
//                            "Chrome/58.0.3029.110 Safari/537.3");
//
//            Document doc = connection.get();
//            Elements linkElements = doc.select("a[href]");
//
//            List<IndexingServiceImpl> subtasks = new ArrayList<>();
//
//            SiteTable siteTable = new SiteTable();
//            siteTable.setName(site.getName());
//            siteTable.setStatus(Status.INDEXING);
//            siteTable.setUrl(site.getUrl());
//
//
//
//           Set<String> uniquePaths = new HashSet<>();
//
//            for (Element element : linkElements) {
//                String linkUrl = element.attr("abs:href");
//                PageTable pageTable = new PageTable();
//
//                if (linkUrl.startsWith(site.getUrl())
//                        && linkUrl.endsWith("/")
//                        && !linkUrl.contains("#")
//                        ) {
//
//                    uniquePaths.add(linkUrl);
//
//                    String relativePath = linkUrl.replace(siteTable.getUrl(), "/");
//
//                    pageTable.setPath(relativePath);
//                    pageTable.setContent(element.html());
//                    pageTable.setCode(200);
//                    pageTable.setSiteId(siteTable);
//                    siteTable.setStatusTime(LocalDateTime.now());
//                    linksList.add(pageTable);
//                }
//            }
//            siteTable.setStatus(Status.INDEXED);
//            pageRepository.saveAllAndFlush(linksList);
//        }
//        IndexingResponse response = new IndexingResponse();
//        response.setResult(true);
//
//        return response;
//    }
}
