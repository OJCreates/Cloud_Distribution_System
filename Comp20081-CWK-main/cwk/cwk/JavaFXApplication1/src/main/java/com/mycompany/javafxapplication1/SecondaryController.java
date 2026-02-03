package com.mycompany.javafxapplication1;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.Optional;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;



public class SecondaryController {
    private User currentUser;
    
    
    @FXML
    private TextField userTextField;
    
    @FXML
    private TableView<User> dataTableView;

    @FXML
    private Button secondaryButton;
    
    @FXML
    private Button refreshBtn;
    
    @FXML 
    private Button delBtn;
    
    @FXML
    private TextField customTextField;
    
    


    private void refreshTable() throws ClassNotFoundException {
        DB db = new DB();
        ObservableList<User> data = db.getDataFromTable();
        dataTableView.setItems(data);
        dataTableView.refresh();
    }
    
    @FXML
    private void RefreshBtnHandler(ActionEvent event){
        try{
            refreshTable();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        Stage primaryStage = (Stage) customTextField.getScene().getWindow();
        customTextField.setText((String)primaryStage.getUserData());
        Object ud = primaryStage.getUserData();
        if (ud != null) customTextField.setText(ud.toString());
    }
    

    
    @FXML 
    private void delAction(){
        if(currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            System.out.println("Only ADMIN can delete users");
            return;
        }
        
        User selected = dataTableView.getSelectionModel().getSelectedItem();
        if(selected == null) {
            System.out.println ("No user selected.");
            return;
        }
        if (selected.getUser().equalsIgnoreCase(currentUser.getUser())) {
            System.out.println("You cant deltere your own account while logged in");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete user: " + selected.getUser());
        alert.setContentText("Are you sure? This cannot be undone.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        
        try {
            DB db = new DB();
            boolean deleted = db.deleteUser(selected.getUser());
            
            if(deleted) {
                System.out.println("Deleted: " + selected.getUser());
                refreshTable();
            } else {
                System.out.println("Delete failed (user may not exist).");
            }
 
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    
    @FXML
    private void switchToPrimary(){
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {
            
        
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialise(User loggedIn) {
        this.currentUser = loggedIn;
        userTextField.setText(loggedIn.getUser() + " (" + loggedIn.getRole() + ")");
        
      
            
        TableColumn <User, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(
        new PropertyValueFactory<>("user"));
        
        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<User, String> passCol = new TableColumn("Pass");
        passCol.setCellValueFactory(
            new PropertyValueFactory<>("pass"));
        
        dataTableView.getColumns().clear();
        
        dataTableView.getColumns().addAll(userCol, roleCol, passCol);
        
        boolean isAdmin = "ADMIN".equalsIgnoreCase(loggedIn.getRole());
        delBtn.setDisable(!isAdmin);
        //adminButton.setDisable(!isAdmin);
        
        try {
            refreshTable();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SecondaryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
