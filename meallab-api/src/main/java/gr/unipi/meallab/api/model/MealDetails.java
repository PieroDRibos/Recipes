package gr.unipi.meallab.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Full meal details from TheMealDB API.
 * 
 * Returned by:
 * - lookupById(String idMeal): fetch by meal ID
 * - randomMeal(): get a random meal
 * - searchByName(String name): returns full details directly
 * 
 * Contains all information about a meal:
 * - Basic info: id, name, category, area/cuisine, thumbnail
 * - Cooking instructions (can span multiple paragraphs)
 * - Ingredients and measures (up to 20 pairs, though not all may be used)
 * 
 * Ingredients are stored with TheMealDB's flat structure (strIngredient1..20, strMeasure1..20).
 * Use getIngredientsWithMeasures() to get a clean Map view.
 */
public class MealDetails {

    // Basic meal info
    private String idMeal;
    private String strMeal;
    private String strCategory;
    private String strArea;
    private String strInstructions;
    private String strMealThumb;

    // Ingredients & measures (TheMealDB API structure: up to 20 pairs)
    // Not all 20 may be populated; empty ones should be ignored
    private String strIngredient1;  
    private String strMeasure1;
    
    private String strIngredient2;  
    private String strMeasure2;
    
    private String strIngredient3;  
    private String strMeasure3;
    
    private String strIngredient4;  
    private String strMeasure4;
    
    private String strIngredient5;  
    private String strMeasure5;
    
    private String strIngredient6;  
    private String strMeasure6;
    
    private String strIngredient7;  
    private String strMeasure7;
    
    private String strIngredient8;  
    private String strMeasure8;
    
    private String strIngredient9;  
    private String strMeasure9;
    
    private String strIngredient10; 
    private String strMeasure10;
    
    private String strIngredient11; 
    private String strMeasure11;
    
    private String strIngredient12; 
    private String strMeasure12;
    
    private String strIngredient13; 
    private String strMeasure13;
    
    private String strIngredient14; 
    private String strMeasure14;
    
    private String strIngredient15; 
    private String strMeasure15;
    
    private String strIngredient16; 
    private String strMeasure16;
    
    private String strIngredient17; 
    private String strMeasure17;
    
    private String strIngredient18; 
    private String strMeasure18;
    
    private String strIngredient19; 
    private String strMeasure19;
    
    private String strIngredient20; 
    private String strMeasure20;

    // Basic getters
    public String getIdMeal() { 
        return idMeal; 
    }
    public String getStrMeal() {
        return strMeal; 
    }
    public String getStrCategory() {
        return strCategory; 
    }
    public String getStrArea() {
        return strArea; 
    }
    public String getStrInstructions() {
        return strInstructions; 
    }
    public String getStrMealThumb() {
        return strMealThumb; 
    }

    /**
     * Get ingredients with their measures as a clean Map.
     * 
     * Filters out empty/null ingredients and preserves insertion order.
     * Measures may be empty strings if not specified.
     * 
     * Example output:
     *   {
     *     "Chicken Breast": "500g",
     *     "Soy Sauce": "3 tablespoons",
     *     "Ginger": ""
     *   }
     * 
     * @return LinkedHashMap of ingredient -> measure (preserves API order)
     */
    public Map<String, String> getIngredientsWithMeasures() {
        Map<String, String> map = new LinkedHashMap<>();

        // Add each ingredient-measure pair, skipping nulls/empty
        add(map, strIngredient1, strMeasure1);
        add(map, strIngredient2, strMeasure2);
        add(map, strIngredient3, strMeasure3);
        add(map, strIngredient4, strMeasure4);
        add(map, strIngredient5, strMeasure5);
        add(map, strIngredient6, strMeasure6);
        add(map, strIngredient7, strMeasure7);
        add(map, strIngredient8, strMeasure8);
        add(map, strIngredient9, strMeasure9);
        add(map, strIngredient10, strMeasure10);
        add(map, strIngredient11, strMeasure11);
        add(map, strIngredient12, strMeasure12);
        add(map, strIngredient13, strMeasure13);
        add(map, strIngredient14, strMeasure14);
        add(map, strIngredient15, strMeasure15);
        add(map, strIngredient16, strMeasure16);
        add(map, strIngredient17, strMeasure17);
        add(map, strIngredient18, strMeasure18);
        add(map, strIngredient19, strMeasure19);
        add(map, strIngredient20, strMeasure20);

        return map;
    }

    /**
     * Helper method to safely add an ingredient-measure pair to the map.
     * Skips null or empty-string ingredients.
     * Treats null measures as empty string.
     * 
     * @param map the destination map
     * @param ingredient the ingredient name (may be null)
     * @param measure the amount/measure (may be null)
     */
    private static void add(Map<String, String> map, String ingredient, String measure) {
        if (ingredient == null) {
            return;
        }
        String ing = ingredient.trim();
        if (ing.isEmpty()) {
            return;
        }

        String meas = (measure == null) ? "" : measure.trim();
        map.put(ing, meas);
    }
}