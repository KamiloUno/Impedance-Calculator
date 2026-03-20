package com.mycompany.view;

import com.mycompany.model.CompositeCircuit;
import com.mycompany.controller.CircuitController;
import com.mycompany.model.ComponentSpec;
import com.mycompany.model.Complex;
import com.mycompany.model.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Console-based view: prompts user for circuit parameters and prints results to standard output.
 *
 * This class performs only input/output and does not perform calculations. It returns a
 * ParseResult DTO used by the controller to perform computations.
 *
 * @author Kamil Fulneczek
 * @version 1.1
 */
public class ConsoleView {
        /**
     * Construct a ConsoleView instance.
     *
     * The constructor initializes reader resources used for interactive input.
     */
    public ConsoleView() {
    }

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /**
     * Prompt the user interactively for parameters (frequency, connection type and components).
     *
     * This method validates user input and reprompts on invalid entries. It throws IOException
     * only if reading from stdin fails or EOF is reached.
     *
     * @return a ParseResult containing parsed frequency, connection type flag and component specs
     * @throws IOException if an I/O error occurs or EOF is reached while reading input
     */
    public CircuitController.ParseResult promptForParameters() throws IOException {
        System.out.println("Interactive mode: please provide parameters.");

        double frequencyHz;
        while (true) {
            System.out.print("Enter frequency in Hz (e.g. 50 or 1e3): ");
            String freqLine = readLineOrThrow();
            try {
                frequencyHz = Double.parseDouble(freqLine.trim());
                if (frequencyHz <= 0) {
                    System.out.println("Frequency must be positive.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Try again.");
            }
        }

        boolean isSeries;
        while (true) {
            System.out.print("Enter connection type ('series' or 'parallel'): ");
            String conn = readLineOrThrow().trim();
            if (conn.equalsIgnoreCase("series") || conn.equalsIgnoreCase("s")) {
                isSeries = true;
                break;
            } else if (conn.equalsIgnoreCase("parallel") || conn.equalsIgnoreCase("p")) {
                isSeries = false;
                break;
            } else {
                System.out.println("Invalid connection type. Try again.");
            }
        }

        int n;
        while (true) {
            System.out.print("Enter number of components (integer >=1): ");
            String nline = readLineOrThrow();
            try {
                n = Integer.parseInt(nline.trim());
                if (n <= 0) {
                    System.out.println("Number must be >= 1");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid integer. Try again.");
            }
        }

        List<ComponentSpec> components = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            while (true) {
                System.out.printf("Component %d (format T:value where T=R,C,L): ", i + 1);
                String token = readLineOrThrow().trim();
                if (token.isEmpty()) {
                    System.out.println("Input empty. Please provide a token like R:100.");
                    continue;
                }
                try {
                    ComponentSpec spec = Parser.parseComponentToken(token);
                    components.add(spec);
                    break;
                } catch (IllegalArgumentException ex) {
                    System.out.println("Invalid component token: " + ex.getMessage());
                }
            }
        }

        return new CircuitController.ParseResult(frequencyHz, isSeries, components);
    }

    /**
     * Display final result to the user.
     *
     * @param frequencyHz frequency used for computation
     * @param circuit the circuit computed used for human-readable output
     * @param impedance computed equivalent impedance as a Complex value
     */
    public void displayResult(double frequencyHz, CompositeCircuit circuit, Complex impedance) {
        System.out.println("-------------------------------------------------");
        System.out.printf("Frequency: %.6g Hz%n", frequencyHz);
        System.out.println("Circuit: " + circuit);
        System.out.println("Equivalent impedance Z = " + impedance);
        System.out.printf("Magnitude |Z| = %.6g Ohm%n", impedance.magnitude());
        System.out.println("-------------------------------------------------");
    }

    /**
     * Display an error message to the user.
     *
     * @param message error message to display
     */
    public void displayError(String message) {
        System.err.println("ERROR: " + message);
    }

    /**
     * Read a single line from stdin, throwing IOException if EOF or read error occurs.
     *
     * @return read line (never null)
     * @throws IOException when input is closed or EOF is reached
     */
    private String readLineOrThrow() throws IOException {
        String line = reader.readLine();
        if (line == null) throw new IOException("Input cancelled (EOF)");
        return line;
    }
}