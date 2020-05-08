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
        //faq:
        /*
            How Hardcore is Raid 1?
            -We're riding a line between casual and semi-hardcore, leaning a little bit to the semi-hardcore.
            -We'll make you a bench slot if you're significantly underperforming (think under the tank in damage most the time), but we'll work with you to improve the whole way.
            -We like to parse for fun, but we're not here to expect everyone to get 99's on every fight. We try and do our best during Parse Week (and every week), but we will not replace a long-time core raider parsing an 80s with an application parsing 90s for example.

            Parse Week?
            -Till Phase 5 (and again after we have C'thun on farm), we have every Darkmoon Faire week be "Parse Week". We all get buffing and consuming harder than usual, and we log out between bosses for big cooldowns (not Recklessness).
            -We usually do BWL + MC Tuesday. On Parse week we do BWL Tuesday and MC Wednesday, getting world buffs again for the 2nd day.

            I don't like something that I'm seeing, how do I give feedback on that?
            -Whether it's an atmosphere you don't like, or mechanics you think we're doing sub-optimally, we'd love to hear it.
            -Open communication has been one of our big asks of raiders, and it's a very important one. At the end of the day, we want everyone to be happy raiding and playing with us.
            -We don't want you to bottle up your frustration out of fear, complacency, or not feeling you'll be listened to. You absolutely will. Some of our raiders have given us a list of feedback at once, and we loved it. Others let it bottle up and already decided they're done before they even give a single point. We're all here to improve every week, in any aspects that are needed.

            So I'm a Social that wants to become a raider. How do I go about doing that?
            -Applying to the raid, trialing for a raid, and going from there. In-guild applications and transfers absolutely welcome. If you feel you're not competent enough, worry not, we'll work with you to help you improve as much as possible.

            Your Loot Council isn't corrupt, is it?
            -Jurisnoctis is a GM Rogue that got the 3rd CTS, not the 1st 8/8 T2 rush, and still doesn't have a Striker's Mark and probably won't get the next one.
            -Nargarzhvog is an officer who did not get the first crossbow.
            -We have a healer loot distribution list that tries to get everyone 1 big ticket item allocated to them in priority order (drops not being crap notwithstanding), to try to equally distribute healer gear.
            -Same thing for magic casters (currently working on nice looking sheet, but the data/information is there).
            -We try to pre-decide some big items before hand. This saves time and comes as a better decision than what we would have had rushing and excited after its dropping. Sometimes the decisions get stale (think we decide 5 weeks ago, but that person has been absent twice, or his performance has severely tanked), and we'll need to update.
            -We want to have everyone feeling satisfied that we make reasonable decisions. Sometimes no healer gear drops for 3 weeks and it's extremely disheartening. Rest assured that we try to have NO character geared to the teeth in true BiS unless we're swimming in drops.

            How can I move up this said list from 4th in line for <Big Item> to say, 2nd or even 1st?
            -Bring it up. Tell us your intent. Tell us what item you want, and then we'll talk about steps to get there.
            -Every case will be different, but it'll generally involve more visible effort.

            I see more than 40 people signed up. How do you bench people?
            -First off, if you don't sign up, I assume you're not coming. Even if you've been here the past 8 weeks on time. Your spot WILL be replaced if you don't sign up, as we'll have the expectation that it's empty.
            -Second, what we might need for the night. If we're down healers we're going to bench DPS. If it's a Parse Week, we might bench newer recruits/players to ensure it's smooth.
            -Otherwise, we do our best to communicate benchings Sunday night. That we would have 40+ signed up every time we send the ping, we'd post them immediately.
            -Ultimately we try to tell people ASAP. If you got benched, double check that your gear is enchanted, you're using consumables, and you've come with world buffs in the past consistently.
            -If you still are, try to understand that recruitment is the biggest boss of Classic WoW, and keeping 40 people and no more can leave you weeks where you go into BWL with 35.

         */

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