package com.mycompany.javafxapplication1;


public class FileMetadata {
    private String filename;
    private String path;

    public FileMetadata(String filename, String path) {
        this.filename = filename;
        this.path = path;
    }

    public String getFilename() { return filename; }
    public String getPath() { return path; }
}