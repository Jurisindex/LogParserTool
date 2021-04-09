import UIHelpers.LogParseInputData;
import helpers.Logger;
import helpers.SheetsAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.time.*;
import java.util.*;

//As a user, I want to be able to see all my guild's reports and name of said reports chronologically
//Definitely work on a UI wrapper first.

public class ApplicationRoot
{
    private static Logger logger = Logger.getInstance();
    private static int totalRaids;

    public static void applicationDryRun(LogParseInputData data) throws Exception
    {
        logger.setLogLevel(Logger.LogLevel.INFO);

        if(!data.verifyAllDataIsHere())
        {
            logger.error("Not all mandatory fields were marked");
            throw new Exception("Not all mandatory fields were marked");
        }
        //Load properties from properties file, and set up our Google Sheets API
        SheetsAPI sheetsAPI = new SheetsAPI();
        //Properties prop = loadProperties("src/main/resources/application.properties");

        initializeUpdateJob(data, sheetsAPI);
    }

    private static void initializeUpdateJob(LogParseInputData data, SheetsAPI sheetsAPI)
    {
        //Extract all properties values so we can use them more easily.
        try
        {
            updateAttendanceEntriesToLatest(sheetsAPI, data);
            List<ReportAggregate> reportAggregateData = createReportAggregates(sheetsAPI, data);
            updateReportAggregatesToLatest(sheetsAPI, data, reportAggregateData);
            List<ReportAggregate> latestReportAggregates = getListFromSpreadsheet(sheetsAPI, data.spreadsheetId, "reportAggregate", ReportAggregate.class);
            List<PlayerAggregate> updatedPlayerAggregates = recalculatePlayerAggregate(sheetsAPI, data, latestReportAggregates);
            updatePlayerAggregatesToLatest(sheetsAPI, data, updatedPlayerAggregates);
        }
        catch (Exception e)
        {
            logger.error("Exception in processing.");
            logger.error(e.getLocalizedMessage());
        }

        //TODO: Make Use altToMainMapping and attendanceEntry to create an attendanceAggregate.
    }

    private static List<PlayerAggregate> recalculatePlayerAggregate(SheetsAPI sheetsAPI, LogParseInputData data, List<ReportAggregate> latestReportAggregates)
    {
        Map<LocalDate,List<ReportAggregate>> raidsOnDateMap = new HashMap<>();
        Map<String, PlayerAggregate> playerNameToAggregateMap = new HashMap<>();
        LocalDate latestLookupDate = getLocalDateTimeWeeksOnDayAgo(data.weeksLookback, DayOfWeek.MONDAY, LocalDateTime.now()).toLocalDate();

        for(ReportAggregate ra : latestReportAggregates)
        {
            LocalDate raDate = getDateFromString(ra.date);
            if(latestLookupDate.isBefore(raDate))
            {
                List<ReportAggregate> reportAggregates = raidsOnDateMap.get(raDate);
                if(reportAggregates == null)
                {
                    reportAggregates = new ArrayList<>();
                }
                reportAggregates.add(ra);
            }
        }

        //Now we have all the raids sorted by dates.


        return (List<PlayerAggregate>) playerNameToAggregateMap.values();
    }

    private static void updatePlayerAggregatesToLatest(SheetsAPI sheetsAPI, LogParseInputData data, List<PlayerAggregate> updatedPlayerAggregates)
    {
        //Get back the previous aggregates.
        //Take the list of current aggregates we have and make it into a hashMap of Player.name => {PlayerAggergate}.
        //Iterate through the prev. Aggregates list.
            //If we find a hit in the hashmap, PUT Update the info.
            //If we do not find a hit, assume the player hasn't been to raid in the past 8 weeks, and reflect that.
        //Push 2dData back up to the sheet.
    }

    private static void updateReportAggregatesToLatest(SheetsAPI sheetsAPI, LogParseInputData data, List<ReportAggregate> reportAggregateData) throws Exception
    {
        List<List<String>> aggregateSheetData = convertObjectListTo2dList(reportAggregateData, ReportAggregate.class);
        sheetsAPI.append2dData(data.spreadsheetId, aggregateSheetData, "reportAggregate");
    }

