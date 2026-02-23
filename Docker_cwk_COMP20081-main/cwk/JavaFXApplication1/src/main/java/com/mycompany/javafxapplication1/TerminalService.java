package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TerminalService {
    private File currentDirectory;
    private String currentUser;

    public TerminalService(String currentUser) {
        this.currentUser = currentUser;
        // Sets the initial simulated directory to the project's root folder
        this.currentDirectory = new File(System.getProperty("user.dir")); 
    }

    public String executeCommand(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        
        String[] parts = input.trim().split("\\s+");
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "whoami":
                    return currentUser != null ? currentUser : System.getProperty("user.name");
                case "ls":
                    return executeLs();
                case "mkdir":
                    return executeMkdir(parts);
                case "cp":
                    return executeCp(parts);
                case "mv":
                    return executeMv(parts);
                case "ps":
                    return executePs();
                case "tree":
                    return executeTree(currentDirectory, 0);
                case "nano":
                    return executeNano(parts);
                case "pwd":
                    return currentDirectory.getAbsolutePath();
                case "cd":
                    return executeCd(parts);
                default:
                    return "Command not found: " + command;
            }
        } catch (Exception e) {
            return "Error executing " + command + ": " + e.getMessage();
        }
    }

    private String executeLs() {
        File[] files = currentDirectory.listFiles();
        if (files == null || files.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            sb.append(f.getName()).append(f.isDirectory() ? "/" : "").append("\t");
        }
        return sb.toString();
    }

    private String executeMkdir(String[] parts) {
        if (parts.length < 2) return "mkdir: missing operand";
        File newDir = new File(currentDirectory, parts[1]);
        if (newDir.mkdir()) {
            return "";
        } else {
            return "mkdir: cannot create directory '" + parts[1] + "': File exists or permission denied";
        }
    }

    private String executeCp(String[] parts) throws IOException {
        if (parts.length < 3) return "cp: missing file operand";
        Path source = Paths.get(currentDirectory.getAbsolutePath(), parts[1]);
        Path target = Paths.get(currentDirectory.getAbsolutePath(), parts[2]);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return "";
    }

    private String executeMv(String[] parts) throws IOException {
        if (parts.length < 3) return "mv: missing file operand";
        Path source = Paths.get(currentDirectory.getAbsolutePath(), parts[1]);
        Path target = Paths.get(currentDirectory.getAbsolutePath(), parts[2]);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        return "";
    }

    private String executePs() {
        // Mocking 'ps' by listing actual running processes on the JVM's host system (Requires Java 9+)
        StringBuilder sb = new StringBuilder(String.format("%-10s %s\n", "PID", "COMMAND"));
        ProcessHandle.allProcesses().forEach(p -> {
            String cmd = p.info().command().orElse("unknown");
            if (cmd.contains(File.separator)) {
                cmd = cmd.substring(cmd.lastIndexOf(File.separator) + 1);
            }
            sb.append(String.format("%-10d %s\n", p.pid(), cmd));
        });
        return sb.toString();
    }

    private String executeTree(File dir, int level) {
        if (level > 3) return ""; // Limits depth to prevent overwhelming the GUI
        StringBuilder sb = new StringBuilder();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                sb.append("  ".repeat(Math.max(0, level)));
                sb.append("|-- ").append(f.getName()).append("\n");
                if (f.isDirectory()) {
                    sb.append(executeTree(f, level + 1));
                }
            }
        }
        return sb.toString();
    }

    private String executeCd(String[] parts) {
        if (parts.length < 2) return "";
        if (parts[1].equals("..")) {
            currentDirectory = currentDirectory.getParentFile() != null ? currentDirectory.getParentFile() : currentDirectory;
        } else {
            File target = new File(currentDirectory, parts[1]);
            if (target.exists() && target.isDirectory()) {
                currentDirectory = target;
            } else {
                return "cd: " + parts[1] + ": No such file or directory";
            }
        }
        return "";
    }

    private String executeNano(String[] parts) {
        if (parts.length < 2) return "nano: missing filename operand";
        return "Opening " + parts[1] + " in nano emulator... \n(Note: Terminal emulation mode. Please use the GUI File reader/editor for modifications.)";
    }
}