import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class Menu {
    private static Menu instance;
    private List<Food> foods = new ArrayList<>();
    private List<Drink> drinks = new ArrayList<>();

    public Menu() {
        instance = this;
        foods.add(new Food(1, "Tapsilog", 75.0));
        foods.add(new Food(2, "Tosilog", 75.0));
        foods.add(new Food(3, "Longsilog", 75.0));

        drinks.add(new Drink(13, "Coke", 25.0, 50));
        drinks.add(new Drink(14, "Sprite", 25.0, 45));
    }

    // Singleton: single shared Menu instance across the app
    public static Menu getInstance() {
        if (instance == null) {
            instance = new Menu();
        }
        return instance;
    }

    public List<Item> getAllItems() {
        List<Item> all = new ArrayList<>();
        all.addAll(foods);
        return all;
    }

    public List<Food> getFoods() { return foods; }
    public void setFoods(List<Food> foods) { this.foods = foods; }

    public List<Drink> getDrinks() { return drinks; }
    public void setDrinks(List<Drink> drinks) { this.drinks = drinks; }

    public Item findById(int id) {
        for (Food f : foods) if (f.getId() == id) return f;
        return null;
    }

    public void saveToFile(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Persist only foods
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DataPaths.getMenuPath()), this.foods);
        } catch (IOException e) {
            System.out.println("Error saving menu: " + e.getMessage());
        }
    }

    public void loadFromFile(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // Expect an array of Food in the file
            Food[] loadedFoods = mapper.readValue(new File(DataPaths.getMenuPath()), Food[].class);
            this.foods = new ArrayList<>(java.util.Arrays.asList(loadedFoods));
        } catch (IOException e) {
            System.out.println("Menu file not found or corrupted, starting with defaults.");
        }
    }
}
