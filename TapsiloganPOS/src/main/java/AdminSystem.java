import java.util.Map;
import java.util.Scanner;
import java.util.List;

public class AdminSystem {
    private IInventory inventory;
    private Menu menu;
    private CashierSystem cashier;

    public AdminSystem(IInventory inventory, Menu menu, CashierSystem cashier) {
        this.inventory = inventory;
        this.menu = menu;
        this.cashier = cashier;
    }

    public void run(Scanner sc) {
        while (true) {
            System.out.println("==========================================");
            System.out.println("           ADMIN SYSTEM");
            System.out.println("==========================================");
            System.out.println("1. View Inventory Status");
            System.out.println("2. Add Drink Stock");
            System.out.println("3. Add Ingredient Stock");
            System.out.println("4. Sales Report");
            System.out.println("0. Back to Main Menu");
            System.out.print("Choose an option: ");
            String opt = sc.nextLine();
            if (opt.equals("0")) break;
            switch(opt) {
                case "1":
                    showInventory();
                    break;
                case "2":
                    addDrinkStock(sc);
                    break;
                case "3":
                    addIngredient(sc);
                    break;
                case "4":
                    showSalesReport();
                    break;
                case "9":
                    menu.saveToFile(DataPaths.getMenuPath());
                    inventory.saveToFile(DataPaths.getInventoryPath());
                    cashier.saveSalesHistory(DataPaths.getSalesPath());
                    System.out.println("Data saved."); 
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void showInventory() {
        System.out.println("================================");
        System.out.println("       INVENTORY STATUS      ");
        System.out.println("================================");
        System.out.println("DRINKS:");
        for (Drink d : menu.getDrinks()) {
            System.out.println(d.getName() + "\t: " + d.getStock());
        }
        System.out.println("--------------------------------");
        System.out.println("INGREDIENTS:");
        for (Map.Entry<String,Ingredient> e : inventory.getIngredients().entrySet()) {
            Ingredient ing = e.getValue();
            System.out.println(ing.getName() + "\t: " + ing.getQuantity() + " " + ing.getUnit());
        }
        System.out.println("================================");
    }

    private void addDrinkStock(Scanner sc) {
        System.out.println("DRINKS:");
        for (Drink d : menu.getDrinks()) {
            System.out.println(d.getId() + ". " + d.getName() + " - Current stock: " + d.getStock());
        }
        System.out.print("Choose drink id: ");
        int id = Integer.parseInt(sc.nextLine());
        Item it = menu.findById(id);
        if (it instanceof Drink) {
            Drink dd = (Drink) it;
            System.out.print("Add amount: ");
            int amt = Integer.parseInt(sc.nextLine());
            inventory.addDrinkStock(dd, amt);
            System.out.println("Updated stock of " + dd.getName() + " to " + dd.getStock());
        } else {
            System.out.println("Invalid drink id.");
        }
    }

    private void addIngredient(Scanner sc) {
        System.out.print("Ingredient name: ");
        String name = sc.nextLine();

        System.out.print("Unit (kg/pcs/tbsp): ");
        String unit = sc.nextLine().trim().toLowerCase();
        if (!(unit.equals("kg") || unit.equals("pcs") || unit.equals("tbsp"))) unit = "kg";

        System.out.print("Amount to add: ");
        String input = sc.nextLine().trim();

        try {
            String numericOnly = input.replaceAll("[^0-9]", "");

            if (numericOnly.isEmpty()) {
                System.out.println("Invalid amount. Please enter a number (e.g., 5).");
                return;
            }

            int amount = Integer.parseInt(numericOnly);
            Map<String,Ingredient> map = inventory.getIngredients();
            String key = name.toLowerCase();
            Ingredient ing = map.get(key);
            if (ing == null) {
                ing = new Ingredient(key, unit, amount);
            } else {
                if (!unit.equals(ing.getUnit())) {
                    ing.setUnit(unit);
                }
                ing.setQuantity(ing.getQuantity() + amount);
            }
            map.put(key, ing);
            System.out.println("Updated " + key + " to " + ing.getQuantity() + " " + ing.getUnit());

        } catch (NumberFormatException e) {
            System.out.println("Invalid amount. Please enter a valid number (e.g., 5).");
        }
    }

    private void showSalesReport() {

        System.out.println("================================");
        System.out.println("          SALES REPORT         ");
        System.out.println("================================");
        
        List<Payment> sales = cashier.getSales();
        
        if (sales.isEmpty()) {
            System.out.println("No sales recorded yet.");
            System.out.println("================================");
            return;
        }
        
        double totalRevenue = 0;
        int totalTransactions = sales.size();
        
        System.out.println("Transaction History:");
        System.out.println("--------------------------------");
        for (Payment payment : sales) {
            System.out.printf("Payment ID: %d | Order ID: %d | Amount: Php %.2f | Date: %s\n",
                payment.getPaymentId(), payment.getOrderId(), payment.getAmountDue(), payment.getTimestamp());
            totalRevenue += payment.getAmountDue();
        }
        
        System.out.println("--------------------------------");
        System.out.printf("Total Transactions: %d\n", totalTransactions);
        System.out.printf("Total Revenue: Php %.2f\n", totalRevenue);
        System.out.println("================================");
    }
}