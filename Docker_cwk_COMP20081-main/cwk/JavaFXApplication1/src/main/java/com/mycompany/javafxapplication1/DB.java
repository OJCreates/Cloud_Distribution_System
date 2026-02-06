/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * @author ntu-user
 */
public class DB {
    private String fileName = "jdbc:sqlite:comp20081.db";
    private int timeout = 30;
    private String dataBaseName = "COMP20081";
    private String dataBaseTableName = "Users";
    Connection connection = null;
    private Random random = new SecureRandom();
    private String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private int iterations = 10000;
    private int keylength = 256;
    private String saltValue;
    
    /**
     * @brief constructor - generates the salt if it doesn't exists or load it from the file .salt
     */
    DB() {
        try {
            File fp = new File(".salt");
            if (!fp.exists()) {
                saltValue = this.getSaltvalue(30);
                FileWriter myWriter = new FileWriter(fp);
                myWriter.write(saltValue);
                myWriter.close();
            } else {
                Scanner myReader = new Scanner(fp);
                while (myReader.hasNextLine()) {
                    saltValue = myReader.nextLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
    /**
     * @brief create a new table
     * @param tableName name of type String
     */
    public void createTable(String tableName) throws ClassNotFoundException {
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + tableName + " ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
              + "name TEXT NOT NULL UNIQUE, "
              + "password TEXT NOT NULL, "
              + "role TEXT NOT NULL DEFAULT 'STANDARD'"
              + ")"
);


        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * @brief delete table
     * @param tableName of type String
     */
    public void delTable(String tableName) throws ClassNotFoundException {
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("drop table if exists " + tableName);
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * @brief add data to the database method
     * @param user name of type String
     * @param password of type String
     */
     public void addDataToDB(String user, String password) throws InvalidKeySpecException, ClassNotFoundException {
    
    String role = getDataFromTable().isEmpty() ? "ADMIN" : "STANDARD";
    
    
    String sql = "INSERT INTO " + dataBaseTableName + " (name, password, role) VALUES (?, ?, ?)";
    
    try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(fileName);
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setQueryTimeout(timeout);
        
        pstmt.setString(1, user);
        pstmt.setString(2, generateSecurePassword(password));
        pstmt.setString(3, role); 
        
        pstmt.executeUpdate();
        log("User " + user + " added to DB with role: " + role);
    } catch (SQLException ex) {
        Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}

    /**
     * @brief get data from the Database method
     * @retunr results as ResultSet
     */
    public ObservableList<User> getDataFromTable() throws ClassNotFoundException {
        ObservableList<User> result = FXCollections.observableArrayList();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select name, role from " + this.dataBaseTableName);
            while (rs.next()) {
                // read the result set
                result.add(new User(rs.getString("name"), rs.getString("role")));
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
        return result;
    }

    /**
     * @brief decode password method
     * @param user name as type String
     * @param pass plain password of type String
     * @return true if the credentials are valid, otherwise false
     */
    public boolean validateUser(String user, String pass) throws InvalidKeySpecException, ClassNotFoundException {
        Boolean flag = false;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select name, password from " + this.dataBaseTableName);
            String inPass = generateSecurePassword(pass);
            // Let's iterate through the java ResultSet
            while (rs.next()) {
                if (user.equals(rs.getString("name")) && rs.getString("password").equals(inPass)) {
                    flag = true;
                    break;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

        return flag;
    }

    
    public User authenticate(String user, String pass) throws InvalidKeySpecException, ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            
            ResultSet rs = statement.executeQuery("select name, password, role from " + this.dataBaseTableName);
            String inPass = generateSecurePassword(pass);
            
            while (rs.next()) {
                if (user.equals(rs.getString("name")) && rs.getString("password").equals(inPass)) {
                    return new User(rs.getString("name"), rs.getString("password"), rs.getString("role"));
                }
            }
        }  catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {if (connection != null) connection.close(); } catch (SQLException e) {System.err.println(e.getMessage()); }
        }
            return null;
    }
    private String getSaltvalue(int length) {
        StringBuilder finalval = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            finalval.append(characters.charAt(random.nextInt(characters.length())));
        }

        return new String(finalval);
    }

    /* Method to generate the hash value */
    private byte[] hash(char[] password, byte[] salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keylength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    public String generateSecurePassword(String password) throws InvalidKeySpecException {
        String finalval = null;

        byte[] securePassword = hash(password.toCharArray(), saltValue.getBytes());

        finalval = Base64.getEncoder().encodeToString(securePassword);

        return finalval;
    }

    /**
     * @brief get table name
     * @return table name as String
     */
    public String getTableName() {
        return this.dataBaseTableName;
    }

    /**
     * @brief print a message on screen method
     * @param message of type String
     */
    public void log(String message) {
        System.out.println(message);

    }

    
    public boolean createdUser(String username, String password, String role)
            throws InvalidKeySpecException, ClassNotFoundException {
        if (role == null || role.isBlank()) role = "STANDARD" ;
        role = role.toUpperCase();
        
        if (!role.equals("STANDARD") && !role.equals("ADMIN")) {
            throw new IllegalArgumentException("Role must be STANDARD OR ADMIN!");
            
        }
        
        Class.forName("org.sqlite.JDBC");
        try(Connection conn = DriverManager.getConnection(fileName)){
            String sql = "INSERT INTO " + dataBaseTableName + "(name, password, role) VALUES (?, ?,?)";
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, generateSecurePassword(password));
                ps.setString(3, role);
                ps.executeUpdate();
                return true;
            }
        } catch(SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            return false;
            
        }
    }
    public boolean deleteUser(String user) throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(fileName)) {
            String sql = "DELETE FROM " + dataBaseTableName + " WHERE name = ?";
            
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user);
                return ps.executeUpdate() > 0;
            }
  
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null,ex);
            return false;
        }
    }
    public boolean updatePassword(String username, String newPassword) 
        throws InvalidKeySpecException, ClassNotFoundException {
        
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(fileName)) {
            String sql = "UPDATE " + dataBaseTableName + " SET password = ? WHERE name = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, generateSecurePassword(newPassword));
                ps.setString(2, username);
                return ps.executeUpdate() > 0;
            }
        }catch (SQLException ex) {
               Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
                return false;
        }
            
        
    }
    public boolean updateRole(String username, String role) throws ClassNotFoundException {
    if (role == null || role.isBlank()) return false;
    role = role.toUpperCase();
    if (!role.equals("STANDARD") && !role.equals("ADMIN")) return false;

    Class.forName("org.sqlite.JDBC");
    try (Connection conn = DriverManager.getConnection(fileName)) {
        String sql = "UPDATE " + dataBaseTableName + " SET role = ? WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        }
    } catch (SQLException ex) {
        Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        return false;
    }
}

    public void createSessionTable () throws ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Sessions (" + 
                                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                                    "username STRING, " +
                                    "login_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
                    
        }
    }
    
    public void createSession(String username) throws ClassNotFoundException {
        String sql = "INSERT INTO Sessions (username) VALUES (?)";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var pstmt = connection.prepareStatement(sql);
            pstmt.setQueryTimeout(timeout);
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            log("Session created in SQLite for user: " + username);
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
    }
    
    
