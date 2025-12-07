package com.moodfeed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MoodFeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoodFeedApplication.class, args);
    }

}
