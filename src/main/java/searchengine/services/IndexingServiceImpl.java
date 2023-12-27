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
    private final String HTTP_PREFIX = "https://";
    private final String SLASH = "/";

    @Override
    public IndexingResponse startIndexing() {
        synchronized (lock) {
            if (forkJoinPool != null && !forkJoinPool.isTerminated()) {
                return new IndexingResponse(false, "Индексирование уже выполняется.");
            }
            initializeThreadPool();
        }
        new Thread(this::startIndexingInternal).start();
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse startIndexingUrl(String url) {
        synchronized (lock) {
            String domain = HTTP_PREFIX + urlToParentUrl(url) + SLASH;
            SiteTable existingSite = siteRepository.findByUrl(domain);
            PageTable existingPage = pageRepository.findByPath(url);

            if (forkJoinPool != null && !forkJoinPool.isTerminated()) {
                return new IndexingResponse(false, "Индексирование уже выполняется.");
            }

            if (!isValidUrl(url)) {
                log.error("Invalid URL: " + url);
                return new IndexingResponse(false, "Неверная ссылка.");
            }

            if (existingPage != null) {
                deleteRelatedData(existingPage);
            }

            if (existingSite == null) {
                existingSite = createSiteInTableForSinglePage(url);
            }

            initializeThreadPool();
            SiteTable finalExistingSite = existingSite;
            new Thread(() -> indexSinglePage(finalExistingSite, url)).start();
            return new IndexingResponse(true);
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
        for (Link link : links) {
            if (link.getPath().startsWith(siteTable.getUrl())) {
                PageTable pageTable = new PageTable();
                pageTable.setSiteId(siteTable);
                pageTable.setContent(link.getContent());
                pageTable.setPath(absoluteToRelative(siteTable.getUrl(), link.getPath()));
                pageTable.setCode(link.getCode());
                pageTables.add(pageTable);
            }
        }
        savePageInRepository(pageTables);
        setLemmaAndIndexSite(siteTable);
        siteTable.setStatus(Status.INDEXED);
        siteRepository.save(siteTable);
    }

    private void saveSitesInRepository() {
        List<Site> siteList = sites.getSites();
        for (Site site : siteList) {
            checkRepeatInBase(site);
            SiteTable siteTable = createSite(site);
            siteRepository.save(siteTable);
        }
    }

    private void indexSinglePage(SiteTable siteTable, String url) {
        try {
            ParseHtml parseHtml = new ParseHtml(url, siteTable, siteRepository, pageRepository);
            forkJoinPool.invoke(parseHtml);
            Set<Link> links = new CopyOnWriteArraySet<>(parseHtml.getLinks());

            Optional<Link> targetLink = links.stream()
                    .filter(link -> link.getPath().equals(url))
                    .findFirst();

            targetLink.ifPresent(link -> {
                PageTable pageTable = new PageTable();
                pageTable.setSiteId(siteTable);
                pageTable.setContent(link.getContent());
                pageTable.setPath(absoluteToRelative(siteTable.getUrl(), link.getPath()));
                pageTable.setCode(link.getCode());

                savePageInRepository(Set.of(pageTable));
                setLemmaAndIndexPage(pageTable);
            });

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

    private SiteTable createSite(Site site) {
        SiteTable siteTable = new SiteTable();
        siteTable.setUrl(site.getUrl());
        siteTable.setName(site.getName());
        siteTable.setStatusTime(LocalDateTime.now());
        siteTable.setStatus(Status.INDEXING);
        return siteTable;
    }


    private void savePageInRepository(Set<PageTable> pageTables) {
        pageRepository.saveAll(pageTables);
    }

    private void setLemmaAndIndexPage(PageTable pageTable) {
        Map<String, LemmaTable> lemmaTableMap = new ConcurrentHashMap<>();
        List<IndexTable> indexTables = new CopyOnWriteArrayList<>();

        if (pageTable.getCode() == 200) {
            String content = pageTable.getContent();
            Map<String, Integer> lemmas = lemmaFinder.collectLemmas(content);

            lemmas.forEach((key, value) -> {
                LemmaTable existingLemmaTable = lemmaTableMap.get(key);
                if (existingLemmaTable == null) {
                    LemmaTable newLemmaTable = new LemmaTable();
                    newLemmaTable.setLemma(key);
                    newLemmaTable.setSiteId(pageTable.getSiteId());
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
        }

        lemmaRepository.saveAll(lemmaTableMap.values());
        indexRepository.saveAll(indexTables);
    }


    private void setLemmaAndIndexSite(SiteTable siteTable) {
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
    }

    private SiteTable createSiteInTableForSinglePage(String url) {
        SiteTable siteTable = new SiteTable();
        String domain = urlToParentUrl(url);

        siteTable.setUrl(HTTP_PREFIX + domain + SLASH);
        siteTable.setName(domain.split("\\.")[0]);
        siteTable.setStatusTime(LocalDateTime.now());
        siteTable.setStatus(Status.INDEXING);

        return siteTable;
    }

    private void checkRepeatInBase(Site site) {
        if (siteRepository.findByUrl(site.getUrl()) != null) {
            deleteSite(siteRepository.findByUrl(site.getUrl()));
        }
    }

    private void startIndexingInternal() {
        try {
            saveSitesInRepository();
            List<Thread> indexingThreads = new ArrayList<>();
            for (SiteTable siteTable : siteRepository.findAll()) {
                Thread indexingThread = new Thread(() -> indexSite(siteTable));
                indexingThreads.add(indexingThread);
                indexingThread.start();
            }

            for (Thread indexingThread : indexingThreads) {
                indexingThread.join();
            }

        } catch (InterruptedException e) {
            log.error("An error occurred while waiting for the indexing thread to complete: " + e.getMessage());
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

    private void initializeThreadPool() {
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }

    private void deleteRelatedData(PageTable existingPage) {
        List<IndexTable> indexTables = indexRepository.findAllByPageId(existingPage.getId());

        List<LemmaTable> lemmaTables = indexTables.stream().map(IndexTable::getLemma).toList();

        indexTables.forEach(indexTable -> indexRepository.deleteByPageId(existingPage.getId()));

        lemmaTables.forEach(lemmaTable -> {
            Optional<LemmaTable> optionalLemma = lemmaRepository.findById(lemmaTable.getId());
            optionalLemma.ifPresent(lemma -> {
                int newFrequency = lemma.getFrequency() - 1;
                if (newFrequency <= 0) {
                    lemmaRepository.delete(lemma);
                } else {
                    lemma.setFrequency(newFrequency);
                    lemmaRepository.save(lemma);
                }
            });
        });

        pageRepository.deleteById(existingPage.getId());
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

    private String absoluteToRelative(String baseUrl, String absoluteUrl) {
        if (absoluteUrl.startsWith(baseUrl)) {
            return absoluteUrl.substring(baseUrl.length());
        }
        return absoluteUrl;
    }
}