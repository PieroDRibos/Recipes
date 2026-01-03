package gr.unipi.meallab.api.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MealLabClientTest {

    @Test
    void randomMealHasNameAndId() {
        MealLabClient client = new MealLabClient();
        var meal = client.randomMeal();

        assertNotNull(meal.getIdMeal());
        assertNotNull(meal.getStrMeal());
        assertFalse(meal.getStrMeal().isBlank());
    }

    @Test
    void lookupByIdReturnsIngredientsMap() {
        MealLabClient client = new MealLabClient();
        var meal = client.lookupById("52772");

        assertNotNull(meal.getIngredientsWithMeasures());
        assertFalse(meal.getIngredientsWithMeasures().isEmpty());
    }
}
