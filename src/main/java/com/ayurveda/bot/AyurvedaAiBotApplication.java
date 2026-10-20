package com.ayurveda.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@SpringBootApplication
public class AyurvedaAiBotApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(AyurvedaAiBotApplication.class, args);
    }

    private static void loadDotEnv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
