package com.gmuprojects;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import net.miginfocom.swing.MigLayout;

/**
 * Class that handles outgoing requests for class availability.
 */
public class ClassChecker {

    // Actual URL to set search mode.
    private static final String SETSEARCH_URL = "https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/term/search";
    
    // Actual URL to perform a course search.
    private static final String COURSESEARCH_URL = "https://ssbstureg.gmu.edu/StudentRegistrationSsb/ssb/searchResults/searchResults";

    // Header pairs to always be sent with every request.
    private static final BasicPair<String, String>[] HEADER_PAIRS;

    static{
        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] headerPairs = new BasicPair[2];
        headerPairs[0] = new BasicPair<String,String>("Accept", "*/*");
        headerPairs[1] = new BasicPair<String,String>("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:131.0) Gecko/20100101 Firefox/131.0");

        HEADER_PAIRS = headerPairs;
    }

    // Update rate in seconds.
    private static final int UPDATE_RATE = 5;

    // HttpClient to send out requests.
    private HttpClient httpClient;

    // All available class searches to do. A rotating queue.
    private Queue<ClassEntry> jobs;

    // Timer that delays each search request by UPDATE_RATE.
    private Timer taskTimer;

    /**
     * Constructor for ClassChecker that takes in the current httpClient being used.
     * @param httpClient HttpClient that is being used by this program.
     */
    public ClassChecker(HttpClient httpClient)
    {
        this.httpClient = httpClient;
        jobs = new LinkedList<ClassEntry>();

        // Create timer that continually updates every UPDATE_RATE seconds.
        taskTimer = new Timer((int) TimeUnit.SECONDS.toMillis(UPDATE_RATE), new UpdateClass());
        taskTimer.stop();
    }

    /**
     * TimerTask that will perform the class searched by UPDATE_RATE seconds.
     */
    private class UpdateClass implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            ClassEntry entry = jobs.poll();

            entry.performSearch();

            // Re-add into queue at end.
            jobs.add(entry);
        }
    }

    /**
     * Adds a new class to be tracked. Will be added to end of the queue.
     * @param dateCode Date code of the class. Should look something like <code>202510</code> for Spring 2025.
     * @param classSymbol Symbol of the class. Should match the letters of the class to track. For CS 262 it would be <code>CS</code>.
     * @param classNum The numbers after the class symbol. For CS 262 it would be <code>262</code>. 
     * @param classSection Optional section to track. For CS 262-001 it would be <code>001</code>.
     * @return JPanel to be added to active class tracked list.
     */
    public JPanel addNewClass(String dateCode, String classSymbol, String classNum, String classSection)
    {
        ClassEntry classEntry = new ClassEntry(dateCode, classSymbol, classNum, classSection);

        // Add to pending jobs.
        jobs.add(classEntry);

        // No jobs were added before until now.
        if(jobs.size() == 1)
        {
            taskTimer.setInitialDelay(0);
            taskTimer.restart();
        }

        return classEntry;
    }

    /**
     * Removes a class to track.
     * @param classToRemove Class that wants to be removed.
     */
    public void removeClass(JPanel classToRemove)
    {
        jobs.remove(classToRemove);

        // Stop timer if there are no more jobs.
        if(jobs.size() == 0)
        {
            taskTimer.stop();
        }
    }

    /**
     * ClassEntry is a JPanel used display one tracked entry.
     */
    private class ClassEntry extends JPanel
    {
        private JPanel mainPanel;

        // Label that gets updated each time class capacity is updated.
        private JLabel classCapacityLabel;

        // HttpRequest to use when wanting to set search mode.
        private HttpRequest setSearchRequest;

        // HttpRequest to use when wanting to search for a class.
        private HttpRequest classSearchRequest;

        // True if was supplied with a valid section.
        private boolean hasValidSection;

        //
        private String classSection;

        /**
         * Constructor for ClassEntry. Takes same arguments as {@link ClassChecker#addNewClass(String, String, String, String)};
         * @param dateCode Date code of the class. Should look something like <code>202510</code> for Spring 2025.
         * @param classSymbol Symbol of the class. Should match the letters of the class to track. For CS 262 it would be <code>CS</code>.
         * @param classNum The numbers after the class symbol. For CS 262 it would be <code>262</code>. 
         * @param classSection Optional section to track. For CS 262-001 it would be <code>001</code>.
         */
        public ClassEntry(String dateCode, String classSymbol, String classNum, String classSection)
        {
            setupPanel(dateCode, classSymbol, classNum, classSection);

            hasValidSection = (classSection != null);
            this.classSection = classSection;

            // Setup query pairs for set search mode.
            @SuppressWarnings("unchecked")
            BasicPair<String, String>[] setSearchQueryPairs = new BasicPair[1];
            setSearchQueryPairs[0] = new BasicPair<String,String>("mode", "search");
    
            // Must also be sent a body with date user wants to search in.
            Map<String, String> body = new HashMap<String, String>();
            body.put("term", dateCode);
    
            setSearchRequest = HttpRequestBuilder.BuildCall(SETSEARCH_URL, setSearchQueryPairs, HEADER_PAIRS, HttpCallType.POST, body);

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

        /**
         * Sets up the panel for display. For more explanation on parameters, see {@link #ClassEntry}.
         * @param dateCode Date code of this class.
         * @param classSymbol Symbol of this class
         * @param classNum The numbers after the class symbol.
         * @param classSection Optional section to track.
         */
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
    
            classCapacityLabel = new JLabel("0/0");

            if(classSection == null)
            {
                classCapacityLabel.setText("Multi-Section");
            }
            
            mainPanel.add(classCapacityLabel, "growx");

            add(mainPanel);
        }

        /**
         * Performs the actual internet search using the HttpClient. Will first set search mode and then perform actual search.
         */
        public void performSearch()
        {
            // First set search mode to setup session.
            CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(setSearchRequest, BodyHandlers.ofString());
            response.thenAccept(result -> classQuerySearch(result));
        }

        /**
         * Performs query to get class data.
         */
        private void classQuerySearch(HttpResponse<String> response)
        {
            CompletableFuture<HttpResponse<String>> queryResponse = httpClient.sendAsync(classSearchRequest, BodyHandlers.ofString());
            queryResponse.thenAccept(result -> updateEntry(result));
        }

        private void updateEntry(HttpResponse<String> bodyResponse)
        {
            JSONObject object = new JSONObject(bodyResponse.body());
            System.out.println(object);

            JSONArray dataArray = object.getJSONArray("data");
            for(int i = 0; i < dataArray.length(); i++)
            {
                JSONObject entry = dataArray.getJSONObject(i);
                if(hasValidSection)
                {
                    String classSequenceNum = entry.getString("sequenceNumber");

                    if(classSequenceNum.equals(classSection))
                    {
                        int seatsAvailable = entry.getInt("enrollment");
                        int maxSize = entry.getInt("maximumEnrollment");
                        
                        updateCapacity(seatsAvailable, maxSize);
                    }
                }
                else
                {
                    int maxSize = entry.getInt("maximumEnrollment");
                    int seatsAvailable = entry.getInt("seatsAvailable");

                    // Update status if available.
                }
            }
        }

        private void updateCapacity(int takenSeats, int maxSeats)
        {
            classCapacityLabel.setText(takenSeats + "/" + maxSeats);
        }
    }
}
