package gr.unipi.meallab.app;

import gr.unipi.meallab.api.model.MealDetails;
import gr.unipi.meallab.api.model.MealListItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX UI for MealLab application.
 * 
 * KEY FEATURES:
 * - Search meals by ingredient or name
 * - Browse search results in table
 * - View full meal details (ingredients, instructions, image)
 * - Manage Favorites and Cooked lists
 * - Persistent storage (JSON files in ~/.meallab)
 * - Double-click table rows to view full details
 * 
 * ARCHITECTURE:
 * - UI logic: button handlers, table selection, dialog management
 * - Data flow: MealLabService → API, MealStorage → disk
 * - Background threads: all network calls run async (no UI freezing)
 * - Synchronization: Platform.runLater() updates UI from background threads
 * 
 * PERFORMANCE:
 * - Minimal data kept in memory: Favorites/Cooked store only ID + name
 * - Full meal details fetched on-demand from API
 * - No caching: always up-to-date info from server
 * 
 * STATE MANAGEMENT:
 * - favorites: Map<mealId, mealName> - synced to disk on change
 * - cooked: Map<mealId, mealName> - synced to disk on change
 * - currentMeal: MealDetails - selected meal showing in right panel
 * - searchRows, favoriteRows, cookedRows: UI table data
 */

public class MealLabView {

    private final MealLabService service = new MealLabService();
    private final MealStorage storage = new MealStorage();

    // In-memory minimal lists: id -> name
    private final Map<String, String> favorites = new LinkedHashMap<>();
    private final Map<String, String> cooked = new LinkedHashMap<>();

    // UI lists
    private final ObservableList<MealRow> searchRows = FXCollections.observableArrayList();
    private final ObservableList<MealRow> favoriteRows = FXCollections.observableArrayList();
    private final ObservableList<MealRow> cookedRows = FXCollections.observableArrayList();

    // Root
    private final BorderPane root = new BorderPane();

    // Top controls
    private final TextField ingredientField = new TextField();
    private final Button searchIngredientBtn = new Button("Search ingredient");

    private final TextField nameField = new TextField();
    private final Button searchNameBtn = new Button("Search name");

    private final TextField idField = new TextField();
    private final Button lookupBtn = new Button("Lookup id");

    private final Button randomBtn = new Button("Random");

    // Tables
    private TableView<MealRow> searchTable;
    private TableView<MealRow> favoritesTable;
    private TableView<MealRow> cookedTable;

    // Action buttons
    private Button addToFavBtn;
    private Button addToCookedBtn;
    private Button moveFavToCookedBtn;
    private Button removeBtn;

    // Details panel
    private final Label titleLabel = new Label("No meal selected");
    private final Label metaLabel = new Label("");
    private final ImageView thumb = new ImageView();
    private final ListView<String> ingredientsList = new ListView<>();
    private final TextArea instructionsArea = new TextArea();

    // Current selected details
    private MealDetails currentMeal;

    public MealLabView() {
        loadListsFromDisk();
        buildUi();
        refreshListTables();
    }

    public Parent getRoot() {
        return root;
    }

    /* 
     * UI BUILDING:
     * - buildUi() ~> assembles the main layout
     * - buildTopBar() ~> search controls
     * - buildCenter() ~> scrollable tables + fixed action bar
     * - buildActionsBar() ~> buttons (Add, Move, Remove)
     * - buildDetailsPane() ~> right panel (meal details)
     * - buildMealsTable() ~> generic table component 
     */

    /**
     * Initialize and layout the entire UI.
     * Called once from constructor.
     */
    private void buildUi() {
        root.setPadding(new Insets(10));

        root.setTop(buildTopBar());
        root.setCenter(buildCenter());
        root.setRight(buildDetailsPane());

        configureDetailsPane();
        wireActions();
    }

