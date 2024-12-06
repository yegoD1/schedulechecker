package com.yegoD;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
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

import javax.imageio.ImageIO;
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

    // Update rate in seconds.
    private static final int UPDATE_RATE = 5;

    static{
        @SuppressWarnings("unchecked")
        BasicPair<String, String>[] headerPairs = new BasicPair[2];
        headerPairs[0] = new BasicPair<String,String>("Accept", "*/*");
        headerPairs[1] = new BasicPair<String,String>("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:131.0) Gecko/20100101 Firefox/131.0");

        HEADER_PAIRS = headerPairs;
    }

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

    private ClassEntry lastUpdatedEntry;

    /**
     * TimerTask that will perform the class searched by UPDATE_RATE seconds.
     */
    private class UpdateClass implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            ClassEntry entry = jobs.poll();
            
            // Clear last updated entry.
            lastUpdatedEntry.setBackground(Color.LIGHT_GRAY);

            entry.performSearch();

            // Highlight current job that got updated.
            entry.setBackground(new Color(120,120,120));

            // Re-add into queue at end.
            jobs.add(entry);

            // Entry is now our lasted updated.
            lastUpdatedEntry = entry;
        }
    }

    /**
     * Adds a new class to be tracked. Will be added to end of the queue.
     * @param dateCode Date code of the class. Should look something like <code>202510</code> for Spring 2025.
     * @param classSymbol Symbol of the class. Should match the letters of the class to track. For CS 262 it would be <code>CS</code>.
     * @param classNum The numbers after the class symbol. For CS 262 it would be <code>262</code>. 
     * @param classSection Optional section to track. For CS 262-001 it would be <code>001</code>.
     * @return JPanel to be added to active class tracked list.
     * @throws ExistingClassException Happens when there is already a tracked class in the list.
     */
    public JPanel addNewClass(String dateCode, String classSymbol, String classNum, String classSection) throws ExistingClassException
    {
        if(isDuplicateClass(classSymbol, classNum, classSection))
        {
            throw new ExistingClassException("Class already exists.");
        }

        ClassEntry classEntry = new ClassEntry(dateCode, classSymbol, classNum, classSection);

        // Add to pending jobs.
        jobs.add(classEntry);

        // No jobs were added before until now.
        if(jobs.size() == 1)
        {
            lastUpdatedEntry = classEntry;
            taskTimer.setInitialDelay(0);
            taskTimer.restart();
        }

        return classEntry;
    }

    /**
     * Takes in information for a class and checks if there is a duplicate.
     * @param classSymbol
     * @param classNum
     * @param classSection
     * @return
     */
    private boolean isDuplicateClass(String classSymbol, String classNum, String classSection)
    {
        for(ClassEntry currentEntry : jobs)
        {
            if(currentEntry.classSymbol.equals(classSymbol) &&
                currentEntry.classNum.equals(classNum))
            {
                if(currentEntry.hasValidSection)
                {
                    if(currentEntry.classSection.equals(classSection))
                    {
                        return true;
                    }
                }
                else
                {
                    // If current entry has no valid section, then incoming classSection must be null to be duplicate to this class.
                    if(classSection == null)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
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
            lastUpdatedEntry = null;
            taskTimer.stop();
        }
    }

    /**
     * ClassEntry is a JPanel used display one tracked entry.
     */
    private class ClassEntry extends JPanel
    {
        // Enum representing all the states a class can be in.
        private enum ClassStatus
        {
            UNAVAILABLE,
            UNCHECKED,
            AVAILABLE
        }

        // Main JPanel where elements will be added to. Will then be added to this JPanel.
        private JPanel mainPanel;

        // Status icon that is shown to the left of the entry.
        private JLabel statusIcon;

        // Label that gets updated each time class capacity is updated.
        private JLabel classCapacityLabel;

        // HttpRequest to use when wanting to set search mode.
        private HttpRequest setSearchRequest;

        // HttpRequest to use when wanting to search for a class.
        private HttpRequest classSearchRequest;

        // True if was supplied with a valid section.
        private boolean hasValidSection;

        // Class symbol to track.
        private String classSymbol;

        // Class number to track.
        private String classNum;

        // Class section to track.
        private String classSection;

        // Enum representing the current status of this class.
        private ClassStatus classStatus;

        /**
         * Constructor for ClassEntry. Takes same arguments as {@link ClassChecker#addNewClass(String, String, String, String)};
         * @param dateCode Date code of the class. Should look something like <code>202510</code> for Spring 2025.
         * @param classSymbol Symbol of the class. Should match the letters of the class to track. For CS 262 it would be <code>CS</code>.
         * @param classNum The numbers after the class symbol. For CS 262 it would be <code>262</code>. 
         * @param classSection Optional section to track. For CS 262-001 it would be <code>001</code>.
         */
        public ClassEntry(String dateCode, String classSymbol, String classNum, String classSection)
        {
            this.classSymbol = classSymbol;
            this.classNum = classNum;
            this.classSection = classSection;

            classStatus = ClassStatus.UNCHECKED;

            hasValidSection = (classSection != null);
            this.classSection = classSection;

            // Initialize display elements.
            setupPanel(dateCode);

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
         */
        public void setupPanel(String dateCode)
        {
            mainPanel = new JPanel(new MigLayout("fillx"));

            // Create loading icon.
            try
            {
                BufferedImage img = ImageIO.read(getClass().getClassLoader().getResource("wait.png"));
                Image scaledImg = img.getScaledInstance(60, 60, Image.SCALE_SMOOTH);

                statusIcon = new JLabel(new ImageIcon(scaledImg));
                statusIcon.setSize(60, 60);

                mainPanel.add(statusIcon);
            }
            catch (Exception e)
            {
                new WarningWindow(e.toString());
            }
    
            // Setup class name and capacity.
            JLabel className = new JLabel();
            classCapacityLabel = new JLabel("0/0");
            classCapacityLabel.setFont(new Font("Arial", Font.BOLD, 18));

            // Setup capacity label.
            if(classSection == null)
            {
                classCapacityLabel.setText("Multi-Section");
                
            }
            className.setText(getClassDisplayName());

            mainPanel.add(className, "growx");
            mainPanel.add(classCapacityLabel, "growx");

            add(mainPanel);
        }

        private String getClassDisplayName()
        {
            if(hasValidSection)
            {
                return classSymbol + " " + classNum + "-" + classSection;
            }

            return classSymbol + " " + classNum;
        }

        /**
         * Performs the actual internet search using the HttpClient. Will first set search mode and then perform actual search.
         */
        public void performSearch()
        {
            // First set search mode to setup session.
            CompletableFuture<HttpResponse<Void>> response = httpClient.sendAsync(setSearchRequest, BodyHandlers.discarding());
            response.thenAccept(result -> classQuerySearch());
        }

        /**
         * Performs query to get class data.
         */
        private void classQuerySearch()
        {
            CompletableFuture<HttpResponse<String>> queryResponse = httpClient.sendAsync(classSearchRequest, BodyHandlers.ofString());
            queryResponse.thenAccept(result -> updateEntry(result));
        }

        /**
         * Updates the entry from recieved class lookup.
         * @param bodyResponse The full body response recieved from http request.
         */
        private void updateEntry(HttpResponse<String> bodyResponse)
        {
            JSONObject object = new JSONObject(bodyResponse.body());

            // Response comes with a large "data" array.
            JSONArray dataArray = object.getJSONArray("data");
            for(int i = 0; i < dataArray.length(); i++)
            {
                JSONObject entry = dataArray.getJSONObject(i);
                if(hasValidSection)
                {
                    // If user is looking for a specific section.
                    String classSequenceNum = entry.getString("sequenceNumber");

                    if(classSequenceNum.equals(classSection))
                    {
                        // Found the specific class.
                        int seatsTaken = entry.getInt("enrollment");
                        int maxSize = entry.getInt("maximumEnrollment");
                        
                        updateCapacity(seatsTaken, maxSize);
                        if(seatsTaken < maxSize && (classStatus == ClassStatus.UNCHECKED || classStatus == ClassStatus.UNAVAILABLE))
                        {
                            changeStatus(true);
                        }
                        else if(seatsTaken >= maxSize && (classStatus == ClassStatus.UNCHECKED || classStatus == ClassStatus.AVAILABLE))
                        {
                            changeStatus(false);
                        }
                    }
                }
                else
                {
                    // If user is looking for any section.
                    int seatsTaken = entry.getInt("enrollment");
                    int maxSize = entry.getInt("maximumEnrollment");
                    
                    // Only care about one class that has availability.
                    if(seatsTaken < maxSize)
                    {
                        if(classStatus == ClassStatus.UNCHECKED || classStatus == ClassStatus.UNAVAILABLE)
                        {
                            changeStatus(true);
                        }

                        return;
                    }
                }
            }

            // Multi-class section and there is no more capacity.
            if(!hasValidSection && (classStatus == ClassStatus.UNCHECKED || classStatus == ClassStatus.AVAILABLE))
            {
                // No classes were found.
                changeStatus(false); 
            }
        }

        /**
         * Internal method for updating the current capacity fo this class.
         * @param takenSeats How many seats are taken up by others.
         * @param maxSeats The maximum available number of seats.
         */
        private void updateCapacity(int takenSeats, int maxSeats)
        {
            classCapacityLabel.setText(takenSeats + "/" + maxSeats);
        }

        /**
         * Changes the status of this entry.
         * @param isAvailable True if this entry is availble for registration.
         */
        private void changeStatus(boolean isAvailable)
        {
            try
            {
                System.out.println("Working Directory: " + System.getProperty("user.dir"));

                // Let's check if we can find the resource using different methods
                URL url1 = getClass().getResource("/success.png");
                URL url2 = getClass().getClassLoader().getResource("success.png");
                URL url3 = ClassChecker.class.getResource("/success.png");

                System.out.println("Method 1 result: " + url1);
                System.out.println("Method 2 result: " + url2);
                System.out.println("Method 3 result: " + url3);

                // Try to update images to correct status.
                if(isAvailable)
                {
                    BufferedImage img = ImageIO.read(getClass().getClassLoader().getResource("success.png"));
                    Image scaledImg = img.getScaledInstance(statusIcon.getWidth(), statusIcon.getHeight(), Image.SCALE_SMOOTH);
                    statusIcon.setIcon(new ImageIcon(scaledImg));
                }
                else
                {
                    BufferedImage img = ImageIO.read(getClass().getClassLoader().getResource("failed.png"));
                    Image scaledImg = img.getScaledInstance(statusIcon.getWidth(), statusIcon.getHeight(), Image.SCALE_SMOOTH);
                    statusIcon.setIcon(new ImageIcon(scaledImg));
                }
            }
            catch (Exception e)
            {
                new WarningWindow(e.toString());
            }

            // Update status.
            if(isAvailable)
            {
                classStatus = ClassStatus.AVAILABLE;
                new WarningWindow("Class " + getClassDisplayName() + " is now open!");
            }
            else
            {
                classStatus = ClassStatus.UNAVAILABLE;
            }
        }
    }
}
