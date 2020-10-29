import helpers.Logger;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportAggregate
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
		this(dbRow.get(0), dbRow.get(1), dbRow.get(2), dbRow.get(3), dbRow.get(4), dbRow.get(5));
	}

	public ReportAggregate(String reportId, String reportTitle, String playersAttended,
						   String playersWithInsufficientWBuffs, String date, String dayOfWeek)
	{
		this.reportId = reportId;
		this.reportTitle = reportTitle;
		this.date = date;
		this.dayOfWeek = DayOfWeek.valueOf(dayOfWeek);
		playersAttended = playersAttended.substring(1, playersAttended.length()-1);
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
