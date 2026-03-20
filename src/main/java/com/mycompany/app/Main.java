package com.mycompany.app;

import com.mycompany.controller.CircuitController;
import com.mycompany.view.ConsoleView;

import java.io.IOException;
import java.util.Scanner;
import java.util.Arrays;
import java.util.List;

/**
 * Command-line entry point that runs the application in console (text) mode.
 *
 * <p>
 * This class starts the controller using a ConsoleView implementation and provides
 * a resilient interactive loop: command-line arguments are attempted once and on failure
 * the program falls back to interactive prompting. Any unexpected exceptions are caught
 * and handled so the program does not terminate abruptly.
 * </p>
 *
 * Supported command-line examples:
 * <ul>
 *   <li>Legacy flat format: {@code <frequencyHz> <connectionType> <component1> <component2> ...}</li>
 *   <li>Nested expression format: {@code <frequencyHz> "series(R:100, parallel(C:1e-6, L:0.01), R:50)"} </li>
 * </ul>
 *
 * @author Kamil Fulneczek
 * @version 1.2
 */
public class Main {

    private static final Scanner yesNoScanner = new Scanner(System.in);
    /**
     * Application main method.
     *
     * If command-line arguments are provided they are processed once. When no arguments are
     * supplied (or argument processing fails) the application enters an interactive loop,
     * repeatedly asking the user for circuit parameters and displaying results until the
     * user chooses to exit.
     *
     * @param args command-line arguments; supported legacy formats are:
     *             {@code <frequencyHz> <connectionType> <component1> <component2> ...}
     *             or nested expression form after frequency (see package documentation).
     */
    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        CircuitController controller = new CircuitController(view);

        List<String> argsList = (args == null) ? List.of() : Arrays.asList(args);
        boolean firstAttemptWithArgs = (argsList != null && !argsList.isEmpty());
        if (firstAttemptWithArgs) {
            try {
                controller.run(args);
            } catch (Exception ex) {
                System.err.println("Error while processing command-line arguments: " + ex.getMessage());
                ex.printStackTrace(System.err);
                System.out.println("Falling back to interactive mode.");
            }
        }

        while (true) {
            try {
                controller.run(null); // interactive prompt via ConsoleView
            } catch (IOException ioe) {
                System.err.println("I/O error: " + ioe.getMessage());
                if (!promptYesNo("Input was cancelled or invalid. Do you want to retry? (y/n): ")) {
                    System.out.println("Exiting.");
                    break;
                } else {
                    continue;
                }
            } catch (Exception ex) {
                System.err.println("Unexpected error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
                ex.printStackTrace(System.err);
                if (!promptYesNo("An unexpected error occurred. Do you want to retry? (y/n): ")) {
                    System.out.println("Exiting.");
                    break;
                } else {
                    continue;
                }
            }

            if (!promptYesNo("Do you want to compute another circuit? (y/n): ")) {
                System.out.println("Goodbye.");
                break;
            }
        }
    }

    /**
     * Prompt a yes/no question on stdin and return true if the user answered yes.
     *
     * This helper treats 'y' or 't' (Polish "tak") as affirmative answers.
     *
     * @param prompt the prompt text to display
     * @return true if the user accepted (yes), false otherwise (including EOF)
     */
    private static boolean promptYesNo(String prompt) {
        System.out.print(prompt);
        try {
            String line = yesNoScanner.nextLine();
            if (line == null) return false;
            line = line.trim().toLowerCase();
            return line.startsWith("y") || line.startsWith("t");
        } catch (Exception e) {
            return false;
        }
    }
}