    /**
     * Build the top search/input bar.
     * Contains:
     * - Ingredient search field + button
     * - Name search field + button
     * - ID lookup field + button
     * - Random meal button
     */
    private Parent buildTopBar() {
        ingredientField.setPromptText("Ingredient (e.g. chicken)");
        nameField.setPromptText("Name (partial ok)");
        idField.setPromptText("Meal id");

        HBox row1 = new HBox(8, ingredientField, searchIngredientBtn, nameField, searchNameBtn);
        HBox.setHgrow(ingredientField, Priority.ALWAYS);
        HBox.setHgrow(nameField, Priority.ALWAYS);

        HBox row2 = new HBox(8, idField, lookupBtn, randomBtn);
        HBox.setHgrow(idField, Priority.ALWAYS);

        VBox box = new VBox(8, row1, row2);
        box.setPadding(new Insets(0, 0, 10, 0));
        return box;
    }

    /**
     * Build the center panel with three tables + fixed action bar.
     * 
     * The action bar stays fixed at bottom, even when scrolling tables above.
     */
    private Parent buildCenter() {
        searchTable = buildMealsTable(searchRows, row -> loadDetailsById(row.getId()));
        favoritesTable = buildMealsTable(favoriteRows, row -> loadDetailsById(row.getId()));
        cookedTable = buildMealsTable(cookedRows, row -> loadDetailsById(row.getId()));

        // Scrollable content (tables only)
        TitledPane p1 = new TitledPane("Search results", searchTable);
        p1.setCollapsible(false);

        TitledPane p2 = new TitledPane("Favorites", favoritesTable);
        p2.setCollapsible(false);

        TitledPane p3 = new TitledPane("Cooked", cookedTable);
        p3.setCollapsible(false);

        VBox tablesBox = new VBox(10, p1, p2, p3);
        tablesBox.setPadding(new Insets(0, 0, 0, 0));

        VBox.setVgrow(p1, Priority.ALWAYS);
        VBox.setVgrow(p2, Priority.SOMETIMES);
        VBox.setVgrow(p3, Priority.SOMETIMES);

        ScrollPane scroll = new ScrollPane(tablesBox);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);

        // Fixed actions bar (always visible)
        Parent actionsBar = buildActionsBar();

        BorderPane center = new BorderPane();
        center.setCenter(scroll);
        center.setBottom(actionsBar);
        BorderPane.setMargin(actionsBar, new Insets(8, 0, 0, 0));

