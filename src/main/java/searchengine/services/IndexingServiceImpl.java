package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import searchengine.parsing.ParseHtml;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.splitter.LemmaFinderImpl;

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
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaFinderImpl lemmaFinder;
    private ExecutorService executorService;
    private final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime()
            .availableProcessors());

    @Override
    @Transactional
    public IndexingResponse startIndexing() {
        executorService = Executors.newFixedThreadPool(1);

        List<Site> siteList = sites.getSites();

        for (Site site : siteList) {
            SiteTable existingSite = siteRepository.findByUrl(site.getUrl());
            if (existingSite != null) {
                deleteSiteData(existingSite);
            }
            executorService.submit(() -> indexSite(site));
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
    public IndexingResponse startIndexingUrl(String url) {
        if (urlValid(url)) {

        }
        return new IndexingResponse(true);
    }

    @Transactional
    public void deleteSiteData(SiteTable siteTable) {
        pageRepository.deleteBySiteId(siteTable.getId());
        siteRepository.deleteByUrl(siteTable.getUrl());
    }

    private boolean urlValid(String url) {
        List<Site> urlList = sites.getSites();
        for (Site s : urlList) {
            if (s.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    private void indexSite(Site site) {
        SiteTable siteTable = new SiteTable();
        try {
            siteTable.setUrl(site.getUrl());
            siteTable.setName(site.getName());
            siteTable.setStatus(Status.INDEXING);
            siteTable.setStatusTime(LocalDateTime.now());
            siteRepository.saveAndFlush(siteTable);

            if (!isMainPageAvailable(site.getUrl())) {
                handleMainPageUnavailable(site.getUrl(), siteTable);
                return;
            }

            ParseHtml parseHtml = new ParseHtml(site.getUrl()
                    ,siteTable
                    ,siteRepository
                    ,lemmaRepository
                    ,indexRepository
                    ,lemmaFinder);
            pool.invoke(new ParseHtml(site.getUrl()
                    ,siteTable
                    ,siteRepository
                    ,lemmaRepository
                    ,indexRepository
                    ,lemmaFinder));

            List<PageTable> pageTables = parseHtml.getPageTable();
            pageRepository.saveAllAndFlush(pageTables);

            for (PageTable pageTable : pageTables) {
                try {
                    HashMap<String, Integer> lemma = lemmaFinder.collectLemmas(pageTable.getContent());
                    for (Map.Entry<String, Integer> entry : lemma.entrySet()) {
                        String lemmaText = entry.getKey();
                        int frequency = entry.getValue();

                        LemmaTable existingLemma = lemmaRepository.findByLemma(lemmaText);
                        if (existingLemma != null) {
                            existingLemma.setFrequency(existingLemma.getFrequency() + frequency);
                        } else {
                            existingLemma = new LemmaTable();
                            existingLemma.setSiteId(siteTable);
                            existingLemma.setLemma(lemmaText);
                            existingLemma.setFrequency(frequency);
                            lemmaRepository.saveAndFlush(existingLemma);
                        }

                        IndexTable indexTable = new IndexTable();
                        indexTable.setLemma(existingLemma);
                        indexTable.setRank(frequency);
                        indexTable.setPage(pageTable);
                        indexRepository.saveAndFlush(indexTable);

                    }
                } catch (Exception ex) {
                    log.error("Произошла ошибка при обработке страницы: " + ex.getMessage());
                }
            }
        } catch (Exception exception) {
            handleIndexingError(exception, siteTable);
        } finally {
            siteRepository.saveAndFlush(siteTable);
        }
    }

    private void handleMainPageUnavailable(String siteUrl, SiteTable siteTable) {
        log.error("Главная страница сайта " + siteUrl + " недоступна.");
        setSiteAsFailed(siteTable);
    }

    private void handleIndexingError(Exception exception, SiteTable siteTable) {
        log.error(exception.getMessage());
        siteTable.setStatus(Status.FAILED);
        siteTable.setLastError("Ошибка индексации: " + exception.getMessage());
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
}