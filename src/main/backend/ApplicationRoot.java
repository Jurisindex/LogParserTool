import helpers.LogParseInputData;
import helpers.Logger;
import helpers.SheetsAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.*;
import java.util.*;

//As a user, I want to be able to see all my guild's reports and name of said reports chronologically
//Definitely work on a UI wrapper first.


public class ApplicationRoot
{
    private static Logger logger = Logger.getInstance();

    public static void applicationDryRun(LogParseInputData data) throws GeneralSecurityException, IOException
    {
        logger.setLogLevel(Logger.LogLevel.INFO);
        //Load properties from properties file, and set up our Google Sheets API
        SheetsAPI sheetsAPI = new SheetsAPI();
        Properties prop = loadProperties("src/main/resources/application.properties");

        initializeRoutine(prop, sheetsAPI);
    }

    private static void initializeRoutine(Properties prop, SheetsAPI sheetsAPI)
    {
        //Extract all properties values so we can use them more easily.
        String spreadsheetId = prop.getProperty("spreadsheetId");
        try
        {
            updateAttendanceToLatest(sheetsAPI, spreadsheetId, prop);
        }
        catch (IOException | GeneralSecurityException e)
        {
            logger.error("Exception in updating sheet.");
            logger.error(e.getLocalizedMessage());
        }

        //TODO: Make Use altToMainMapping and attendanceEntry to create an attendanceAggregate.
    }

    private static void updateAttendanceToLatest(SheetsAPI sheetsAPI, String spreadsheetId, Properties prop) throws IOException, GeneralSecurityException
    {
        String weeksLookback = prop.getProperty("weeksLookback");
        List<String> inclusionText = Arrays.asList(prop.getProperty("inclusionText").split(","));
        List<String> splitIndicator = Arrays.asList(prop.getProperty("splitIndicator").split(","));

        //Create guild, and set up the WarcraftLogsAPI to properly send and retrieve calls.
        Guild myGuild = new Guild(prop.getProperty("guildName"), prop.getProperty("serverName"), prop.getProperty("region"));
        WarcraftLogsAPI wlogsApi = new WarcraftLogsAPI(prop.getProperty("apiKey"));

        //Get all the raids we haven't processed yet.
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Long epochTimeNow = dateTimeNow.toEpochSecond(ZoneOffset.UTC) * 1000;   //Need in milliseconds.
        Long latestWeProcessed = getLatestProcessedDate(sheetsAPI, spreadsheetId);
        LocalDateTime lookbackTime = getLocalDateTimeWeeksOnDayAgo(Integer.parseInt(weeksLookback), DayOfWeek.MONDAY, dateTimeNow);
        Long lookbackEpochTime = lookbackTime.toEpochSecond(ZoneOffset.UTC) * 1000; //Need in milliseconds.
        lookbackEpochTime = latestWeProcessed > lookbackEpochTime ? latestWeProcessed : lookbackEpochTime;
        JSONArray reportsInTimeframe = getLatestReportsWeHaventProcessed(lookbackEpochTime, epochTimeNow, myGuild, wlogsApi);

        //Get all raid group relevant reportIds.
        List<String> relevantReportIDs = getAllRaidRelevantReportIds(reportsInTimeframe, inclusionText, splitIndicator);
        //README: Very hacky workaround. See comment near end of ApplicationRoot.getAllRaidRelevantReportIds.
        Integer totalRaids = Integer.parseInt(relevantReportIDs.remove(0));

        //Fill out the list of attendanceEntries. This is the big one.
        List<AttendanceEntry> attendanceEntries = fillOutAttendanceEntries(relevantReportIDs, wlogsApi);
        //Don't continue processing if we don't need to
        if(attendanceEntries.isEmpty())
        {
            logger.info("No new attendance entries to process.");
            return;
        }
        //Debug message to show where we're working from.
        String oldestRaidLookedUpTitle = reportsInTimeframe.getJSONObject(reportsInTimeframe.length()-1).getString("title");
        logger.info("The oldest raid we're looking up is one titled: " + oldestRaidLookedUpTitle);

        //The first one is guaranteed to be the most recent one.
        //The comparison we later use is greater than or equal to
        Long latestReportStartTime = reportsInTimeframe.getJSONObject(0).getLong("end") -1L;

        //Convert to 2dList and print in sheet.
        List<List<String>> attendencesAsStringLists = convertAttendenceListTo2dList(attendanceEntries);

        //Index what we've processed this so far.
        //We used to do LocalDateTime, but I went for a more
//        LocalDateTime latestUpdate = getDateFromListInColumn(attendencesAsStringLists, 4).atStartOfDay();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<String> dateUpdate = new ArrayList<>();
        dateUpdate.add(String.valueOf(latestReportStartTime));
        dateUpdate.add(String.valueOf(attendanceEntries.size()));

        sheetsAPI.append2dData(spreadsheetId, Collections.singletonList(dateUpdate), "datesUpdated");
        sheetsAPI.append2dData(spreadsheetId, attendencesAsStringLists, "attendanceEntry");
    }

