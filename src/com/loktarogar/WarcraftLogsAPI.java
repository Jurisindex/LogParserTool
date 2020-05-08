package com.loktarogar;

import com.loktarogar.helpers.HttpResponse;
import com.loktarogar.helpers.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class WarcraftLogsAPI
{
	private RestClient restClient;
	private String apiKey = "";
	private String baseAPIEndpoint = "https://classic.warcraftlogs.com/v1";
	private String reportsByGuildRoute = "/reports/guild/";
	private String reportsByIdRoute = "/report/fights/";

	public WarcraftLogsAPI(String apiKey)
	{
		this.apiKey = apiKey;
		this.restClient = new RestClient();
	}

	public void setApiKey(String key)
	{
		this.apiKey = key;
	}

	public JSONArray getReportsByGuild(Guild guild, Long epochTimeEnd, Long epochTimeStart)
	{
		String queryParamsString = "?start="+epochTimeStart+"&end="+epochTimeEnd+"&api_key="+apiKey;
		String qualifiedGuildName = guild.guildName.replaceAll(" ", "%20");
		String fullGuildParam = qualifiedGuildName + "/" + guild.serverName + "/" + guild.region;

		String fullUrl = baseAPIEndpoint + reportsByGuildRoute + fullGuildParam;
		HttpResponse response = restClient.get(fullUrl, queryParamsString, "Jurisnoctis");
		if(response.is2xx())
		{
			JSONArray array = new JSONArray(response.getResponseText());
			return array;
		}
		else
		{
			return null;
		}
	}

	public JSONObject getReportById(String reportId)
	{
		String fullUrl = baseAPIEndpoint + reportsByIdRoute + reportId;
		String queryParamsString = "?api_key="+apiKey;
		HttpResponse response = restClient.get(fullUrl, queryParamsString, "Jurisnoctis");
		if(response.is2xx())
		{
			JSONObject object = new JSONObject(response.getResponseText());
			return object;
		}
		else
		{
			return null;
		}
	}
	//
//	function getUploadMetadataJSONFromWarcraftLogs(query, endpoint, route, apiKey)
//	{
//		var call = endpoint + route + query + "&api_key=" + apiKey;
//		var response = UrlFetchApp.fetch(call, {'muteHttpExceptions': true});
//		var jsonText = response.getContentText();
//		var jsonData = JSON.parse(jsonText);
//
//		return getLast2WeeksOfReportIds(jsonData);
//	}
}
