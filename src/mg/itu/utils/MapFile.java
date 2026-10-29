package mg.itu.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.http.Part;

public class MapFile {
    private String fileName;
    private String extension;
    private byte[] bytes;

    public MapFile() {
    }

    public MapFile(String fileName, String extension, byte[] bytes) {
        this.fileName = fileName;
        this.extension = extension;
        this.bytes = bytes;
    }

    // Construit un MapFile depuis un Part (formulaire multipart)
    public static MapFile fromPart(Part part) throws IOException {
        if (part == null || part.getSize() == 0)
            return null;

        String fileName = part.getSubmittedFileName();
        if (fileName == null || fileName.isEmpty())
            return null;

        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            extension = fileName.substring(dot + 1).toLowerCase();
        }

        byte[] bytes = part.getInputStream().readAllBytes();
        return new MapFile(fileName, extension, bytes);
    }

    // Sauvegarde le fichier dans un dossier donne
    public void save(String directory) throws IOException {
        if (bytes == null)
            return;

        Path dir = Paths.get(directory);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path target = dir.resolve(fileName);
        Files.write(target, bytes);
    }

    // >>> ICI : nom encode pour URL <<<
    public String getEncodedFileName() {
        try {
            return java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}