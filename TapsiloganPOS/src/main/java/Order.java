import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;

public class Order implements Payable {
    private static final SecureRandom RNG = new SecureRandom();
    private String orderId;
    private int tableNumber;
    private int tableCapacity;
    private List<Item> items = new ArrayList<>();
    private boolean premade = false;
    private String status;

    public Order() {}

    public Order(int tableNumber, int tableCapacity) {
        this.orderId = String.valueOf(100000 + RNG.nextInt(900000));
        this.tableNumber = tableNumber;
        this.tableCapacity = tableCapacity;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public int getTableCapacity() { return tableCapacity; }
    public void setTableCapacity(int tableCapacity) { this.tableCapacity = tableCapacity; }

    public void addItem(Item item, int qty) { for(int i=0; i<qty; i++) items.add(item); }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    @Override
    public double getTotal() {
        double sum = 0;
        for (Item it : items) sum += it.getPrice();
        return sum;
    }

    public boolean isPremade() { return premade; }
    public void setPremade(boolean premade) { this.premade = premade; }
}
