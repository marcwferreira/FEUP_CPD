package myutils;

import java.io.Console;
import java.util.Arrays;

public class MyPasswordPrompt {
    public static String readPasswordFromConsole(String prompt) {
        Console console = System.console();
        if (console == null) {
            throw new UnsupportedOperationException("Console is not available");
        }

        char[] passwordArray = console.readPassword(prompt);
        String password = new String(passwordArray);

        // Clear the password from memory for security
        Arrays.fill(passwordArray, ' ');

        return password;
    }
}
