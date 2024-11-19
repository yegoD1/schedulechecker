package com.gmuprojects;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.swing.BoxLayout;
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

public class MainFrame extends JFrame{

    private static final String TERM_URL = "https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/classSearch/getTerms";

    private static final HttpRequest TERM_REQUEST;

    static{
        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] headerPairs = new BasicPair[2];
        headerPairs[0] = new BasicPair<String,String>("Accept", "*/*");
        headerPairs[1] = new BasicPair<String,String>("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:131.0) Gecko/20100101 Firefox/131.0");

        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] termQueryPairs = new BasicPair[2];
        termQueryPairs[0] = new BasicPair<String,String>("offset", "1");
        termQueryPairs[1] = new BasicPair<String,String>("max", "10");

        TERM_REQUEST = HttpRequestBuilder.BuildCall(TERM_URL, termQueryPairs, headerPairs, HttpCallType.GET, null);
    }

    // Main panel for this application.
    private JPanel mainPanel;

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

    private CookieManager cookieManager;

    public MainFrame() throws Exception
    {
        // Construct all elements to be added to this frame.
        setupPanel();
        add(mainPanel);

        // Handle cookies for JSESSION.
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        // Construct http client to use for http requests.
        httpClient = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .followRedirects(Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .cookieHandler(CookieHandler.getDefault())
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
            return description;
        }
    }

    private void onTermsRecieved(HttpResponse<String> response)
    {
        JSONArray jsonArray = new JSONArray(response.body());

        fillDatePicker(jsonArray);
    }

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
    }

    private class addClassListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent event) {
            System.out.println("Adding Class...");

            ClassDateCode code = (ClassDateCode) classDatePicker.getSelectedItem();

            try
            {
                String classSec = null;

                // Keep classSec null if there is no text.
                if(!classSecText.getText().isEmpty())
                {
                    classSec = classSecText.getText();
                }
                
                // Add to tracker panel (which lists all actively tracked classes).
                JPanel classTrackerPanel = classChecker.addNewClass(Integer.toString(code.getCode()), classSymbolText.getText(), classNumText.getText(), classSec);

                // Add to visible class list.
                classList.add(classTrackerPanel,  "wrap");
                revalidate();
            }
            catch (NullPointerException exception)
            {
                new WarningWindow(exception.toString());
            }
        }
    }

    private void setupPanel()
    {
        // Use custom layout.
        mainPanel = new JPanel(new MigLayout("fillx, debug"));

        JLabel titleLabel = new JLabel("GMU Class Availability Checker");
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 24));

        CC centerConstraint = new CC();
        centerConstraint.alignX("center").spanX().wrap();

        // Title at top of the window.
        mainPanel.add(titleLabel, centerConstraint);

        JLabel datePickerLabel = new JLabel("Class Date:");
        mainPanel.add(datePickerLabel, "split 2, span, align center");

        // Date picker for classes.
        classDatePicker = new JComboBox<ClassDateCode>();
        classDatePicker.setEditable(true);

        mainPanel.add(classDatePicker);

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

        classList = new JPanel(new MigLayout("fillx, debug"));

        //classScrollPane = new JScrollPane(classList);
        mainPanel.add(classList, "growx, span");
    }
}
