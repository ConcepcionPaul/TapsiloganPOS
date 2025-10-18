import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CustomerSystem {
    private List<Table> tables = new ArrayList<>();
    private Menu menu;
    private IInventory inventory;
    private CashierSystem cashier;

    public CustomerSystem(Menu menu, IInventory inventory, CashierSystem cashier) {
        this.menu = menu;
        this.inventory = inventory;
        this.cashier = cashier;
        tables.add(new Table(1,2));
        tables.add(new Table(2,2));
        tables.add(new Table(3,4));
        tables.add(new Table(4,4));
        tables.add(new Table(5,4));
    }

    public void run(Scanner sc) {
        System.out.println("AVAILABLE TABLES:");
        for (Table t : tables) {
            System.out.println(t.toString());
        }
        System.out.print("Choose table number: ");
        int tableNum = Integer.parseInt(sc.nextLine());
        Table chosen = null;
        for (Table t : tables) if (t.getTableNumber() == tableNum) chosen = t;
        if (chosen == null) {
            System.out.println("Invalid table number.");
            return;
        }
        System.out.println("Ordering for Table " + tableNum);
        Order order = new Order(tableNum, chosen.getCapacity());
        while (true) {
            System.out.println("FOOD MENU:");
            for (Food f : menu.getFoods()) {
                System.out.println(f.getId() + ". " + f.toString());
            }
            System.out.println("DRINKS:");
            for (Drink d : menu.getDrinks()) {
                System.out.println(d.getId() + ". " + d.toString());
            }
            System.out.println("0. Finish Order");
            System.out.print("Choose item id: ");
            int id = Integer.parseInt(sc.nextLine());
            if (id == 0) break;
            Item it = menu.findById(id);
            if (it == null) {
                System.out.println("Invalid item.");
                continue;
            }
            if (it instanceof Drink) {
                Drink dd = (Drink) it;
                if (!inventory.consumeDrink(dd)) {
                    System.out.println("Sorry, not enough stock for " + dd.getName());
                    continue;
                }
            } else {
                inventory.applyRecipeForFood((Food) it, 1);
            }
            order.addItem(it, 1);
            System.out.println("Added " + it.getName() + " to order.");
            System.out.print("Add more items? (y/n): ");
            String more = sc.nextLine().trim().toLowerCase();
            if (!more.equals("y")) break;
        }
        cashier.addOrder(order);
        if (inventory instanceof Inventory) {
            ((Inventory) inventory).saveToFile(DataPaths.getInventoryPath());
        }
        System.out.println("Order completed! Your order ID is: " + order.getOrderId());
        System.out.printf("Total amount: Php %.2f\n", order.getTotal());
        System.out.println("Press Enter to continue...");
        sc.nextLine();
    }
}
