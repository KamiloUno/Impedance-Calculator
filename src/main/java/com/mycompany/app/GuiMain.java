package com.mycompany.app;

import com.mycompany.view.SwingView;

import javax.swing.*;

/**
 * Launcher for the Swing GUI.
 *
 *
 * @author Kamil Fulneczek
 * @version 1.0
 */
public class GuiMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SwingView view = new SwingView();
            view.setVisible(true);
        });
    }
}