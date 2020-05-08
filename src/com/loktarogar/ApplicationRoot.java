package com.loktarogar;

import com.loktarogar.helpers.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class ApplicationRoot
{
    private static Logger logger = Logger.getInstance(Logger.LogLevel.DEBUG);

    public static void main(String[] args)
    {

	    //Get the current DateTime and day.
        LocalDateTime dateTimeNow = LocalDateTime.now();
        Long epochTimeNow = dateTimeNow.toEpochSecond(ZoneOffset.UTC) * 1000;   //Need in milliseconds.
        DayOfWeek dayOfWeekNow = dateTimeNow.getDayOfWeek();

        //Set the lookbackPeriod to 8 weeks from this week's Monday.
        //We raid on Tues/Wed, if we run this report on Monday, or Pre-raid Tuesday,
        //The Agreement is that we only get 7 weeks data.
        LocalDateTime lookbackPeriod = getLocalDateTimeWeeksOnDayAgo(8, DayOfWeek.MONDAY, dateTimeNow);
        Long epochTimeLookbackPeriod = lookbackPeriod.toEpochSecond(ZoneOffset.UTC) * 1000; //Need in milliseconds.

        //Get properties, create guild, and set up the WarcraftLogsAPI to properly send and retrieve calls.
        Properties prop = loadProperties("src/resources/application.properties");
        Guild myGuild = new Guild(prop.getProperty("guildName"), prop.getProperty("serverName"), prop.getProperty("region"));
        WarcraftLogsAPI wlogsApi = new WarcraftLogsAPI(prop.getProperty("apiKey"));

        //Get all reports within a timeframe
        JSONArray reportsInTimeframe = wlogsApi.getReportsByGuild(myGuild, epochTimeNow, epochTimeLookbackPeriod);
        List<String> relevantReportIDs = new ArrayList<>();

        //Get all reports that are Raid1 Relevant.
        for(int i=0; i < reportsInTimeframe.length(); i++)
        {
            JSONObject jsonObj  = reportsInTimeframe.getJSONObject(i);
            String title = jsonObj.getString("title");
            if(title.contains("Raid1") || title.contains("R1") || title.contains("Raid 1"))
            {
                relevantReportIDs.add(jsonObj.getString("id"));
            }
        }
        Integer totalRaids = relevantReportIDs.size();

        //For each report that's Raid1 relevant, get the list of raiders in that report.
        List<JSONArray> raidAttendences = new ArrayList<>();
        for(String s : relevantReportIDs)
        {
            JSONObject output = wlogsApi.getReportById(s);
            JSONArray raiders = output.getJSONArray("exportedCharacters");
            raidAttendences.add(raiders);
        }

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

        System.out.println("hello");
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


/*
    function fillOutAttendanceMainFunction()
    {
      var routeReport = '/report/fights/';
      var attendanceListCount = getAttendanceForRaids(endpoint, routeReport, apiKey, relevantRaidIds);
      var attendanceAsPercentage = new Map();

      for (const [key, value] of Object.entries(attendanceListCount))
      {
        var percentage = value/relevantRaidIds.length * 100;
        percentage = Math.floor(percentage * 100) / 100;
        attendanceAsPercentage[key] = (percentage);
      }

      fillInExcelForms(attendanceAsPercentage, relevantRaidIds);
    }

    function fillInExcelForms()
    {
      //Intentionally left blank
    }

    function getAttendanceForRaids(endpoint, routeReport, apiKey, relevantRaidIds)
    {
      var attendanceListCount = new Map();
      for(var i = 0; i < relevantRaidIds.length; i++)
      {
        var fullCall = endpoint + routeReport + relevantRaidIds[i] + '?api_key=' + apiKey;
        var raidData = getRaidData(fullCall);

        var raidersPresent = new Array();
        var characters = raidData['exportedCharacters'];
        for(var j = 0; j < characters.length; j++)
        {
          raidersPresent.push(characters[j]['name']);
        }

        for(var j = 0; j < raidersPresent.length; j++)
        {
          //check if the raider is in the list
          var raider = raidersPresent[j];
          if(attendanceListCount[raider] !== undefined)
          {
    //        var attendanceCount = attendanceListCount[raider];
    //        attendanceCount++;
    //        attendanceListCount[raider] = attendanceCount;
            attendanceListCount[raider]++;
          }
          else
          {
            attendanceListCount[raider] = 1;
          }
        }
      }
      return attendanceListCount;
    }

    function getRaidData(fullCall)
    {
      var response = UrlFetchApp.fetch(fullCall, {'muteHttpExceptions': true});
      var jsonText = response.getContentText();
      var jsonData = JSON.parse(jsonText);

      return jsonData;
    }

    function getLast2WeeksOfReportIds(jsonData)
    {
      var relevantRaidIds = new Array();  //Should be 16 raids in the list.

      for (var i = 0; i < jsonData.length; i++)
      {
        var obj = jsonData[i];
        var title = String(obj['title']);
        var shouldInclude = title.includes('Raid1');
        if(shouldInclude)
        {
          relevantRaidIds.push(obj['id']);
        }
      }

      return relevantRaidIds;
    }

    function getUploadMetadataJSONFromWarcraftLogs(query, endpoint, route, apiKey)
    {
      var call = endpoint + route + query + "&api_key=" + apiKey;
      var response = UrlFetchApp.fetch(call, {'muteHttpExceptions': true});
      var jsonText = response.getContentText();
      var jsonData = JSON.parse(jsonText);

      return getLast2WeeksOfReportIds(jsonData);
    }
 */