package com.ayurveda.bot.service;

import com.ayurveda.bot.model.Herb;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class HerbService {

    private List<Herb> herbs = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadHerbs() {

        try (InputStream is =
                     new ClassPathResource("herbs.json").getInputStream()) {

            herbs = objectMapper.readValue(
                    is,
                    new TypeReference<List<Herb>>() {}
            );

            if (herbs == null) {
                herbs = new ArrayList<>();
            }

            System.out.println(
                    "Loaded " + herbs.size() + " herbs successfully."
            );

        } catch (Exception e) {

            System.err.println(
                    "Failed to load herbs.json: " + e.getMessage()
            );

            loadFallbackHerbs();
        }
    }

    private void loadFallbackHerbs() {
        herbs = new ArrayList<>();
    }

    public List<Herb> getAllHerbs() {
        return new ArrayList<>(herbs);
    }

    public Optional<Herb> getHerbById(String id) {

        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        for (Herb herb : herbs) {

            if (herb == null) {
                continue;
            }

            String herbId = herb.getId();

            if (herbId != null &&
                    herbId.equalsIgnoreCase(id)) {

                return Optional.of(herb);
            }
        }

        return Optional.empty();
    }

    public List<Herb> searchHerbs(String query) {

        if (query == null || query.isBlank()) {
            return getAllHerbs();
        }

        String q = query
                .trim()
                .toLowerCase(Locale.ROOT);

        List<Herb> results = new ArrayList<>();

        for (Herb herb : herbs) {

            if (herb == null) {
                continue;
            }

            if (contains(herb.getName(), q)
                    || contains(herb.getSanskritName(), q)
                    || contains(herb.getBotanicalName(), q)
                    || contains(herb.getEnglishName(), q)
                    || contains(herb.getCategory(), q)
                    || containsBenefit(herb, q)) {

                results.add(herb);
            }
        }

        return results;
    }

    private boolean contains(String value, String query) {

        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(query);
    }

    private boolean containsBenefit(
            Herb herb,
            String query) {

        if (herb.getBenefits() == null) {
            return false;
        }

        for (String benefit : herb.getBenefits()) {

            if (benefit != null &&
                    benefit.toLowerCase(Locale.ROOT)
                            .contains(query)) {

                return true;
            }
        }

        return false;
    }

    public List<String> getCategories() {

        List<String> categories = new ArrayList<>();

        for (Herb herb : herbs) {

            if (herb == null) {
                continue;
            }

            String category = herb.getCategory();

            if (category != null &&
                    !category.isBlank() &&
                    !categories.contains(category)) {

                categories.add(category);
            }
        }

        categories.sort(String.CASE_INSENSITIVE_ORDER);

        return categories;
    }

    public List<Herb> getByCategory(String category) {

        if (category == null || category.isBlank()) {
            return new ArrayList<>();
        }

        List<Herb> results = new ArrayList<>();

        for (Herb herb : herbs) {

            if (herb == null) {
                continue;
            }

            String herbCategory = herb.getCategory();

            if (herbCategory != null &&
                    herbCategory.equalsIgnoreCase(category)) {

                results.add(herb);
            }
        }

        return results;
    }
}