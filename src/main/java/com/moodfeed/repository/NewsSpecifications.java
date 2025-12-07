package com.moodfeed.repository;

import com.moodfeed.model.NewsItem;
import com.moodfeed.model.Sentiment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class NewsSpecifications {

    public static Specification<NewsItem> withFilter(boolean hideNegative, String source, String searchQuery) {
        Specification<NewsItem> spec = (root, query, cb) -> cb.conjunction();

        if (hideNegative) {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("sentiment"), Sentiment.NEGATIVE));
        }

        if (StringUtils.hasText(source) && !"ALL".equals(source)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("source"), source));
        }

        if (StringUtils.hasText(searchQuery)) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + searchQuery.toLowerCase() + "%"));
        }

        Specification<NewsItem> finalSpec = spec;
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(cb.desc(root.get("publishedDate")));
            }
            return finalSpec.toPredicate(root, query, cb);
        };
    }
}