    private static List<List<String>> convertAttendenceListTo2dList(List<AttendanceEntry> attendanceList)
    {
        List<List<String>> attendanceStringList = new ArrayList<>(attendanceList.size());

        for(AttendanceEntry entry : attendanceList)
        {
            List<String> attendanceEntryAsList = entry.toList();
            attendanceStringList.add(attendanceEntryAsList);
        }

        return attendanceStringList;
    }

    //The big one.
    private static List<AttendanceEntry> fillOutAttendanceEntries(List<String> relevantReportIDs, WarcraftLogsAPI wlogsApi)
    {
        List<AttendanceEntry> attendanceEntries = new ArrayList<>();
        for(String reportId : relevantReportIDs)
        {
            HashMap<Integer, Player> sourceIdToPlayerMapping = new HashMap<>();
            HashMap<Integer, Set<WorldBuffs>> sourceIdToWorldBuffsMapping = new HashMap<>();

            //Create all the sourceId and PlayerMappings.
            JSONObject output = wlogsApi.getReportById(reportId);
            JSONArray fights = output.getJSONArray("fights");
            Long firstBossTimeStart = 50000000L;
            for(int i = 0; i < fights.length(); i++)
            {
                JSONObject fight = fights.getJSONObject(i);
                if(fight.getInt("boss") != 0)
                {
                    logger.info(output.getString("title") + "'s first boss fight is: " + fight.getString("name"));
                    firstBossTimeStart = fight.getLong("start_time");
                    break;
                }
            }
            JSONArray combatInfo = wlogsApi.getEventTypesByReportId(reportId, 0L, firstBossTimeStart, "combatantinfo").getJSONArray("events");
            JSONArray removedBuffs = wlogsApi.getEventTypesByReportId(reportId, 0L, firstBossTimeStart, "removebuff").getJSONArray("events");
            JSONArray friendlies = output.getJSONArray("friendlies");
            for(int i = 0; i < friendlies.length(); i++)
            {
                JSONObject friendly = (JSONObject) friendlies.get(i);
                Integer sourceId = friendly.getInt("id");
                String classType = friendly.getString("type");
                WoWClass wClass = WoWClass.getClass(classType);
                if(wClass == null)
                {
                    continue;
                }
                Player friendlyPlayer = new Player(friendly.getString("name"), WoWClass.valueOf(classType));
                sourceIdToPlayerMapping.put(sourceId, friendlyPlayer);
            }

            //Get all the WorldBuff mappings at the first boss
            sourceIdToWorldBuffsMapping = updateWorldBuffMappingGivenCombatantInfoEvents(sourceIdToWorldBuffsMapping, combatInfo);


            //Get all the world buffs that fell off between first boss and log start
            sourceIdToWorldBuffsMapping = updateWorldBuffMappingGivenRemovedBuffsEvents(sourceIdToWorldBuffsMapping, removedBuffs);

            //Get auxillary information
            String zone = wlogsApi.getZoneMapping(output.getInt("zone"));
            Long startTime = output.getLong("start");
            LocalDateTime dateOfLog = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).toLocalDateTime();

            //Create attendance entries from the mappings.
            for(Map.Entry<Integer, Player> idPlayerMapping : sourceIdToPlayerMapping.entrySet())
            {
                Integer id = idPlayerMapping.getKey();
                Player player = idPlayerMapping.getValue();
                Set<WorldBuffs> wbuffs = sourceIdToWorldBuffsMapping.get(id);
                if(wbuffs == null)
                {
                    logger.warn("World Buffs null for Player " + player.name + ". Friendlies in \""+output.getString("title")+"\" are: ");
                    logger.warn(friendlies.toString());
                }
                AttendanceEntry entry = new AttendanceEntry(player, id, reportId, dateOfLog, zone, wbuffs);
                attendanceEntries.add(entry);
            }
        }

