package gr.unipi.meallab.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 * 
 * Launches the graphical desktop UI for MealLab.
 * 
 * Run with:
 *   mvn -pl meallab-app javafx:run
 * 
 * Or directly:
 *   java -cp <classpath> gr.unipi.meallab.app.MainFx
 * 
 * Window:
 * - Default size: 1200x750 pixels
 * - Contains the main MealLabView UI
 * - Supports search, meal details, favorites management, etc.
 * 
 * Related:
 * - Main.java: command-line interface version
 */
public class MainFx extends Application {

    /**
     * JavaFX lifecycle method called when the application starts.
     * 
     * Initializes the UI and shows the primary window.
     * 
     * @param stage the primary Stage (main window)
     */
    @Override
    public void start(Stage stage) {
        // Build the UI from MealLabView
        MealLabView view = new MealLabView();
        Scene scene = new Scene(view.getRoot(), 1200, 750);

        // Configure and show the window
        stage.setTitle("MealLab - JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Main method to launch the JavaFX application.
     * Automatically calls start() method on the JavaFX thread.
     * 
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
