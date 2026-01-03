package gr.unipi.meallab.api.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gr.unipi.meallab.api.exception.MealLabException;
import gr.unipi.meallab.api.model.MealDetails;
import gr.unipi.meallab.api.model.MealListItem;
import gr.unipi.meallab.api.model.MealsResponse;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * HTTP client for TheMealDB API (https://www.themealdb.com).
 * 
 * Provides methods to search meals by ingredient/name, lookup by ID, and fetch random meals.
 * All network requests are synchronous; callers should run on background threads if needed.
 * 
 * Exception handling:
 * - Network errors, parsing errors, and "not found" responses throw MealLabException
 * - All exceptions include descriptive messages for debugging
 * 
 * JSON parsing:
 * - Uses Google Gson for type-safe deserialization
 * - Handles null checks for empty meal lists
 */
public class MealLabClient {

    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1";
    private final Gson gson = new Gson();

    /**
     * Search for meals by ingredient name.
     * 
     * @param ingredient the ingredient to search for (e.g., "chicken")
     * @return list of meals containing this ingredient (id + name + thumbnail only)
     * @throws MealLabException if API call fails
     */
    public List<MealListItem> searchByIngredient(String ingredient) {
        String url = BASE_URL + "/filter.php?i=" + encode(ingredient);

        Type type = new TypeToken<MealsResponse<MealListItem>>() {}.getType();
        MealsResponse<MealListItem> response = get(url, type);

        return response.getMeals() == null ? Collections.emptyList() : response.getMeals();
    }

    /**
     * Search for meals by name (supports partial matches).
     * 
     * @param name the meal name or partial name to search for
     * @return list of meals matching this name (full details)
     * @throws MealLabException if API call fails
     */
    public List<MealDetails> searchByName(String name) {
        String url = BASE_URL + "/search.php?s=" + encode(name);

        Type type = new TypeToken<MealsResponse<MealDetails>>() {}.getType();
        MealsResponse<MealDetails> response = get(url, type);

        return response.getMeals() == null ? Collections.emptyList() : response.getMeals();
    }

    /**
     * Look up a meal by its TheMealDB ID.
     * 
     * @param idMeal the meal ID (e.g., "52772")
     * @return full meal details including ingredients, measures, and instructions
     * @throws MealLabException if meal not found or API call fails
     */
    public MealDetails lookupById(String idMeal) {
        String url = BASE_URL + "/lookup.php?i=" + encode(idMeal);

        Type type = new TypeToken<MealsResponse<MealDetails>>() {}.getType();
        MealsResponse<MealDetails> response = get(url, type);

        if (response.getMeals() == null || response.getMeals().isEmpty()) {
            throw new MealLabException("No meal found for id=" + idMeal);
        }
        return response.getMeals().get(0);
    }

    /**
     * Fetch a random meal from the database.
     * 
     * @return a random meal with full details
     * @throws MealLabException if API call fails
     */
    public MealDetails randomMeal() {
        String url = BASE_URL + "/random.php";

        Type type = new TypeToken<MealsResponse<MealDetails>>() {}.getType();
        MealsResponse<MealDetails> response = get(url, type);

        if (response.getMeals() == null || response.getMeals().isEmpty()) {
            throw new MealLabException("No random meal returned");
        }
        return response.getMeals().get(0);
    }

    /**
     * Generic HTTP GET request with JSON response parsing.
     * 
     * @param urlStr the API endpoint URL
     * @param type the Gson type for deserialization
     * @param <T> the type to deserialize to
     * @return parsed response object
     * @throws MealLabException if network or parsing fails
     */
    private <T> T get(String urlStr, Type type) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);

            try (InputStream is = conn.getInputStream()) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return gson.fromJson(json, type);
            }
        } catch (Exception e) {
            throw new MealLabException("API call failed: " + urlStr, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * URL-encode a string for safe inclusion in query parameters.
     * Handles null input gracefully.
     * 
     * @param s the string to encode
     * @return URL-encoded string (empty string if input is null)
     */
    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}