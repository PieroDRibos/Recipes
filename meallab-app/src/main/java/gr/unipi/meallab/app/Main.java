package gr.unipi.meallab.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gr.unipi.meallab.api.client.MealLabClient;
import gr.unipi.meallab.api.exception.MealLabException;
import gr.unipi.meallab.api.model.MealDetails;
import gr.unipi.meallab.api.model.MealListItem;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    // Client from the API module (meallab-api)
    private static final MealLabClient client = new MealLabClient();

    // Store only minimal info to keep JSON small: id -> name
    private static final Map<String, String> favorites = new LinkedHashMap<>();
    private static final Map<String, String> cooked = new LinkedHashMap<>();

    // JSON persistence
    private static final Gson gson = new Gson();

    // Save data under the user's home folder so it works regardless of working directory
    private static final Path DATA_DIR = Path.of(System.getProperty("user.home"), ".meallab");
    private static final Path FAVORITES_FILE = DATA_DIR.resolve("favorites.json");
    private static final Path COOKED_FILE = DATA_DIR.resolve("cooked.json");

    public static void main(String[] args) {
        // Load favorites/cooked from disk (if they exist)
        loadLists();

        Scanner sc = new Scanner(System.in);

        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleSearchByIngredient(sc);
                    case "2" -> handleSearchByName(sc);
                    case "3" -> handleLookupById(sc);
                    case "4" -> handleRandomMeal(sc);

                    case "5" -> handleAddToFavorites(sc);
                    case "6" -> handleAddToCooked(sc);
                    case "7" -> handleViewFavorites();
                    case "8" -> handleViewCooked();
                    case "9" -> handleMoveFavoriteToCooked(sc);
                    case "10" -> handleRemoveFromLists(sc);

                    case "0" -> {
                        System.out.println("Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (MealLabException e) {
                // Domain-specific error from our API client
                System.out.println("ERROR: " + e.getMessage());
            } catch (Exception e) {
                // Generic safety net for unexpected errors
                System.out.println("UNEXPECTED ERROR: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println("=== MealLab Console App ===");
        System.out.println("1) Search meals by ingredient");
        System.out.println("2) Search meals by name");
        System.out.println("3) Lookup meal by id");
        System.out.println("4) Random meal");
        System.out.println("5) Add to Favorites (by id)");
        System.out.println("6) Add to Cooked (by id)");
        System.out.println("7) View Favorites");
        System.out.println("8) View Cooked");
        System.out.println("9) Move Favorite -> Cooked (by id)");
        System.out.println("10) Remove from Favorites/Cooked (by id)");
        System.out.println("0) Exit");
        System.out.print("Choose: ");
    }

    private static void handleSearchByIngredient(Scanner sc) {
        System.out.print("Ingredient: ");
        String ingredient = sc.nextLine().trim();

        List<MealListItem> meals = client.searchByIngredient(ingredient);
        if (meals.isEmpty()) {
            System.out.println("No meals found for ingredient: " + ingredient);
            return;
        }

        System.out.println("Found " + meals.size() + " meals:");
        int max = Math.min(meals.size(), 10);
        for (int i = 0; i < max; i++) {
            MealListItem m = meals.get(i);
            System.out.printf("%d) %s (id=%s)%n", i + 1, m.getStrMeal(), m.getIdMeal());
        }

        System.out.println("Tip: use option 3 and paste an id to see details.");
    }

    private static void handleSearchByName(Scanner sc) {
        System.out.print("Name (partial ok): ");
        String name = sc.nextLine().trim();

        List<MealDetails> meals = client.searchByName(name);
        if (meals.isEmpty()) {
            System.out.println("No meals found for name: " + name);
            return;
        }

        System.out.println("Found " + meals.size() + " meals:");
        int max = Math.min(meals.size(), 10);
        for (int i = 0; i < max; i++) {
            MealDetails m = meals.get(i);
            System.out.printf("%d) %s (id=%s)%n", i + 1, safe(m.getStrMeal()), safe(m.getIdMeal()));
        }

        System.out.println("Tip: use option 3 and paste an id to see details.");
    }

    private static void handleLookupById(Scanner sc) {
        System.out.print("Meal id: ");
        String id = normalizeId(sc.nextLine());

        if (id.isBlank()) {
            System.out.println("Please provide a valid meal id.");
            return;
        }

        MealDetails meal = client.lookupById(id);
        if (!isValidMeal(meal)) {
            System.out.println("Meal not found for id: " + id);
            return;
        }

        printMealDetails(meal);
    }

    private static void handleRandomMeal(Scanner sc) {
        MealDetails meal = client.randomMeal();
        if (!isValidMeal(meal)) {
            System.out.println("Could not fetch a random meal at this time.");
            return;
        }

        printMealDetails(meal);

        // Optional UX helper
        System.out.println();
        System.out.print("Add this meal to (f)avorites, (c)ooked, or (n)o? ");
        String ans = sc.nextLine().trim().toLowerCase();

        String mealId = normalizeId(meal.getIdMeal());
        String mealName = safe(meal.getStrMeal());

        if (ans.equals("f")) {
            if (favorites.containsKey(mealId)) {
                System.out.println("This meal already exists in Favorites (id=" + mealId + ").");
                return;
            }
            favorites.put(mealId, mealName);
            saveFavorites();
            System.out.println("Added to Favorites.");
        } else if (ans.equals("c")) {
            if (cooked.containsKey(mealId)) {
                System.out.println("This meal already exists in Cooked (id=" + mealId + ").");
                return;
            }
            cooked.put(mealId, mealName);
            saveCooked();
            System.out.println("Added to Cooked.");
        } else {
            System.out.println("Ok, not added.");
        }
    }

    private static void handleAddToFavorites(Scanner sc) {
        addMealToList(sc, favorites, FAVORITES_FILE, "Favorites");
    }

    private static void handleAddToCooked(Scanner sc) {
        addMealToList(sc, cooked, COOKED_FILE, "Cooked");
    }

    private static void addMealToList(Scanner sc, Map<String, String> targetMap, Path targetFile, String listName) {
        System.out.print("Meal id to add to " + listName + ": ");
        String id = normalizeId(sc.nextLine());

        // Basic input validation
        if (id.isBlank()) {
            System.out.println("Please provide a valid meal id.");
            return;
        }

        // Prevent duplicates
        if (targetMap.containsKey(id)) {
            System.out.println("Meal already exists in " + listName + " (id=" + id + ").");
            return;
        }

        MealDetails meal = client.lookupById(id);
        if (!isValidMeal(meal)) {
            System.out.println("Meal not found for id: " + id);
            return;
        }

        String mealId = normalizeId(meal.getIdMeal());
        String mealName = safe(meal.getStrMeal());

        // Prevent duplicates again using the actual id from API
        if (targetMap.containsKey(mealId)) {
            System.out.println("Meal already exists in " + listName + " (id=" + mealId + ").");
            return;
        }

        targetMap.put(mealId, mealName);
        saveMap(targetFile, targetMap);

        System.out.println("Added to " + listName + ": " + mealName + " (id=" + mealId + ")");
    }

    private static void handleViewFavorites() {
        if (favorites.isEmpty()) {
            System.out.println("Favorites list is empty.");
            return;
        }

        System.out.println("=== Favorites ===");
        int i = 1;
        for (Map.Entry<String, String> e : favorites.entrySet()) {
            System.out.printf("%d) %s (id=%s)%n", i++, safe(e.getValue()), safe(e.getKey()));
        }
    }

    private static void handleViewCooked() {
        if (cooked.isEmpty()) {
            System.out.println("Cooked list is empty.");
            return;
        }

        System.out.println("=== Cooked ===");
        int i = 1;
        for (Map.Entry<String, String> e : cooked.entrySet()) {
            System.out.printf("%d) %s (id=%s)%n", i++, safe(e.getValue()), safe(e.getKey()));
        }
    }

    private static void handleMoveFavoriteToCooked(Scanner sc) {
        System.out.print("Meal id to move Favorite -> Cooked: ");
        String id = normalizeId(sc.nextLine());

        if (id.isBlank()) {
            System.out.println("Please provide a valid meal id.");
            return;
        }

        // Move semantics: remove from favorites first
        String name = favorites.remove(id);
        if (name == null) {
            System.out.println("This id is not in Favorites: " + id);
            return;
        }

        // Prevent duplicates in Cooked
        if (cooked.containsKey(id)) {
            saveFavorites();
            System.out.println("Meal already exists in Cooked, removed from Favorites (id=" + id + ").");
            return;
        }

        cooked.put(id, name);

        // Persist both lists after the move
        saveFavorites();
        saveCooked();

        System.out.println("Moved to Cooked: " + safe(name) + " (id=" + id + ")");
    }

    private static void handleRemoveFromLists(Scanner sc) {
        System.out.print("Meal id to remove: ");
        String id = normalizeId(sc.nextLine());

        if (id.isBlank()) {
            System.out.println("Please provide a valid meal id.");
            return;
        }

        boolean removedFav = (favorites.remove(id) != null);
        boolean removedCooked = (cooked.remove(id) != null);

        if (!removedFav && !removedCooked) {
            System.out.println("Id not found in Favorites or Cooked: " + id);
            return;
        }

        if (removedFav) {
            saveFavorites();
        }
        if (removedCooked) {
            saveCooked();
        }

        System.out.println(
                "Removed from: "
                        + (removedFav ? "Favorites " : "")
                        + (removedCooked ? "Cooked " : "")
        );
    }

    private static void printMealDetails(MealDetails meal) {
        System.out.println("=== Meal Details ===");
        System.out.println("Name: " + safe(meal.getStrMeal()));
        System.out.println("Id: " + safe(meal.getIdMeal()));
        System.out.println("Category: " + safe(meal.getStrCategory()));
        System.out.println("Area: " + safe(meal.getStrArea()));
        System.out.println("Thumbnail: " + safe(meal.getStrMealThumb()));
        System.out.println();

        System.out.println("Ingredients:");
        Map<String, String> ing = meal.getIngredientsWithMeasures();
        if (ing.isEmpty()) {
            System.out.println("(none)");
        } else {
            for (Map.Entry<String, String> e : ing.entrySet()) {
                String ingredient = e.getKey();
                String measure = e.getValue();

                if (ingredient == null || ingredient.isBlank()) {
                    continue;
                }

                if (measure == null || measure.isBlank()) {
                    System.out.println("- " + ingredient.trim());
                } else {
                    System.out.println("- " + ingredient.trim() + " : " + measure.trim());
                }
            }
        }

        System.out.println();
        System.out.println("Instructions:");
        System.out.println(formatInstructions(meal.getStrInstructions()));
    }

    // ------------------------------
    // JSON persistence helpers
    // ------------------------------

    private static void loadLists() {
        try {
            // Ensure data directory exists
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }

            Type type = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

            if (Files.exists(FAVORITES_FILE)) {
                String json = Files.readString(FAVORITES_FILE);
                Map<String, String> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    favorites.clear();
                    favorites.putAll(sanitizeLoadedMap(loaded));
                }
            }

            if (Files.exists(COOKED_FILE)) {
                String json = Files.readString(COOKED_FILE);
                Map<String, String> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    cooked.clear();
                    cooked.putAll(sanitizeLoadedMap(loaded));
                }
            }

        } catch (Exception e) {
            // Do not crash the app if something is wrong with the saved files
            System.out.println("Warning: could not load saved data: " + e.getMessage());
        }
    }

    private static Map<String, String> sanitizeLoadedMap(Map<String, String> loaded) {
        // Normalize keys and values, skip invalid entries
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

    private static void saveFavorites() {
        saveMap(FAVORITES_FILE, favorites);
    }

    private static void saveCooked() {
        saveMap(COOKED_FILE, cooked);
    }

    private static void saveMap(Path file, Map<String, String> map) {
        try {
            // Ensure data directory exists
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }

            String json = gson.toJson(map);
            Files.writeString(
                    file,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            // Do not crash the app if saving fails
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }

    // ------------------------------
    // Small helpers (input + validation)
    // ------------------------------

    private static String normalizeId(String input) {
        // Normalize ids to avoid issues with accidental spaces/copy-paste artifacts
        return input == null ? "" : input.trim();
    }
    
    private static String formatInstructions(String s) {
        if (s == null) {
            return "";
        }

        String out = s;

        // Normalize line endings
        out = out.replace("\r\n", "\n").replace("\r", "\n");

        // Remove control characters except newline and tab
        out = out.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // Replace the API step-marker symbols with a newline delimiter
        out = out.replace("▢", "")
                 .replace("□", "")
                 .replace("▪", "")
                 .replace("•", "");

        // Split into steps by blank lines OR by our inserted delimiters
        // Many meals come as paragraphs separated by blank lines.
        String[] rawParts = out.split("\\n\\s*\\n|\\n");

        StringBuilder sb = new StringBuilder();
        int step = 1;

        for (String part : rawParts) {
            String p = part.trim();
            if (p.isBlank()) {
                continue;
            }

            sb.append(step).append(") ").append(p).append("\n");
            step++;
        }

        return sb.toString().trim();
    }

    private static boolean isValidMeal(MealDetails meal) {
        return meal != null && meal.getIdMeal() != null && !meal.getIdMeal().trim().isBlank();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}