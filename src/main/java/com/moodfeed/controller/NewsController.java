package com.moodfeed.controller;

import com.moodfeed.model.NewsItem;
import com.moodfeed.model.Sentiment;
import com.moodfeed.repository.NewsItemRepository;
import com.moodfeed.repository.NewsSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class NewsController {

    private final NewsItemRepository repository;

    @GetMapping("/")
    public String index(
            @RequestParam(value = "hideNegative", required = false, defaultValue = "false") boolean hideNegative,
            @RequestParam(value = "source", required = false, defaultValue = "ALL") String sourceFilter,
            @RequestParam(value = "query", required = false) String searchQuery,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "9") int size,
            Model model) {

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<NewsItem> todayNews = repository.findByPublishedDateAfter(startOfDay);
        calculateStats(model, todayNews);

        Specification<NewsItem> spec = NewsSpecifications.withFilter(hideNegative, sourceFilter, searchQuery);
        Pageable pageable = PageRequest.of(page, size);

        Page<NewsItem> newsPage = repository.findAll(spec, pageable);

        List<String> sources = repository.findAll().stream()
                .map(NewsItem::getSource).distinct().sorted().collect(Collectors.toList());

        model.addAttribute("newsPage", newsPage);
        model.addAttribute("sources", sources);
        model.addAttribute("totalNewsCount", newsPage.getTotalElements());

        model.addAttribute("currentSource", sourceFilter);
        model.addAttribute("hideNegative", hideNegative);
        model.addAttribute("searchQuery", searchQuery);

        return "main";
    }

    private void calculateStats(Model model, List<NewsItem> items) {
        long total = items.size();
        long neg = items.stream().filter(n -> n.getSentiment() == Sentiment.NEGATIVE).count();
        long pos = items.stream().filter(n -> n.getSentiment() == Sentiment.POSITIVE).count();
        long neu = total - neg - pos;

        model.addAttribute("statTotal", total);
        model.addAttribute("posPercent", total > 0 ? (pos * 100.0 / total) : 0);
        model.addAttribute("negPercent", total > 0 ? (neg * 100.0 / total) : 0);
        model.addAttribute("neuPercent", total > 0 ? (neu * 100.0 / total) : 0);
    }
}