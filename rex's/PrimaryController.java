package com.mycompany.javafxapplication1;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PrimaryController {

    @FXML private TextField userTextField;
    @FXML private PasswordField passPasswordField;
    @FXML private Button registerBtn;
    @FXML private Button loginBtn; // Ensure this matches FXML fx:id

@FXML
private void switchToSecondary() {
    System.out.println("DEBUG: Login Button Clicked!"); // Check 1: Does the button work?

    String username = userTextField.getText();
    String password = passPasswordField.getText();

    if (username.isEmpty() || password.isEmpty()) {
        System.out.println("DEBUG: Empty fields detected.");
        showAlert("Error", "Fields cannot be empty");
        return;
    }

    try {
        DB myObj = new DB();
        System.out.println("DEBUG: Attempting DB Validation for user: " + username);
        
        String role = myObj.validateUserWithRole(username, password);
        System.out.println("DEBUG: DB returned role: " + role); // Check 2: What did the DB say?

        if (role != null) {
            System.out.println("DEBUG: Login Success! Loading Secondary View...");
            Stage stage = (Stage) userTextField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            
            SecondaryController controller = loader.getController();
            controller.initialise(username, role);
            
            stage.setScene(new Scene(root, 640, 480));
            stage.show();
        } else {
            System.out.println("DEBUG: Login Failed - Invalid Credentials");
            showAlert("Login Failed", "Invalid credentials.");
        }
    } catch (Exception e) {
        System.out.println("DEBUG: CRITICAL ERROR IN LOGIN LOGIC");
        e.printStackTrace(); // Check 3: Look for red text in your IDE output!
    }
}
    @FXML
    private void registerBtnHandler() {
        try {
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("register.fxml"));
            stage.setScene(new Scene(loader.load(), 640, 480));
            stage.setTitle("Register");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}