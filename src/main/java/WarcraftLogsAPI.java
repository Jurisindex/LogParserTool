import helpers.HttpResponse;
import helpers.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class WarcraftLogsAPI
{
	private RestClient restClient;
	private String apiKey = "";
	private String baseAPIEndpoint = "https://classic.warcraftlogs.com/v1";
	private String reportsByGuildRoute = "/reports/guild/";
	private String reportsByIdRoute = "/report/fights/";
	private String eventsInReportId = "/report/events/";
	private HashMap<Integer, String> zoneMappings;

	public WarcraftLogsAPI(String apiKey)
	{
		this.apiKey = apiKey;
		this.restClient = new RestClient();
		this.zoneMappings = new HashMap<>();
		setUpZoneMappings();
	}

	private void setUpZoneMappings()
	{
		//Maybe the right way to do it is ping them each time for the updated list, but ehhh.
		zoneMappings.put(1000,"MC");
		zoneMappings.put(1001,"Ony");
		zoneMappings.put(1002,"BWL");
		zoneMappings.put(1003,"ZG");
		zoneMappings.put(1004,"AQ20");
		zoneMappings.put(1005,"AQ40");
		zoneMappings.put(1500,"Placeholder");
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

	public JSONObject getEventTypesByReportId(String reportId, Long startTime, Long endTime, String filterType)
	{
		String fullUrl = baseAPIEndpoint + eventsInReportId + reportId;
		String queryParamsString = "?start="+startTime+"&end="+endTime+"&filter=type=\""+filterType+"\"" + "&api_key="+apiKey;
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

	public String getZoneMapping(Integer zoneId)
	{
		return zoneMappings.get(zoneId);
	}
}
