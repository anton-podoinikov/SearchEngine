package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private SiteTable siteTable;
    @Override
    public IndexingResponse startIndexing() {
        new Thread(() -> {
            List<Site> siteList = sites.getSites();
            for (Site site : siteList) {
                try {
                    siteTable = new SiteTable();
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
                    siteRepository.saveAndFlush(siteTable);

                } catch (Exception exception) {
                    log.error(exception.getMessage());
                    siteTable.setStatus(Status.FAILED);
                    siteTable.setLastError("Ошибка индексации");
                    siteRepository.saveAndFlush(siteTable);
                }
            }
        }).start();
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexing() {
        return new IndexingResponse(true);
    }
}