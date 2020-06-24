import java.time.LocalDateTime;
import java.util.Set;

public class AttendanceEntry
{
	Player player;
	LocalDateTime date;
	String mainName;
	Set<WorldBuffs> worldBuffs;
	Boolean acceptableWbuffs;

	AttendanceEntry(Player player, LocalDateTime date, String mainName, Set<WorldBuffs> worldBuffs)
	{
		this.player = player;
		this.date = date;
		this.mainName = mainName;
		this.worldBuffs = worldBuffs;
		acceptableWbuffs = calculateWbuffsAcceptable();
	}

	private boolean calculateWbuffsAcceptable()
	{
		return false;
	}
}
