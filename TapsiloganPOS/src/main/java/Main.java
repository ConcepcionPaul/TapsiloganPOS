import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.loadFromFile(DataPaths.getMenuPath());

        Inventory inventory = new Inventory();
        inventory.loadFromFile(DataPaths.getInventoryPath());

        PaymentProcess paymentProcess = new PaymentProcess();
        CashierSystem cashier = new CashierSystem(inventory, paymentProcess);
        cashier.loadSalesHistory(DataPaths.getSalesPath());

        AdminSystem admin = new AdminSystem(inventory, menu, cashier);
        CustomerSystem customer = new CustomerSystem(menu, inventory, cashier);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Saving data before exit...");
            menu.saveToFile(DataPaths.getMenuPath());
            inventory.saveToFile(DataPaths.getInventoryPath());
            cashier.saveSalesHistory(DataPaths.getSalesPath());
        }));

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("==========================================");
            System.out.println("           TOPSILOGAN");
            System.out.println("==========================================");
            System.out.println("1. Customer (Order)");
            System.out.println("2. Cashier");
            System.out.println("3. Admin");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");
            String opt = sc.nextLine();
            switch(opt) {
                case "1":
                    customer.run(sc);
                    break;
                case "2":
                    cashier.run(sc);
                    break;
                case "3":
                    admin.run(sc);
                    break;
                case "0":
                    System.out.println("Goodbye!");
                    sc.close();
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }
}
