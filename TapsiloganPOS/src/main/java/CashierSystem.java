import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CashierSystem {
    private List<Order> pending = new ArrayList<>();
    private List<Payment> sales = new ArrayList<>();
    @SuppressWarnings("unused")
    private IInventory inventory;
    // Strategy: payment behavior is injected via IPaymentProcess
    private IPaymentProcess paymentProcess;

    public CashierSystem(IInventory inventory, IPaymentProcess paymentProcess) {
        this.inventory = inventory;
        this.paymentProcess = paymentProcess;
    }

    public void addOrder(Order o) { pending.add(o); }

    public List<Payment> getSales() {
        return sales;
    }

    public void run(Scanner sc) {
        while (true) {
            System.out.println("==========================================");
            System.out.println("           CASHIER SYSTEM");
            System.out.println("==========================================");
            if (pending.isEmpty()) {
                System.out.println("No pending orders");
            } else {
                System.out.println("PENDING ORDERS:");
                for (Order o : pending) {
                    System.out.println("Order ID: " + o.getOrderId() + ", Table: " + o.getTableNumber() + ", Total: Php " + String.format("%.2f", o.getTotal()));
                }
                System.out.print("Choose Order ID to process (or 0 to refresh): ");
                int id = Integer.parseInt(sc.nextLine());
                if (id == 0) continue;
                Order found = null;
                for (Order o : pending) if (Integer.parseInt(o.getOrderId()) == id) found = o;
                if (found == null) {
                    System.out.println("Order not found.");
                } else {
                    printReceipt(found);
                    System.out.print("Enter amount paid: Php ");
                    double paid = Double.parseDouble(sc.nextLine());
                    Payment p = paymentProcess.processPayment(found, paid);
                    sales.add(p);
                    System.out.println(p.getReceipt());
                    System.out.println("Payment successful! Salamat po!");
                    pending.remove(found);
                }
            }
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose: ");
            String opt = sc.nextLine();
            if (opt.equals("0")) break;
        }
    }

    private void printReceipt(Order o) {
        System.out.println("================================");
        System.out.println("         RECEIPT       ");
        System.out.println("================================");
        System.out.println("Order ID: " + o.getOrderId());
        System.out.println("Table: " + o.getTableNumber() + " (" + o.getTableCapacity() + " persons)") ;
        System.out.println("--------------------------------");
        for (Item it : o.getItems()) {
            System.out.printf("%-20s Php %6.2f\n", it.getName(), it.getPrice());
        }
        System.out.println("--------------------------------");
        System.out.printf("TOTAL: Php %.2f\n", o.getTotal());
        System.out.println("================================");
    }

    public void saveSalesHistory(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), this.sales);
        } catch (IOException e) {
            System.out.println("Error saving sales history: " + e.getMessage());
        }
    }

    public void loadSalesHistory(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Payment[] loaded = mapper.readValue(new File(filename), Payment[].class);
            this.sales = new ArrayList<>(java.util.Arrays.asList(loaded));
        } catch (IOException e) {
            System.out.println("Sales file not found or corrupted, starting fresh.");
        }
    }
}