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
import javafx.scene.control.TextInputDialog;
import java.io.File;
import java.io.FileWriter;
import java.util.Optional;
import java.io.IOException;
import javafx.scene.control.ChoiceDialog;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import javafx.scene.control.TextArea;
import java.io.OutputStream;
import java.io.PrintStream;
import javafx.application.Platform;





public class SecondaryController {
    private User currentUser;
    private final Semaphore trafficLimiter = new Semaphore(2); 
    
    @FXML
    private TextArea terminalArea;
    
    @FXML
    private TextField terminalInput;
    
    @FXML
    private TerminalService terminalService;
    
    @FXML
    private TextField userTextField;
    
    @FXML
    private TableView<User> dataTableView;
    
    @FXML
    private TableView<FileMetadata> fileTableView;

    @FXML
    private Button secondaryButton;
    
    @FXML
    private Button refreshBtn;
    
    @FXML 
    private Button delBtn;
    
    @FXML
    private TextField customTextField;
    
    @FXML
    private Button createFileBtn;
    
    @FXML
    private Button updateFileBtn;
    
    @FXML
    private Button deleteFileBtn;
    
    @FXML
    private Button shareFileBtn;
    
    @FXML
    private Button readFileBtn;
    
    @FXML
    private Button healthCheckBtn;
    
    @FXML
private void handleHealthCheck() {
    DB db = new DB();
    StringBuilder healthReport = new StringBuilder("Storage Container Status:\n");
    
    for (int i = 1; i <= 3; i++) {
        String folder = "storage_" + i;
        if (db.isContainerHealthy(folder)) {
            healthReport.append("Healthy").append(folder).append(": HEALTHY\n");
        } else {
            healthReport.append("Unhealthy").append(folder).append(": UNHEALTHY (Missing or Read-Only)\n");
        }
    }

    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("System Health Check");
    alert.setHeaderText("Requirement 9: Health Monitoring");
    alert.setContentText(healthReport.toString());
    alert.show();
}
    
