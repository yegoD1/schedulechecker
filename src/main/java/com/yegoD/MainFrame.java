package com.yegoD;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.json.JSONArray;
import org.json.JSONObject;

import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

/**
 * Mainframe is of type JFrame that is to be used to display the main program UI.
 */
public class MainFrame extends JFrame{

    // Actual URL to use to query class terms.
    private static final String TERM_URL = "https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/classSearch/getTerms";

    // HttpRequest object to be used when querying for available class terms.
    private static HttpRequest TERM_REQUEST;

    static{
        // Constructs reusable header pairs to be sent with every request.
        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] headerPairs = new BasicPair[2];
        headerPairs[0] = new BasicPair<String,String>("Accept", "*/*");
        headerPairs[1] = new BasicPair<String,String>("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:131.0) Gecko/20100101 Firefox/131.0");

        // Constructs query pairs to be used when getting available terms.
        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] termQueryPairs = new BasicPair[2];
        termQueryPairs[0] = new BasicPair<String,String>("offset", "1");
        termQueryPairs[1] = new BasicPair<String,String>("max", "10");

        try
        {
            TERM_REQUEST = HttpRequestBuilder.BuildCall(TERM_URL, termQueryPairs, headerPairs, HttpCallType.GET, null);
        }
        catch(Exception e)
        {
            new WarningWindow(e.toString());
        }
    }

    // Main panel for this application.
    private JPanel mainPanel;

    // Label shown on startup before intial requests are completed.
    private JLabel loadingLabel;

    // Date picker to select date range for classes.
    private JComboBox<ClassDateCode> classDatePicker;

    // Text field for the class symbol.
    private JTextField classSymbolText;

    // Text field for the class number.
    private JTextField classNumText;

    // Text field for the class section. This is optional.
    private JTextField classSecText;

    // Button when user wants to add class to track.
    private JButton addClassButton;

    // Panel that sorts elements vertically to show user all actively tracked classes.
    private JPanel classList;

    // Scroll pane container for classList.
    private JScrollPane classScrollPane;

    private ClassChecker classChecker;

    // HttpClient to send out requests.
    private HttpClient httpClient;

    /**
     * Default constructor for MainFrame. Called when program is first ran.
     * @throws Exception
     */
    public MainFrame() throws Exception
    {
        // Construct all elements to be added to this frame.
        setupPanel();
        add(mainPanel);

        // Disable until inital load happens (prevents clicking and interacting).
        setEnabled(false);

        // Construct http client to use for http requests.
        httpClient = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .cookieHandler(new CookieManager())
            .build();

        try{
            // This response should respond with a json array.
            CompletableFuture<HttpResponse<String>> termFuture = httpClient.sendAsync(TERM_REQUEST, BodyHandlers.ofString());
            termFuture.thenAccept(result -> onTermsRecieved(result));
        }
        catch (Exception e){
            throw e;
        }

        classChecker = new ClassChecker(httpClient);
    }

    /**
     * Sets up all the panel elements to display for the user.
     */
    private void setupPanel()
    {
        // Use custom layout.
        mainPanel = new JPanel(new MigLayout("fillx"));

        CC centerConstraint = new CC();
        centerConstraint.alignX("center").spanX().wrap();

        loadingLabel = new JLabel("Loading...");
        loadingLabel.setFont(new Font("Arial", Font.ITALIC, 24));

        mainPanel.add(loadingLabel, centerConstraint);

        JLabel titleLabel = new JLabel("GMU Class Availability Checker");
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 24));

        // Title at top of the window.
        mainPanel.add(titleLabel, centerConstraint);

        JLabel datePickerLabel = new JLabel("Class Date:");
        mainPanel.add(datePickerLabel, "split 2, span, align center");

        // Date picker for classes.
        classDatePicker = new JComboBox<ClassDateCode>();
        classDatePicker.setEditable(false);

        mainPanel.add(classDatePicker, "width 100:125:200");

        JLabel classNameLabel = new JLabel("Class Symbol:");
        classSymbolText = new JTextField();
        classSymbolText.setToolTipText("The class symbol to track (CS, CDI, HNRS, etc).");

        mainPanel.add(classNameLabel, "split 2");
        mainPanel.add(classSymbolText, "growx, width 60:100:150");

        JLabel classNumLabel = new JLabel("Class Number:");
        classNumText = new JTextField();
        classNumText.setToolTipText("The number of the class to track, after the symbol.");

        mainPanel.add(classNumLabel, "split 2");
        mainPanel.add(classNumText, "growx, width 60:100:150");

        JLabel classSecLabel = new JLabel("Class Section (optional):");
        classSecText = new JTextField();
        classSecText.setToolTipText("The section of the class (001, 002, etc). This can be left empty to track all classes of that type.");

        mainPanel.add(classSecLabel, "split 2");
        mainPanel.add(classSecText, "growx, width 60:100:150, wrap");

        addClassButton = new JButton("Track Class");
        addClassButton.addActionListener(new addClassListener());

        mainPanel.add(addClassButton, centerConstraint);

        classList = new JPanel(new MigLayout("fillx"));

        classScrollPane = new JScrollPane(classList);
        mainPanel.add(classScrollPane, "growx, span");

        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(new aboutButtonListener());
        mainPanel.add(aboutButton, centerConstraint);
    }
    
    private class aboutButtonListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            new AboutWindow().setVisible(true);
        }
    }

    /**
     * Called when available terms are recieved from http request. Also clears out loading text.
     * @param response Actual response message. Should be a JSON array.
     */
    private void onTermsRecieved(HttpResponse<String> response)
    {
        JSONArray jsonArray = new JSONArray(response.body());
        fillDatePicker(jsonArray);

        // Enable frame now since dates are loaded.
        setEnabled(true);
        mainPanel.remove(loadingLabel);
        revalidate();
        repaint();
    }

    /**
     * Called after <code>onTermsRecieved</code> to fill out the dropdown date picker.
     * @param responseArray Array received from terms query.
     * @see {@link MainFrame#onTermsRecieved(HttpResponse)}
     */
    private void fillDatePicker(JSONArray responseArray)
    {
        for(int i = 0; i < responseArray.length(); i++)
        {
            JSONObject obj = responseArray.getJSONObject(i);
            int codeValue = obj.getInt("code");
            String description = obj.getString("description");

            // Filter out view only dates.
            if(!description.contains("(View Only)"))
            {
                classDatePicker.addItem(new ClassDateCode(codeValue, description));
            }
        }

        if(classDatePicker.getItemCount() > 0)
        {
            classDatePicker.setSelectedIndex(0);
        }
    }

    /**
     * Internal class to easily put together data for a term the user can select.
     */
    private class ClassDateCode
    {
        private int code;
        private String description;

        public ClassDateCode(int code, String description)
        {
            this.code = code;
            this.description = description;
        }

        public int getCode()
        {
            return code;
        }

        public String getDescription()
        {
            return description;
        }

        @Override
        public String toString()
        {
            return getDescription();
        }
    }

    /**
     * ActionListener that is called whenever a class is wanting to be removed.
     */
    private class removeClassListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            JPanel owningPanel = (JPanel) ((JButton) e.getSource()).getParent();
            classChecker.removeClass((JPanel) owningPanel.getComponent(0));
            classList.remove(owningPanel);
            revalidate();
            repaint();
        }
    }

    /**
     * Checks if given classSymbol is valid. classSymbol must contain only letters.
     * @param classSymbol Class symbol to validate.
     * @return True if classSymbol contains only letters.
     */
    private boolean isValidClassSymbol(String classSymbol)
    {
        // Prevent overly long class symbols.
        if(classSymbol.length() > 6)
        {
            return false;
        }

        for (int i = 0; i < classSymbol.length(); i++) 
        {
            char currentChar = classSymbol.charAt(i);
            if(!Character.isLetter(currentChar))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if given String number is valid. Must only contain numbers.
     * Used in checking class number and section.
     * @param number Number to validate.
     * @return True if number contains only numbers.
     */
    private boolean isValidNum(String number)
    {
        if(number.length() != 3)
        {
            return false;
        }

        for (int i = 0; i < number.length(); i++) 
        {
            char currentChar = number.charAt(i);
            if(!Character.isDigit(currentChar))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if given section string is a valid section. Must only contain three numbers or
     * two letters and a number (DL1 for example).
     * @param section Section string to check.
     * @return True if given section is valid. False if otherwise.
     */
    private boolean isValidSection(String section)
    {
        if(section.length() != 3)
        {
            return false;
        }

        // First check if it contains only numbers.
        if(isValidNum(section))
        {
            return true;
        }

        // If not, this means it is an online class. First two characters should be a letter with the last one being a number.
        if(Character.isLetter(section.charAt(0)) && Character.isLetter(section.charAt(1)) &&
            Character.isDigit(section.charAt(2)))
        {
            return true;
        }

        return false;
    }

    /**
     * ActionListener that is called whenever the user wants to add a class to be tracked.
     */
    private class addClassListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent event) {
            if(classSymbolText.getText().isEmpty())
            {
                new WarningWindow("Class symbol field is empty. Please fill out before adding a class. For CS 262 you would type CS.");
                return;
            }
            if(classNumText.getText().isEmpty())
            {
                new WarningWindow("Class number field is empty. Please fill out before adding a class. For CS 262 you would type 262.");
                return;
            }

            if(!isValidClassSymbol(classSymbolText.getText()))
            {
                new WarningWindow("Class symbol is invalid. It should only contain letters.");
                return;
            }

            if(!isValidNum(classNumText.getText()))
            {
                new WarningWindow("Class number is invalid. It should only contain three numbers.");
                return;
            }

            System.out.println("Adding Class...");
            ClassDateCode code = (ClassDateCode) classDatePicker.getSelectedItem();

            try
            {
                String classSec = null;

                // Keep classSec null if there is no text.
                if(!classSecText.getText().isEmpty())
                {
                    // Convert to uppercase just in case user does not put online section in all uppercase.
                    classSec = classSecText.getText().toUpperCase();
                    if(!isValidSection(classSec))
                    {
                        new WarningWindow("Class section is invalid. It should only contain numbers or DL[1,2,3...].");
                        return;
                    }
                }

                // Add to tracker panel (which lists all actively tracked classes).
                JPanel classTrackerPanel = classChecker.addNewClass(Integer.toString(code.getCode()), classSymbolText.getText().toUpperCase(), classNumText.getText(), classSec);
                JPanel mainEntry = new JPanel(new MigLayout());

                mainEntry.add(classTrackerPanel);

                JButton stopTrackingButton = new JButton("Stop Tracking");
                stopTrackingButton.addActionListener(new removeClassListener());

                mainEntry.add(stopTrackingButton);

                // Add to visible class list.
                classList.add(mainEntry, "wrap");
                
                revalidate();
                repaint();
            }
            catch (ExistingClassException e)
            {
                new WarningWindow("Class already exists.");
            }
            catch (NullPointerException exception)
            {
                new WarningWindow(exception.toString());
            }
        }
    }
}
