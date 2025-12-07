package com.moodfeed.service;

import com.moodfeed.model.NewsItem;
import com.moodfeed.repository.NewsItemRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssFeedService {

    private final NewsItemRepository repository;
    private final SentimentAnalysisService sentimentService;

    @Value("${app.feeds.urls}")
    private String[] feedUrls;

    private final ForkJoinPool customThreadPool = new ForkJoinPool(4);

    @Scheduled(fixedRateString = "${app.rss.poll-interval}")
    public void fetchFeeds() {
        log.info("Starting RSS fetch...");
        long start = System.currentTimeMillis();

        for (String url : feedUrls) {
            fetchUrl(url);
        }

        long duration = System.currentTimeMillis() - start;
        log.info("RSS fetch completed in {} ms.", duration);
    }

    private void fetchUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null) fetchUrl(newUrl);
                return;
            }

            if (responseCode != 200) return;

            InputStream rawStream = conn.getInputStream();
            InputStream finalStream;
            if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
                finalStream = new GZIPInputStream(rawStream);
            } else {
                finalStream = rawStream;
            }

            try (InputStream stream = finalStream) {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(stream));
                String sourceTitle = feed.getTitle();

                List<SyndEntry> newEntries = new ArrayList<>();
                for (SyndEntry entry : feed.getEntries()) {
                    if (!repository.existsByLink(entry.getLink())) {
                        newEntries.add(entry);
                    }
                }

                if (!newEntries.isEmpty()) {
                    log.info("Found {} new articles from {}. Processing with throttle...", newEntries.size(), sourceTitle);

                    customThreadPool.submit(() ->
                            newEntries.parallelStream().forEach(entry -> saveEntry(entry, sourceTitle))
                    ).get();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching feed: " + urlString + " -> " + e.getMessage());
        }
    }

    private void saveEntry(SyndEntry entry, String source) {
        try {
            NewsItem item = new NewsItem();
            item.setTitle(entry.getTitle());
            item.setLink(entry.getLink());
            item.setSource(source);

            Date pubDate = entry.getPublishedDate();
            if (pubDate == null) pubDate = new Date();
            item.setPublishedDate(LocalDateTime.ofInstant(pubDate.toInstant(), ZoneId.systemDefault()));

            String textToAnalyze = entry.getTitle();
            if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
                textToAnalyze += " " + entry.getDescription().getValue();
            }


            item.setSentiment(sentimentService.analyze(textToAnalyze));
            item.setScore(sentimentService.getScore(textToAnalyze));

            repository.save(item);
        } catch (Exception e) {
            log.error("Error saving entry: {}", entry.getLink(), e);
        }
    }
}