import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventory implements IInventory {
    private static Inventory instance;
    private Map<String, Ingredient> ingredients = new HashMap<>(); // key: lowercase name
    private Menu menu;
    private java.util.List<Drink> drinks = new java.util.ArrayList<>();
    private final Map<String, Map<String, Integer>> recipes = new HashMap<>();

    public Inventory() {
        this.menu = Menu.getInstance();
        instance = this;
        // defaults (use lowercase keys)
        ingredients.put("rice", new Ingredient("rice", "kg", 100));
        ingredients.put("chicken", new Ingredient("chicken", "kg", 50));
        ingredients.put("pork", new Ingredient("pork", "kg", 40));
        ingredients.put("beef", new Ingredient("beef", "kg", 30));
        ingredients.put("vegetables", new Ingredient("vegetables", "kg", 20));

        // default drinks can be empty; controllers may add via admin
        // recipes
        Map<String, Integer> tapsilog = new HashMap<>();
        tapsilog.put("beef", 1);
        tapsilog.put("garlic", 1);
        tapsilog.put("rice", 1);
        tapsilog.put("egg", 1);
        tapsilog.put("cooking oil", 5);
        recipes.put("Tapsilog", tapsilog);

        Map<String, Integer> tosilog = new HashMap<>();
        tosilog.put("rice", 1);
        tosilog.put("garlic", 1);
        tosilog.put("tocino", 2);
        tosilog.put("egg", 1);
        tosilog.put("cooking oil", 5);
        recipes.put("Tosilog", tosilog);

        Map<String, Integer> longsilog = new HashMap<>();
        longsilog.put("skinless longanisa", 2);
        longsilog.put("rice", 1);
        longsilog.put("egg", 1);
        longsilog.put("garlic", 1);
        longsilog.put("cooking oil", 5);
        recipes.put("Longsilog", longsilog);
    }

    // Singleton: single shared Inventory instance across the app
    public static Inventory getInstance() {
        if (instance == null) {
            instance = new Inventory();
        }
        return instance;
    }

    public List<Item> getAllItems() {
        return menu.getAllItems();
    }

    @Override
    public boolean consumeDrink(Drink d) { return d.consume(1); }

    @Override
    public void addDrinkStock(Drink d, int amount) { d.addStock(amount); }

    @Override
    public Map<String, Ingredient> getIngredients() { return ingredients; }

    @Override
    public void deductIngredient(String name, int amount) {
        String key = name == null ? "" : name.toLowerCase();
        ensureIngredientExists(key);
        Ingredient ing = ingredients.get(key);
        int newQty = Math.max(0, ing.getQuantity() - amount);
        ing.setQuantity(newQty);
        ingredients.put(key, ing);
    }

    @Override
    public void setIngredients(Map<String,Ingredient> map) {
        this.ingredients = map;
    }

    public void saveToFile(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            java.nio.file.Path target = java.nio.file.Paths.get(DataPaths.getInventoryPath());
            java.nio.file.Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), this);
            try {
                java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.out.println("Error saving inventory: " + e.getMessage());
        }
    }

    public void loadFromFile(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        java.io.File file = new File(DataPaths.getInventoryPath());
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Inventory loaded = mapper.readValue(file, Inventory.class);
                this.ingredients = loaded.ingredients;
                this.drinks = loaded.drinks == null ? new java.util.ArrayList<>() : loaded.drinks;
                // load recipes if present
                if (loaded.recipes != null && !loaded.recipes.isEmpty()) {
                    this.recipes.clear();
                    this.recipes.putAll(loaded.recipes);
                }
                return;
            } catch (IOException e) {
                if (attempt < 2) {
                    try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                } else {
                    System.out.println("Inventory file not found or corrupted, starting with defaults.");
                }
            }
        }
    }

    // Ensure ingredient exists in the map with default 0
    public void ensureIngredientExists(String name) {
        String key = name == null ? "" : name.toLowerCase();
        if (!ingredients.containsKey(key)) {
            String unit = key.equals("cooking oil") ? "tbsp" : "pcs";
            ingredients.put(key, new Ingredient(key, unit, 0));
        }
    }

    // Apply recipe by food name for given quantity
    public void applyRecipe(String foodName, int quantity) {
        Map<String, Integer> rec = recipes.get(foodName);
        if (rec == null) return; // no recipe defined
        for (Map.Entry<String, Integer> e : rec.entrySet()) {
            String ing = e.getKey();
            int amount = e.getValue() * quantity;
            ensureIngredientExists(ing);
            deductIngredient(ing, amount);
        }
    }

    // Convenience for Food
    public void applyRecipeForFood(Food food, int quantity) {
        if (food == null) return;
        applyRecipe(food.getName(), quantity);
    }

    // Drinks accessors for persistence in inventory.json
    public java.util.List<Drink> getDrinks() { return drinks; }
    public void setDrinks(java.util.List<Drink> drinks) { this.drinks = drinks; }

    // Recipe management for Admin
    public void setRecipe(String foodName, Map<String, Integer> recipe) {
        if (foodName == null || recipe == null) return;
        this.recipes.put(foodName, new java.util.HashMap<>(recipe));
    }

    public void removeRecipe(String foodName) {
        if (foodName == null) return;
        this.recipes.remove(foodName);
    }

    // Check if all ingredients for a food are sufficient for quantity
    public boolean isRecipeAvailable(String foodName, int quantity) {
        if (foodName == null || quantity <= 0) return false;
        Map<String, Integer> rec = this.recipes.get(foodName);
        if (rec == null || rec.isEmpty()) return true; // no recipe means no constraints
        for (Map.Entry<String,Integer> e : rec.entrySet()) {
            String key = e.getKey() == null ? "" : e.getKey().toLowerCase();
            int need = e.getValue() * quantity;
            ensureIngredientExists(key);
            Ingredient ing = this.ingredients.get(key);
            if (ing == null || ing.getQuantity() < need) return false;
        }
        return true;
    }
}