public void createFileTable() throws ClassNotFoundException {
    try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(fileName);
        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);
        
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Files (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "filename TEXT NOT NULL, " +
                                "owner TEXT NOT NULL, " +
                                "path TEXT NOT NULL, " +
                                "FOREIGN KEY(owner) REFERENCES Users(name))");
    } catch (SQLException ex) {
        Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
        try { if (connection != null) connection.close(); } catch (SQLException e) {}
    }
}

    public void addFileToDB(String filename, String owner, String path) throws ClassNotFoundException {
    String sql = "INSERT INTO Files (filename, owner, path) VALUES (?, ?, ?)";
    try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(fileName);
        var pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, filename);
        pstmt.setString(2, owner);
        pstmt.setString(3, path);
        pstmt.executeUpdate();
    } catch (SQLException ex) {
        Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
        try { if (connection != null) connection.close(); } catch (SQLException e) {}
    }
}
    
}


    
//    public static void main(String[] args) throws InvalidKeySpecException {
//        DB myObj = new DB();
//        myObj.log("-------- Simple Tutorial on how to make JDBC connection to SQLite DB ------------");
//        myObj.log("\n---------- Drop table ----------");
//        myObj.delTable(myObj.getTableName());
//        myObj.log("\n---------- Create table ----------");
//        myObj.createTable(myObj.getTableName());
//        myObj.log("\n---------- Adding Users ----------");
//        myObj.addDataToDB("ntu-user", "12z34");
//        myObj.addDataToDB("ntu-user2", "12yx4");
//        myObj.addDataToDB("ntu-user3", "a1234");
//        myObj.log("\n---------- get Data from the Table ----------");
//        myObj.getDataFromTable(myObj.getTableName());
//        myObj.log("\n---------- Validate users ----------");
//        String[] users = new String[]{"ntu-user", "ntu-user", "ntu-user1"};
//        String[] passwords = new String[]{"12z34", "1235", "1234"};
//        String[] messages = new String[]{"VALID user and password",
//            "VALID user and INVALID password", "INVALID user and VALID password"};
//
//        for (int i = 0; i < 3; i++) {
//            System.out.println("Testing " + messages[i]);
//            if (myObj.validateUser(users[i], passwords[i], myObj.getTableName())) {
//                myObj.log("++++++++++VALID credentials!++++++++++++");
//            } else {
//                myObj.log("----------INVALID credentials!----------");
//            }
//        }
//    }