    @FXML
private void handleReadFile() {
    FileMetadata selected = fileTableView.getSelectionModel().getSelectedItem();
    if (selected == null) return;

    try {
        DB db = new DB();
        
        boolean isOwner = selected.getOwner().equals(currentUser.getUser());
        boolean hasReadAccess = db.hasPermission(selected.getFilename(), currentUser.getUser(), "READ");

        if (isOwner || hasReadAccess) {
            
            File file = new File(selected.getPath());
            Scanner reader = new Scanner(file);
            String encryptedContent = "";
            if (reader.hasNextLine()) {
                encryptedContent = reader.nextLine();
            }
            reader.close();

            
            String decryptedContent = db.decrypt(encryptedContent);

            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("File Content");
            alert.setHeaderText("Content of: " + selected.getFilename());
            alert.setContentText(decryptedContent);
            alert.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Access Denied: No READ permission.");
            alert.show();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    
    @FXML
private void handleShareFile() {
    FileMetadata selected = fileTableView.getSelectionModel().getSelectedItem();
    if (selected == null) return;

    
    TextInputDialog userDialog = new TextInputDialog();
    userDialog.setTitle("Share File");
    userDialog.setHeaderText("Grant access to another user");
    userDialog.setContentText("Enter username:");

    Optional<String> userResult = userDialog.showAndWait();
    
    userResult.ifPresent(targetUser -> {
        
        List<String> choices = Arrays.asList("READ", "WRITE");
        ChoiceDialog<String> permDialog = new ChoiceDialog<>("READ", choices);
        permDialog.setTitle("Permission Level");
        permDialog.setHeaderText("Select access level for " + targetUser);
        permDialog.setContentText("Permission:");

        Optional<String> permResult = permDialog.showAndWait();
        
        permResult.ifPresent(permission -> {
            try {
                DB db = new DB();
                db.grantPermission(selected.getFilename(), targetUser, permission);
                System.out.println("Granted " + permission + " to " + targetUser);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    });
}
    
  @FXML
private void handleDeleteFile() {
    FileMetadata selected = fileTableView.getSelectionModel().getSelectedItem();
    if (selected == null) return;

    try {
        DB db = new DB();
        String filePath = selected.getPath();

        boolean isOwner = selected.getOwner().equals(currentUser.getUser());
        
        if (!isOwner) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Access Denied: Only the file owner can delete this file.");
            alert.show();
            return; 
        }

        if (!db.lockFile(filePath)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Conflict Detected: This file is currently in use and cannot be deleted.");
            alert.show();
            return;
        }

        try {
            File file = new File(filePath);
            if (file.exists() && file.delete()) {
                db.deleteFileFromDB(selected.getFilename());
                
                refreshFileTable();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("File deleted successfully from " + filePath);
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Error: Physical file could not be deleted.");
                alert.show();
            }
        } finally {
            db.unlockFile(filePath); 
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

@FXML
private void handleUpdateFile() {
    if (!trafficLimiter.tryAcquire()) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Traffic Management");
        alert.setHeaderText("System Busy");
        alert.setContentText("The server is currently processing other updates. Please try again in a few seconds.");
        alert.show();
        return; 
    }

    FileMetadata selected = fileTableView.getSelectionModel().getSelectedItem();
    
    if (selected == null) {
        trafficLimiter.release();
        return;
    }
    
    try {
        DB db = new DB();
        String filePath = selected.getPath();

        boolean isOwner = selected.getOwner().equals(currentUser.getUser());
        boolean hasWriteAccess = db.hasPermission(selected.getFilename(), currentUser.getUser(), "WRITE");
        
        if (!isOwner && !hasWriteAccess) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Access Denied: You do not have WRITE permission.");
            alert.show();
            trafficLimiter.release(); 
            return; 
        }

        if (!db.lockFile(filePath)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Conflict Detected: This file is currently being accessed by another process.");
            alert.show();
            trafficLimiter.release();
            return;
        }

        try {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Update File");
            dialog.setHeaderText("Updating content for: " + selected.getFilename());
            dialog.setContentText("Enter new file content:");

            Optional<String> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                String newContent = result.get();
                
                String encryptedContent = db.encrypt(newContent); 
                
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write(encryptedContent);
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText("File content updated, encrypted, and locked successfully.");
                    alert.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            db.unlockFile(filePath); 
        }

    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        trafficLimiter.release(); 
    }
}
    
    @FXML
private void handleCreateFile() {
    
    if (!trafficLimiter.tryAcquire()) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Traffic Management");
        alert.setHeaderText("System Busy");
        alert.setContentText("The server is currently handling too many requests. Please try again shortly.");
        alert.show();
        return;
    }

    TextInputDialog dialog = new TextInputDialog("myFile.txt");
    dialog.setTitle("Create New File");
    dialog.setHeaderText("Create a New File");
    dialog.setContentText("Please enter the filename:");

    Optional<String> result = dialog.showAndWait();
    
    result.ifPresent(fileName -> {
        try {
            DB db = new DB();
            
            String targetFolder = db.getNextStorageContainer();
            File file = new File(targetFolder + fileName);
            
            
            if (file.createNewFile()) {
                String originalContent = "Default content for " + fileName;
                String encryptedContent = db.encrypt(originalContent);
                
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(encryptedContent);
                }
                
               
                db.addFileToDB(fileName, currentUser.getUser(), file.getAbsolutePath());
                refreshFileTable();
                
                
                System.out.println("File created: " + file.getAbsolutePath());
            } else {
                System.out.println("File already exists.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            trafficLimiter.release();
        }
    });
    
    if (!result.isPresent()) {
        trafficLimiter.release();
    }
}

    private void refreshTable() throws ClassNotFoundException {
        DB db = new DB();
        ObservableList<User> data = db.getDataFromTable();
        dataTableView.setItems(data);
        dataTableView.refresh();
    }
    
    @FXML
    private void RefreshBtnHandler(ActionEvent event) {
        try {
            refreshTable();
            refreshFileTable(); 
        
            Stage primaryStage = (Stage) customTextField.getScene().getWindow();
            Object ud = primaryStage.getUserData();
            if (ud != null) {
                customTextField.setText(ud.toString());
            }
        
        
            System.out.println("Refresh button clicked: Tables updated from SQLite.");
        
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
    this.terminalService = new TerminalService(loggedIn.getUser());
        if (terminalArea != null) {
            terminalArea.setText("Terminal ready. Type a command and press Enter.\n");
        }
        
        OutputStream out = new OutputStream() {
                @Override
                public void write(int b) {
                    Platform.runLater(() -> terminalArea.appendText(String.valueOf((char) b)));
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    String text = new String(b, off, len);
                    Platform.runLater(() -> terminalArea.appendText(text));
                }
            };

            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(out, true));

        
    userTextField.setText(loggedIn.getUser() + " (" + loggedIn.getRole() + ")");

    
    TableColumn<User, String> userCol = new TableColumn<>("User");
    userCol.setCellValueFactory(new PropertyValueFactory<>("user"));
    TableColumn<User, String> roleCol = new TableColumn<>("Role");
    roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
    TableColumn<User, String> passCol = new TableColumn<>("Pass");
    passCol.setCellValueFactory(new PropertyValueFactory<>("pass"));

    dataTableView.getColumns().clear();
    dataTableView.getColumns().addAll(userCol, roleCol, passCol);

    
    TableColumn<FileMetadata, String> fileNameCol = new TableColumn<>("File Name");
    fileNameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));

    TableColumn<FileMetadata, String> filePathCol = new TableColumn<>("Full Path");
    filePathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
    
    
    TableColumn<FileMetadata, String> ownerCol = new TableColumn<>("Owner");
    ownerCol.setCellValueFactory(new PropertyValueFactory<>("owner"));

    fileTableView.getColumns().clear();
    fileTableView.getColumns().addAll(fileNameCol, filePathCol, ownerCol);

   
    try {
        DB db = new DB();
        db.createFileTable();        
        db.createPermissionsTable();
        refreshTable();
        refreshFileTable();
    } catch (ClassNotFoundException ex) {
        Logger.getLogger(SecondaryController.class.getName()).log(Level.SEVERE, null, ex);
    }
}
    
    private void refreshFileTable() {
        try {
            DB db = new DB();
        
            ObservableList<FileMetadata> files = db.getFilesForUser(currentUser.getUser());
            fileTableView.setItems(files);
            fileTableView.refresh();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleTerminalCommand(ActionEvent event) {
        String command = terminalInput.getText();
        if (command.trim().isEmpty()) return;
        
        terminalArea.appendText(terminalService.executeCommand("pwd") + " $ " + command + "\n");
        String result = terminalService.executeCommand(command);
        
        if (!result.isEmpty()) {
            terminalArea.appendText(result + (result.endsWith("\n") ? "" : "\n"));
        }
        
        terminalInput.clear();
    }
    
}
