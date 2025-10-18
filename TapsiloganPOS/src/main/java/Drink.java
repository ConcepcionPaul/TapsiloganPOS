public class Drink extends Product implements Stockable {
    private int stock;

    public Drink() { super(); }

    public Drink(int id, String name, double price, int stock) {
        super(id, name, price);
        this.stock = stock;
    }

    @Override
    public int getStock() { return stock; }
    @Override
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public void addStock(int amount) { stock += amount; }

    @Override
    public boolean consume(int amount) {
        if (stock >= amount) {
            stock -= amount;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getName() + " - Php " + String.format("%.2f", getPrice()) + "  [Stock: " + stock + "]";
    }
}
