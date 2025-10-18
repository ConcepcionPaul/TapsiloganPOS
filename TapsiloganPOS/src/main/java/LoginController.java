import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.InputStream;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private Map<String, Map<String,String>> creds;

    public void initialize() {
        // load credentials.json from resources
        try (InputStream in = getClass().getResourceAsStream("/credentials.json")) {
            ObjectMapper mapper = new ObjectMapper();
            creds = mapper.readValue(in, new TypeReference<Map<String, Map<String,String>>>(){});
        } catch (Exception e) {
            statusLabel.setText("Failed to load credentials");
            e.printStackTrace();
        }
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Enter username & password");
            return;
        }
        if (!creds.containsKey(user)) {
            statusLabel.setText("Invalid credentials");
            return;
        }
        String role = creds.get(user).get("role");
        String expected = creds.get(user).get("password");
        if (!expected.equals(pass)) {
            statusLabel.setText("Invalid credentials");
            return;
        }
        try {
            if ("admin".equalsIgnoreCase(role)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
                Stage s = new Stage();
                s.setTitle("Admin Dashboard");
                Scene adminScene = new Scene(loader.load(), 800, 600);
                adminScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                s.setScene(adminScene);
                s.show();
            } else {
                // Cashier: open two windows
                FXMLLoader loader1 = new FXMLLoader(getClass().getResource("/fxml/CashierWindow.fxml"));
                Stage cashierStage = new Stage();
                cashierStage.setTitle("Cashier Window");
                Scene cashierScene = new Scene(loader1.load(), 600, 400);
                cashierScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                cashierStage.setScene(cashierScene);
                cashierStage.show();

                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/fxml/CustomerWindow.fxml"));
                Stage customerStage = new Stage();
                customerStage.setTitle("Customer Window");
                Scene customerScene = new Scene(loader2.load(), 600, 400);
                customerScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                customerStage.setScene(customerScene);
                customerStage.setX(cashierStage.getX() + 620);
                customerStage.show();
            }
            // close login window
            ((Stage) usernameField.getScene().getWindow()).close();
        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Cannot open window: " + ex.getMessage());
        }
    }
}
