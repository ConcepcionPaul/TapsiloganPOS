import java.util.stream.Collectors;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.beans.property.SimpleStringProperty;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.stage.Window;

public class CashierController {
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> colOrderId;
    @FXML private TableColumn<Order, String> colItems;
    @FXML private TableColumn<Order, String> colStatus;
    private ObservableList<Order> orderList = FXCollections.observableArrayList();
    // Menu availability view
    @FXML private TableView<Food> menuTable;
    @FXML private TableColumn<Food, String> colMenuFoodName;
    @FXML private TableColumn<Food, Double> colMenuFoodPrice;
    @FXML private TableColumn<Food, String> colMenuFoodAvail;

    public void initialize() {
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colItems.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getItems().stream().map(Item::getName).collect(Collectors.joining(", "))
        ));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        orderList.setAll(OrderManager.getInstance().getObservableOrders());
        ordersTable.setItems(orderList);
        // Observer: react to OrderManager's observable orders list updates
        OrderManager.getInstance().getObservableList().addListener((javafx.collections.ListChangeListener.Change<? extends Order> c) -> {
            orderList.setAll(OrderManager.getInstance().getObservableOrders());
        });

        // Setup menu availability table if present in FXML
        if (menuTable != null) {
            colMenuFoodName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colMenuFoodPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
            colMenuFoodAvail.setCellValueFactory(cd -> new SimpleStringProperty(
                Inventory.getInstance().isRecipeAvailable(cd.getValue().getName(), 1) ? "Available" : "Unavailable"
            ));
            loadMenuAvailability();
            startInventoryWatcher();
        }
    }

    private void loadMenuAvailability() {
        if (menuTable == null) return;
        ObservableList<Food> foods = FXCollections.observableArrayList(Menu.getInstance().getFoods());
        menuTable.setItems(foods);
        menuTable.refresh();
    }

    private Thread invWatchThread;
    private volatile long lastInvRefresh = 0L;

    // Observer: file watcher notifies UI when inventory.json changes
    private void startInventoryWatcher() {
        try {
            final java.nio.file.Path invPath = java.nio.file.Paths.get(DataPaths.getInventoryPath());
            final java.nio.file.Path dir = invPath.getParent();
            if (dir == null) return;
            final java.nio.file.WatchService ws = java.nio.file.FileSystems.getDefault().newWatchService();
            dir.register(ws, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
            invWatchThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        java.nio.file.WatchKey key = ws.take();
                        for (java.nio.file.WatchEvent<?> ev : key.pollEvents()) {
                            if (ev.kind() == java.nio.file.StandardWatchEventKinds.OVERFLOW) continue;
                            java.nio.file.Path changed = dir.resolve((java.nio.file.Path) ev.context());
                            if (changed.getFileName().toString().equals(invPath.getFileName().toString())) {
                                long now = System.currentTimeMillis();
                                if (now - lastInvRefresh < 250) continue;
                                lastInvRefresh = now;
                                Inventory.getInstance().loadFromFile(DataPaths.getInventoryPath());
                                javafx.application.Platform.runLater(this::loadMenuAvailability);
                            }
                        }
                        key.reset();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                    }
                }
            }, "CashierInventoryWatcher");
            invWatchThread.setDaemon(true);
            invWatchThread.start();
        } catch (Exception ignored) {}
    }

    @FXML
    private void onMarkCompleted() {
        Order sel = (Order) ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        sel.setStatus("Completed");
        OrderManager.getInstance().updateOrder(sel);
    }

    @FXML
    private void onProcessPayment() {
        Order sel = (Order) ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // 1) Summary first (with dynamic VAT and discount)
        String itemsList = sel.getItems().stream().map(Item::getName).collect(Collectors.joining("\n"));
        final double subtotal = sel.getItems().stream().mapToDouble(Item::getPrice).sum();

        Dialog<ButtonType> summaryDialog = new Dialog<>();
        summaryDialog.setTitle("Order Summary");
        summaryDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane sGrid = new GridPane();
        sGrid.setHgap(10);
        sGrid.setVgap(8);
        sGrid.setPadding(new Insets(10));

        Label sItemsLbl = new Label("Items:");
        TextArea sItemsArea = new TextArea(itemsList);
        sItemsArea.setEditable(false);
        sItemsArea.setPrefRowCount(8);
        VBox sItemsBox = new VBox(sItemsArea);
        VBox.setVgrow(sItemsArea, Priority.ALWAYS);

        Label sSubtotalText = new Label("Subtotal:");
        Label sSubtotalVal = new Label(format(subtotal));

        Label sVatText = new Label("VAT (12%):");
        Label sVatVal = new Label();

        CheckBox seniorCB = new CheckBox("Senior Citizen (20% discount)");
        CheckBox pwdCB = new CheckBox("PWD (20% discount)");

        Label sDiscountText = new Label("Discount:");
        Label sDiscountVal = new Label();

        Label sTotalText = new Label("Total Due:");
        Label sTotalVal = new Label();

        sGrid.add(sItemsLbl, 0, 0);
        sGrid.add(sItemsBox, 1, 0, 2, 1);
        sGrid.add(sSubtotalText, 0, 1);
        sGrid.add(sSubtotalVal, 1, 1);
        sGrid.add(sVatText, 0, 2);
        sGrid.add(sVatVal, 1, 2);
        sGrid.add(seniorCB, 1, 3);
        sGrid.add(pwdCB, 2, 3);
        sGrid.add(sDiscountText, 0, 4);
        sGrid.add(sDiscountVal, 1, 4);
        sGrid.add(sTotalText, 0, 5);
        sGrid.add(sTotalVal, 1, 5);

        summaryDialog.getDialogPane().setContent(sGrid);

        final double vatRate = 0.12;
        final double discountRate = 0.20;

        final double[] calcTotal = new double[1];
        final double[] calcVat = new double[1];
        final double[] calcDiscount = new double[1];

        java.util.function.BiConsumer<Boolean, Boolean> recalcSummary = (sen, pwd) -> {
            double base = subtotal;
            boolean anyDisc = sen || pwd;
            double discount = anyDisc ? base * discountRate : 0.0;
            double taxable = Math.max(0.0, base - discount);
            double vat = taxable * vatRate;
            double total = taxable + vat;
            calcDiscount[0] = discount;
            calcVat[0] = vat;
            calcTotal[0] = total;
            sDiscountVal.setText("- " + format(discount));
            sVatVal.setText(format(vat));
            sTotalVal.setText(format(total));
        };

        recalcSummary.accept(false, false);
        seniorCB.selectedProperty().addListener((obs, o, n) -> {
            if (n) pwdCB.setSelected(false);
            recalcSummary.accept(n, pwdCB.isSelected());
        });
        pwdCB.selectedProperty().addListener((obs, o, n) -> {
            if (n) seniorCB.setSelected(false);
            recalcSummary.accept(seniorCB.isSelected(), n);
        });

        java.util.Optional<ButtonType> sResult = summaryDialog.showAndWait();
        if (!sResult.isPresent() || sResult.get() != ButtonType.OK) return;

        final double finalSubtotal = subtotal;
        final double finalVat = calcVat[0];
        final double finalDiscount = calcDiscount[0];
        final double finalTotalDue = calcTotal[0];

        // 2) Prompt for payment after showing summary
        TextInputDialog payDialog = new TextInputDialog();
        payDialog.setTitle("Enter Payment");
        payDialog.setHeaderText("Total Due: " + format(finalTotalDue));
        payDialog.setContentText("Payment Amount (Php):");
        java.util.Optional<String> payRes = payDialog.showAndWait();
        if (!payRes.isPresent()) return;
        double payment;
        try { payment = Double.parseDouble(payRes.get().trim()); } catch (Exception e) { showAlert("Invalid amount"); return; }
        if (payment + 1e-6 < finalTotalDue) { showAlert("Insufficient payment"); return; }

        double change = payment - finalTotalDue;

        // 3) Show final receipt with all details
        StringBuilder receipt = new StringBuilder();
        receipt.append("Subtotal: ").append(format(finalSubtotal)).append("\n");
        receipt.append("VAT (12%): ").append(format(finalVat)).append("\n");
        receipt.append("Discount: -").append(format(finalDiscount)).append("\n");
        receipt.append("Total Due: ").append(format(finalTotalDue)).append("\n");
        receipt.append("Payment: ").append(format(payment)).append("\n");
        receipt.append("Change: ").append(format(change)).append("\n");

        // Receipt will be shown after building detailed receipt text below

        // 4) Update status
        sel.setStatus("Paid");
        OrderManager.getInstance().updateOrder(sel);

        // 4.1) Build payment details and persist sale to sales.json
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Payment> salesList = new ArrayList<>();
            File salesFile = new File(DataPaths.getSalesPath());
            if (salesFile.exists()) {
                Payment[] loaded = mapper.readValue(salesFile, Payment[].class);
                salesList = new ArrayList<>(Arrays.asList(loaded));
            }

            // Aggregate items into name -> (qty, unitPrice)
            Map<String, int[]> qtyMap = new HashMap<>(); // name -> [qty]
            Map<String, Double> priceMap = new HashMap<>();
            for (Item it : sel.getItems()) {
                String nm = it.getName();
                qtyMap.putIfAbsent(nm, new int[]{0});
                qtyMap.get(nm)[0] += 1;
                priceMap.put(nm, it.getPrice());
            }

            Payment paymentEntry = new Payment(Integer.parseInt(sel.getOrderId()), finalTotalDue, payment);
            java.util.List<Payment.PaymentLine> lines = new ArrayList<>();
            for (Map.Entry<String, int[]> e : qtyMap.entrySet()) {
                String nm = e.getKey();
                int qty = e.getValue()[0];
                double unit = priceMap.getOrDefault(nm, 0.0);
                lines.add(new Payment.PaymentLine(nm, qty, unit));
            }
            paymentEntry.setItems(lines);
            paymentEntry.setSubtotal(finalSubtotal);
            paymentEntry.setVat(finalVat);
            paymentEntry.setDiscount(finalDiscount);
            paymentEntry.setChange(change);

            // Build receipt text with details
            StringBuilder r = new StringBuilder();
            r.append("================================\n");
            r.append("       PAYMENT RECEIPT\n");
            r.append("================================\n");
            r.append("Payment ID: ").append(paymentEntry.getPaymentId()).append("\n");
            r.append("Date/Time: ").append(paymentEntry.getTimestamp()).append("\n");
            r.append("Order ID: ").append(paymentEntry.getOrderId()).append("\n");
            r.append("--------------------------------\n");
            for (Payment.PaymentLine ln : lines) {
                r.append(String.format("%s x%d  Php %.2f\n", ln.getName(), ln.getQuantity(), ln.getLineTotal()));
            }
            r.append("--------------------------------\n");
            r.append(String.format("Subtotal: Php %.2f\n", finalSubtotal));
            r.append(String.format("Discount: -Php %.2f\n", finalDiscount));
            r.append(String.format("VAT (12%%): Php %.2f\n", finalVat));
            r.append(String.format("Total Due: Php %.2f\n", finalTotalDue));
            r.append(String.format("Amount Paid: Php %.2f\n", payment));
            r.append(String.format("Change: Php %.2f\n", change));
            r.append("================================\n");
            paymentEntry.setReceipt(r.toString());
            // Show the detailed receipt to the cashier (same format as Admin View Receipt)
            Alert receiptAlert = new Alert(Alert.AlertType.INFORMATION);
            receiptAlert.setTitle("Payment Receipt");
            receiptAlert.setHeaderText("Order Paid");
            receiptAlert.setContentText(r.toString());
            receiptAlert.showAndWait();

            salesList.add(paymentEntry);
            mapper.writerWithDefaultPrettyPrinter().writeValue(salesFile, salesList);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 5) Auto-remove from Active Orders after 2 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> OrderManager.getInstance().getObservableList().remove(sel));
        delay.play();
    }

    @FXML
    private void onLogout() {
        try {
            // Prevent app from exiting when closing the last window
            javafx.application.Platform.setImplicitExit(false);

            // Close all currently open windows
            java.util.List<Window> open = new java.util.ArrayList<>(Window.getWindows());
            for (Window w : open) {
                if (w instanceof Stage) {
                    ((Stage) w).close();
                }
            }

            // Open login window fresh
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Stage loginStage = new Stage();
            loginStage.setTitle("TapsiloganPOS - Login");
            loginStage.setScene(new Scene(loader.load()));
            loginStage.setResizable(false);
            loginStage.show();

            // Restore default behavior
            javafx.application.Platform.setImplicitExit(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String format(double amount) {
        return "Php " + String.format("%.2f", amount);
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
