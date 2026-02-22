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
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.ConcurrentHashMap;



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
    private static final String KEY = "aesEncryptionKey"; 


public String encrypt(String strToEncrypt) {
    try {
        SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
    } catch (Exception e) {
        System.out.println("Error while encrypting: " + e.toString());
    }
    return null;
}


public String decrypt(String strToDecrypt) {
    try {
        SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
    } catch (Exception e) {
        System.out.println("Error while decrypting: " + e.toString());
    }
    return null;
}
    
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
    
    private static final ConcurrentHashMap<String, Boolean> fileLocks = new ConcurrentHashMap<>();

    public boolean lockFile(String filePath) {
        return fileLocks.putIfAbsent(filePath, true) == null;
    }

    public void unlockFile(String filePath) {
        fileLocks.remove(filePath);
    }
    
    private static int currentServer = 1; 

    public String getNextStorageContainer() {
    int attempts = 0;
    
    while (attempts < 3) {
        String folderName = "storage_" + currentServer;
        
        if (isContainerHealthy(folderName)) {
            currentServer = (currentServer % 3) + 1; 
            return folderName + "/";
        } else {
            System.out.println("Health Check Failed for: " + folderName + ". Skipping...");
            currentServer = (currentServer % 3) + 1;
            attempts++;
        }
    }
    
    new File("storage_1").mkdir();
    return "storage_1/";
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



public ObservableList<FileMetadata> getFilesForUser(String user) throws ClassNotFoundException {
    ObservableList<FileMetadata> files = FXCollections.observableArrayList();
    String sql = "SELECT filename, path, owner FROM Files WHERE owner = ? " +
                 "UNION " +
                 "SELECT f.filename, f.path, f.owner FROM Files f " +
                 "JOIN Permissions p ON f.filename = p.filename WHERE p.user = ?";
    
    try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(fileName);
        var pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, user);
        pstmt.setString(2, user);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            files.add(new FileMetadata(
                rs.getString("filename"), 
                rs.getString("path"), 
                rs.getString("owner")
            ));
        }
    } catch (SQLException ex) {
        Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
        try { if (connection != null) connection.close(); } catch (SQLException e) {}
    }
    return files;
}

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
    
    
    public boolean deleteFileFromDB(String filename) throws ClassNotFoundException {
        String sql = "DELETE FROM Files WHERE filename = ?";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, filename);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try { if (connection != null) connection.close(); } catch (SQLException e) {}
        }
    }
    
    
    public void createPermissionsTable() throws ClassNotFoundException {
        String sql = "CREATE TABLE IF NOT EXISTS Permissions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "filename TEXT NOT NULL, " +
                    "user TEXT NOT NULL, " +
                    "permission TEXT NOT NULL, " + 
                    "FOREIGN KEY(filename) REFERENCES Files(filename), " +
                    "FOREIGN KEY(user) REFERENCES Users(name))";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { if (connection != null) connection.close(); } catch (SQLException e) {}
        }
    }


    public void grantPermission(String filename, String user, String permission) throws ClassNotFoundException {
        String sql = "INSERT OR REPLACE INTO Permissions (filename, user, permission) VALUES (?, ?, ?)";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, filename);
            pstmt.setString(2, user);
            pstmt.setString(3, permission.toUpperCase());
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { if (connection != null) connection.close(); } catch (SQLException e) {}
        }
    }


    public boolean hasPermission(String filename, String user, String requiredPermission) throws ClassNotFoundException {
        String sql = "SELECT permission FROM Permissions WHERE filename = ? AND user = ?";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, filename);
            pstmt.setString(2, user);
            ResultSet rs = pstmt.executeQuery();
        
            if (rs.next()) {
                String actualPermission = rs.getString("permission");
            
                if (actualPermission.equals("WRITE")) return true;
                return actualPermission.equals(requiredPermission);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try { if (connection != null) connection.close(); } catch (SQLException e) {}
        }
        return false; 
    }
    
    public boolean isContainerHealthy(String folderName) {
    File folder = new File(folderName);
    return folder.exists() && folder.isDirectory() && folder.canWrite();
}   
    

public void simulateNetworkLatency() {
    try {
        Random rand = new Random();
        int delaySeconds = rand.nextInt(10) + 5; 
        
        System.out.println("Simulating network latency: Delaying for " + delaySeconds + " seconds...");
        
        Thread.sleep(delaySeconds * 1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        e.printStackTrace();
    }
}

public Connection getConnection() throws ClassNotFoundException, SQLException {
    String dataBaseName = "comp20081_db"; 
    String userName = "admin"; 
    String userPassword = "VzIIgagBo66x"; 

    String url = "jdbc:mysql://lamp-server:3306/" + dataBaseName + "?useSSL=false";

    Class.forName("com.mysql.cj.jdbc.Driver"); 
    
    return DriverManager.getConnection(url, userName, userPassword);
}

    
public void registerUser(String username, String password, String role) {
    String insertQuery = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

    try (Connection conn = getConnection(); 
         PreparedStatement prepareStat = conn.prepareStatement(insertQuery)) {
        
        prepareStat.setString(1, username);
        prepareStat.setString(2, password); 
        prepareStat.setString(3, role);

        prepareStat.executeUpdate();
        System.out.println("User '" + username + "' added successfully to MySQL.");
        
    } catch (Exception e) {
        e.printStackTrace();
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