package com.mycompany.view;

import com.mycompany.controller.CircuitController;
import com.mycompany.model.Component;
import com.mycompany.model.ConnectionNode;
import com.mycompany.model.Resistor;
import com.mycompany.model.Capacitor;
import com.mycompany.model.Inductor;
import com.mycompany.model.Complex;
import com.mycompany.model.InvalidCircuitException;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Swing-based graphical user interface view for building and computing circuits.
 *
 * <p>
 * This view presents a hierarchical editor (JTree) for mixed series/parallel circuits,
 * a flat component table for quick inspection, controls to add/remove components and groups,
 * and a simple result display. All model computation is delegated to the underlying model
 * classes; the view handles only user interaction and presentation.
 * </p>
 *
 * <p>
 * The tree root can be set to series or parallel by the user via the "Root type" selector.
 * Changing the root type preserves existing children by moving them under a newly-created root
 * of the chosen type. Once the user adds any element to the tree the selector is disabled
 * to prevent changing the root while the circuit contains elements; when the tree becomes empty
 * again the selector is re-enabled.
 * </p>
 *
 * @author Kamil Fulneczek
 * @version 1.1
 */
@Getter
public class SwingView extends JFrame {

    /**
     * Serialization identifier.
     *
     * @since 1.0
     */
    private static final long serialVersionUID = 1L;

    /**
     * Swing tree component that visualizes the circuit structure.
     *
     * This JTree's nodes wrap ConnectionNode or Component objects from the model.
     */
    private final JTree circuitTree;

    /**
     * Tree model backing {@link #circuitTree}.
     */
    private final DefaultTreeModel treeModel;

    /**
     * Mutable tree node that wraps the current root ConnectionNode.
     *
     * Keeps a reference to the DefaultMutableTreeNode to allow moving children when the root type is changed.
     */
    private DefaultMutableTreeNode rootTreeNode;

    /**
     * Model object representing the root connection node (top-level circuit).
     *
     * This field may be replaced when the user changes the root type.
     */
    private ConnectionNode rootNode;

    /**
     * Table used to display a flat list of leaf components.
     */
    private final JTable componentTable;

    /**
     * Table model backing {@link #componentTable}.
     */
    private final ComponentTableModel tableModel;

    /**
     * Text field where the user enters the frequency in Hertz.
     */
    private final JTextField frequencyField = new JTextField(10);

    /**
     * Root type selector control (series / parallel).
     */
    private final JComboBox<String> rootTypeSelector = new JComboBox<>();

    /**
     * Flag used to suppress handling of programmatic selection changes on the rootTypeSelector.
     */
    private boolean suppressRootSelectorEvents = false;

    /**
     * Button which adds a resistor to the selected node.
     */
    private final JButton addResistorBtn = new JButton("Add Resistor");

    /**
     * Button which adds a capacitor to the selected node.
     */
    private final JButton addCapacitorBtn = new JButton("Add Capacitor");

    /**
     * Button which adds an inductor to the selected node.
     */
    private final JButton addInductorBtn = new JButton("Add Inductor");

    /**
     * Button which adds a series group as a child of the selected node.
     */
    private final JButton addSeriesBtn = new JButton("Add Series Group");

    /**
     * Button which adds a parallel group as a child of the selected node.
     */
    private final JButton addParallelBtn = new JButton("Add Parallel Group");

    /**
     * Button which removes the currently selected node.
     */
    private final JButton removeBtn = new JButton("Remove Selected");

    /**
     * Button which computes the equivalent impedance for the current circuit.
     */
    private final JButton computeBtn = new JButton("Compute Impedance");

    /**
     * Label that displays the last computed result.
     */
    private final JLabel resultLabel = new JLabel("Result: ");

    /**
     * Internal controller instance used for lightweight operations where appropriate.
     *
     * Note: the controller here is instantiated with a ConsoleView to provide a minimal
     * view implementation for controller APIs; the Swing UI handles primary user interaction.
     */
    private final CircuitController controller = new CircuitController(new ConsoleView());

