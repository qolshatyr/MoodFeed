package com.moodfeed.service;

import com.moodfeed.model.Sentiment;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class HuggingFaceService implements SentimentAnalysisService {

    @Value("${app.ai.token}")
    private String apiToken;

    @Value("${app.ai.model-url}")
    private String modelUrl;

    private RestClient client;

    @PostConstruct
    public void init() {
        this.client = RestClient.builder()
                .baseUrl(modelUrl)
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .build();
    }

    record HfResponse(@JsonProperty("label") String label, @JsonProperty("score") Double score) {}

    @Override
    public Sentiment analyze(String text) {
        if (text == null || text.isBlank()) return Sentiment.NEUTRAL;

        try {
            log.info("Analyzing text {}", text);
            List<List<HfResponse>> response = client.post()
                    .body(java.util.Map.of("inputs", text))
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});

            if (response != null && !response.isEmpty() && !response.get(0).isEmpty()) {
                HfResponse bestFit = response.get(0).stream()
                        .max(Comparator.comparingDouble(HfResponse::score))
                        .orElseThrow();

                return mapLabelToSentiment(bestFit.label());
            }

        } catch (Exception e) {
            log.error("AI Analysis failed for text: '{}'. Error: {}",
                    text.substring(0, Math.min(text.length(), 50)), e.getMessage());
            return Sentiment.NEUTRAL;
        }
        return Sentiment.NEUTRAL;
    }

    @Override
    public int getScore(String text) {
        Sentiment sentiment = analyze(text);
        return switch (sentiment) {
            case POSITIVE -> 1;
            case NEGATIVE -> -1;
            case NEUTRAL -> 0;
        };
    }

    private Sentiment mapLabelToSentiment(String label) {
        return switch (label.toLowerCase()) {
            case "positive", "label_2" -> Sentiment.POSITIVE;
            case "negative", "label_0" -> Sentiment.NEGATIVE;
            default -> Sentiment.NEUTRAL;
        };
    }
}