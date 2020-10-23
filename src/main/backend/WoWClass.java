import java.util.HashMap;
import java.util.Map;

public enum WoWClass
{
	Warrior("Warrior"),
	Druid("Druid"),
	Rogue("Rogue"),
	Hunter("Hunter"),
	Mage("Mage"),
	Warlock("Warlock"),
	Priest("Priest"),
	Shaman("Shaman"),
	Paladin("Paladin");

	String className;
	private static final Map<String, WoWClass> BY_STRING = new HashMap<>();

	static
	{
		for (WoWClass c: values())
		{
			BY_STRING.put(c.className, c);
		}
	}

	WoWClass(String className)
	{
		this.className = className;
	}

	public static WoWClass getClass(String name)
	{
		return BY_STRING.get(name);
	}
}
