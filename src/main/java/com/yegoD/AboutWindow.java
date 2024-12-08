package com.yegoD;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

public class AboutWindow extends JFrame
{
    public AboutWindow()
    {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 300);

        JPanel panel = new JPanel(new MigLayout("fillx"));

        CC centerConstraint = new CC();
        centerConstraint.alignX("center").spanX().wrap();

        JLabel title = new JLabel("About");
        title.setFont(new Font("Arial", Font.BOLD, 18));

        panel.add(title, centerConstraint);

        JLabel topText = new JLabel("Created with love by yegoD");
        topText.setFont(new Font("Arial", Font.ITALIC, 16));

        panel.add(topText, centerConstraint);

        JLabel sourceTitle = new JLabel("Open Source Dependencies");
        sourceTitle.setFont(new Font("Arail", Font.BOLD, 18));
    
        panel.add(sourceTitle, centerConstraint);

        // MigLayout uses 3-Clause BSD license
        JLabel migLayout = new JLabel("MiGLayout - Copyright 2024 Mikael Grev");
        panel.add(migLayout, centerConstraint);

        // JSON-java is public domain
        JLabel json = new JLabel("JSON-java - Public Domain");
        panel.add(json, centerConstraint);

        JLabel iconTitle = new JLabel("Icons");
        iconTitle.setFont(new Font("Arail", Font.BOLD, 18));

        panel.add(iconTitle, centerConstraint);

        JLabel iconsText = new JLabel("Status icons by https://icons8.com/");
        panel.add(iconsText, centerConstraint);

        JButton gitRepoButton = new JButton("Github Repository");
        gitRepoButton.addActionListener(new OpenRepoAction());

        panel.add(gitRepoButton, centerConstraint);

        add(panel);
    }

    private class OpenRepoAction implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(Desktop.isDesktopSupported())
            {
                try
                {
                    Desktop.getDesktop().browse(new URI("https://github.com/yegoD1/schedulechecker"));
                }
                catch (Exception exception)
                {
                    new WarningWindow(exception.toString());
                }
            }
        }

    }
}