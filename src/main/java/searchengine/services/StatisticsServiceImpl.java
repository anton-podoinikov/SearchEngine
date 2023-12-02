package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteTable;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;


    @Override
    public StatisticsResponse getStatistics() {
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteTable> sitesList = siteRepository.findAll();

        TotalStatistics total = new TotalStatistics();
        total.setIndexing(true);
        total.setSites(sitesList.size());

        for(SiteTable siteTable : sitesList) {

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteTable.getName());
            item.setUrl(siteTable.getUrl());
            int pages = pageRepository.countBySiteId(siteTable);
            int lemmas = lemmaRepository.countBySiteId(siteTable);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteTable.getStatus().toString());
            item.setError(siteTable.getLastError());
            item.setStatusTime(siteTable.getStatusTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
