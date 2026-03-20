package com.mycompany.controller;

import com.mycompany.model.*;
import com.mycompany.view.ConsoleView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controller class: handles input parsing, invokes model calculations, and uses the view to display results.
 *
 * This class accepts both the legacy flat command-line format and a nested expression syntax for mixed
 * connections. It coordinates parsing, building a circuit graph, computing the equivalent impedance,
 * and delegating output to the configured view implementation.
 *
 * @author Kamil Fulneczek
 * @version 1.4
 */
public class CircuitController {

    private final ConsoleView view;

    /**
     * Create a controller using the provided view for input/output.
     *
     * @param view view implementation used for prompting and display; must not be null
     */
    public CircuitController(ConsoleView view) {
        this.view = view;
    }

    /**
     * Run the controller either using supplied command-line arguments or interactively.
     *
     * If args is null or contains fewer than 2 tokens the method will request parameters
     * from the configured view. When args are provided this method attempts to parse
     * a frequency and either a nested expression or legacy flat format and then computes
     * and displays the result via the view.
     *
     * @param args command-line arguments or null to force interactive prompting via the view
     * @throws IOException if interactive input fails or EOF is reached while reading from the view
     */
    public void run(String[] args) throws IOException {
        List<String> argsList = (args == null) ? new ArrayList<>() : new ArrayList<>(Arrays.asList(args));

        if (argsList.isEmpty() || argsList.size() < 2) {
            CircuitController.ParseResult parseResult = view.promptForParameters();
            try {
                Complex impedance = computeEquivalent(parseResult);
                ConnectionNode node = buildConnectionNodeFromParseResult(parseResult);
                CompositeCircuit circuit = new CompositeCircuit(node);
                view.displayResult(parseResult.getFrequencyHz(), circuit, impedance);
            } catch (InvalidCircuitException e) {
                view.displayError("Invalid circuit: " + e.getMessage());
            }
            return;
        }

        double frequencyHz;
        try {
            frequencyHz = Double.parseDouble(argsList.get(0));
        } catch (NumberFormatException ex) {
            view.displayError("Invalid frequency: " + ex.getMessage());
            return;
        }

        String rest = joinArgs(1, argsList);

        try {
            if (looksLikeExpression(rest)) {
                CircuitElement root = ExpressionParser.parse(rest);
                if (!(root instanceof ConnectionNode)) {
                    Component comp = (Component) root;
                    ConnectionNode wrapper = new ConnectionNode(true);
                    wrapper.addChild(comp);
                    root = wrapper;
                }
                Complex impedance = ((ConnectionNode) root).getImpedance(frequencyHz);
                CompositeCircuit circuit = new CompositeCircuit((ConnectionNode) root);
                view.displayResult(frequencyHz, circuit, impedance);
                return;
            }

            List<String> tokens = splitRespectingQuotes(rest);
            if (tokens.size() < 2) {
                view.displayError("Expected connection type and at least one component");
                return;
            }
            String conn = tokens.get(0).trim().toLowerCase();
            boolean isSeries;
            if (conn.equals("series") || conn.equals("s")) isSeries = true;
            else if (conn.equals("parallel") || conn.equals("p")) isSeries = false;
            else {
                view.displayError("Unknown connection type: " + tokens.get(0));
                return;
            }
            List<ComponentSpec> specs = new ArrayList<>();
            for (int i = 1; i < tokens.size(); i++) {
                specs.add(Parser.parseComponentToken(tokens.get(i)));
            }
            ParseResult pr = new ParseResult(frequencyHz, isSeries, specs);
            Complex impedance = computeEquivalent(pr);
            ConnectionNode node = buildConnectionNodeFromParseResult(pr);
            CompositeCircuit circuit = new CompositeCircuit(node);
            view.displayResult(pr.getFrequencyHz(), circuit, impedance);
        } catch (IllegalArgumentException ex) {
            view.displayError("Parsing error: " + ex.getMessage());
        } catch (InvalidCircuitException ex) {
            view.displayError("Invalid circuit: " + ex.getMessage());
        } catch (Exception ex) {
            view.displayError("Unexpected error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }
    }

    /**
     * Return true when the rest of command-line looks like nested expression.
     *
     * @param rest joined args
     * @return true if looks like expression
     */
    private boolean looksLikeExpression(String rest) {
        String t = rest.trim().toLowerCase();
        return t.contains("(") || t.startsWith("series") || t.startsWith("parallel");
    }

    private String joinArgs(int from, List<String> argsList) {
        if (argsList == null || from >= argsList.size()) return "";
        return String.join(" ", argsList.subList(Math.max(from, 0), argsList.size())).trim();
    }

    /**
     * Split tokens by whitespace unless they are quoted; keeps simple tokens like R:100 intact.
     *
     * @param rest input string
     * @return tokens as a List
     */
    private List<String> splitRespectingQuotes(String rest) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '"' || c == '\'') {
                inQuote = !inQuote;
                continue;
            }
            if (!inQuote && Character.isWhitespace(c)) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /**
     * Public API used by GUI: compute equivalent impedance for already-parsed parameters.
     *
     * @param parseResult parsed parameters (frequency, isSeries, components)
     * @return equivalent impedance as Complex
     * @throws InvalidCircuitException when circuit is invalid (e.g. no components, short circuit)
     */
    public Complex computeEquivalent(ParseResult parseResult) throws InvalidCircuitException {
        if (parseResult == null) throw new IllegalArgumentException("parseResult must not be null");
        ConnectionNode node = buildConnectionNodeFromParseResult(parseResult);
        return node.getImpedance(parseResult.getFrequencyHz());
    }

    /**
     * Public helper to compute impedance from an existing ConnectionNode.
     *
     * @param node ConnectionNode representing circuit
     * @param frequencyHz frequency in Hz
     * @return impedance
     * @throws InvalidCircuitException when invalid
     */
    public Complex computeEquivalent(ConnectionNode node, double frequencyHz) throws InvalidCircuitException {
        if (node == null) throw new IllegalArgumentException("node must not be null");
        return node.getImpedance(frequencyHz);
    }

    /**
     * Helper: build a ConnectionNode from ParseResult by instantiating Component leaves.
     *
     * @param pr parse result containing specs
     * @return connection node
     * @throws InvalidCircuitException when invalid spec encountered
     */
    private ConnectionNode buildConnectionNodeFromParseResult(ParseResult pr) throws InvalidCircuitException {
        if (pr == null) throw new IllegalArgumentException("pr must not be null");
        ConnectionNode node = new ConnectionNode(pr.isSeries());
        try {
            pr.getSpecs().forEach(spec -> {
                try {
                    Component comp;
                    if (spec.isResistor()) comp = new Resistor(spec.getValue());
                    else if (spec.isCapacitor()) comp = new Capacitor(spec.getValue());
                    else if (spec.isInductor()) comp = new Inductor(spec.getValue());
                    else throw new InvalidCircuitException("Unknown component type in spec");
                    node.addChild(comp);
                } catch (InvalidCircuitException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException re) {
            if (re.getCause() instanceof InvalidCircuitException) {
                throw (InvalidCircuitException) re.getCause();
            }
            throw re;
        }
        return node;
    }

    /**
     * Data holder for parsed parameters.
     *
     * @param frequencyHz frequency in Hertz (must be > 0)
     * @param series true when top-level connection is series, false for parallel
     * @param specs list of parsed ComponentSpec (defensively copied by the record constructor)
     */
    public static record ParseResult(double frequencyHz, boolean series, List<ComponentSpec> specs) {
        public ParseResult {
            if (specs == null) {
                specs = List.of();
            } else {
                specs = new ArrayList<>(specs);
            }
        }

        public double getFrequencyHz() {
            return frequencyHz;
        }

        public boolean isSeries() {
            return series;
        }

        public List<ComponentSpec> getSpecs() {
            return new ArrayList<>(specs);
        }
    }
}