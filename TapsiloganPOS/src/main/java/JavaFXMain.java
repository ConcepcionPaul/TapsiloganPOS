import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Facade/Bootstrapper: centralizes app startup and scene initialization
public class JavaFXMain extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Menu.getInstance().loadFromFile(DataPaths.getMenuPath());
        Inventory.getInstance().loadFromFile(DataPaths.getInventoryPath());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setTitle("TapsiloganPOS - Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
