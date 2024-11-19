package com.gmuprojects;

import java.net.ConnectException;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class Core {

    // Main frame for this application.
    private static JFrame mainFrame;

    public static void main(String[] args) throws Exception {
        try
        {
            mainFrame = new MainFrame();

            mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            mainFrame.setVisible(true);
            mainFrame.setSize(600, 600);
        }
        catch (SSLHandshakeException e){
            // User likely does not have certification. Show warning window.

            new WarningWindow("Certificate is not added to keytool. Please go to github repository and follow instructions.");
        }
        catch (ConnectException e)
        {
            new WarningWindow("Connection timeout. You may not be connected to the internet.");
        }
        catch (Exception e)
        {
            System.err.println(e);
            System.err.println("Quitting program due to failure...");
            return;
        }
    }
}
