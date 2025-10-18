import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerController {
    @FXML private TabPane tabPane;

    // Table selection
    @FXML private ListView<Table> tableList;
    @FXML private Label statusLabel;
    @FXML private Tab foodTab;
    @FXML private Tab drinkTab;
    @FXML private Tab summaryTab;

    // Food tab
    @FXML private ListView<Food> foodList;
    @FXML private TextField foodQtyField;
    @FXML private Label foodStatusLabel;

    // Drink tab
    @FXML private ListView<Drink> drinkList;
    @FXML private TextField drinkQtyField;
    @FXML private Label drinkStatusLabel;

    // Summary
    @FXML private Label selectedTableLabel;
    @FXML private ListView<String> selectedFoods;
    @FXML private ListView<String> selectedDrinks;
    @FXML private Label totalLabel;

    private final ObservableList<String> selectedFoodsObs = FXCollections.observableArrayList();
    private final ObservableList<String> selectedDrinksObs = FXCollections.observableArrayList();

    private Table selectedTable;
    private final List<Item> orderItems = new ArrayList<>();
    private final LinkedHashMap<Food, Integer> foodCounts = new LinkedHashMap<>();
    private final LinkedHashMap<Drink, Integer> drinkCounts = new LinkedHashMap<>();

    public void initialize() {
        try {
            // Load persisted data
            Menu.getInstance().loadFromFile(DataPaths.getMenuPath());
            Inventory.getInstance().loadFromFile(DataPaths.getInventoryPath());
            // Setup tables: 5 tables with capacities 2,3,5 (example distribution)
            ObservableList<Table> tables = FXCollections.observableArrayList();
            tables.add(new Table(1, 2));
            tables.add(new Table(2, 2));
            tables.add(new Table(3, 3));
            tables.add(new Table(4, 5));
            tables.add(new Table(5, 5));
            tableList.setItems(tables);

            // Load menus and drinks
            Menu menu = Menu.getInstance();
            foodList.getItems().setAll(menu.getFoods());
            drinkList.getItems().setAll(Inventory.getInstance().getDrinks());

            // Bind summary lists
            selectedFoods.setItems(selectedFoodsObs);
            selectedDrinks.setItems(selectedDrinksObs);

            // Ensure tabs are disabled until table chosen
            foodTab.setDisable(true);
            drinkTab.setDisable(true);
            summaryTab.setDisable(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSelectTable() {
        Table t = tableList.getSelectionModel().getSelectedItem();
        if (t == null) {
            statusLabel.setText("Select a table first");
            return;
        }
        if (t.isOccupied()) {
            statusLabel.setText("Table already occupied");
            return;
        }
        selectedTable = t;
        statusLabel.setText("Selected " + t.toString());
        selectedTableLabel.setText("Selected Table: " + t.toString());
        foodTab.setDisable(false);
        drinkTab.setDisable(false);
        summaryTab.setDisable(false);
        tabPane.getSelectionModel().select(foodTab);
    }

    @FXML
    private void onAddFood() {
        if (!ensureTableSelected()) return;
        Food f = foodList.getSelectionModel().getSelectedItem();
        if (f == null) {
            foodStatusLabel.setText("Choose a food item");
            return;
        }
        int qty = parseQty(foodQtyField.getText());
        if (qty <= 0) qty = 1;
        for (int i = 0; i < qty; i++) orderItems.add(f);
        foodCounts.put(f, foodCounts.getOrDefault(f, 0) + qty);
        foodStatusLabel.setText("Added to order");
        refreshSelectedLists();
        updateTotal();
    }

    @FXML
    private void onAddDrink() {
        if (!ensureTableSelected()) return;
        Drink d = drinkList.getSelectionModel().getSelectedItem();
        if (d == null) {
            drinkStatusLabel.setText("Choose a drink item");
            return;
        }
        int qty = parseQty(drinkQtyField.getText());
        if (qty <= 0) qty = 1;
        Inventory inv = Inventory.getInstance();
        int added = 0;
        for (int i = 0; i < qty; i++) {
            if (inv.consumeDrink(d)) {
                orderItems.add(d);
                added++;
            } else {
                break;
            }
        }
        if (added == 0) {
            drinkStatusLabel.setText("Insufficient stock");
            return;
        }
        drinkCounts.put(d, drinkCounts.getOrDefault(d, 0) + added);
        drinkStatusLabel.setText("Added to order");
        refreshSelectedLists();
        updateTotal();
    }

    @FXML
    private void onPlaceOrder() {
        if (!ensureTableSelected()) return;
        if (orderItems.isEmpty()) {
            statusLabel.setText("Add items before placing order");
            return;
        }
        Order o = new Order(selectedTable.getTableNumber(), selectedTable.getCapacity());
        for (Item it : orderItems) {
            if (it instanceof Drink) {
                // already consumed stock per add
                o.addItem(it, 1);
            } else {
                o.addItem(it, 1);
            }
        }
        // Apply recipes per food count
        Inventory inv = Inventory.getInstance();
        for (Map.Entry<Food, Integer> e : foodCounts.entrySet()) {
            inv.applyRecipeForFood(e.getKey(), e.getValue());
        }
        inv.saveToFile(DataPaths.getInventoryPath());
        // Persist menu to save any drink stock changes
        Menu.getInstance().saveToFile(DataPaths.getMenuPath());
        o.setStatus("New");
        OrderManager.getInstance().addOrder(o);
        statusLabel.setText("Order placed. ID: " + o.getOrderId());
        // mark table occupied for this order
        if (selectedTable != null) {
            selectedTable.setOccupied(true);
        }
        // reset for next person
        orderItems.clear();
        selectedFoodsObs.clear();
        selectedDrinksObs.clear();
        foodCounts.clear();
        drinkCounts.clear();
        updateTotal();
        // reset UI back to table selection
        tableList.getSelectionModel().clearSelection();
        selectedTable = null;
        selectedTableLabel.setText("Selected Table: None");
        foodTab.setDisable(true);
        drinkTab.setDisable(true);
        summaryTab.setDisable(true);
        tabPane.getSelectionModel().selectFirst();
        if (foodQtyField != null) foodQtyField.clear();
        if (drinkQtyField != null) drinkQtyField.clear();
        statusLabel.setText("Order placed. Please select a table for the next customer.");
    }

    @FXML
    private void onRemoveOneFood() {
        int idx = selectedFoods.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Food f = getFoodByIndex(idx);
        if (f == null) return;
        Integer count = foodCounts.getOrDefault(f, 0);
        if (count <= 0) return;
        // remove one from orderItems
        for (int i = 0; i < orderItems.size(); i++) {
            if (orderItems.get(i) == f) { orderItems.remove(i); break; }
        }
        if (count == 1) foodCounts.remove(f); else foodCounts.put(f, count - 1);
        refreshSelectedLists();
        updateTotal();
    }

    @FXML
    private void onDeleteFood() {
        int idx = selectedFoods.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Food f = getFoodByIndex(idx);
        if (f == null) return;
        int toRemove = foodCounts.getOrDefault(f, 0);
        if (toRemove <= 0) return;
        // remove all occurrences from orderItems
        orderItems.removeIf(it -> it == f);
        foodCounts.remove(f);
        refreshSelectedLists();
        updateTotal();
    }

    @FXML
    private void onRemoveOneDrink() {
        int idx = selectedDrinks.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Drink d = getDrinkByIndex(idx);
        if (d == null) return;
        Integer count = drinkCounts.getOrDefault(d, 0);
        if (count <= 0) return;
        // remove one from orderItems and restock 1
        for (int i = 0; i < orderItems.size(); i++) {
            if (orderItems.get(i) == d) { orderItems.remove(i); break; }
        }
        Inventory.getInstance().addDrinkStock(d, 1);
        if (count == 1) drinkCounts.remove(d); else drinkCounts.put(d, count - 1);
        refreshSelectedLists();
        updateTotal();
    }

    @FXML
    private void onDeleteDrink() {
        int idx = selectedDrinks.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Drink d = getDrinkByIndex(idx);
        if (d == null) return;
        int toRemove = drinkCounts.getOrDefault(d, 0);
        if (toRemove <= 0) return;
        // remove all and restock accordingly
        int removed = 0;
        for (int i = orderItems.size() - 1; i >= 0; i--) {
            if (orderItems.get(i) == d) { orderItems.remove(i); removed++; }
        }
        if (removed > 0) Inventory.getInstance().addDrinkStock(d, removed);
        drinkCounts.remove(d);
        refreshSelectedLists();
        updateTotal();
    }

    private void refreshSelectedLists() {
        selectedFoodsObs.clear();
        for (Map.Entry<Food, Integer> e : foodCounts.entrySet()) {
            Food f = e.getKey();
            int c = e.getValue();
            selectedFoodsObs.add(f.getName() + " x" + c + " - Php " + String.format("%.2f", f.getPrice()*c));
        }
        selectedDrinksObs.clear();
        for (Map.Entry<Drink, Integer> e : drinkCounts.entrySet()) {
            Drink d = e.getKey();
            int c = e.getValue();
            selectedDrinksObs.add(d.getName() + " x" + c + " - Php " + String.format("%.2f", d.getPrice()*c));
        }
    }

    private Food getFoodByIndex(int index) {
        int i = 0;
        for (Food key : foodCounts.keySet()) {
            if (i == index) return key;
            i++;
        }
        return null;
    }

    private Drink getDrinkByIndex(int index) {
        int i = 0;
        for (Drink key : drinkCounts.keySet()) {
            if (i == index) return key;
            i++;
        }
        return null;
    }

    private boolean ensureTableSelected() {
        if (selectedTable == null) {
            statusLabel.setText("Please select a table first");
            tabPane.getSelectionModel().selectFirst();
            return false;
        }
        return true;
    }

    private void updateTotal() {
        double sum = 0;
        for (Item it : orderItems) sum += it.getPrice();
        totalLabel.setText("Php " + String.format("%.2f", sum));
    }

    private int parseQty(String txt) {
        try { return Integer.parseInt(txt == null ? "1" : txt.trim()); } catch (Exception e) { return 1; }
    }
}