    private static void updateAttendanceEntriesToLatest(SheetsAPI sheetsAPI, LogParseInputData data) throws IOException, GeneralSecurityException
    {
        Integer weeksLookback = data.weeksLookback;
        String spreadsheetId = data.spreadsheetId;
        List<String> inclusionText = data.inclusionText;
        List<String> splitIndicator = data.splitIndicators;

        //Create guild, and set up the WarcraftLogsAPI to properly send and retrieve calls.
        Guild myGuild = new Guild(data.guildName, data.serverName, data.region);
        WarcraftLogsAPI wlogsApi = new WarcraftLogsAPI(data.apiKey);

        //Get all the raids we haven't processed yet.
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Long epochTimeNow = dateTimeNow.toEpochSecond(ZoneOffset.UTC) * 1000;   //Need in milliseconds.
        Long lookbackEpochTime = getEpochMillisecondTimeWeeksOnDayAgo(weeksLookback, DayOfWeek.MONDAY, dateTimeNow);

        List<String> reportsProcessed = getReportsProcessed(sheetsAPI, spreadsheetId);
        JSONArray reportsInTimeframe = getLatestReportsWeHaventProcessed(lookbackEpochTime, epochTimeNow, myGuild, wlogsApi, reportsProcessed);

        //Get all raid group relevant reportIds.
        List<String> relevantReportIDs = getAllRaidRelevantReportIds(reportsInTimeframe, inclusionText, splitIndicator);
        //README: Very hacky workaround. See comment near end of ApplicationRoot.getAllRaidRelevantReportIds.
        totalRaids = Integer.parseInt(relevantReportIDs.remove(0));
        Collections.reverse(relevantReportIDs); //So we can have the chronological parsing we want.

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

        //Convert to 2dList and print in sheet.
        List<List<String>> attendencesAsStringLists = convertAttendenceListTo2dList(attendanceEntries);

        //Index what we've already indexed.
        List<List<String>> reportsUpdate = extractReportIDsAndTimestamp(relevantReportIDs, reportsInTimeframe, dateTimeNow);

        sheetsAPI.append2dData(spreadsheetId, reportsUpdate, "reportsProcessed");
        sheetsAPI.append2dData(spreadsheetId, attendencesAsStringLists, "attendanceEntry");
    }

    private static List<ReportAggregate> createReportAggregates(SheetsAPI sheetsAPI, LogParseInputData data)
    {
        List<AttendanceEntry> attendanceEntries = getAllAttendanceEntries(sheetsAPI, data.spreadsheetId);
        Map<String, String> reportIdToTitleMapping = getReportIdToReportTitleMappings(sheetsAPI, data.spreadsheetId);
        List<ReportAggregate> reportAggregatesAlreadyProcessed = getListFromSpreadsheet(sheetsAPI, data.spreadsheetId, "reportAggregate", ReportAggregate.class);
        Set<String> reportIDsAlreadyAggregated = extractReportIDsFromReportAggregates(reportAggregatesAlreadyProcessed);
        List<AttendanceEntry> attendanceEntriesFANCY_WAY = getListFromSpreadsheet(sheetsAPI, data.spreadsheetId, "attendanceEntry", AttendanceEntry.class);

        List<ReportAggregate> aggregationsToAdd = calculateNewReportAggregates(attendanceEntries, reportIDsAlreadyAggregated, reportIdToTitleMapping);
        return aggregationsToAdd;
    }

    private static List<ReportAggregate> calculateNewReportAggregates(List<AttendanceEntry> attendanceEntries,
                                                                      Set<String> alreadyProcessed,
                                                                      Map<String, String> reportIdToTitleMapping)
    {
        Map<String, ReportAggregate> reportIdToAggregateMappings = new HashMap<>();
        for(AttendanceEntry entry : attendanceEntries)
        {
            String reportId = entry.reportId;
            //If this hasn't already been processed...
            if(!alreadyProcessed.contains(reportId))
            {
                //Get or create
                ReportAggregate aggregate = reportIdToAggregateMappings.get(reportId);
                if(aggregate == null)
                {
                    aggregate = new ReportAggregate(reportId, reportIdToTitleMapping.get(reportId),
                                                    new ArrayList<>(), new ArrayList<>(), entry.date, entry.dayOfWeek);
                }

                //Update and persist
                String playerName = entry.player.name;
                aggregate.playersAttended.add(playerName);
                if(!entry.acceptableWbuffs)
                {
                    aggregate.playersWithInsufficientWBuffs.add(playerName);
                }
                reportIdToAggregateMappings.put(reportId, aggregate);
            }
        }
        List<ReportAggregate> aggregates = new ArrayList<>();
        for(Map.Entry<String,ReportAggregate> entry : reportIdToAggregateMappings.entrySet())
        {
            aggregates.add(entry.getValue());
        }
        return aggregates;
    }

