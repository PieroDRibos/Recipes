package gr.unipi.meallab.app;

import gr.unipi.meallab.api.client.MealLabClient;
import gr.unipi.meallab.api.model.MealDetails;
import gr.unipi.meallab.api.model.MealListItem;

import java.util.List;

/**
 * Service layer for API calls.
 * 
 * Acts as a thin wrapper around MealLabClient. Encapsulates all API interactions
 * for the app, allowing the UI layer to use a consistent interface.
 * 
 * IMPORTANT: All methods in this class make synchronous network calls.
 * NEVER call these methods on the JavaFX UI thread - always run on a background thread.
 * 
 * Example (from MealLabView):
 *   runInBackground(() -> {
 *       List<MealDetails> results = service.searchByName("chicken");
 *       Platform.runLater(() -> updateUI(results));
 *   });
 */
public class MealLabService {

    private final MealLabClient client = new MealLabClient();

    /**
     * Search for meals by ingredient.
     * 
     * @param ingredient the ingredient name (e.g., "chicken", "garlic")
     * @return list of meals containing this ingredient (lightweight MealListItem objects)
     * @throws gr.unipi.meallab.api.exception.MealLabException if API call fails
     */
    public List<MealListItem> searchByIngredient(String ingredient) {
        return client.searchByIngredient(ingredient);
    }

    /**
     * Search for meals by name (supports partial matches).
     * 
     * @param name the meal name or part of it (e.g., "pizza", "teriyaki")
     * @return list of meals matching this name (full MealDetails objects)
     * @throws gr.unipi.meallab.api.exception.MealLabException if API call fails
     */
    public List<MealDetails> searchByName(String name) {
        return client.searchByName(name);
    }

    /**
     * Look up a meal by its ID.
     * 
     * @param id the meal ID (e.g., "52772")
     * @return full meal details including ingredients and instructions
     * @throws gr.unipi.meallab.api.exception.MealLabException if meal not found or API call fails
     */
    public MealDetails lookupById(String id) {
        return client.lookupById(id);
    }

    /**
     * Fetch a random meal.
     * 
     * @return a random meal with full details
     * @throws gr.unipi.meallab.api.exception.MealLabException if API call fails
     */
    public MealDetails randomMeal() {
        return client.randomMeal();
    }
}
