package com.gmuprojects;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class ClassChecker {

    private static final String SETSEARCH_URL = "https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/term/search";
    private static final String COURSESEARCH_URL = "https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/searchResults/searchResults";

    private static final HttpRequest SETSEARCH_REQUEST;

    // Header pairs to always be sent with every request.
    private static final BasicPair<String, String>[] HEADER_PAIRS;

    static{
        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] headerPairs = new BasicPair[2];
        headerPairs[0] = new BasicPair<String,String>("Accept", "*/*");
        headerPairs[1] = new BasicPair<String,String>("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:131.0) Gecko/20100101 Firefox/131.0");

        HEADER_PAIRS = headerPairs;

        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] setSearchQueryPairs = new BasicPair[1];
        setSearchQueryPairs[0] = new BasicPair<String,String>("mode", "search");

        SETSEARCH_REQUEST = HttpRequestBuilder.BuildCall(SETSEARCH_URL, setSearchQueryPairs, HEADER_PAIRS, HttpCallType.POST, "term=202510");
    }

    // Update rate in seconds.
    private static final int UPDATE_RATE = 15;

    // HttpClient to send out requests.
    private HttpClient httpClient;

    private Queue<ClassEntry> jobs;

    private Timer taskTimer;

    public ClassChecker(HttpClient httpClient)
    {
        this.httpClient = httpClient;
        jobs = new LinkedList<ClassEntry>();

        // Create timer that continually updates every UPDATE_RATE seconds.
        taskTimer = new Timer();
        taskTimer.schedule(new UpdateClass(), 0, TimeUnit.SECONDS.toMillis(UPDATE_RATE));
    }

    private class UpdateClass extends TimerTask
    {
        @Override
        public void run() {
            ClassEntry entry = jobs.poll();
            

            jobs.add(entry);
        }
    }

    public JPanel addNewClass(String dateCode, String classSymbol, String classNum, String classSection)
    {
        ClassEntry classEntry = new ClassEntry(dateCode, classSymbol, classNum, classSection);

        // Add to pending jobs.
        jobs.add(classEntry);

        return classEntry;
    }

    /**
     * ClassEntry is a JPanel used display one tracked entry.
     */
    private class ClassEntry extends JPanel
    {
        private JPanel mainPanel;
        private JPanel loadingPanel;
        private JButton removeButton;

        private HttpRequest classSearchRequest;

        public ClassEntry(String dateCode, String classSymbol, String classNum, String classSection)
        {
            setupPanel(dateCode, classSymbol, classNum, classSection);

            @SuppressWarnings("unchecked")
            BasicPair<String, String>[] queries = new BasicPair[7];
            queries[0] = new BasicPair<String,String>("txt_subject", classSymbol);
            queries[1] = new BasicPair<String,String>("txt_courseNumber", classNum);
            queries[2] = new BasicPair<String,String>("txt_term", dateCode);
            queries[3] = new BasicPair<String,String>("pageOffset", "0");
            queries[4] = new BasicPair<String,String>("pageMaxSize", "25");
            queries[5] = new BasicPair<String,String>("sortColumn", "subjectDescription");
            queries[6] = new BasicPair<String,String>("sortDirection", "asc");

            classSearchRequest = HttpRequestBuilder.BuildCall(COURSESEARCH_URL, queries, HEADER_PAIRS, HttpCallType.GET, null);
        }

        public void performSearch()
        {
            
        }

        private class stopTrackingListener implements ActionListener
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        }

        public void setupPanel(String dateCode, String classSymbol, String classNum, String classSection)
        {
            mainPanel = new JPanel(new MigLayout("fillx, debug"));

            try
            {
                ImageIcon imageIcon = new ImageIcon(getClass().getResource("resources/loading.gif"));
                JLabel loadingIcon = new JLabel(imageIcon);
                loadingIcon.setSize(20, 20);
                mainPanel.add(loadingIcon);
            }
            catch (Exception e)
            {
                new WarningWindow(e.toString());
            }
    
            JLabel className = new JLabel(classSymbol + " " + classNum);
            mainPanel.add(className, "growx");
    
            JLabel classCapacity = new JLabel("0/0");
            mainPanel.add(classCapacity, "growx");

            removeButton = new JButton("Stop Tracking");
            removeButton.addActionListener(new stopTrackingListener());
            mainPanel.add(removeButton, "growx");

            add(mainPanel);
        }

        public HttpRequest getHttpRequest()
        {
            return classSearchRequest;
        }

        public void setMaxCapacity(int maxCapacity)
        {

        }

        public void setCurrentCapacity(int capacity)
        {

        }
    }
}
