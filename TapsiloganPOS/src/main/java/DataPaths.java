import java.nio.file.Paths;

public class DataPaths {
    public static String getMenuPath() {
        return Paths.get(System.getProperty("user.dir"), "menu.json").toAbsolutePath().toString();
    }
    public static String getInventoryPath() {
        return Paths.get(System.getProperty("user.dir"), "inventory.json").toAbsolutePath().toString();
    }
    public static String getSalesPath() {
        return Paths.get(System.getProperty("user.dir"), "sales.json").toAbsolutePath().toString();
    }
}
