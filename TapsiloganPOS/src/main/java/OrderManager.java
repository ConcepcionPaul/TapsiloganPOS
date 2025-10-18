import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;

public class OrderManager {
    private static OrderManager instance = new OrderManager();
    private ObservableList<Order> orders = FXCollections.observableArrayList();

    private OrderManager() {}
    public static OrderManager getInstance() { return instance; }

    public void addOrder(Order o) {
        orders.add(o);
    }

    public void updateOrder(Order o) {
        // ensure list fires change
        int idx = orders.indexOf(o);
        if (idx >= 0) {
            orders.set(idx, o);
        }
    }

    public ObservableList<Order> getObservableList() { return orders; }
    public List<Order> getObservableOrders() { return new ArrayList<>(orders); }
}
