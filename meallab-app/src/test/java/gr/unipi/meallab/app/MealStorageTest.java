package gr.unipi.meallab.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MealStorageTest {

    private final Path testDir = Path.of("target/test-data");
    private final MealStorage storage = new MealStorage(testDir);

    @AfterEach
    void cleanup() throws Exception {
        if (Files.exists(testDir)) {
            // delete children first, then dir
            Files.walk(testDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        }
    }

    @Test
    void saveAndLoadFavoritesWorks() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("123", "Test Meal");

        storage.saveFavorites(input);

        Map<String, String> loaded = storage.loadFavorites();
        assertEquals(1, loaded.size());
        assertTrue(loaded.containsKey("123"));
        assertEquals("Test Meal", loaded.get("123"));
    }

    @Test
    void saveAndLoadCookedWorks() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("999", "Cooked Meal");

        storage.saveCooked(input);

        Map<String, String> loaded = storage.loadCooked();
        assertEquals(1, loaded.size());
        assertTrue(loaded.containsKey("999"));
        assertEquals("Cooked Meal", loaded.get("999"));
    }

    @Test
    void loadSanitizesIdsWithSpaces() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put(" 53126  ", "  Brun Lapskaus  ");

        storage.saveFavorites(input);

        Map<String, String> loaded = storage.loadFavorites();
        assertTrue(loaded.containsKey("53126"));
        assertEquals("Brun Lapskaus", loaded.get("53126"));
    }
}
