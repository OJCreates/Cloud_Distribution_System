package com.mycompany.javafxapplication1;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files; 
import java.nio.file.Path;  
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox; 
import javafx.scene.control.Alert.AlertType;

import loadbalancer.LoadBalancer;

public class SecondaryController {
    @FXML private TableView<User> dataTableView;
    @FXML private Label welcomeLabel; 
    @FXML private Button deleteBtn;
    @FXML private Button revokeBtn;
    @FXML private Button sshTestBtn; 
    @FXML private Button promoteBtn;
    @FXML private TextField newNameField;
    @FXML private PasswordField newPassField;
    @FXML private TextArea terminalOutput;
    @FXML private TextField terminalInput;
    @FXML private Button uploadDoc;
    @FXML private Button downloadBtn;
    @FXML private Button deleteFile;
    @FXML private ListView<String> fileListView;
    
    private String currentUserRole;
    private String currentUsername;

    public void initialise(String username, String role) {
        this.currentUsername = username;
        this.currentUserRole = role;
        if (welcomeLabel != null) welcomeLabel.setText("Welcome " + username + " (" + role + ")");
       
        boolean isAdmin = "Admin".equalsIgnoreCase(role);
        
        if (deleteBtn != null) deleteBtn.setVisible(isAdmin);
        if (promoteBtn != null) promoteBtn.setVisible(isAdmin); 
        if (revokeBtn != null) revokeBtn.setVisible(isAdmin);

        setupTable();
        refreshTable();
        refreshFileList();
        LoadBalancer.getInstance();
    }

