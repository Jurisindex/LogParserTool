import helpers.DatabaseEntity;
import helpers.Fraction;
import helpers.Logger;

import java.util.ArrayList;
import java.util.List;

public class PlayerAggregate extends DatabaseEntity
{
	private static Logger logger = Logger.getInstance();

	Player player;
	Integer totalAttendance;
	Integer tuesdayAttendance;
	Integer wednesdayAttendance;
	Fraction worldBuffsOnTuesdays;

	//DAO wrapper
	public PlayerAggregate(List<String> dbRow)
	{
		this.player = new Player(dbRow.get(0), WoWClass.getClass(dbRow.get(1)));
		this.totalAttendance = Integer.parseInt(dbRow.get(2));
		this.tuesdayAttendance = Integer.parseInt(dbRow.get(3));
		this.wednesdayAttendance = Integer.parseInt(dbRow.get(4));
		this.worldBuffsOnTuesdays = new Fraction(dbRow.get(5));
	}

	public PlayerAggregate(Player player, Integer totalAttendance, Integer tuesdayAttendance,
						   Integer wednesdayAttendance, String worldBuffsOnTuesdays)
	{
		this(player, totalAttendance, tuesdayAttendance, wednesdayAttendance, new Fraction(worldBuffsOnTuesdays));
	}

	public PlayerAggregate(Player player, Integer totalAttendance, Integer tuesdayAttendance,
						   Integer wednesdayAttendance, Fraction worldBuffsOnTuesdays)
	{
		this.player = player;
		this.totalAttendance = totalAttendance;
		this.tuesdayAttendance = tuesdayAttendance;
		this.wednesdayAttendance = wednesdayAttendance;
		this.worldBuffsOnTuesdays = worldBuffsOnTuesdays;
	}

	@Override
	public List<String> toList()
	{
		List<String> listRepresentation = new ArrayList<>(6);
		listRepresentation.add(player.name);
		listRepresentation.add(player.wowClass.className);
		listRepresentation.add(tuesdayAttendance.toString());
		listRepresentation.add(wednesdayAttendance.toString());
		listRepresentation.add(worldBuffsOnTuesdays.toString());

		return listRepresentation;
	}
}