    /**
     * Construct the SwingView and prepare the user interface controls.
     *
     * The constructor initializes the UI components, sets accessibility attributes and
     * registers event handlers. The window is packed but not shown; callers may call
     * {@code setVisible(true)} to display it.
     */
    public SwingView() {
        super("Impedance Calculator");
        this.rootNode = new ConnectionNode(true);
        this.rootTreeNode = new DefaultMutableTreeNode(rootNode);
        this.treeModel = new DefaultTreeModel(rootTreeNode);
        this.circuitTree = new JTree(treeModel);
        this.tableModel = new ComponentTableModel();
        this.componentTable = new JTable(tableModel);

        initUI();
    }

    /**
     * Initialize and layout user interface components.
     *
     * This method configures accessibility information, mnemonics, tooltips and event handlers.
     */
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.BOTH;

        JLabel freqLabel = new JLabel("Frequency (Hz):");
        freqLabel.setDisplayedMnemonic(KeyEvent.VK_F);
        freqLabel.setLabelFor(frequencyField);
        frequencyField.setToolTipText("Enter frequency in Hertz (numeric)");
        frequencyField.getAccessibleContext().setAccessibleDescription("Frequency in Hertz input");

        JLabel rootLabel = new JLabel("Root type:");
        rootLabel.setLabelFor(rootTypeSelector);
        rootTypeSelector.setToolTipText("Select top-level connection type (series or parallel)");

        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>(new Vector<>(List.of("series", "parallel")));
        rootTypeSelector.setModel(comboModel);

        suppressRootSelectorEvents = true;
        rootTypeSelector.setSelectedIndex(rootNode.isSeries() ? 0 : 1);
        suppressRootSelectorEvents = false;