    private static Map<String, String> getReportIdToReportTitleMappings(SheetsAPI sheetsAPI, String spreadsheetId)
    {
        try
        {
            List<List<String>> rowsAs2DList = sheetsAPI.getRows(spreadsheetId, "reportsProcessed", "A2:Z");

            Map<String, String> idTitleMap = new HashMap<>();
            for(List<String> dbRow : rowsAs2DList)
            {
                idTitleMap.put(dbRow.get(0), dbRow.get(1));
            }
            return idTitleMap;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private static List getListFromSpreadsheet(SheetsAPI sheetsAPI, String spreadsheetId, String subsheetName, Class reference)
    {
        try
        {
            List<List<String>> rowsAs2DList = sheetsAPI.getRows(spreadsheetId, subsheetName, "A2:Z");

            List entries = new ArrayList<>();
            for(List<String> dbRow : rowsAs2DList)
            {
                Constructor listConstructor = reference.getDeclaredConstructor(List.class);
                entries.add(listConstructor.newInstance(dbRow));
            }
            return entries;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ArrayList();
        }
    }

    private static List<AttendanceEntry> getAllAttendanceEntries(SheetsAPI sheetsAPI, String spreadsheetId)
    {
        try
        {
            List<List<String>> attendanceList = sheetsAPI.getRows(spreadsheetId,
                    "attendanceEntry", "A2:Z");
            List<AttendanceEntry> attendanceEntries = new ArrayList<>();
            for(List<String> attendanceRow : attendanceList)
            {
                attendanceEntries.add(new AttendanceEntry(attendanceRow));
            }
            return attendanceEntries;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static Set<String> extractReportIDsFromReportAggregates(List<ReportAggregate> reportAggregatesAlreadyProcessed)
    {
        Set<String> reportIDs = new HashSet<>();

        for(ReportAggregate ra : reportAggregatesAlreadyProcessed)
        {
            reportIDs.add(ra.reportId);
        }

        return reportIDs;
    }

    private static List<List<String>> extractReportIDsAndTimestamp(List<String> relevantReportIDs,
                                                                   JSONArray reportsInTimeframe,
                                                                   LocalDateTime dateTimeNow)
    {
        List<List<String>> return2DList = new ArrayList<>();
        String dateTimeString = dateTimeNow.toString();

        for(int i = 0; i < reportsInTimeframe.length(); i++)
        {
            JSONObject report = reportsInTimeframe.getJSONObject(i);
            String id = report.getString("id");
            if(relevantReportIDs.contains(id))
            {
                List<String> entry = new ArrayList<>(Arrays.asList(id, report.getString("title"), dateTimeString));
                return2DList.add(entry);
            }
        }

        return return2DList;
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

    private static List<List<String>> convertObjectListTo2dList(List list, Class classType) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        List<List<String>> obj2DList = new ArrayList<>(list.size());
        Method toListMethod = classType.getDeclaredMethod("toList");
        for(int i = 0; i < list.size(); i++)
        {
            List<String> attendanceEntryAsList = (List<String>) toListMethod.invoke(list.get(i));
            obj2DList.add(attendanceEntryAsList);
        }

        return obj2DList;
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

    private static List<String> getReportsProcessed(SheetsAPI sheetsAPI, String spreadsheetId) throws GeneralSecurityException, IOException
    {
        //Get the attendanceRecords that exist in the sheet.
        List<List<String>> reportsProcessed = sheetsAPI.getRows(spreadsheetId,
                "reportsProcessed", "A2:Z");

        List<String> justReportIDs = new ArrayList<>();
        if(reportsProcessed != null)
        {
            for(List<String> reportEntry : reportsProcessed)
            {
                justReportIDs.add(reportEntry.get(0));
            }
        }

        return justReportIDs;
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

    private static JSONArray getLatestReportsWeHaventProcessed(Long lookbackPeriod, Long epochTimeNow, Guild myGuild, WarcraftLogsAPI wlogsApi, List<String> reportsProcessed)
    {
        //Search wlogs till the earliest one, between the timeframes of then and now
        JSONArray reportsInTimeframe = wlogsApi.getReportsByGuild(myGuild, epochTimeNow, lookbackPeriod);

        for(int i=0; i < reportsInTimeframe.length(); i++)
        {
            JSONObject jsonObj = reportsInTimeframe.getJSONObject(i);
            if(reportsProcessed.contains(jsonObj.getString("id")))
            {
                reportsInTimeframe.remove(i);
                i--;
            }
        }

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

    public static Properties loadProperties(String propertiesFilename)
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

    private static Long getEpochMillisecondTimeWeeksOnDayAgo(int weeksAgo, DayOfWeek day, LocalDateTime inputDateTime)
    {
        LocalDateTime outputTime = getLocalDateTimeWeeksOnDayAgo(weeksAgo, day, inputDateTime);
        return outputTime.toEpochSecond(ZoneOffset.UTC) * 1000; //Need in milliseconds.;
    }

    private static LocalDateTime getLocalDateTimeWeeksOnDayAgo(int weeksAgo, DayOfWeek day, LocalDateTime inputDateTime)
    {
        int daysToSubtract = inputDateTime.getDayOfWeek().getValue() - day.getValue();
        LocalDateTime outputTime = inputDateTime.minusDays(daysToSubtract);
        outputTime = outputTime.minusWeeks(weeksAgo);
        return outputTime;
    }

    private static LocalDate getDateFromString(String input)
    {
        LocalDate date = LocalDate.parse(input);
        return date;
    }
}