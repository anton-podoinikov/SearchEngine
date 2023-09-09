package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<IndexingResponse> startIndexing() {
        List<Site> siteList = sites.getSites();
        try {
            for (Site site : siteList) {
                SiteTable siteTable = new SiteTable();
                siteTable.setUrl(site.getUrl());
                siteTable.setName(site.getName());
                siteTable.setStatus(Status.INDEXING);
                siteTable.setStatusTime(LocalDateTime.now());
                siteRepository.saveAndFlush(siteTable);

                ParseHtml parseHtml = new ParseHtml(site.getUrl(), siteTable);
                ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime()
                        .availableProcessors());
                pool.invoke(new ParseHtml(site.getUrl(), siteTable));
                pageRepository.saveAllAndFlush(parseHtml.getPageTable());
            }
            return CompletableFuture.completedFuture(new IndexingResponse(true));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            return CompletableFuture.completedFuture(new IndexingResponse(false
                    , "Ошибка индексации: главная страница сайта не доступна"));
        }
    }

    @Override
    public IndexingResponse stopIndexing() {
        return new IndexingResponse(true);
    }
}