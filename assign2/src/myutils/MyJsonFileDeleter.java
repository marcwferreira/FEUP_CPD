package myutils;

import java.io.File;

public class MyJsonFileDeleter {
    public static boolean deleteJsonFile(String filePath) {
        File file = new File(filePath);

        if (file.exists()) {
            if (file.delete()) {
                System.out.println("[GAME] Game JSON file deleted.");
                return true;
            } else {
                System.out.println("[GAME - Error] Failed to delete the JSON file.");
            }
        } else {
            System.out.println("[GAME - error] JSON file does not exist.");
        }

        return false;
    }
}
