package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

import java.io.IOException;
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
            // Проверка, что индексация не запущена
            if (forkJoinPool != null && !forkJoinPool.isTerminated()) {
                return new IndexingResponse(false, "Индексация уже запущена");
            }

            // Проверка, что переданный URL валиден
            if (!isValidUrl(url)) {
                log.error("Недопустимый URL: " + url);
                return new IndexingResponse(false, "Недопустимый URL");
            }

//            SiteTable existingSite = siteRepository.findByUrl(url);
//
//            // Проверка, что сайт существует в базе данных
//            if (existingSite == null) {
//                log.error("Сайт с URL " + url + " не найден в базе данных.");
//                return new IndexingResponse(false, "Сайт не найден в базе данных");
//            }

            // Проверка, что страница не была уже проиндексирована
            PageTable existingPage = pageRepository.findByPath(url);

            if (existingPage != null) {
                // Удаление связанных данных
//                indexRepository.deleteByPageId(existingPage.getId());
                lemmaRepository.deleteBySiteId(existingPage.getSiteId().getId());
                pageRepository.delete(existingPage);
            }

            SiteTable siteTable = new SiteTable();

            try {
                URL urlObject = new URL(url);
                String domain = urlObject.getHost();
                siteTable.setUrl("https://" + domain);
                siteTable.setName(domain.split("\\.")[0]);
                siteTable.setStatusTime(LocalDateTime.now());
                siteTable.setStatus(Status.INDEXING);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            // Запуск индексации только для этой страницы
            initializeThreadPool();
            new Thread(() -> indexSinglePage(siteTable, url)).start();

            return new IndexingResponse(true);
        }
    }

    private void indexSinglePage(SiteTable siteTable, String url) {
        try {
            // Индексация одной страницы
            ParseHtml parseHtml = new ParseHtml(url, siteTable, siteRepository);
            forkJoinPool.invoke(parseHtml);
            Set<Link> links = Collections.synchronizedSet(new HashSet<>(parseHtml.getLinks()));
            Set<PageTable> pageTables = Collections.synchronizedSet(new HashSet<>());

            for (Link link : links) {
                if (isPageFromIndexedSite(link.getPath(), siteTable)) {
                    PageTable pageTable = new PageTable();
                    pageTable.setSiteId(siteTable);
                    pageTable.setContent(link.getContent());
                    pageTable.setPath(link.getPath());
                    pageTable.setCode(link.getCode());
                    pageTables.add(pageTable);
                } else {
                    log.error("Попытка индексации страницы с другого сайта: " + link.getPath());
                    // Вернуть ошибку, например, через API или бросить исключение
                }
            }

            savePageInRepository(pageTables, siteTable);
            parseHtml.clearPages();
        } catch (Exception exception) {
            log.info(exception.getMessage());
        } finally {
            synchronized (lock) {
                forkJoinPool.shutdownNow();
                forkJoinPool = null;
            }
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

    private void indexSite(SiteTable siteTable) {
        ParseHtml parseHtml = new ParseHtml(siteTable.getUrl(), siteTable, siteRepository);
        forkJoinPool.invoke(parseHtml);
        Set<Link> links = Collections.synchronizedSet(new HashSet<>(parseHtml.getLinks()));
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
            savePageInRepository(pageTables, siteTable);
            parseHtml.clearPages();
        } catch (Exception exception) {
            log.info(exception.getMessage());
        }
    }

    private void savePageInRepository(Set<PageTable> pageTables, SiteTable siteTable) {
        pageRepository.saveAll(pageTables);
        siteTable.setStatus(Status.INDEXED);
        siteRepository.save(siteTable);
        setLemmaAndIndex(siteTable);
    }

    private void setLemmaAndIndex(SiteTable siteTable) {
        for (PageTable pageTable : pageRepository.findAll()) {
            try {
                Map<String, Integer> lemmas = lemmaFinder.collectLemmas(pageTable.getContent());
                List<LemmaTable> lemmaTables = new ArrayList<>();
                List<IndexTable> indexTables = new ArrayList<>();

                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
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
                        lemmaTables.add(existingLemma);
                    }
                    IndexTable indexTable = new IndexTable();
                    indexTable.setLemma(existingLemma);
                    indexTable.setRank(frequency);
                    indexTable.setPage(pageTable);
                    indexTables.add(indexTable);
                }
                lemmaRepository.saveAll(lemmaTables);
                indexRepository.saveAll(indexTables);
                lemmaTables.clear();
                indexTables.clear();
            } catch (Exception exception) {
                log.error("An error occurred while processing the page: " + exception.getMessage());
            }
        }
    }

    private void checkRepeatInBase(Site site) {
        if (siteRepository.findByUrl(site.getUrl()) != null) {
            deleteSite(siteRepository.findByUrl(site.getUrl()));
        }
    }

    private void deleteSite(SiteTable siteTable) {
        siteRepository.deleteByUrl(siteTable.getUrl());
    }

    private boolean isValidUrl(String url) {
        List<Site> urlList = sites.getSites();
        return urlList.stream().anyMatch(s -> url.startsWith(s.getUrl()));
    }

    //TODO вынести userAgent в конфиг.
    private boolean isMainPageAvailable(String mainPageUrl) {
        try {
            Document mainPage = Jsoup.connect(mainPageUrl)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)" +
                            "AppleWebKit/537.36 (HTML, like Gecko)" +
                            "Chrome/58.0.3029.110 Safari/537.3")
                    .get();
            int statusCode = mainPage.connection().response().statusCode();
            return statusCode == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private void handleMainPageUnavailable(String siteUrl, SiteTable siteTable) {
        log.error("Главная страница сайта " + siteUrl + " недоступна.");
        setSiteAsFailed(siteTable);
    }

    private void setSiteAsFailed(SiteTable siteTable) {
        siteTable.setStatus(Status.FAILED);
        siteTable.setLastError("Главная страница недоступна");
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

    private void initializeThreadPool() {
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }

    private boolean isPageFromIndexedSite(String pagePath, SiteTable siteTable) {
        return pagePath.startsWith(siteTable.getUrl());
    }
}