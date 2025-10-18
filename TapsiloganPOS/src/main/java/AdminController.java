import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class AdminController {
    @FXML private TabPane tabPane;
    @FXML private TableView<Ingredient> ingredientsTable;
    @FXML private TableColumn<Ingredient, String> colIngName;
    @FXML private TableColumn<Ingredient, String> colIngUnit;
    @FXML private TableColumn<Ingredient, Integer> colIngQty;
    @FXML private TableView<Drink> drinksTable;
    @FXML private TableColumn<Drink, String> colDrinkName;
    @FXML private TableColumn<Drink, Double> colDrinkPrice;
    @FXML private TableColumn<Drink, Integer> colDrinkStock;
    @FXML private TableView<Payment> salesTable;
    @FXML private TableColumn<Payment, Integer> colPaymentId;
    @FXML private TableColumn<Payment, Integer> colOrderId;
    @FXML private TableColumn<Payment, Double> colAmountDue;
    @FXML private TableColumn<Payment, String> colTimestamp;
    // Menu (foods)
    @FXML private TableView<Food> foodsTable;
    @FXML private TableColumn<Food, String> colFoodName;
    @FXML private TableColumn<Food, Double> colFoodPrice;
    @FXML private TableColumn<Food, String> colFoodAvail;

    public void initialize() {
        Menu.getInstance().loadFromFile(DataPaths.getMenuPath());
        Inventory.getInstance().loadFromFile(DataPaths.getInventoryPath());
        colIngName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colIngUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colIngQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colDrinkName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDrinkPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDrinkStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colPaymentId.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colAmountDue.setCellValueFactory(new PropertyValueFactory<>("amountDue"));
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        if (foodsTable != null) {
            colFoodName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colFoodPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
            colFoodAvail.setCellValueFactory(cd -> new SimpleStringProperty(
                Inventory.getInstance().isRecipeAvailable(cd.getValue().getName(), 1) ? "Available" : "Unavailable"
            ));
        }
        loadIngredients();
        loadDrinks();
        loadSales();
        loadFoods();
        startInventoryWatcher();
    }

    private Thread inventoryWatcherThread;
    private volatile long lastInventoryRefresh = 0L;

    private void startInventoryWatcher() {
        try {
            final java.nio.file.Path invPath = java.nio.file.Paths.get(DataPaths.getInventoryPath());
            final java.nio.file.Path dir = invPath.getParent();
            if (dir == null) return;
            final java.nio.file.WatchService ws = java.nio.file.FileSystems.getDefault().newWatchService();
            dir.register(ws, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
            inventoryWatcherThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        java.nio.file.WatchKey key = ws.take();
                        for (java.nio.file.WatchEvent<?> ev : key.pollEvents()) {
                            if (ev.kind() == java.nio.file.StandardWatchEventKinds.OVERFLOW) continue;
                            java.nio.file.Path changed = dir.resolve((java.nio.file.Path) ev.context());
                            if (changed.getFileName().toString().equals(invPath.getFileName().toString())) {
                                long now = System.currentTimeMillis();
                                if (now - lastInventoryRefresh < 250) continue; // debounce
                                lastInventoryRefresh = now;
                                Inventory.getInstance().loadFromFile(DataPaths.getInventoryPath());
                                javafx.application.Platform.runLater(() -> {
                                    loadIngredients();
                                    loadDrinks();
                                    loadFoods();
                                });
                            }
                        }
                        key.reset();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                    }
                }
            }, "InventoryJsonWatcher");
            inventoryWatcherThread.setDaemon(true);
            inventoryWatcherThread.start();
        } catch (Exception ex) {
        }
    }

    private int nextItemId() {
        int maxId = 0;
        for (Food f : Menu.getInstance().getFoods()) {
            if (f.getId() > maxId) maxId = f.getId();
        }
        for (Drink d : Inventory.getInstance().getDrinks()) {
            if (d.getId() > maxId) maxId = d.getId();
        }
        return maxId + 1;
    }

    private void loadIngredients() {
        Inventory inv = Inventory.getInstance();
        Map<String, Ingredient> ingMap = inv.getIngredients();
        ObservableList<Ingredient> list = FXCollections.observableArrayList(ingMap.values());
        ingredientsTable.setItems(list);
    }

    private void loadDrinks() {
        List<Drink> drinks = Inventory.getInstance().getDrinks();
        ObservableList<Drink> list = FXCollections.observableArrayList(drinks);
        drinksTable.setItems(list);
    }

    private void loadFoods() {
        if (foodsTable == null) return;
        List<Food> foods = Menu.getInstance().getFoods();
        ObservableList<Food> list = FXCollections.observableArrayList(foods);
        foodsTable.setItems(list);
    }

    private void loadSales() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Payment> sales = Arrays.asList(mapper.readValue(new File(DataPaths.getSalesPath()), Payment[].class));
            ObservableList<Payment> list = FXCollections.observableArrayList(sales);
            salesTable.setItems(list);
        } catch (IOException e) {
            salesTable.setItems(FXCollections.observableArrayList());
        }
    }

    @FXML
    private void onViewReceipt() {
        Payment sel = salesTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Select a sale first.");
            a.showAndWait();
            return;
        }
        String txt = sel.getReceipt();
        if (txt == null || txt.isEmpty()) {
            StringBuilder r = new StringBuilder();
            r.append("================================\n");
            r.append("       PAYMENT RECEIPT\n");
            r.append("================================\n");
            r.append("Payment ID: ").append(sel.getPaymentId()).append("\n");
            r.append("Date/Time: ").append(sel.getTimestamp()).append("\n");
            r.append("Order ID: ").append(sel.getOrderId()).append("\n");
            r.append("--------------------------------\n");
            if (sel.getItems() != null) {
                for (Payment.PaymentLine ln : sel.getItems()) {
                    r.append(String.format("%s x%d  Php %.2f\n", ln.getName(), ln.getQuantity(), ln.getLineTotal()));
                }
                r.append("--------------------------------\n");
            }
            r.append(String.format("Subtotal: Php %.2f\n", sel.getSubtotal()));
            r.append(String.format("Discount: -Php %.2f\n", sel.getDiscount()));
            r.append(String.format("VAT (12%%): Php %.2f\n", sel.getVat()));
            r.append(String.format("Total Due: Php %.2f\n", sel.getAmountDue()));
            r.append(String.format("Amount Paid: Php %.2f\n", sel.getPaid()));
            r.append(String.format("Change: Php %.2f\n", sel.getChange()));
            r.append("================================\n");
            txt = r.toString();
        }
        Alert receipt = new Alert(Alert.AlertType.INFORMATION, txt);
        receipt.setTitle("Payment Receipt");
        receipt.setHeaderText("Receipt Details");
        receipt.showAndWait();
    }

    @FXML
    private void onAddIngredient() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Ingredient");
        nameDialog.setHeaderText("Enter ingredient name:");
        nameDialog.setContentText("Name:");
        String name = nameDialog.showAndWait().orElse("");
        if (name.isEmpty()) return;

        TextInputDialog unitDialog = new TextInputDialog("kg");
        unitDialog.setTitle("Add Ingredient");
        unitDialog.setHeaderText("Enter unit for " + name + " (kg, pcs, or tbsp):");
        unitDialog.setContentText("Unit:");
        String unit = unitDialog.showAndWait().orElse("kg").trim().toLowerCase();
        if (!(unit.equals("kg") || unit.equals("pcs") || unit.equals("tbsp"))) unit = "kg";

        TextInputDialog qtyDialog = new TextInputDialog("0");
        qtyDialog.setTitle("Add Ingredient");
        qtyDialog.setHeaderText("Enter amount to add for " + name + ":");
        qtyDialog.setContentText("Amount:");
        try {
            int amount = Integer.parseInt(qtyDialog.showAndWait().orElse("0"));
            Inventory inv = Inventory.getInstance();
            Map<String, Ingredient> map = inv.getIngredients();
            String key = name.toLowerCase();
            inv.ensureIngredientExists(key);
            Ingredient existing = map.get(key);
            if (existing.getUnit() == null || existing.getUnit().isEmpty()) {
                existing.setUnit(unit);
            }
            if (!unit.equals(existing.getUnit())) {
                existing.setUnit(unit);
            }
            existing.setName(key);
            existing.setQuantity(existing.getQuantity() + amount);
            map.put(key, existing);
            inv.saveToFile(DataPaths.getInventoryPath());
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid amount value");
            alert.showAndWait();
        }
    }

    @FXML
    private void onAddDrink() {
        int id = nextItemId();

        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Drink");
        nameDialog.setHeaderText("Enter drink name:");
        nameDialog.setContentText("Name:");
        String name = nameDialog.showAndWait().orElse("");
        if (name.isEmpty()) return;

        TextInputDialog priceDialog = new TextInputDialog("0.0");
        priceDialog.setTitle("Add Drink");
        priceDialog.setHeaderText("Enter price for " + name + ":");
        priceDialog.setContentText("Price:");
        double price;
        try {
            price = Double.parseDouble(priceDialog.showAndWait().orElse("0.0"));
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid price");
            alert.showAndWait();
            return;
        }

        TextInputDialog stockDialog = new TextInputDialog("0");
        stockDialog.setTitle("Add Drink");
        stockDialog.setHeaderText("Enter initial stock for " + name + ":");
        stockDialog.setContentText("Stock:");
        int stock;
        try {
            stock = Integer.parseInt(stockDialog.showAndWait().orElse("0"));
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid stock");
            alert.showAndWait();
            return;
        }

        Drink newDrink = new Drink(id, name, price, stock);
        Inventory inv = Inventory.getInstance();
        inv.getDrinks().add(newDrink);
        inv.saveToFile(DataPaths.getInventoryPath());
    }

    @FXML
    private void onRefresh() {
        loadIngredients();
        loadDrinks();
        loadSales();
        loadFoods();
    }

    @FXML
    private void onSaveAll() {
        try {
            Menu.getInstance().saveToFile(DataPaths.getMenuPath());

            Inventory.getInstance().saveToFile(DataPaths.getInventoryPath());

            ObjectMapper mapper = new ObjectMapper();
            ObservableList<Payment> salesItems = salesTable.getItems();
            java.util.List<Payment> salesList = new java.util.ArrayList<>(salesItems);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DataPaths.getSalesPath()), salesList);

            Alert ok = new Alert(Alert.AlertType.INFORMATION, "All data saved.");
            ok.showAndWait();
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage());
            err.showAndWait();
        }
    }

    @FXML
    private void onRemoveIngredient() {
        Ingredient selected = ingredientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select an ingredient to remove");
            alert.showAndWait();
            return;
        }
        Inventory inv = Inventory.getInstance();
        Map<String, Ingredient> map = inv.getIngredients();
        map.remove(selected.getName().toLowerCase());
        inv.saveToFile(DataPaths.getInventoryPath());
    }

    @FXML
    private void onRemoveDrink() {
        Drink selected = drinksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Select a drink to remove");
            alert.showAndWait();
            return;
        }
        Inventory inv = Inventory.getInstance();
        java.util.List<Drink> drinks = inv.getDrinks();
        // Remove by id to avoid reference mismatch
        drinks.removeIf(d -> d.getId() == selected.getId());
        inv.saveToFile(DataPaths.getInventoryPath());
        loadDrinks();
    }

    @FXML
    private void onAddFood() {
        int id = nextItemId();
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Food");
        nameDialog.setHeaderText("Enter food name:");
        nameDialog.setContentText("Name:");
        String name = nameDialog.showAndWait().orElse("").trim();
        if (name.isEmpty()) return;

        TextInputDialog priceDialog = new TextInputDialog("0.0");
        priceDialog.setTitle("Add Food");
        priceDialog.setHeaderText("Enter price for " + name + ":");
        priceDialog.setContentText("Price:");
        double price;
        try {
            price = Double.parseDouble(priceDialog.showAndWait().orElse("0.0"));
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid price").showAndWait();
            return;
        }

        // Recipe input: comma-separated pairs name:qty
        TextInputDialog recipeDialog = new TextInputDialog("garlic:1, rice:1, egg:1");
        recipeDialog.setTitle("Add Food Recipe");
        recipeDialog.setHeaderText("Enter recipe ingredients as 'name:qty' pairs, separated by commas:");
        recipeDialog.setContentText("Recipe:");
        String recipeStr = recipeDialog.showAndWait().orElse("");
        java.util.HashMap<String,Integer> recipe = new java.util.HashMap<>();
        if (!recipeStr.trim().isEmpty()) {
            String[] parts = recipeStr.split(",");
            for (String p : parts) {
                String[] nv = p.trim().split(":");
                if (nv.length == 2) {
                    String ingName = nv[0].trim().toLowerCase();
                    try {
                        int qty = Integer.parseInt(nv[1].trim());
                        if (qty > 0) recipe.put(ingName, qty);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Persist food and recipe
        Food newFood = new Food(id, name, price);
        Menu.getInstance().getFoods().add(newFood);
        Menu.getInstance().saveToFile(DataPaths.getMenuPath());

        if (!recipe.isEmpty()) {
            Inventory inv = Inventory.getInstance();
            // ensure all declared ingredients exist
            for (String ing : recipe.keySet()) {
                inv.ensureIngredientExists(ing);
            }
            inv.setRecipe(name, recipe);
            inv.saveToFile(DataPaths.getInventoryPath());
        }

        loadFoods();
    }

    @FXML
    private void onRemoveFood() {
        if (foodsTable == null) return;
        Food selected = foodsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.INFORMATION, "Select a food to remove").showAndWait();
            return;
        }
        Menu.getInstance().getFoods().removeIf(f -> f.getId() == selected.getId());
        Menu.getInstance().saveToFile(DataPaths.getMenuPath());
        Inventory.getInstance().removeRecipe(selected.getName());
        Inventory.getInstance().saveToFile(DataPaths.getInventoryPath());
        loadFoods();
    }

    @FXML
    private void onLogout() {
        try {
            Stage currentStage = (Stage) tabPane.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("TapsiloganPOS - Login");
            loginStage.setScene(new Scene(loader.load()));
            loginStage.setResizable(false);
            loginStage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
