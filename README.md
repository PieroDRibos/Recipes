# Recipes
=======
# MealLab

Multi-module Maven project for meal management with JavaFX UI.

## Modules

- **meallab-api**: Client library for TheMealDB API
- **meallab-app**: JavaFX desktop application + command-line interface

## Requirements

- Java 17+
- Maven 3.6+

## Build & Test

```bash
mvn clean test
```

## Run Command-Line App

```bash
mvn -pl meallab-app exec:java -Dexec.mainClass="gr.unipi.meallab.app.Main"
```

## Run JavaFX App

```bash
mvn -pl meallab-app javafx:run
```

## Build Shaded JAR

```bash
mvn -pl meallab-app package
```

## The runnable fat JAR will be at:
```
meallab-app/target/meallab-app-1.0.0-shaded.jar
```

## Run it with:
```bash
java -jar meallab-app/target/meallab-app-1.0.0-shaded.jar
```

## Features

- Search meals by ingredient or name
- View random meal suggestions
- Manage favorites and cooked meals
- Persistent storage (saved to `~/.meallab/`)
- Cross-platform JavaFX support (Windows, Linux, macOS)

## Project Structure

```
meallab/
├── meallab-api/ # API client module
│   └── src/
│       ├── main/java/
│       │   └── gr/unipi/meallab/api/
│       │       ├── client/ # MealLabClient
│       │       ├── model/ # POJOs (MealDetails, etc.)
│       │       └── exception/ # MealLabException
│       └── test/java/
├── meallab-app/ # Application module
│   └── src/
│       ├── main/java/
│       │   └── gr/unipi/meallab/app/
│       │       ├── Main.java # CLI entry point
│       │       ├── MainFx.java # JavaFX entry point
│       │       ├── MealLabView.java # JavaFX UI
│       │       ├── MealLabService.java
│       │       ├── MealStorage.java # Persistence layer
│       │       └── MealRow.java
│       └── test/java/
└── pom.xml # Parent POM
```

## Dependencies

- **JUnit 5.11.0** - Testing framework
- **Gson 2.11.0** - JSON serialization
- **JavaFX 21.0.4** - Desktop UI framework

All dependencies are managed via parent POM for version consistency.
