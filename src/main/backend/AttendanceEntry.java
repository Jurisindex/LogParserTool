import helpers.Logger;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttendanceEntry
{
	private static Logger logger = Logger.getInstance();

	Player player;
	Integer idThisRaid;
	String reportId;
	String date;
	DayOfWeek dayOfWeek;
	String zone;
	Set<WorldBuffs> worldBuffs;
	Boolean acceptableWbuffs;

	public AttendanceEntry(Player player, Integer idThisRaid, String reportId, LocalDateTime date, String zone, Set<WorldBuffs> worldBuffs)
	{
		this.player = player;
		this.idThisRaid = idThisRaid;
		this.reportId = reportId;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		this.date = date.format(formatter);
		this.dayOfWeek = date.getDayOfWeek();
		this.worldBuffs = worldBuffs;
		this.zone = zone;
		acceptableWbuffs = calculateWbuffsAcceptable();
	}

	public AttendanceEntry(List<String> databaseRow)
	{
		this(databaseRow.get(0),databaseRow.get(1),databaseRow.get(2),databaseRow.get(3),
			 databaseRow.get(4),databaseRow.get(5),databaseRow.get(6), databaseRow.get(7),
		     databaseRow.get(8));
	}

	//This acts as an AttendanceEntryDAO. This is hilarious, but... :)
	public AttendanceEntry(String playerName, String playerClass, String idThisRaid, String reportId, String date,
						   String dayOfWeek, String zone, String worldBuffs, String wbuffsCheck)
	{
		Player player = new Player(playerName, WoWClass.getClass(playerClass));
		this.player = player;
		this.idThisRaid = Integer.parseInt(idThisRaid);
		this.reportId = reportId;
		this.date = date;
		this.dayOfWeek = DayOfWeek.valueOf(dayOfWeek);
		this.zone = zone;
		//Get rid of brackets, check for error, split.
		worldBuffs = worldBuffs.substring(1, worldBuffs.length()-1);
		if(worldBuffs.contains("ERROR"))
		{
			this.worldBuffs = null;
		}
		else
		{
			this.worldBuffs = new HashSet<>();
			String[] wbuffsSplit = worldBuffs.split(",");
			for(String buff : wbuffsSplit)
			{
				WorldBuffs thisBuff = WorldBuffs.getBuffByShorthand(buff);
				this.worldBuffs.add(thisBuff);
			}
		}
		this.acceptableWbuffs = Boolean.valueOf(wbuffsCheck);
	}

	private boolean calculateWbuffsAcceptable()
	{
		if(worldBuffs == null)
		{
			return false;
		}
		/*
		RCD + DM Buffs is what's originally required as a minimum.

		If you don't get 1 of the above, you must have at least 3.
		 */
		//If you have 3 or more world buffs, you're good.
		if(worldBuffs.size() >= 3)
		{
			return true;
		}

		if(worldBuffs.contains(WorldBuffs.RALLYING_CRY_OF_THE_DRAGONSLAYER))
		{
			if(worldBuffs.contains(WorldBuffs.DIRE_MAUL_TRIBUTE))
			{
				//DMT + RCOTD are the minimum set of effort.
				return true;
			}
		}

		return false;
	}

	public List<String> toList()
	{
		List<String> listRepresentation = new ArrayList<>(9);
		listRepresentation.add(player.name);
		listRepresentation.add(String.valueOf(player.wowClass));
		listRepresentation.add(String.valueOf(idThisRaid));
		listRepresentation.add(reportId);
		listRepresentation.add(date);
		listRepresentation.add(dayOfWeek.toString());
		listRepresentation.add(zone);
		listRepresentation.add(worldBuffsToString());
		listRepresentation.add(acceptableWbuffs.toString());

		return listRepresentation;
	}

	private String worldBuffsToString()
	{
		if(this.worldBuffs == null)
		{
//			logger.warn("Player: " + player.name + ". Date: " + date + ". For reportID " + reportId + ".");
//			logger.warn("Wbuffs are null instead of empty.");
			return "[ERROR. Were you in the first boss fight?]";
		}
		String worldBuffs = "[";

		for(WorldBuffs buffs : this.worldBuffs)
		{
			worldBuffs += WorldBuffs.getShorthand(buffs) + ",";
		}

		if(worldBuffs.length() > 1)
		{
			worldBuffs = worldBuffs.substring(0, worldBuffs.length()-1);
		}
		worldBuffs += "]";
		return worldBuffs;
	}
}