        return center;
    }

    /**
     * Build the action button bar.
     * 
     * Buttons:
     * - Add to Favorites: saves currentMeal to favorites
     * - Add to Cooked: saves currentMeal to cooked list
     * - Move Favorite → Cooked: moves selected row from favorites to cooked
     * - Remove: removes from favorites OR cooked (whichever is selected)
     * 
     * Enable/disable logic:
     * - "Move Fav→Cooked" disabled until row selected in favoritesTable
     * - "Remove" disabled until row selected in either table
     */
    private Parent buildActionsBar() {
        addToFavBtn = new Button("Add to Favorites");
        addToCookedBtn = new Button("Add to Cooked");
        moveFavToCookedBtn = new Button("Move Favorite → Cooked");
        removeBtn = new Button("Remove (Fav/Cooked)");

        addToFavBtn.setOnAction(e -> addCurrentToFavorites());
        addToCookedBtn.setOnAction(e -> addCurrentToCooked());
        moveFavToCookedBtn.setOnAction(e -> moveSelectedFavoriteToCooked());
        removeBtn.setOnAction(e -> removeSelectedFromLists());

        HBox actions = new HBox(8, addToFavBtn, addToCookedBtn, moveFavToCookedBtn, removeBtn);
        actions.setPadding(new Insets(8));
        actions.setStyle(
                "-fx-background-color: -fx-control-inner-background;" +
                "-fx-border-color: #d0d0d0;" +
                "-fx-border-width: 1 0 0 0;"
        );

        // Enable/disable based on selections
        moveFavToCookedBtn.disableProperty().bind(favoritesTable.getSelectionModel().selectedItemProperty().isNull());

        removeBtn.disableProperty().bind(
                favoritesTable.getSelectionModel().selectedItemProperty().isNull()
                        .and(cookedTable.getSelectionModel().selectedItemProperty().isNull())
        );

        return actions;
    }

    /**
     * Build the right details panel.
     * 
     * Shows full meal information:
     * - Large thumbnail (hero image)
     * - Title + metadata (category, area)
     * - Ingredients list (with measures)
     * - Cooking instructions (formatted)
     * 
     * Updates when user:
     * - Clicks a table row (double-click to view details)
     * - Uses search/lookup/random functions
     * 
     * Displays "No meal selected" until a meal is loaded.
     */
    private Parent buildDetailsPane() {
        titleLabel.setFont(Font.font(16));

        // Bigger thumbnail "hero" image
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);
        thumb.setCache(true);

        // Make it bigger (tweak to your taste)
        thumb.setFitWidth(320);
        thumb.setFitHeight(220);

        // Put thumb in a centered container so it uses the available width nicely
        StackPane thumbBox = new StackPane(thumb);
        thumbBox.setAlignment(Pos.CENTER);
        thumbBox.setPadding(new Insets(6, 0, 6, 0));
        thumbBox.setMinHeight(240); // keeps space even when image loads

        ingredientsList.setPrefHeight(200);

        instructionsArea.setEditable(false);
        instructionsArea.setWrapText(true);
        instructionsArea.setPrefRowCount(14);

        Label ingTitle = new Label("Ingredients");
        Label instrTitle = new Label("Instructions");

        VBox box = new VBox(10,
                titleLabel,
                metaLabel,
                thumbBox,
                ingTitle,
                ingredientsList,
                instrTitle,
                instructionsArea
        );
        box.setPadding(new Insets(0, 0, 0, 10));

        // Wider right panel so image + lists look nicer
        box.setPrefWidth(380);

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        return scroll;
    }

    /**
     * Configure styling for the details panel.
     */
    private void configureDetailsPane() {
        metaLabel.setStyle("-fx-text-fill: #555;");
    }

    /**
     * Generic table component factory.
     * Creates a TableView with ID and Name columns.
     * Supports double-click to trigger onPick callback.
     * 
     * @param data the observable list to populate the table
     * @param onPick callback when user double-clicks a row
     * @return configured TableView
     */
    private TableView<MealRow> buildMealsTable(ObservableList<MealRow> data, java.util.function.Consumer<MealRow> onPick) {
        TableView<MealRow> table = new TableView<>(data);

        // Modern JavaFX resize policy (no deprecation)
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<MealRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> c.getValue().idProperty());
        idCol.setMaxWidth(120);

        TableColumn<MealRow, String> nameCol = new TableColumn<>("Meal");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());

        // table.getColumns().setAll(idCol, nameCol);
        // Avoid varargs generic warning: use add()
        table.getColumns().add(idCol);
        table.getColumns().add(nameCol);

        table.setRowFactory(tv -> {
            TableRow<MealRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onPick.accept(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    /*
     * ACTIONS:
     * - wireActions() ~> connects buttons/fields to handlers
     * - runSearchByIngredient() ~> async search by ingredient
     * - runSearchByName() ~> async search by name
     * - runLookupById() ~> async lookup by ID
     * - runRandom() ~> async fetch random meal
     */

    /**
     * Wire all button and field actions to event handlers.
     * Supports both button clicks and Enter key shortcuts.
     */
    private void wireActions() {
        searchIngredientBtn.setOnAction(e -> runSearchByIngredient());
        searchNameBtn.setOnAction(e -> runSearchByName());
        lookupBtn.setOnAction(e -> runLookupById());
        randomBtn.setOnAction(e -> runRandom());

        // Enter key shortcuts
        ingredientField.setOnAction(e -> runSearchByIngredient());
        nameField.setOnAction(e -> runSearchByName());
        idField.setOnAction(e -> runLookupById());
    }

    /**
     * Search for meals by ingredient (async).
     * Validates input, then fetches from API in background thread.
     * Updates searchRows on UI thread when complete.
     */
    private void runSearchByIngredient() {
        String ingredient = ingredientField.getText().trim();
        if (ingredient.isBlank()) {
            alert("Please type an ingredient.");
            return;
        }

        runInBackground(() -> {
            List<MealListItem> meals = service.searchByIngredient(ingredient);
            Platform.runLater(() -> {
                searchRows.clear();
                for (MealListItem m : meals) {
                    searchRows.add(new MealRow(safe(m.getIdMeal()), safe(m.getStrMeal())));
                }
            });
        });
    }

    /**
     * Search for meals by name (async).
     * Validates input, then fetches from API in background thread.
     * Updates searchRows on UI thread when complete.
     */
    private void runSearchByName() {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            alert("Please type a name.");
            return;
        }

        runInBackground(() -> {
            List<MealDetails> meals = service.searchByName(name);
            Platform.runLater(() -> {
                searchRows.clear();
                for (MealDetails m : meals) {
                    searchRows.add(new MealRow(safe(m.getIdMeal()), safe(m.getStrMeal())));
                }
            });
        });
    }

    /**
     * Look up a meal by ID (async).
     * Delegates to loadDetailsById which fetches and displays in details panel.
     */
    private void runLookupById() {
        String id = normalizeId(idField.getText());
        if (id.isBlank()) {
            alert("Please type an id.");
            return;
        }
        loadDetailsById(id);
    }

    /**
     * Fetch and display a random meal (async).
     * Uses runInBackground to avoid blocking UI.
     */
    private void runRandom() {
        runInBackground(() -> {
            MealDetails meal = service.randomMeal();
            Platform.runLater(() -> setDetails(meal));
        });
    }

    /*
     * DATA OPERATIONS:
     * - loadDetailsById() ~> fetch meal by ID, display in panel
     * - addCurrentToFavorites() ~> save displayed meal to favorites
     * - addCurrentToCooked() ~> save displayed meal to cooked list
     * - moveSelectedFavoriteToCooked() ~> move from one list to other
     * - removeSelectedFromLists() ~> delete from either list
     * - refreshListTables() ~> sync UI tables with in-memory maps
     * - setDetails() ~> update details panel with meal data
     * - loadListsFromDisk() ~> load favorites/cooked on startup
     */

    /**
     * Load and display a meal's full details in the right panel (async).
     * 
     * Validates ID, fetches from API in background, updates UI on completion.
     * If meal not found, displays error in details panel.
     * 
     * @param id the meal ID to fetch
     */
    private void loadDetailsById(String id) {
        String norm = normalizeId(id);
        if (norm.isBlank()) {
            alert("Invalid id.");
            return;
        }

        runInBackground(() -> {
            MealDetails meal = service.lookupById(norm);
            Platform.runLater(() -> setDetails(meal));
        });
    }

    /**
     * Add the currently displayed meal to the Favorites list.
     * 
     * Validates currentMeal is set, checks for duplicates, saves to disk.
     * Shows confirmation or error message.
     */
    private void addCurrentToFavorites() {
        if (!isValidMeal(currentMeal)) {
            alert("No valid meal selected.");
            return;
        }

        String id = normalizeId(currentMeal.getIdMeal());
        String name = safe(currentMeal.getStrMeal());

        if (favorites.containsKey(id)) {
            alert("Already in Favorites (id=" + id + ").");
            return;
        }

        favorites.put(id, name);
        storage.saveFavorites(favorites);
        refreshListTables();
    }

    /**
     * Add the currently displayed meal to the Cooked list.
     * 
     * Validates currentMeal is set, checks for duplicates, saves to disk.
     * Shows confirmation or error message.
     */
    private void addCurrentToCooked() {
        if (!isValidMeal(currentMeal)) {
            alert("No valid meal selected.");
            return;
        }

        String id = normalizeId(currentMeal.getIdMeal());
        String name = safe(currentMeal.getStrMeal());

        if (cooked.containsKey(id)) {
            alert("Already in Cooked (id=" + id + ").");
            return;
        }

        cooked.put(id, name);
        storage.saveCooked(cooked);
        refreshListTables();
    }

    /**
     * Move a selected meal from Favorites to Cooked.
     * 
     * Validates selection in favoritesTable, prevents duplicates in Cooked,
     * saves both lists to disk after move.
     */
    private void moveSelectedFavoriteToCooked() {
        MealRow selected = favoritesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("Select a Favorite first.");
            return;
        }

        String id = normalizeId(selected.getId());
        if (id.isBlank()) {
            alert("Invalid favorite id.");
            return;
        }

        // Move semantics: remove from favorites first
        String name = favorites.remove(id);
        if (name == null) {
            alert("This id is not in Favorites: " + id);
            return;
        }

        // Prevent duplicates in Cooked
        if (cooked.containsKey(id)) {
            storage.saveFavorites(favorites);
            refreshListTables();
            alert("Already in Cooked. Removed from Favorites (id=" + id + ").");
            return;
        }

        cooked.put(id, name);

        storage.saveFavorites(favorites);
        storage.saveCooked(cooked);
        refreshListTables();
    }

    /**
     * Remove a selected meal from either Favorites or Cooked list.
     * 
     * Can remove from one or both lists (if rows are selected in both).
     */
    private void removeSelectedFromLists() {
        MealRow favSel = favoritesTable.getSelectionModel().getSelectedItem();
        MealRow cookedSel = cookedTable.getSelectionModel().getSelectedItem();

        boolean removedFav = false;
        boolean removedCooked = false;

        if (favSel != null) {
            String id = normalizeId(favSel.getId());
            removedFav = (favorites.remove(id) != null);
            if (removedFav) storage.saveFavorites(favorites);
        }

        if (cookedSel != null) {
            String id = normalizeId(cookedSel.getId());
            removedCooked = (cooked.remove(id) != null);
            if (removedCooked) storage.saveCooked(cooked);
        }

        if (!removedFav && !removedCooked) {
            alert("Select a row in Favorites or Cooked first.");
            return;
        }

        refreshListTables();
    }

    /**
     * Refresh the UI tables (favorites and cooked) from in-memory maps.
     * Call this after any change to favorites/cooked maps.
     */
    private void refreshListTables() {
        favoriteRows.clear();
        for (var e : favorites.entrySet()) {
            favoriteRows.add(new MealRow(e.getKey(), e.getValue()));
        }

        cookedRows.clear();
        for (var e : cooked.entrySet()) {
            cookedRows.add(new MealRow(e.getKey(), e.getValue()));
        }
    }

    /**
     * Update the details panel with a meal's information.
     * 
     * If meal is valid:
     * - Sets title, metadata (category, area)
     * - Loads and displays thumbnail image
     * - Populates ingredients list with measures
     * - Formats and displays cooking instructions
     * 
     * If meal is invalid:
     * - Clears all fields to "no meal selected" state
     * 
     * @param meal the meal to display (may be null or invalid)
     */
    private void setDetails(MealDetails meal) {
        currentMeal = meal;

        if (!isValidMeal(meal)) {
            titleLabel.setText("No details available");
            metaLabel.setText("");
            thumb.setImage(null);
            ingredientsList.getItems().clear();
            instructionsArea.setText("");
            return;
        }

        titleLabel.setText(safe(meal.getStrMeal()));

        String meta = "ID: " + safe(meal.getIdMeal())
                + " | " + safe(meal.getStrCategory())
                + " | " + safe(meal.getStrArea());
        metaLabel.setText(meta);

        // Thumbnail
        String url = safe(meal.getStrMealThumb()).trim();
        if (!url.isBlank()) {
            try {
                thumb.setImage(new Image(url, true));
            } catch (Exception ignored) {
                thumb.setImage(null);
            }
        } else {
            thumb.setImage(null);
        }

        // Ingredients
        ingredientsList.getItems().clear();
        Map<String, String> ing = meal.getIngredientsWithMeasures();
        for (Map.Entry<String, String> e : ing.entrySet()) {
            String ingredient = safe(e.getKey()).trim();
            String measure = safe(e.getValue()).trim();

            if (ingredient.isBlank()) {
                continue;
            }

            if (measure.isBlank()) {
                ingredientsList.getItems().add(ingredient);
            } else {
                ingredientsList.getItems().add(ingredient + " : " + measure);
            }
        }

        // Instructions formatting (avoid weird squares / bullets)
        instructionsArea.setText(formatInstructions(meal.getStrInstructions()));
    }

    /*
     * STARTUP & HELPERS:
     * - loadListsFromDisk() ~> restore favorites/cooked on startup
     * - runInBackground() ~> spawn daemon thread for API calls
     * - alert() ~> show dialog box to user
     * - Utility methods: normalizeId, isValidMeal, safe, formatInstructions
     */

    /**
     * Load saved favorites and cooked lists from disk on startup.
     * If files don't exist or are corrupted, starts with empty lists.
     */
    private void loadListsFromDisk() {
        favorites.clear();
        favorites.putAll(storage.loadFavorites());

        cooked.clear();
        cooked.putAll(storage.loadCooked());
    }

    /**
     * Run a task on a daemon background thread.
     * 
     * Use this for any long-running operation (network calls, file I/O)
     * to avoid blocking the JavaFX UI thread.
     * 
     * If the task throws an exception, shows error alert on UI thread.
     * 
     * @param job the runnable task to execute
     */
    private void runInBackground(Runnable job) {
        Thread t = new Thread(() -> {
            try {
                job.run();
            } catch (Exception e) {
                Platform.runLater(() -> alert("Error: " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Show an information dialog to the user.
     * 
     * @param msg the message to display
     */
    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("MealLab");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    /**
     * Normalize a meal ID: trim whitespace, handle nulls.
     * 
     * @param input the input string (may be null or have whitespace)
     * @return trimmed string, or empty string if null
     */
    private static String normalizeId(String input) {
        return input == null ? "" : input.trim();
    }

    /**
     * Check if a meal object is valid (not null, has non-empty ID).
     * 
     * @param meal the meal to check
     * @return true if meal is valid and can be displayed
     */
    private static boolean isValidMeal(MealDetails meal) {
        return meal != null && meal.getIdMeal() != null && !meal.getIdMeal().trim().isBlank();
    }

    /**
     * Safe null-to-empty conversion.
     * Prevents NullPointerException when accessing string methods.
     * 
     * @param s the input string (may be null)
     * @return the string, or empty string if null
     */
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Format cooking instructions for display.
     * 
     * Normalizes line endings, removes control characters and API bullet symbols,
     * then splits into numbered steps.
     * 
     * Example:
     *   Input: "Step 1\nStep 2\nStep 3"
     *   Output: "1) Step 1\n\n2) Step 2\n\n3) Step 3"
     * 
     * @param s the raw instruction text from API (may be null)
     * @return formatted instruction text with numbered steps
     */
    private static String formatInstructions(String s) {
        if (s == null) {
            return "";
        }

        String out = s;

        // Normalize line endings
        out = out.replace("\r\n", "\n").replace("\r", "\n");

        // Remove control characters except newline/tab
        out = out.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // Remove weird bullet symbols from API
        out = out.replace("▢", "")
                .replace("□", "")
                .replace("▪", "")
                .replace("•", "")
                .replace("?", "");

        // Split on blank lines OR single newlines, then number steps
        String[] rawParts = out.split("\\n\\s*\\n|\\n");

        StringBuilder sb = new StringBuilder();
        int step = 1;

        for (String part : rawParts) {
            String p = part.trim();
            if (p.isBlank()) {
                continue;
            }

            sb.append(step).append(") ").append(p).append("\n\n");
            step++;
        }

        return sb.toString().trim();
    }
}