        rootTypeSelector.addActionListener(e -> {
            if (suppressRootSelectorEvents) return;
            if (!rootTypeSelector.isEnabled()) {
                suppressRootSelectorEvents = true;
                rootTypeSelector.setSelectedIndex(rootNode != null && rootNode.isSeries() ? 0 : 1);
                suppressRootSelectorEvents = false;
                return;
            }
            String sel = (String) rootTypeSelector.getSelectedItem();
            boolean isSeries = "series".equalsIgnoreCase(sel);
            changeRootType(isSeries);
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        add(freqLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        add(frequencyField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        add(rootLabel, gbc);
        gbc.gridx = 3; gbc.weightx = 0;
        add(rootTypeSelector, gbc);

        circuitTree.setRootVisible(true);
        circuitTree.getAccessibleContext().setAccessibleDescription("Hierarchical circuit structure");
        JScrollPane treeScroll = new JScrollPane(circuitTree);
        treeScroll.setPreferredSize(new Dimension(350, 300));
        treeScroll.setToolTipText("Circuit tree: right-click or use buttons to add/remove elements");

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.weighty = 1;
        add(treeScroll, gbc);

        JPanel btnPanel = new JPanel(new GridLayout(2, 4, 6, 6));
        addResistorBtn.setToolTipText("Add a resistor to selected node");
        addResistorBtn.getAccessibleContext().setAccessibleDescription("Add resistor");
        addResistorBtn.setMnemonic(KeyEvent.VK_R);
        addResistorBtn.addActionListener(e -> onAddComponent("R"));

        addCapacitorBtn.setToolTipText("Add a capacitor to selected node");
        addCapacitorBtn.getAccessibleContext().setAccessibleDescription("Add capacitor");
        addCapacitorBtn.setMnemonic(KeyEvent.VK_C);
        addCapacitorBtn.addActionListener(e -> onAddComponent("C"));

        addInductorBtn.setToolTipText("Add an inductor to selected node");
        addInductorBtn.getAccessibleContext().setAccessibleDescription("Add inductor");
        addInductorBtn.setMnemonic(KeyEvent.VK_L);
        addInductorBtn.addActionListener(e -> onAddComponent("L"));

        addSeriesBtn.setToolTipText("Add a series group as child of selected node");
        addSeriesBtn.getAccessibleContext().setAccessibleDescription("Add series group");
        addSeriesBtn.setMnemonic(KeyEvent.VK_S);
        addSeriesBtn.addActionListener(e -> onAddGroup(true));

        addParallelBtn.setToolTipText("Add a parallel group as child of selected node");
        addParallelBtn.getAccessibleContext().setAccessibleDescription("Add parallel group");
        addParallelBtn.setMnemonic(KeyEvent.VK_P);
        addParallelBtn.addActionListener(e -> onAddGroup(false));

        removeBtn.setToolTipText("Remove selected tree node");
        removeBtn.getAccessibleContext().setAccessibleDescription("Remove node");
        removeBtn.setMnemonic(KeyEvent.VK_D);
        removeBtn.addActionListener(e -> onRemoveSelected());

        computeBtn.setToolTipText("Compute equivalent impedance for current tree");
        computeBtn.getAccessibleContext().setAccessibleDescription("Compute impedance");
        computeBtn.setMnemonic(KeyEvent.VK_Z);
        computeBtn.addActionListener(e -> onCompute());

        btnPanel.add(addResistorBtn);
        btnPanel.add(addCapacitorBtn);
        btnPanel.add(addInductorBtn);
        btnPanel.add(removeBtn);
        btnPanel.add(addSeriesBtn);
        btnPanel.add(addParallelBtn);
        btnPanel.add(computeBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.weighty = 0;
        add(btnPanel, gbc);

        componentTable.setFillsViewportHeight(true);
        componentTable.getAccessibleContext().setAccessibleDescription("Flat list of components in the circuit");
        JScrollPane tableScroll = new JScrollPane(componentTable);
        tableScroll.setPreferredSize(new Dimension(600, 120));
        tableScroll.setToolTipText("Table shows all leaf components (for quick edit/overview)");

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4; gbc.weighty = 0.3;
        add(tableScroll, gbc);

        resultLabel.setPreferredSize(new Dimension(600, 24));
        resultLabel.getAccessibleContext().setAccessibleDescription("Displays computed impedance and magnitude");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        add(resultLabel, gbc);

        List<java.awt.Component> order = new ArrayList<>();
        order.add(frequencyField);
        order.add(circuitTree);
        order.add(addResistorBtn);
        order.add(addCapacitorBtn);
        order.add(addInductorBtn);
        order.add(addSeriesBtn);
        order.add(addParallelBtn);
        order.add(removeBtn);
        order.add(computeBtn);
        order.add(componentTable);
        setFocusTraversalPolicy(new CustomFocusPolicy(order));
        setFocusTraversalPolicyProvider(true);

        circuitTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        circuitTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                updateTableModel();
            }
        });

        updateRootSelectorState();

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Change the type of the root connection node to series or parallel.
     *
     * Existing children (tree nodes) are preserved and moved under the new root.
     *
     * @param isSeries true to set root as series, false for parallel
     */
    private void changeRootType(boolean isSeries) {
        if (rootNode.isSeries() == isSeries) return;

        ConnectionNode newRoot = new ConnectionNode(isSeries);
        DefaultMutableTreeNode newRootTreeNode = new DefaultMutableTreeNode(newRoot);

        while (rootTreeNode.getChildCount() > 0) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootTreeNode.getChildAt(0);
            rootTreeNode.remove(0);
            newRootTreeNode.add(child);
        }

        this.rootNode = newRoot;
        this.rootTreeNode = newRootTreeNode;
        treeModel.setRoot(newRootTreeNode);
        treeModel.reload();
        circuitTree.setRootVisible(true);
        updateTableModel();
        suppressRootSelectorEvents = true;
        rootTypeSelector.setSelectedIndex(rootNode.isSeries() ? 0 : 1);
        suppressRootSelectorEvents = false;
    }

    /**
     * Add a primitive component of the given kind to the currently selected tree node (or root).
     *
     * @param kind component kind code: "R", "C" or "L"
     */
    private void onAddComponent(String kind) {
        try {
            DefaultMutableTreeNode sel = getSelectedTreeNodeOrRoot();
            ConnectionNode parent = (ConnectionNode) sel.getUserObject();
            String input = JOptionPane.showInputDialog(this, "Enter value (numeric):", "Component value", JOptionPane.PLAIN_MESSAGE);
            if (input == null) return;
            double val = Double.parseDouble(input);
            Component comp;
            switch (kind) {
                case "R": comp = new Resistor(val); break;
                case "C": comp = new Capacitor(val); break;
                case "L": comp = new Inductor(val); break;
                default: throw new IllegalArgumentException("Unknown kind");
            }
            parent.addChild(comp);
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(comp);
            sel.add(child);
            treeModel.reload(sel);
            updateTableModel();
            updateRootSelectorState();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid numeric value: " + ex.getMessage(), "Input error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Add a series or parallel group as a child of the currently selected node (or root).
     *
     * @param isSeries true to add a series group, false to add a parallel group
     */
    private void onAddGroup(boolean isSeries) {
        DefaultMutableTreeNode sel = getSelectedTreeNodeOrRoot();
        ConnectionNode parent = (ConnectionNode) sel.getUserObject();
        ConnectionNode childNode = new ConnectionNode(isSeries);
        parent.addChild(childNode);
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(childNode);
        sel.add(child);
        treeModel.reload(sel);
        updateTableModel();
        updateRootSelectorState();
    }

    /**
     * Return the currently selected tree node, or the root node if none is selected.
     *
     * @return selected {@link DefaultMutableTreeNode} or the root node
     */
    private DefaultMutableTreeNode getSelectedTreeNodeOrRoot() {
        TreePath path = circuitTree.getSelectionPath();
        if (path == null) {
            return (DefaultMutableTreeNode) treeModel.getRoot();
        } else {
            return (DefaultMutableTreeNode) path.getLastPathComponent();
        }
    }

    /**
     * Remove the currently selected node from the tree and from the model.
     *
     * The root node cannot be removed; attempting to do so shows a warning dialog.
     */
    private void onRemoveSelected() {
        DefaultMutableTreeNode sel = getSelectedTreeNodeOrRoot();
        if (sel == treeModel.getRoot()) {
            JOptionPane.showMessageDialog(this, "Cannot remove root node", "Operation not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) sel.getParent();
        ConnectionNode parent = (ConnectionNode) parentNode.getUserObject();
        Object userObj = sel.getUserObject();
        if (userObj instanceof Component) {
            parent.removeChild((Component) userObj);
        } else if (userObj instanceof ConnectionNode) {
            parent.removeChild((ConnectionNode) userObj);
        } else {
            JOptionPane.showMessageDialog(this, "Unknown node selected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        parentNode.remove(sel);
        treeModel.reload(parentNode);
        updateTableModel();
        updateRootSelectorState();
    }

    /**
     * Compute the equivalent impedance for the tree rooted at the current model and display it.
     *
     * The frequency is read from {@link #frequencyField}. Errors are reported with dialogs.
     */
    private void onCompute() {
        try {
            double freq = Double.parseDouble(frequencyField.getText());
            ConnectionNode root = buildConnectionFromTree((DefaultMutableTreeNode) treeModel.getRoot());
            Complex z = root.getImpedance(freq);
            resultLabel.setText(String.format("Z = %s   |Z| = %.6g", z.toString(), z.magnitude()));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid frequency: " + ex.getMessage(), "Input error", JOptionPane.ERROR_MESSAGE);
        } catch (InvalidCircuitException ex) {
            JOptionPane.showMessageDialog(this, "Invalid circuit: " + ex.getMessage(), "Computation error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Build a ConnectionNode structure from the Swing tree nodes recursively.
     *
     * @param treeNode tree node whose user object wraps ConnectionNode or Component
     * @return ConnectionNode representing the subtree
     * @throws InvalidCircuitException if the tree structure cannot be converted to a model
     */
    private ConnectionNode buildConnectionFromTree(DefaultMutableTreeNode treeNode) throws InvalidCircuitException {
        Object obj = treeNode.getUserObject();
        if (obj instanceof ConnectionNode) {
            ConnectionNode node = new ConnectionNode(((ConnectionNode)obj).isSeries());
            for (int i = 0; i < treeNode.getChildCount(); i++) {
                DefaultMutableTreeNode ch = (DefaultMutableTreeNode) treeNode.getChildAt(i);
                Object chObj = ch.getUserObject();
                if (chObj instanceof ConnectionNode) {
                    node.addChild(buildConnectionFromTree(ch));
                } else if (chObj instanceof Component) {
                    node.addChild((Component) chObj);
                } else {
                    throw new InvalidCircuitException("Unknown node object: " + chObj);
                }
            }
            return node;
        } else {
            throw new InvalidCircuitException("Root is not a ConnectionNode");
        }
    }

    /**
     * Update the flat component table model from the current tree.
     *
     * This collects all leaf components and sets them on the table model.
     */
    private void updateTableModel() {
        List<Component> leaves = new ArrayList<>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        collectLeaves(root, leaves);
        tableModel.setComponents(leaves);
        updateRootSelectorState();
    }

    private void collectLeaves(DefaultMutableTreeNode node, List<Component> leaves) {
        Object obj = node.getUserObject();
        if (obj instanceof Component) {
            leaves.add((Component) obj);
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectLeaves((DefaultMutableTreeNode) node.getChildAt(i), leaves);
        }
    }

    /**
     * Adjust the enabled/disabled state of the root type selector.
     *
     * The selector is disabled when the current root has any children (i.e. circuit contains elements).
     * It is enabled when the root has no children.
     */
    private void updateRootSelectorState() {
        if (rootTreeNode == null) {
            suppressRootSelectorEvents = true;
            rootTypeSelector.setEnabled(true);
            rootTypeSelector.setSelectedIndex(rootNode != null && rootNode.isSeries() ? 0 : 1);
            suppressRootSelectorEvents = false;
            return;
        }
        boolean hasChildren = rootTreeNode.getChildCount() > 0;
        suppressRootSelectorEvents = true;
        rootTypeSelector.setEnabled(!hasChildren);
        rootTypeSelector.setSelectedIndex(rootNode != null && rootNode.isSeries() ? 0 : 1);
        suppressRootSelectorEvents = false;
    }

    /**
     * Table model showing flat list of components.
     */
    @Getter
    private static class ComponentTableModel extends AbstractTableModel {
        private final List<String> cols = List.of("Type", "Value", "Description");
        private List<Component> components = new ArrayList<>();

        public void setComponents(List<Component> components) {
            this.components = components;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return components.size();
        }

        @Override
        public int getColumnCount() {
            return cols.size();
        }

        @Override
        public String getColumnName(int column) {
            return cols.get(column);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Component c = components.get(rowIndex);
            switch (columnIndex) {
                case 0: return c.getClass().getSimpleName();
                case 1: return c.getValue();
                case 2: return c.description();
                default: return "";
            }
        }
    }

    /**
     * Custom focus traversal policy to enforce logical tab order.
     */
    @Getter
    private static class CustomFocusPolicy extends FocusTraversalPolicy {
        private final List<java.awt.Component> order;
        CustomFocusPolicy(List<java.awt.Component> order) { this.order = order; }
        @Override public java.awt.Component getComponentAfter(Container aContainer, java.awt.Component aComponent) {
            int idx = order.indexOf(aComponent);
            int next = (idx + 1) % order.size();
            return order.get(next);
        }
        @Override public java.awt.Component getComponentBefore(Container aContainer, java.awt.Component aComponent) {
            int idx = order.indexOf(aComponent);
            int prev = (idx - 1 + order.size()) % order.size();
            return order.get(prev);
        }
        @Override public java.awt.Component getFirstComponent(Container aContainer) { return order.get(0); }
        @Override public java.awt.Component getLastComponent(Container aContainer) { return order.get(order.size()-1); }
        @Override public java.awt.Component getDefaultComponent(Container aContainer) { return order.get(0); }
    }
}