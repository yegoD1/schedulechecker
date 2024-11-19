package com.gmuprojects;

import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.plaf.DimensionUIResource;

import net.miginfocom.swing.MigLayout;

public class WarningWindow extends JFrame {

    private JPanel mainPanel;

    private JTextArea warningTextArea;

    public WarningWindow(String warningText)
    {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 200);

        mainPanel = new JPanel(new MigLayout("fillx"));

        // Setup text in JTextArea for word wrapping.
        warningTextArea = new JTextArea(warningText);
        warningTextArea.setLineWrap(true);
        warningTextArea.setWrapStyleWord(true);
        warningTextArea.setPreferredSize(new DimensionUIResource(getWidth(), getHeight()));
        warningTextArea.setEditable(false);
        warningTextArea.setFont(new Font("Arial", Font.PLAIN, 18));
        warningTextArea.setBackground(getBackground());

        mainPanel.add(warningTextArea, "span, align center");

        add(mainPanel);

        setVisible(true);
    }
}
