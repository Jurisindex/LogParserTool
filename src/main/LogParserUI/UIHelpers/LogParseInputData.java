package UIHelpers;

import java.util.List;

public class LogParseInputData
{
	public String guildName;
	public String serverName;
	public String region;
	public String apiKey;
	public Integer weeksLookback;
	public List<String> inclusionText;
	public List<String> splitIndicators;
	public String spreadsheetId;

	public boolean verifyAllDataIsHere()
	{
		if(guildName == null || serverName == null || region == null || apiKey == null)
		{
			return false;
		}

		//Planning to make spreadsheetId optional in the future, and work only in-memory.
		if(spreadsheetId == null)
		{
			return false;
		}

		return true;
	}
}
