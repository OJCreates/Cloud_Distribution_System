package com.mycompany.javafxapplication1;


public class FileMetadata {
    private String filename;
    private String path;
    private String owner;

    public FileMetadata(String filename, String path, String owner) {
        this.filename = filename;
        this.path = path;
        this.owner = owner;
    }

    public String getFilename() { return filename; }
    public String getPath() { return path; }
    public String getOwner() { return owner; }
}