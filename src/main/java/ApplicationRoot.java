import helpers.Logger;
import helpers.SheetsAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.*;
import java.util.*;

public class ApplicationRoot
{
    private static Logger logger = Logger.getInstance(Logger.LogLevel.DEBUG);

    public static void main(String[] args)
    {
        //Load properties from properties file
        Properties prop = loadProperties("src/main/resources/application.properties");

        //Get the current DateTime and day.
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Long epochTimeNow = dateTimeNow.toEpochSecond(ZoneOffset.UTC) * 1000;   //Need in milliseconds.
        DayOfWeek dayOfWeekNow = dateTimeNow.getDayOfWeek();

        //Set the lookbackPeriod to X weeks from this week's Monday.
        //We raid on Tues/Wed, if we run this report on Monday, or Pre-raid Tuesday,
        //The Agreement is that we only get X weeks data.
        Integer weeksToLookBack = Integer.parseInt(prop.getProperty("weeksLookback"));
        LocalDateTime lookbackPeriod = getLocalDateTimeWeeksOnDayAgo(weeksToLookBack, DayOfWeek.MONDAY, dateTimeNow);
        Long epochTimeLookbackPeriod = lookbackPeriod.toEpochSecond(ZoneOffset.UTC) * 1000; //Need in milliseconds.

        //Create guild, and set up the WarcraftLogsAPI to properly send and retrieve calls.
        Guild myGuild = new Guild(prop.getProperty("guildName"), prop.getProperty("serverName"), prop.getProperty("region"));
        WarcraftLogsAPI wlogsApi = new WarcraftLogsAPI(prop.getProperty("apiKey"));
        List<String> inclusionText = Arrays.asList(prop.getProperty("inclusionText").split(","));
        List<String> splitIndicator = Arrays.asList(prop.getProperty("splitIndicator").split(","));

        //Get all reports within a timeframe
        JSONArray reportsInTimeframe = wlogsApi.getReportsByGuild(myGuild, epochTimeNow, epochTimeLookbackPeriod);
        String oldestRaidLookedUpTitle = ((JSONObject) reportsInTimeframe.get(reportsInTimeframe.length()-1)).getString("title");
        logger.info("The oldest raid we're looking up is one titled: " + oldestRaidLookedUpTitle);
        List<String> relevantReportIDs = new ArrayList<>();
        Calendar.getInstance().getTime();

        //ONE OFF TEST.
        JSONObject getCombatantInfoByReportId = wlogsApi.getCombatantInfoByReportId("anXyAgGtLNFZV3kD");
        JSONObject totalReport = wlogsApi.getReportById("anXyAgGtLNFZV3kD");
        JSONArray combatInfoArray = getCombatantInfoByReportId.getJSONArray("events");

        //Get all reports that are Raid1 Relevant.
        Integer amountSplits = 0;
        for(int i=0; i < reportsInTimeframe.length(); i++)
        {
            JSONObject jsonObj  = reportsInTimeframe.getJSONObject(i);
            String title = jsonObj.getString("title");
            if(containsOneOfInList(title, inclusionText))
            {
                relevantReportIDs.add(jsonObj.getString("id"));
                if(containsOneOfInList(title, splitIndicator))
                {
                    amountSplits++;
                }
            }
        }
        //This is liable to fuck up should we ever do 1 ZG only in a night.
        Integer totalRaids = relevantReportIDs.size() - (amountSplits/2);

        //For each report that's Raid1 relevant, get the list of raiders in that report.
        List<JSONArray> raidAttendences = new ArrayList<>();
        List<JSONArray> friendlies = new ArrayList<>();
        List<Long> startTimes = new ArrayList<>();
        for(String s : relevantReportIDs)
        {
            JSONObject output = wlogsApi.getReportById(s);
            JSONArray raiders = output.getJSONArray("exportedCharacters");
            friendlies.add(output.getJSONArray("friendlies"));
            startTimes.add(output.getLong("start"));
            raidAttendences.add(raiders);
        }

        LocalDateTime ldt = Instant.ofEpochMilli(123L).atZone(ZoneId.systemDefault()).toLocalDateTime();
        ldt.getDayOfWeek();

        //Separate out each raider and count their appearances.
        Map<String, Integer> raiderAttendanceCount = new HashMap<>();
        for(JSONArray array : raidAttendences)
        {
            for(int i = 0; i < array.length(); i++)
            {
                JSONObject raiderObject = array.getJSONObject(i);
                String name = raiderObject.getString("name");
                raiderAttendanceCount = addOrUpdate(raiderAttendanceCount, name);
            }
        }
        Map<String, Integer> sortedRaiderAttendanceCount = new TreeMap<>(raiderAttendanceCount);
        List<List<String>> cellRepresentations = new ArrayList<>();

        for(Map.Entry<String, Integer> entry : sortedRaiderAttendanceCount.entrySet())
        {
            List<String> raiderInfo = new ArrayList<>();
            raiderInfo.add(entry.getKey());
            raiderInfo.add(String.valueOf(entry.getValue()));
            cellRepresentations.add(raiderInfo);
        }
        cellRepresentations.add(0, Arrays.asList("Raiders:", "Attendance (out of " + totalRaids + ")"));

        SheetsAPI sheetsAPI = new SheetsAPI();
        try
        {
            sheetsAPI.write2dDataStartingAt(prop.getProperty("spreadsheetId"), cellRepresentations, "A1");
        }
        catch (IOException | GeneralSecurityException e)
        {
            System.out.println("Error writing to spreadsheet:");
            e.printStackTrace();
        }
    }

    private static boolean containsOneOfInList(String title, List<String> inclusionText)
    {
        for(String s : inclusionText)
        {
            if(title.contains(s))
            {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Integer> addOrUpdate(Map<String, Integer> raiderAttendanceCount, String name)
    {
        Integer count = raiderAttendanceCount.get(name);
        if(count == null)
        {
            count = 0;
        }
        count++;

        raiderAttendanceCount.put(name, count);
        return raiderAttendanceCount;
    }

    private static Properties loadProperties(String propertiesFilename)
    {
        Properties prop = new Properties();
        try
        {
            InputStream inputStream = new FileInputStream(propertiesFilename);
            prop.load(inputStream);
        }
        catch (IOException e)
        {
            System.out.println("IO Exception on " + propertiesFilename + " load. File Not Found.");
        }

        return prop;
    }

    private static LocalDateTime getLocalDateTimeWeeksOnDayAgo(int weeksAgo, DayOfWeek day, LocalDateTime inputDateTime)
    {
        int daysToSubtract = inputDateTime.getDayOfWeek().getValue() - day.getValue();
        LocalDateTime outputTime = inputDateTime.minusDays(daysToSubtract);
        outputTime = outputTime.minusWeeks(weeksAgo);
        return outputTime;
    }
}