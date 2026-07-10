package mg.itu.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PackageScanner {

    public static List<String> getClasses(String packageName) {
        List<String> classes = new ArrayList<>();

        try {
            String path = packageName.replace('.', '/');
            URL resource = Thread.currentThread().getContextClassLoader().getResource(path);

            if (resource == null) {
                System.out.println("[Framework] Package non trouve: " + packageName);
                return classes;
            }

            File directory = new File(resource.getFile()); // ito ilay manao anle package scanner
            if (!directory.exists()) {
                System.out.println("[Framework] Dossier non trouve: " + directory.getAbsolutePath());
                return classes;
            }

            for (File file : directory.listFiles()) {
                if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    classes.add(className);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return classes;
    }
}