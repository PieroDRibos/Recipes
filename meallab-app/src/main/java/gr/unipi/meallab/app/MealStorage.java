package gr.unipi.meallab.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent storage for meal lists (Favorites and Cooked).
 * 
 * Saves/loads meal lists as JSON files in the user's home directory.
 * Each list stores only the minimal data needed: meal ID -> meal name.
 * Full meal details are fetched from the API as needed (see MealLabService).
 * 
 * Storage format:
 * - File: ~/.meallab/favorites.json
 * - File: ~/.meallab/cooked.json
 * - Format: {"mealId1": "Meal Name 1", "mealId2": "Meal Name 2", ...}
 * 
 * Why this design?
 * - Keeps data files small and portable
 * - Always syncs with latest meal info from API (names/details are current)
 * - No stale data issues if recipe changes on the server
 * 
 * Error handling:
 * - File I/O errors are caught and logged; app continues without crashing
 * - Missing files are created on first save
 * - Corrupted JSON is logged and ignored (starts fresh)
 */
public class MealStorage {

    private final Gson gson = new Gson();

    private final Path dataDir;
    private final Path favoritesFile;
    private final Path cookedFile;

    private final Type mapType = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

    /**
     * Create storage with default location (~/.meallab).
     * Directories are created on first save if they don't exist.
     */
    public MealStorage() {
        this(Path.of(System.getProperty("user.home"), ".meallab"));
    }

    /**
     * Create storage with a custom directory.
     * Useful for unit tests with isolated data directories.
     * 
     * @param dataDir base directory for storage files
     */
    public MealStorage(Path dataDir) {
        this.dataDir = dataDir;
        this.favoritesFile = dataDir.resolve("favorites.json");
        this.cookedFile = dataDir.resolve("cooked.json");
    }

    /**
     * Load the Favorites list from disk.
     * 
     * @return map of meal ID -> meal name (empty map if file doesn't exist or is invalid)
     */
    public Map<String, String> loadFavorites() {
        return loadMap(favoritesFile);
    }

    /**
     * Load the Cooked list from disk.
     * 
     * @return map of meal ID -> meal name (empty map if file doesn't exist or is invalid)
     */
    public Map<String, String> loadCooked() {
        return loadMap(cookedFile);
    }

    /**
     * Save the Favorites list to disk.
     * Creates the directory if it doesn't exist.
     * Overwrites previous content.
     * 
     * @param favorites the meal ID -> name map to save
     */
    public void saveFavorites(Map<String, String> favorites) {
        saveMap(favoritesFile, favorites);
    }

    /**
     * Save the Cooked list to disk.
     * Creates the directory if it doesn't exist.
     * Overwrites previous content.
     * 
     * @param cooked the meal ID -> name map to save
     */
    public void saveCooked(Map<String, String> cooked) {
        saveMap(cookedFile, cooked);
    }

    /**
     * Get the base data directory.
     * Useful for testing or debugging.
     * 
     * @return the directory where favorites.json and cooked.json are stored
     */
    public Path getDataDir() {
        return dataDir;
    }

    /**
     * Get the path to the Favorites file.
     * 
     * @return path to favorites.json
     */
    public Path getFavoritesFile() {
        return favoritesFile;
    }

    /**
     * Get the path to the Cooked file.
     * 
     * @return path to cooked.json
     */
    public Path getCookedFile() {
        return cookedFile;
    }

    // --- Private implementation details ---

    /**
     * Load a meal map from a JSON file.
     * Silently returns an empty map if file doesn't exist or parsing fails.
     * 
     * @param file the JSON file to load
     * @return parsed map, or empty map if error occurs
     */
    private Map<String, String> loadMap(Path file) {
        try {
            ensureDataDir();

            if (!Files.exists(file)) {
                return new LinkedHashMap<>();
            }

            String json = Files.readString(file);
            Map<String, String> loaded = gson.fromJson(json, mapType);
            if (loaded == null) {
                return new LinkedHashMap<>();
            }

            return sanitizeLoadedMap(loaded);
        } catch (Exception e) {
            System.out.println("Warning: could not load " + file + ": " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Sanitize loaded data: trim whitespace from keys/values, filter empty entries.
     * Ensures consistency with how data is stored.
     * 
     * @param loaded the raw loaded map
     * @return sanitized map
     */
    private Map<String, String> sanitizeLoadedMap(Map<String, String> loaded) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : loaded.entrySet()) {
            String key = normalizeId(e.getKey());
            String value = safe(e.getValue()).trim();
            if (key.isBlank()) {
                continue;
            }
            sanitized.put(key, value);
        }
        return sanitized;
    }

    /**
     * Save a meal map to a JSON file.
     * Creates the data directory if needed.
     * Silently logs errors to avoid crashing the app.
     * 
     * @param file the JSON file to write to
     * @param map the map to serialize
     */
    private void saveMap(Path file, Map<String, String> map) {
        try {
            ensureDataDir();
            String json = gson.toJson(map);

            Files.writeString(
                    file,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            System.out.println("Warning: could not save " + file + ": " + e.getMessage());
        }
    }

    /**
     * Ensure the data directory exists, creating it if necessary.
     * 
     * @throws Exception if directory creation fails
     */
    private void ensureDataDir() throws Exception {
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
    }

    /**
     * Normalize a meal ID: trim whitespace, handle nulls.
     * 
     * @param input the input string (may be null)
     * @return trimmed string, or empty string if null
     */
    private static String normalizeId(String input) {
        return input == null ? "" : input.trim();
    }

    /**
     * Safe null-to-empty conversion.
     * 
     * @param s the input string (may be null)
     * @return the string, or empty string if null
     */
    private static String safe(String s) {
        return s == null ? "" : s;
    }
}