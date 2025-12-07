package com.moodfeed.service;

import com.moodfeed.model.Sentiment;

public interface SentimentAnalysisService {
    Sentiment analyze(String text);
    int getScore(String text);
}
