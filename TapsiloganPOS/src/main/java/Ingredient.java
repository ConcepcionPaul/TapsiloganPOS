public class Ingredient {
    private String name;
    private String unit; // "kg" or "pcs"
    private int quantity;

    public Ingredient() {}

    public Ingredient(String name, String unit, int quantity) {
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
