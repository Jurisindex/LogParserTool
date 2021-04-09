import helpers.DatabaseEntity;
import helpers.Logger;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportAggregate extends DatabaseEntity
{
	private static Logger logger = Logger.getInstance();

	String reportId;
	String reportTitle;
	List<String> playersAttended;
	List<String> playersWithInsufficientWBuffs;
	String date;
	DayOfWeek dayOfWeek;

	//DAO wrapper
	public ReportAggregate(List<String> dbRow)
	{
		this.reportId = dbRow.get(0);
		this.reportTitle = dbRow.get(1);
		this.date = dbRow.get(4);
		this.dayOfWeek = DayOfWeek.valueOf(dbRow.get(5));
		String playersAttended = dbRow.get(2);
		playersAttended = playersAttended.substring(1, playersAttended.length()-1);
		String playersWithInsufficientWBuffs = dbRow.get(3);
		playersWithInsufficientWBuffs = playersWithInsufficientWBuffs.substring(1, playersWithInsufficientWBuffs.length()-1);
		String[] players = playersAttended.split(",");
		String[] wbufflessPlayers = playersWithInsufficientWBuffs.split(",");
		this.playersAttended = new ArrayList(Arrays.asList(players));
		this.playersWithInsufficientWBuffs = new ArrayList(Arrays.asList(wbufflessPlayers));
	}

	public ReportAggregate(String reportId, String reportTitle, List<String> playersAttended,
						   List<String> playersWithInsufficientWBuffs, String date, DayOfWeek dayOfWeek)
	{
		this.reportId = reportId;
		this.reportTitle = reportTitle;
		this.playersAttended = playersAttended;
		this.playersWithInsufficientWBuffs = playersWithInsufficientWBuffs;
		this.date = date;
		this.dayOfWeek = dayOfWeek;
	}

	@Override
	public List<String> toList()
	{
		List<String> listRepresentation = new ArrayList<>(6);
		listRepresentation.add(reportId);
		listRepresentation.add(reportTitle);
		listRepresentation.add(playersAttended.toString());
		listRepresentation.add(playersWithInsufficientWBuffs.toString());
		listRepresentation.add(date);
		listRepresentation.add(dayOfWeek.toString());

		return listRepresentation;
	}
}
