import java.util.Map;

public interface IInventory {
    boolean consumeDrink(Drink d);
    void addDrinkStock(Drink d, int amount);
    Map<String,Ingredient> getIngredients();
    void deductIngredient(String name, int amount);
    void setIngredients(Map<String,Ingredient> map);

    void applyRecipeForFood(Food food, int quantity);

    void saveToFile(String filename);
    void loadFromFile(String filename);
}