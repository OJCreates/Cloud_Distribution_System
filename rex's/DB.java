package com.mycompany.javafxapplication1;

import java.io.*;
import java.sql.*;
import java.util.*;
import javafx.collections.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;

public class DB {
    private static final String URL = "jdbc:mysql://lamp-server:3306/docker-cwk?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "ntu-user";
    private static final String PASSWORD = "ntu-password";
    
    private final String dataBaseTableName = "users"; 
    private String saltValue;

    public DB() {
        try {
            File fp = new File(".salt");
            if (!fp.exists()) {
                saltValue = getSaltvalue(30);
                try (FileWriter myWriter = new FileWriter(fp)) {
                    myWriter.write(saltValue);
                }
            } else {
                try (Scanner myReader = new Scanner(fp)) {
                    if (myReader.hasNextLine()) saltValue = myReader.nextLine();
                }
            }
            
            createTable(dataBaseTableName);
            createFileTable();
            
        } catch (IOException | ClassNotFoundException e) { 
            e.printStackTrace(); 
        }
    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public void createTable(String tableName) throws ClassNotFoundException {
        try {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                           + "id INT AUTO_INCREMENT PRIMARY KEY, "
                           + "username VARCHAR(255) UNIQUE NOT NULL, " 
                           + "password VARCHAR(255) NOT NULL, "
                           + "role VARCHAR(50) NOT NULL"
                           + ")";
                stmt.executeUpdate(sql);
                
                createDefaultAdmin(conn, tableName);
            }
        } catch (SQLException ex) { 
            System.err.println("Database Error: Is the Docker container running?");
            ex.printStackTrace(); 
        }
    }
    
    private void createDefaultAdmin(Connection conn, String tableName) {
        try {
            String check = "SELECT count(*) FROM " + tableName;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(check);
            if (rs.next() && rs.getInt(1) == 0) {

                String pass = generateSecurePassword("admin123"); 
                String sql = "INSERT INTO " + tableName + " (username, password, role) VALUES ('admin', '" + pass + "', 'Admin')";
                stmt.executeUpdate(sql);
                System.out.println("Default Admin Created (admin/admin123)");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String validateUserWithRole(String user, String pass) {
        String roleFound = null;
        String sql = "SELECT role FROM " + dataBaseTableName + " WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user);
            pstmt.setString(2, generateSecurePassword(pass));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                roleFound = rs.getString("role");
                new AuditService().logAction(user, "LOGIN", "System");
            }
        } catch (Exception e) { 
            System.out.println("Login Error: " + e.getMessage()); 
        }
        return roleFound;
    }

    public void addDataToDB(String user, String password, String role) {
        String sql = "INSERT INTO " + dataBaseTableName + " (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user);
            pstmt.setString(2, generateSecurePassword(password));
            pstmt.setString(3, role);
            pstmt.executeUpdate();
            
            new AuditService().logAction(user, "REGISTER", "System");
            
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public void deleteUser(String username) {
        String sql = "DELETE FROM " + dataBaseTableName + " WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public ObservableList<User> getDataFromTable() {
        ObservableList<User> result = FXCollections.observableArrayList();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + dataBaseTableName);
            while (rs.next()) {
                result.add(new User(rs.getString("username"), rs.getString("password"), rs.getString("role")));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return result;
    }

    private String getSaltvalue(int length) {
        StringBuilder finalval = new StringBuilder(length);
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random random = new SecureRandom();
        for (int i = 0; i < length; i++) finalval.append(chars.charAt(random.nextInt(chars.length())));
        return finalval.toString();
    }
    
    public String generateSecurePassword(String password) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltValue.getBytes(), 10000, 256);
        Arrays.fill(password.toCharArray(), Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return Base64.getEncoder().encodeToString(skf.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) { throw new AssertionError("Hashing Error: " + e.getMessage()); }
    }

    public void updateUserRole(String username, String newRole) {
        String sql = "UPDATE " + dataBaseTableName + " SET role = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newRole);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public void updateUser(String oldUsername, String newUsername, String newPassword) {
        String sql = "UPDATE " + dataBaseTableName + " SET username = ?, password = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newUsername);
            pstmt.setString(2, generateSecurePassword(newPassword));
            pstmt.setString(3, oldUsername);
            pstmt.executeUpdate();
            
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public void createFileTable() {
        String sql = "CREATE TABLE IF NOT EXISTS file_metadata ("
                   + "id INT AUTO_INCREMENT PRIMARY KEY, "
                   + "filename VARCHAR(255) NOT NULL, "
                   + "file_path VARCHAR(255) NOT NULL, "
                   + "server_name VARCHAR(50) NOT NULL, " 
                   + "file_size BIGINT, "
                   + "upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                   + ")";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("File Metadata Table Checked/Created.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveFileMetadata(String filename, String path, String serverName, long size) {
        String sql = "INSERT INTO file_metadata (filename, file_path, server_name, file_size) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, filename);
            pstmt.setString(2, path);
            pstmt.setString(3, serverName);
            pstmt.setLong(4, size);
            pstmt.executeUpdate();
            System.out.println("Metadata saved to MySQL for: " + filename);
            
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}