    private void setupTable() {
        dataTableView.getColumns().clear();
        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("user"));
        
        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        dataTableView.getColumns().addAll(userCol, roleCol);
    }

    private void refreshTable() {
        DB myObj = new DB();
        dataTableView.setItems(myObj.getDataFromTable());
    }
    
    @FXML
    private void handlePromoteUser() {
        User selectedUser = dataTableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a user to promote.").show();
            return;
        }

        try {
            DB myObj = new DB();
            myObj.updateUserRole(selectedUser.getUser(), "Admin");
            refreshTable();
            
            new Alert(Alert.AlertType.INFORMATION, "Success! " + selectedUser.getUser() + " is now an Admin.").show();
            new AuditService().logAction(currentUsername, "PROMOTE TO ADMIN", selectedUser.getUser());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleRevokeAdmin() {
        User selectedUser = dataTableView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a user to revoke.").show();
            return;
        }

        try {
            DB myObj = new DB();
            myObj.updateUserRole(selectedUser.getUser(), "User");
            
            refreshTable();
            new Alert(Alert.AlertType.INFORMATION, "Success! " + selectedUser.getUser() + " is now a User.").show();
            new AuditService().logAction(currentUsername, "REVOKE ADMIN", selectedUser.getUser());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleUpdateProfile() {
        String newName = newNameField.getText();
        String newPass = newPassField.getText();
     
        if (newName.isEmpty() || newPass.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Fields cannot be empty.").show();
            return;
        }

        try {
            DB myObj = new DB();
            myObj.updateUser(currentUsername, newName, newPass);

            currentUsername = newName; 
            if (welcomeLabel != null) welcomeLabel.setText("Welcome " + newName + " (" + currentUserRole + ")");

            newNameField.clear();
            newPassField.clear();

            refreshTable();
            
            new Alert(Alert.AlertType.INFORMATION, "Profile Updated Successfully!").show();
            new AuditService().logAction(currentUsername, "UPDATE PROFILE", "Self");
            
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error updating profile.").show();
        }
    }
  
    @FXML
    private void handleDeleteUser() {
        User selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected != null && "Admin".equalsIgnoreCase(currentUserRole)) {
            new DB().deleteUser(selected.getUser());
            new AuditService().logAction(currentUsername, "DELETE USER", selected.getUser());
            refreshTable();
        }
    }
    
    
    @FXML
    private void handleSSHTest() {
        SSHService ssh = new SSHService();
        boolean success = ssh.testConnection("localhost", 4848, "ntu-user", "ntu-user");
        
        if (success) {
            new Alert(Alert.AlertType.INFORMATION, "SSH Connection to Container Successful!").show();
        } else {
            new Alert(Alert.AlertType.ERROR, "SSH Connection Failed.").show();
        }
    }

    @FXML
    private void switchToPrimary() {
        SessionManager.clearSession();

        try {
            Stage stage = (Stage) dataTableView.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            stage.setScene(new Scene(loader.load(), 640, 480));
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    @FXML
    private void handleTerminalCommand() {
        String command = terminalInput.getText().trim();
        if (command.isEmpty()) return;

        terminalOutput.appendText("> " + command + "\n");
        terminalInput.clear();

        new Thread(() -> {
            SSHService ssh = new SSHService();
 
            String result = ssh.runCommand("localhost", 4848, "ntu-user", "ntu-user", command);

            javafx.application.Platform.runLater(() -> {
                terminalOutput.appendText(result + "\n");
            });
        }).start();
    }
    
@FXML
    private void uploadDoc() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload File");
        List<File> files = fileChooser.showOpenMultipleDialog(null);

        if (files != null) {
            ACLService acl = new ACLService(); 

            for (File file : files) {
                try {
                    File encryptedFile = EncryptionService.encryptFileForUpload(file, currentUsername);
                    
                    loadbalancer.LoadBalancerClient.sendFile(encryptedFile);
                    acl.grantOwner(file.getName(), currentUsername);
                    
                    new AuditService().logAction(currentUsername, "UPLOAD", file.getName());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Encryption or Upload Failed for: " + file.getName()).show();
                }
            }
            refreshFileList(); 
            new Alert(Alert.AlertType.INFORMATION, "Upload(s) Completed!").show();
        }
    }

@FXML
    private void handleDownloadFile() {
        ACLService acl = new ACLService();
        SSHService ssh = new SSHService();
        String validHost = "host.docker.internal";

        List<String> myFiles = acl.getAccessibleFiles(currentUsername);

        if (myFiles.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No files available to download.").show();
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(myFiles.get(0), myFiles);
        dialog.setTitle("Download File");
        dialog.setHeaderText("Select a file to download:");
        dialog.setContentText("File:");

        dialog.showAndWait().ifPresent(selectedFile -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Downloaded File");
            fileChooser.setInitialFileName(selectedFile);
            File saveLocation = fileChooser.showSaveDialog(null);

            if (saveLocation != null) {
                boolean downloaded = ssh.downloadFile(validHost, 4848, "ntu-user", "ntu-user", selectedFile, saveLocation.getAbsolutePath());
                if (!downloaded) {
                    downloaded = ssh.downloadFile(validHost, 4849, "ntu-user", "ntu-user", selectedFile, saveLocation.getAbsolutePath());
                }

                if (downloaded) {
                    try {
                        EncryptionService.decryptDownloadedFile(saveLocation, currentUsername);
                        
                        new AuditService().logAction(currentUsername, "DOWNLOAD", selectedFile);
                        new Alert(Alert.AlertType.INFORMATION, "File downloaded and decrypted successfully!").show();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Error decrypting file.").show();
                    }
                } else {
                    new Alert(Alert.AlertType.ERROR, "Download failed from servers.").show();
                }
            }
        });
    }
    
@FXML
    private void handleDeleteFile() {
        SSHService ssh = new SSHService();
        ACLService acl = new ACLService();
        List<String> allFiles = new ArrayList<>();

        String validHost = "host.docker.internal";

        List<String> files1 = ssh.listFiles(validHost, 4848, "ntu-user", "ntu-user");
        if (files1 != null) allFiles.addAll(files1);

        List<String> files2 = ssh.listFiles(validHost, 4849, "ntu-user", "ntu-user");
        if (files2 != null) allFiles.addAll(files2);

        List<String> allowedFiles = acl.getAccessibleFiles(currentUsername);
        
        List<String> finalViewList = new ArrayList<>();
        for (String file : allFiles) {
            if (allowedFiles.contains(file)) {
                finalViewList.add(file);
            }
        }

        finalViewList = finalViewList.stream().distinct().collect(java.util.stream.Collectors.toList());

        if (finalViewList.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No files found to delete.").show();
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(finalViewList.get(0), finalViewList);
        dialog.setTitle("Delete File");
        dialog.setHeaderText("WARNING: This will permanently delete the file.");
        dialog.setContentText("Select file to DELETE:");

        dialog.showAndWait().ifPresent(selectedFile -> {
            if (!acl.canWrite(selectedFile, currentUsername)) {
                new Alert(Alert.AlertType.ERROR, "ACCESS DENIED: You do not have permission to delete this file.").show();
                return;
            }

            boolean success = false;

            if (files1 != null && files1.contains(selectedFile)) {
                if(ssh.deleteFile(validHost, 4848, "ntu-user", "ntu-user", selectedFile)) {
                    success = true;
                }
            }

            if (files2 != null && files2.contains(selectedFile)) {
                if(ssh.deleteFile(validHost, 4849, "ntu-user", "ntu-user", selectedFile)) {
                    success = true;
                }
            }

            if (success) {
                new AuditService().logAction(currentUsername, "DELETE", selectedFile);
                refreshFileList(); 
                new Alert(Alert.AlertType.INFORMATION, "File deleted successfully!").show();
            } else {
                new Alert(Alert.AlertType.ERROR, "Delete Failed.").show();
            }
        });
    }
    @FXML
    private void refreshFileList() {
        if (fileListView == null) return; 

        ACLService acl = new ACLService();
        SSHService ssh = new SSHService();
        String validHost = "host.docker.internal";

        List<String> allowedFiles = acl.getAccessibleFiles(currentUsername);
        
        List<String> serverFiles = new ArrayList<>();
        List<String> f1 = ssh.listFiles(validHost, 4848, "ntu-user", "ntu-user");
        List<String> f2 = ssh.listFiles(validHost, 4849, "ntu-user", "ntu-user");
        
        if (f1 != null) serverFiles.addAll(f1);
        if (f2 != null) serverFiles.addAll(f2);

        List<String> visibleFiles = new ArrayList<>();
        for (String file : allowedFiles) {
            if (serverFiles.contains(file)) {
                visibleFiles.add(file);
            }
        }
   
        visibleFiles = visibleFiles.stream().distinct().collect(java.util.stream.Collectors.toList());

        fileListView.getItems().clear();
        fileListView.getItems().addAll(visibleFiles);
    }
    
    // --- REQUIREMENT 5: File Sharing ---
    @FXML
    private void handleShareFile() {
        ACLService acl = new ACLService();
        List<String> myFiles = acl.getAccessibleFiles(currentUsername);

        if (myFiles.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "You have no files to share.").show();
            return;
        }

        ChoiceDialog<String> fileDialog = new ChoiceDialog<>(myFiles.get(0), myFiles);
        fileDialog.setTitle("Share File");
        fileDialog.setHeaderText("Select a file to share:");
        fileDialog.setContentText("File:");

        fileDialog.showAndWait().ifPresent(selectedFile -> {
            TextInputDialog userDialog = new TextInputDialog();
            userDialog.setTitle("Share File");
            userDialog.setHeaderText("Sharing file: " + selectedFile);
            userDialog.setContentText("Enter username to share with:");

            userDialog.showAndWait().ifPresent(targetUser -> {
                if (targetUser.equalsIgnoreCase(currentUsername)) {
                    new Alert(Alert.AlertType.ERROR, "You cannot share a file with yourself.").show();
                    return;
                }
                
                List<String> permissionOptions = new ArrayList<>();
                permissionOptions.add("Read Only");     
                permissionOptions.add("Read & Write");  
                
                ChoiceDialog<String> permDialog = new ChoiceDialog<>("Read Only", permissionOptions);
                permDialog.setTitle("Permission Level");
                permDialog.setHeaderText("Select access level for " + targetUser + ":");
                permDialog.setContentText("Permission:");
                
                permDialog.showAndWait().ifPresent(selection -> {
                    String dbCode = "READ"; 
                    if (selection.equals("Read & Write")) dbCode = "WRITE";
                    
                    acl.addPermission(selectedFile, targetUser, dbCode);
                    new Alert(Alert.AlertType.INFORMATION, "Success! Shared " + selectedFile + " with " + targetUser).show();
                    new AuditService().logAction(currentUsername, "SHARE with " + targetUser, selectedFile);
                });
            });
        });
    }
    
    @FXML
    private void handleEditFile() {
        ACLService acl = new ACLService();
        SSHService ssh = new SSHService();
        String validHost = "host.docker.internal";

        List<String> myFiles = acl.getAccessibleFiles(currentUsername);

        if (myFiles.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No files available to edit.").show();
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(myFiles.get(0), myFiles);
        dialog.setTitle("Edit File");
        dialog.setHeaderText("Select a file to edit:");
        dialog.setContentText("File:");

        dialog.showAndWait().ifPresent(selectedFile -> {
            
            if (!acl.canWrite(selectedFile, currentUsername)) {
                new Alert(Alert.AlertType.ERROR, "ACCESS DENIED: You do not have permission to edit this file.").show();
                return;
            }

            String tempPath = System.getProperty("java.io.tmpdir") + File.separator + "EDIT_" + selectedFile;
            File tempFile = new File(tempPath);

            boolean downloaded = ssh.downloadFile(validHost, 4848, "ntu-user", "ntu-user", selectedFile, tempPath);
            if (!downloaded) {
                downloaded = ssh.downloadFile(validHost, 4849, "ntu-user", "ntu-user", selectedFile, tempPath);
            }

            if (!downloaded) {
                new Alert(Alert.AlertType.ERROR, "Could not fetch file content from server.").show();
                return;
            }

            try {
                byte[] encryptedBytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
                byte[] decryptedBytes = EncryptionService.decryptBytes(encryptedBytes, currentUsername, selectedFile);
                String content = new String(decryptedBytes);
                
                TextArea editor = new TextArea(content);
                editor.setPrefSize(400, 300);
                
                Alert editAlert = new Alert(Alert.AlertType.CONFIRMATION);
                editAlert.setTitle("Editing: " + selectedFile);
                editAlert.setHeaderText("Modify the file content:");
                editAlert.getDialogPane().setContent(editor);
                
                editAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            File rawFile = new File(System.getProperty("java.io.tmpdir") + File.separator + selectedFile);
                            java.nio.file.Files.write(rawFile.toPath(), editor.getText().getBytes());
                            
                            File encryptedUpload = EncryptionService.encryptFileForUpload(rawFile, currentUsername);
                            
                            loadbalancer.LoadBalancerClient.sendFile(encryptedUpload);
                            
                            new AuditService().logAction(currentUsername, "EDIT", selectedFile);
                            
                            new Alert(Alert.AlertType.INFORMATION, "File updated and encrypted successfully!").show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            new Alert(Alert.AlertType.ERROR, "Failed to encrypt and save changes.").show();
                        }
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error reading file. Make sure it was an encrypted file!").show();
            }
        });
    }
    
@FXML
    private void handleViewFile() {
        ACLService acl = new ACLService();
        SSHService ssh = new SSHService();
        String validHost = "host.docker.internal";

        List<String> myFiles = acl.getAccessibleFiles(currentUsername);

        if (myFiles.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No files available to view.").show();
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(myFiles.get(0), myFiles);
        dialog.setTitle("View File");
        dialog.setHeaderText("Select a file to view:");
        dialog.setContentText("File:");

        dialog.showAndWait().ifPresent(selectedFile -> {

            String tempPath = System.getProperty("java.io.tmpdir") + File.separator + "VIEW_" + selectedFile;
            File tempFile = new File(tempPath);

            boolean downloaded = ssh.downloadFile(validHost, 4848, "ntu-user", "ntu-user", selectedFile, tempPath);
            if (!downloaded) {
                downloaded = ssh.downloadFile(validHost, 4849, "ntu-user", "ntu-user", selectedFile, tempPath);
            }

            if (!downloaded) {
                new Alert(Alert.AlertType.ERROR, "Could not fetch file content.").show();
                return;
            }

            try {
                byte[] encryptedBytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
                
                byte[] decryptedBytes = EncryptionService.decryptBytes(encryptedBytes, currentUsername, selectedFile);

                String content = new String(decryptedBytes);
                
                TextArea viewer = new TextArea(content);
                viewer.setPrefSize(400, 300);
                viewer.setEditable(false);
                viewer.setStyle("-fx-control-inner-background: #f4f4f4;");
                
                Alert viewAlert = new Alert(Alert.AlertType.INFORMATION);
                viewAlert.setTitle("Viewing: " + selectedFile);
                viewAlert.setHeaderText("File Content (Decrypted Read-Only):");
                viewAlert.getDialogPane().setContent(viewer);
                
                new AuditService().logAction(currentUsername, "VIEW", selectedFile);
                
                viewAlert.showAndWait();
                
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error reading file. Make sure it was encrypted!").show();
            }
        });
    }
    
@FXML
    private void handleCreateFile() {
        TextInputDialog nameDialog = new TextInputDialog("new_document.txt");
        nameDialog.setTitle("Create New File");
        nameDialog.setHeaderText("Enter the name for your new file:");
        nameDialog.setContentText("Filename:");

        nameDialog.showAndWait().ifPresent(fileName -> {
            
            if (fileName.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Filename cannot be empty.").show();
                return;
            }

            TextArea editor = new TextArea();
            editor.setPrefSize(400, 300);
            editor.setPromptText("Type your file content here...");

            Alert createAlert = new Alert(Alert.AlertType.CONFIRMATION);
            createAlert.setTitle("Creating: " + fileName);
            createAlert.setHeaderText("Enter file content:");
            createAlert.getDialogPane().setContent(editor);

            createAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        String tempPath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
                        File tempFile = new File(tempPath);

                        java.nio.file.Files.write(tempFile.toPath(), editor.getText().getBytes());

                        File encryptedFile = EncryptionService.encryptFileForUpload(tempFile, currentUsername);

                        loadbalancer.LoadBalancerClient.sendFile(encryptedFile);

                        ACLService acl = new ACLService();
                        acl.grantOwner(fileName, currentUsername);

                        refreshFileList();
                        
                        new AuditService().logAction(currentUsername, "CREATE", fileName);
                        
                        new Alert(Alert.AlertType.INFORMATION, "File created and encrypted successfully!").show();

                    } catch (Exception e) {
                        e.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Failed to create or encrypt file.").show();
                    }
                }
            });
        });
    }
 
    @FXML
    private void handleViewLogs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("logs.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("System Logs");
            stage.setScene(new Scene(root, 650, 450));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not open Logs.").show();
        }
    }
}