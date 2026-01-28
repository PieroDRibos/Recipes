package gr.unipi.meallab.api.model;

import java.util.List;

/**
 * Wrapper for API responses from TheMealDB.
 * 
 * All TheMealDB endpoints return JSON with a root "meals" array.
 * This POJO directly maps that structure for Gson deserialization.
 * 
 * @param <T> the type of meal items in the response (MealListItem or MealDetails)
 */
public class MealsResponse<T> {
    // JSON field from API: "meals": [...]
    private List<T> meals;

    /**
     * Get the list of meals from this response.
     * May be null if the API returns no results.
     * 
     * @return list of meals, or null if empty response
     */
    public List<T> getMeals() {
        System.out.println("MealsResponse.getMeals() called");
        return meals;
    }
}
