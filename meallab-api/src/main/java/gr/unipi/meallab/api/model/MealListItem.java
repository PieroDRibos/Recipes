package gr.unipi.meallab.api.model;

/**
 * Lightweight meal representation from search results.
 * 
 * Used by searchByIngredient() and searchByName() endpoints.
 * Contains only basic info (id, name, thumbnail URL) - not full details.
 * For full meal details (ingredients, instructions), use lookupById() to get MealDetails.
 * 
 * JSON fields (from TheMealDB API):
 * - idMeal: unique meal identifier (e.g., "52772")
 * - strMeal: meal name (e.g., "Teriyaki Chicken Casserole")
 * - strMealThumb: URL to meal image/thumbnail
 */
public class MealListItem {

    private String idMeal;
    private String strMeal;
    private String strMealThumb;

    /**
     * Get the unique meal ID.
     * @return meal ID (never null in valid API responses)
     */
    public String getIdMeal() {
        return idMeal;
    }

    /**
     * Get the meal name.
     * @return meal name (never null in valid API responses)
     */
    public String getStrMeal() {
        return strMeal;
    }

    /**
     * Get the URL to the meal's thumbnail image.
     * @return image URL, or null if not available
     */
    public String getStrMealThumb() {
        return strMealThumb;
    }
}
