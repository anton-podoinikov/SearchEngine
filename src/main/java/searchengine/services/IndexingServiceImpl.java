package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.parsing.Link;
import searchengine.parsing.ParseHtml;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.morphology.LemmaFinderImpl;


import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

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
    private final List<Thread> indexingThreads;
    private ForkJoinPool forkJoinPool;
    private final Object lock = new Object();

    @Override
    public IndexingResponse startIndexing() {
        synchronized (lock) {
            if (forkJoinPool != null && !forkJoinPool.isTerminated()) {
                return new IndexingResponse(false, "Индексация уже запущена");
            }
            initializeThreadPool();
        }
        new Thread(this::startIndexingInternal).start();
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse startIndexingUrl(String url) {
        synchronized (lock) {
            String domain = "https://" + urlToParentUrl(url) + "/";
            SiteTable existingSite = siteRepository.findByUrl(domain);
            PageTable existingPage = pageRepository.findByPath(url);

            if (forkJoinPool != null && !forkJoinPool.isTerminated()) {
                return new IndexingResponse(false, "Indexing is already in progress");
            }

            if (!isValidUrl(url)) {
                log.error("Invalid URL: " + url);
                return new IndexingResponse(false, "Invalid URL");
            }

            if (existingPage != null) {
                deleteRelatedData(existingPage);
            }

            if (existingSite == null) {
                existingSite = createSiteTable(url);
            }

            initializeThreadPool();
            SiteTable finalExistingSite = existingSite;
            new Thread(() -> indexSinglePage(finalExistingSite, url)).start();
            return new IndexingResponse(true);
        }
    }

    private void indexSinglePage(SiteTable siteTable, String url) {
        try {
            ParseHtml parseHtml = new ParseHtml(url, siteTable, siteRepository, pageRepository);
            forkJoinPool.invoke(parseHtml);
            Set<Link> links = new CopyOnWriteArraySet<>(parseHtml.getLinks());
            Set<PageTable> pageTables = new CopyOnWriteArraySet<>();

            links.stream()
                    .filter(link -> isPageFromIndexedSite(link.getPath(), siteTable))
                    .forEach(link -> {
                        PageTable pageTable = new PageTable();
                        pageTable.setSiteId(siteTable);
                        pageTable.setContent(link.getContent());
                        pageTable.setPath(link.getPath());
                        pageTable.setCode(link.getCode());
                        pageTables.add(pageTable);
                    });

            savePageInRepository(pageTables);
            saveLemmaAndIndex(siteTable);
            parseHtml.clearPages();
        } catch (Exception exception) {
            log.info(exception.getMessage());
        } finally {
            synchronized (lock) {
                if (forkJoinPool != null) {
                    forkJoinPool.shutdownNow();
                    forkJoinPool = null;
                }
            }
        }
    }

    private void saveSaitInRepository() {
        List<Site> siteList = sites.getSites();
        for (Site site : siteList) {
            checkRepeatInBase(site);
            SiteTable siteTable = new SiteTable();
            siteTable.setUrl(site.getUrl());
            siteTable.setName(site.getName());
            siteTable.setStatusTime(LocalDateTime.now());
            siteTable.setStatus(Status.INDEXING);
            siteRepository.save(siteTable);
        }
    }

    @Override
    public IndexingResponse stopIndexing() {
        try {
            indexingThreads.forEach(Thread::interrupt);
            forkJoinPool.shutdownNow();
            return new IndexingResponse(true);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new IndexingResponse(false, "Произошла ошибка при остановке индексирования.");
        }
    }


    private void indexSite(SiteTable siteTable) {
        ParseHtml parseHtml = new ParseHtml(siteTable.getUrl(), siteTable, siteRepository, pageRepository);
        forkJoinPool.invoke(parseHtml);
        Set<Link> links = new CopyOnWriteArraySet<>(parseHtml.getLinks());
        Set<PageTable> pageTables = Collections.synchronizedSet(new HashSet<>());
        try {
            for (Link link : links) {
                if (link.getPath().startsWith(siteTable.getUrl())) {
                    PageTable pageTable = new PageTable();
                    pageTable.setSiteId(siteTable);
                    pageTable.setContent(link.getContent());
                    pageTable.setPath(link.getPath());
                    pageTable.setCode(link.getCode());
                    pageTables.add(pageTable);
                }
            }
            savePageInRepository(pageTables);
            saveLemmaAndIndex(siteTable);
        } catch (Exception exception) {
            log.info(exception.getMessage());
        }
    }


    private void savePageInRepository(Set<PageTable> pageTables) {
        pageRepository.saveAll(pageTables);
    }

    private void saveLemmaAndIndex(SiteTable siteTable) {
        setLemmaAndIndex(siteTable);
    }


    private void setLemmaAndIndex(SiteTable siteTable) {
        Map<String, LemmaTable> lemmaTableMap = new ConcurrentHashMap<>();
        List<IndexTable> indexTables = new CopyOnWriteArrayList<>();

        pageRepository.findAllBySiteId(siteTable)
                .stream()
                .filter(pageTable -> pageTable.getCode() == 200)
                .forEach(pageTable -> {
                    String content = pageTable.getContent();
                    Map<String, Integer> lemmas = lemmaFinder.collectLemmas(content);

                    lemmas.forEach((key, value) -> {
                        LemmaTable existingLemmaTable = lemmaTableMap.get(key);
                        if (existingLemmaTable == null) {
                            LemmaTable newLemmaTable = new LemmaTable();
                            newLemmaTable.setLemma(key);
                            newLemmaTable.setSiteId(siteTable);
                            newLemmaTable.setFrequency(1);
                            lemmaTableMap.put(key, newLemmaTable);
                        } else {
                            existingLemmaTable.setFrequency(existingLemmaTable.getFrequency() + 1);
                        }

                        IndexTable indexTable = new IndexTable();
                        indexTable.setLemma(lemmaTableMap.get(key));
                        indexTable.setPage(pageTable);
                        indexTable.setRank(value);
                        indexTables.add(indexTable);
                    });
                });

        lemmaRepository.saveAll(lemmaTableMap.values());
        indexRepository.saveAll(indexTables);

        siteTable.setStatus(Status.INDEXED);
        siteRepository.save(siteTable);
    }


    private SiteTable createSiteTable(String url) {
        SiteTable siteTable = new SiteTable();
        String domain = urlToParentUrl(url);

            siteTable.setUrl("https://" + domain + "/");
            siteTable.setName(domain.split("\\.")[0]);
            siteTable.setStatusTime(LocalDateTime.now());
            siteTable.setStatus(Status.INDEXING);

        return siteTable;
    }

//    private void setLemmaAndIndex(SiteTable siteTable) {
//        Map<String, LemmaTable> lemmaTableMap = new ConcurrentHashMap<>();
//        List<IndexTable> indexTables = new CopyOnWriteArrayList<>();
//
//        pageRepository.findAllBySiteId(siteTable)
//                .stream()
//                .filter(pageTable -> pageTable.getCode() == 200)
//                .forEach(pageTable -> {
//                    String content = pageTable.getContent();
//                    Map<String, Integer> lemmas = lemmaFinder.collectLemmas(content);
//
//                    lemmas.forEach((key, value) -> {
//                        lemmaTableMap.compute(key, (k, existingLemmaTable) -> {
//                            if (existingLemmaTable == null) {
//                                LemmaTable newLemmaTable = new LemmaTable();
//                                newLemmaTable.setLemma(k);
//                                newLemmaTable.setSiteId(siteTable);
//                                newLemmaTable.setFrequency(value);
//                                return newLemmaTable;
//                            } else {
//                                AtomicInteger frequency = new AtomicInteger(existingLemmaTable.getFrequency());
//                                frequency.addAndGet(value);
//                                existingLemmaTable.setFrequency(frequency.get());
//                                return existingLemmaTable;
//                            }
//                        });
//
//                        LemmaTable lemmaTable = lemmaTableMap.get(key);
//
//                        IndexTable indexTable = new IndexTable();
//                        indexTable.setLemma(lemmaTable);
//                        indexTable.setPage(pageTable);
//                        indexTable.setRank(lemmaTable.getFrequency());
//                        indexTables.add(indexTable);
//                    });
//                });
//
//        lemmaRepository.saveAll(lemmaTableMap.values());
//        indexRepository.saveAll(indexTables);
//
//        siteTable.setStatus(Status.INDEXED);
//        siteRepository.save(siteTable);
//    }


//    private void setLemmaAndIndex(SiteTable siteTable) {
//        Map<String, LemmaTable> lemmaTableMap = new ConcurrentHashMap<>();
//        List<IndexTable> indexTables = new CopyOnWriteArrayList<>();
//
//        pageRepository.findAllBySiteId(siteTable)
//                .stream()
//                .filter(pageTable -> pageTable.getCode() == 200)
//                .forEach(pageTable -> {
//                    String content = pageTable.getContent();
//                    Map<String, Integer> lemmas = lemmaFinder.collectLemmas(content);
//
//                    lemmas.forEach((key, value) -> {
//                        LemmaTable lemmaTable = lemmaTableMap.computeIfAbsent(key, k -> {
//                            LemmaTable newLemmaTable = new LemmaTable();
//                            newLemmaTable.setLemma(k);
//                            newLemmaTable.setSiteId(siteTable);
//                            return newLemmaTable;
//                        });
//
//                        synchronized (lemmaTable) {
//                            lemmaTable.setFrequency(lemmaTable.getFrequency() + value);
//                        }
//
//                        IndexTable indexTable = new IndexTable();
//                        indexTable.setLemma(lemmaTable);
//                        indexTable.setPage(pageTable);
//                        indexTable.setRank(lemmaTable.getFrequency());
//
//                        synchronized (indexTables) {
//                            indexTables.add(indexTable);
//                        }
//                    });
//                });
//
//        lemmaRepository.saveAll(lemmaTableMap.values());
//        indexRepository.saveAll(indexTables);
//
//        siteTable.setStatus(Status.INDEXED);
//        siteRepository.save(siteTable);
//    }


//        pageRepository.findAllBySiteId(siteTable).forEach(pageTable -> {
//            if (pageTable.getCode() == 200) {
//                String content = pageTable.getContent();
//                Map<String, Integer> lemmas = lemmaFinder.collectLemmas(content);
//                lemmas.forEach((key, value) -> {
//                    if (lemmaRepository.findByLemmaAAndSiteId(key, siteTable) == null) {
//                        LemmaTable lemmaTable = new LemmaTable();
//                        IndexTable indexTable = new IndexTable();
//                        lemmaTable.setLemma(key);
//                        lemmaTable.setFrequency(value);
//                        lemmaTable.setSiteId(siteTable);
//                        indexTable.setPage(pageTable);
//                        indexTable.setLemma(lemmaTable);
//                        indexTable.setRank(value);
//                        lemmaRepository.save(lemmaTable);
//                        indexRepository.save(indexTable);
//                    } else {
//                        LemmaTable lemmaTable = lemmaRepository.findByLemmaAAndSiteId(key, siteTable);
//                        lemmaTable.setFrequency(lemmaTable.getFrequency() + value);
//                        IndexTable indexTable = new IndexTable();
//                        indexTable.setLemma(lemmaTable);
//                        indexTable.setPage(pageTable);
//                        indexTable.setRank(lemmaTable.getFrequency());
//                        lemmaRepository.save(lemmaTable);
//                        indexRepository.save(indexTable);
//                    }
//                });
//            }
//        });

    private void checkRepeatInBase(Site site) {
        if (siteRepository.findByUrl(site.getUrl()) != null) {
            deleteSite(siteRepository.findByUrl(site.getUrl()));
        }
    }

    private void startIndexingInternal() {
        try {
            saveSaitInRepository();
            for (SiteTable siteTable : siteRepository.findAll()) {
                Thread indexingThread = new Thread(() -> indexSite(siteTable));
                indexingThreads.add(indexingThread);
                indexingThread.start();
                log.info("Старт индексации: сайт - {}", siteTable.getName());
            }

            for (Thread indexingThread : indexingThreads) {
                try {
                    indexingThread.join();
                } catch (InterruptedException e) {
                    log.error("Произошла ошибка при ожидании завершения потока индексации: " + e.getMessage());
                }
            }

        } catch (Exception exception) {
            log.error(exception.getMessage());
        } finally {
            synchronized (lock) {
                forkJoinPool.shutdownNow();
                forkJoinPool = null;
            }
        }
    }

    private void deleteSite(SiteTable siteTable) {
        siteRepository.deleteByUrl(siteTable.getUrl());
    }

    private boolean isValidUrl(String url) {
        List<Site> urlList = sites.getSites();
        return urlList.stream().anyMatch(s -> url.startsWith(s.getUrl()));
    }

    private void handleMainPageUnavailable(String siteUrl, SiteTable siteTable) {
        log.error("Главная страница сайта " + siteUrl + " недоступна.");
        setSiteAsFailed(siteTable);
    }

    private void setSiteAsFailed(SiteTable siteTable) {
        siteTable.setStatus(Status.FAILED);
        siteTable.setLastError("Главная страница недоступна");
    }

    private void initializeThreadPool() {
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }

    private boolean isPageFromIndexedSite(String pagePath, SiteTable siteTable) {
        return pagePath.startsWith(siteTable.getUrl());
    }

    private void deleteRelatedData(PageTable existingPage) {
        lemmaRepository.deleteBySiteId(existingPage.getSiteId());
        pageRepository.deleteBySiteId(existingPage.getSiteId());
    }

    private String urlToParentUrl(String url) {
        URL urlObject;
        try {
            urlObject = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return urlObject.getHost();
    }
}