        return attendanceEntries;
    }

    private static HashMap<Integer, Set<WorldBuffs>> updateWorldBuffMappingGivenCombatantInfoEvents(HashMap<Integer, Set<WorldBuffs>> sourceIdToWorldBuffsMapping, JSONArray combatInfo)
    {
        for(int i = 0; i < combatInfo.length(); i++)
        {
            JSONObject combatant = (JSONObject) combatInfo.get(i);
            Set<WorldBuffs> worldBuffsSet = new HashSet<>();
            JSONArray worldBuffAuras = combatant.getJSONArray("auras");
            Integer id = combatant.getInt("sourceID");
            //Add every wbuff aura to the set
            for(int j = 0; j < worldBuffAuras.length(); j++)
            {
                JSONObject oneAura = worldBuffAuras.getJSONObject(j);
                Integer wbuffId = oneAura.getInt("ability");
                //Figure out which wbuff it is by Id
                WorldBuffs wbuff = WorldBuffs.whichWorldBuff(wbuffId);
                //Add it if it's a wbuff
                if(wbuff != null)
                {
                    worldBuffsSet.add(wbuff);
                }
            }
            sourceIdToWorldBuffsMapping.put(id, worldBuffsSet);
        }

        return sourceIdToWorldBuffsMapping;
    }

    private static HashMap<Integer, Set<WorldBuffs>> updateWorldBuffMappingGivenRemovedBuffsEvents(HashMap<Integer, Set<WorldBuffs>> sourceIdToWorldBuffsMapping, JSONArray removedBuffs)
    {
        for(int i = 0; i < removedBuffs.length(); i++)
        {
            JSONObject removedBuff = removedBuffs.getJSONObject(i);
            Integer wbuffId = removedBuff.getJSONObject("ability").getInt("guid");
            WorldBuffs wbuff = WorldBuffs.whichWorldBuff(wbuffId);
            //Add it to that player's set if it's a wbuff
            if(wbuff != null)
            {
                Integer playerId = 0;
                try
                {
                    playerId = removedBuff.getInt("targetID");
                }
                catch (JSONException e)
                {
                    logger.warn("No key \"targetID\" for JSON object:");
                    logger.warn(removedBuff.toString());
                }
                Set<WorldBuffs> buffsAlready = sourceIdToWorldBuffsMapping.get(playerId);
                if(buffsAlready == null)
                {
                    buffsAlready = new HashSet<>();
                }
                buffsAlready.add(wbuff);
                sourceIdToWorldBuffsMapping.put(playerId, buffsAlready);
            }
        }

        return sourceIdToWorldBuffsMapping;
    }

    private static List<String> getAllRaidRelevantReportIds(JSONArray reportsInTimeframe,
                                                            List<String> inclusionText, List<String> splitIndicator)
    {
        //If nothing specific we're looking for, pick up everything.
        if(inclusionText.isEmpty())
        {
            inclusionText.add("");
        }
        List<String> relevantReportIDs = new ArrayList<>();
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

        //README: This is some VERY Hacky stuff. I'm essentially using this to pass back a 2nd return value.
        //This represents the COUNT of unique expected raids a raider could possible have attended.
        //I pass it as a string in the 0th index, and then remove it first thing after I return.
        relevantReportIDs.add(0, String.valueOf(totalRaids));

        return relevantReportIDs;
    }

    private static Long getLatestProcessedDate(SheetsAPI sheetsAPI, String spreadsheetId) throws GeneralSecurityException, IOException
    {
        //Get the attendanceRecords that exist in the sheet.
        List<List<String>> datesUpdated = sheetsAPI.getRows(spreadsheetId,
                "datesUpdated", "A:Z");

        return getLatestDateInList(datesUpdated);
    }

    private static Long getLatestDateInList(List<List<String>> attendencesAsStringLists)
    {
        Long ld = 0L;

        List<String> headersRow = attendencesAsStringLists.get(0);
        Integer dateColumn = getIndexThatEquals(headersRow, "date");

        if(attendencesAsStringLists.size() <= 1 || dateColumn.equals(-1))
        {
            return ld;
        }

        ld = getDateFromListInColumn(attendencesAsStringLists, dateColumn);

        return ld;
    }

    private static Long getDateFromListInColumn(List<List<String>> list, Integer dateColumn)
    {
        Long ld = 0L;

        for(int i = 1; i < list.size(); i++)
        {
            List<String> thisRow = list.get(i);

            Long thisRowTime = Long.parseLong(thisRow.get(dateColumn));
            if(thisRowTime > ld)
            {
                ld = thisRowTime;
            }
        }

        return ld;
    }

    private static Integer getIndexThatEquals(List<String> headersRow, String match)
    {
        int index = 0;
        for(; index < headersRow.size(); index++)
        {
            if(headersRow.get(index).equals(match))
            {
                return index;
            }
        }

        return -1;
    }

    private static JSONArray getLatestReportsWeHaventProcessed(Long lookbackPeriod, Long epochTimeNow,
                                                               Guild myGuild, WarcraftLogsAPI wlogsApi)
    {
        //Search wlogs till the earliest one, between the timeframes of then and now
        JSONArray reportsInTimeframe = wlogsApi.getReportsByGuild(myGuild, epochTimeNow, lookbackPeriod);

        return reportsInTimeframe;
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