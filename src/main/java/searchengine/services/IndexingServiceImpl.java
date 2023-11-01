package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.parsing.ParseHtml;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteTable;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime()
            .availableProcessors());

    @Override
    @Transactional
    public IndexingResponse startIndexing() {
        List<Site> siteList = sites.getSites();
        for (Site site : siteList) {
            SiteTable existingSite = siteRepository.findByUrl(site.getUrl());
            if (existingSite != null) {
                deleteSiteData(existingSite);
            }
            executorService.submit(() -> {
                SiteTable siteTable = new SiteTable();
                try {
                    siteTable.setUrl(site.getUrl());
                    siteTable.setName(site.getName());
                    siteTable.setStatus(Status.INDEXING);
                    siteTable.setStatusTime(LocalDateTime.now());
                    siteRepository.saveAndFlush(siteTable);
                    if (!isMainPageAvailable(site.getUrl())) {
                        log.error("Главная страница сайта " + site.getUrl() + " недоступна.");
                        setSiteAsFailed(siteTable);
                        return;
                    }
                    ParseHtml parseHtml = new ParseHtml(site.getUrl(), siteTable, siteRepository);
                    pool.invoke(new ParseHtml(site.getUrl(), siteTable, siteRepository));
                    pageRepository.saveAllAndFlush(parseHtml.getPageTable());
                } catch (Exception exception) {
                    log.error(exception.getMessage());
                    siteTable.setStatus(Status.FAILED);
                    siteTable.setLastError("Ошибка индексации: " + exception.getMessage());
                } finally {
                    siteRepository.saveAndFlush(siteTable);
                }
            });
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        executorService.shutdownNow();
        pool.shutdownNow();
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse indexPage(String url) {

        return new IndexingResponse(true);
    }

    private boolean isMainPageAvailable(String mainPageUrl) {
        try {
            Document mainPage = Jsoup.connect(mainPageUrl)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                            "AppleWebKit/537.36 (KHTML, like Gecko)" +
                            "Chrome/58.0.3029.110 Safari/537.3")
                    .get();
            int statusCode = mainPage.connection().response().statusCode();
            return statusCode == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private void setSiteAsFailed(SiteTable siteTable) {
        siteTable.setStatus(Status.FAILED);
        siteTable.setLastError("Главная страница недоступна");
    }

    @Transactional
    public void deleteSiteData(SiteTable siteTable) {
        pageRepository.deleteBySiteId(siteTable.getId());
        siteRepository.deleteByUrl(siteTable.getUrl());
    }
}