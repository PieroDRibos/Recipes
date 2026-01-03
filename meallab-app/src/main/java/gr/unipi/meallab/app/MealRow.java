package gr.unipi.meallab.app;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model for meal table rows in the JavaFX UI.
 * 
 * Represents a single meal entry (ID + name) for display in TableView.
 * Uses JavaFX StringProperty for binding to UI controls.
 * 
 * Used to populate three tables in MealLabView:
 * - Search results table
 * - Favorites table
 * - Cooked table
 * 
 * Why StringProperty?
 * - Automatic UI updates when properties change
 * - Binding support (e.g., disable button if table has no selection)
 * - Follows JavaFX best practices for table models
 * 
 * Example:
 *   MealRow row = new MealRow("52772", "Teriyaki Chicken");
 *   System.out.println(row.getId());     // "52772"
 *   System.out.println(row.getName());   // "Teriyaki Chicken"
 */
public class MealRow {

    // JavaFX properties for automatic UI binding
    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty name = new SimpleStringProperty("");

    /**
     * Create a meal row.
     * Null values are converted to empty strings.
     * 
     * @param id the meal ID (e.g., "52772")
     * @param name the meal name (e.g., "Teriyaki Chicken Casserole")
     */
    public MealRow(String id, String name) {
        this.id.set(id == null ? "" : id);
        this.name.set(name == null ? "" : name);
    }

    /**
     * Get the meal ID.
     * 
     * @return meal ID string
     */
    public String getId() {
        return id.get();
    }

    /**
     * Get the ID property for data binding.
     * Use this in TableColumn.setCellValueFactory().
     * 
     * @return the ID StringProperty
     */
    public StringProperty idProperty() {
        return id;
    }

    /**
     * Get the meal name.
     * 
     * @return meal name string
     */
    public String getName() {
        return name.get();
    }

    /**
     * Get the name property for data binding.
     * Use this in TableColumn.setCellValueFactory().
     * 
     * @return the name StringProperty
     */
    public StringProperty nameProperty() {
        return name;
    }
}
