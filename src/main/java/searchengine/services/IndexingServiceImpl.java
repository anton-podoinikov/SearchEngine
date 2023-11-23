package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
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

import javax.annotation.PreDestroy;
import java.io.IOException;
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
    private ExecutorService executorService;
    private ForkJoinPool pool;


    //TODO Доработать так что бы после остановки индексации, метод startIndexing смог снова запуститься.
    @Override
    public IndexingResponse startIndexing() {

        executorService = Executors.newFixedThreadPool(1);
        pool = new ForkJoinPool(Runtime.getRuntime()
                .availableProcessors());

        List<Site> siteList = sites.getSites();

        try {
            for (Site site : siteList) {

                if (siteRepository.findByUrl(site.getUrl()) != null) {
                    deleteSite(siteRepository.findByUrl(site.getUrl()));
                }

                SiteTable siteTable = new SiteTable();
                siteTable.setUrl(site.getUrl());
                siteTable.setName(site.getName());
                siteTable.setStatus(Status.INDEXING);
                siteTable.setStatusTime(LocalDateTime.now());

                siteRepository.saveAndFlush(siteTable);
            }

            for (SiteTable siteTable : siteRepository.findAll()) {
                executorService.submit(() -> indexSite(siteTable));
            }

        } catch (Exception e) {
            log.error("Произошла ошибка во время индексации: " + e.getMessage());
            return new IndexingResponse(false);
        }
        return new IndexingResponse(true);
    }


    //TODO Метод не работат должным образом, разобраться почему и решить проблему!!!
    @Override
    public IndexingResponse startIndexingUrl(String url) {
        if (urlValid(url)) {
            SiteTable existingSite = siteRepository.findByUrl(url);

            if (existingSite == null) {
                log.error("Сайт с URL " + url + " не найден в базе данных.");
                return new IndexingResponse(false);
            }

            Site site = new Site();
            site.setUrl(url);
            site.setName(existingSite.getName());

            return new IndexingResponse(true);
        } else {
            log.error("Недопустимый URL: " + url);
            return new IndexingResponse(false);
        }
    }


    //TODO Доработать метод stopIndexing.
    @Override
    @PreDestroy
    public IndexingResponse stopIndexing() {
        try {
            executorService.shutdownNow();
            pool.shutdownNow();
            boolean executorTerminated = executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            boolean poolTerminated = pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            if (executorTerminated && poolTerminated) {
                log.info("Индексирование успешно остановлено.");
                return new IndexingResponse(true);
            } else {
                log.error("Прекращение индексации не завершилось в течение ожидаемого времени.");
                return new IndexingResponse(false, "Прекращение индексации не завершилось в течение ожидаемого времени.");
            }
        } catch (InterruptedException e) {
            log.error("Произошла ошибка при остановке индексирования.: " + e.getMessage());
            return new IndexingResponse(false, "Произошла ошибка при остановке индексирования.: " + e.getMessage());
        }
    }

    public void deleteSite(SiteTable siteTable) {
        siteRepository.deleteByUrl(siteTable.getUrl());
    }


    //TODO Разделить метод indexSize на более мелкие методы.
    private void indexSite(SiteTable siteTable) {

        if (!isMainPageAvailable(siteTable.getUrl())) {
            handleMainPageUnavailable(siteTable.getUrl(), siteTable);
            return;
        }

        try {
            ParseHtml parseHtml = new ParseHtml(siteTable.getUrl()
                    , siteTable
                    , siteRepository
                    , lemmaRepository
                    , indexRepository
                    , pageRepository
                    , lemmaFinder);

            pool.invoke(new ParseHtml(siteTable.getUrl()
                    , siteTable
                    , siteRepository
                    , lemmaRepository
                    , indexRepository
                    , pageRepository
                    , lemmaFinder));

            List<PageTable> pageTables = parseHtml.getPageTable();

            pageRepository.saveAll(pageTables);
            siteRepository.save(siteTable);

            setLemmaAndIndex(siteTable);

        } catch (Exception exception) {
            handleIndexingError(exception, siteTable);
        }
    }

    private void handleMainPageUnavailable(String siteUrl, SiteTable siteTable) {
        log.error("Главная страница сайта " + siteUrl + " недоступна.");
        setSiteAsFailed(siteTable);
    }

    private boolean urlValid(String url) {
        List<Site> urlList = sites.getSites();
        return urlList.stream().anyMatch(s -> url.startsWith(s.getUrl()));
    }

    private void handleIndexingError(Exception exception, SiteTable siteTable) {
        log.error(exception.getMessage());
        siteTable.setStatus(Status.FAILED);
        siteTable.setLastError("Ошибка индексации: " + exception.getMessage());
    }

    //TODO вынести userAgent в конфиг.
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

    private void setLemmaAndIndex(SiteTable siteTable){
        for (PageTable pageTable : pageRepository.findAll()) {
            try {
                HashMap<String, Integer> lemma = lemmaFinder.collectLemmas(pageTable.getContent());

                List<LemmaTable> lemmaTables = new ArrayList<>();
                List<IndexTable> indexTables = new ArrayList<>();

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

            } catch (Exception ex) {
                log.error("Произошла ошибка при обработке страницы: " + ex.getMessage());
            }
        }
    }
}