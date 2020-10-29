import java.util.HashMap;
import java.util.Map;

public enum WorldBuffs
{
	DIRE_MAUL_TRIBUTE(22817),	//Need to special case check for 22817, 22818, or 22820
	DARKMOON_FAIRE(23735),		//Need to special case check for 2373(5-8), 2376(6-9)
	SPIRIT_OF_ZANDALAR(24425),
	RALLYING_CRY_OF_THE_DRAGONSLAYER(22888),
	WARCHIEFS_BLESSING(16609),
	SONGFLOWER_SERENADE(15366);

	Integer abilityId;
	private static final Map<Integer, WorldBuffs> BY_ID = new HashMap<>();
	private static final Map<WorldBuffs, String> buffShorthand = new HashMap<>();
	private static final Map<String, WorldBuffs> shorthandToBuff = new HashMap<>();

	WorldBuffs(Integer abilityId)
	{
		this.abilityId = abilityId;
	}

	static
	{
		for (WorldBuffs w: values())
		{
			BY_ID.put(w.abilityId, w);

			//Turns WARCHIEFS_BLESSING to WB, etc etc
			String fullString = w.toString();
			String shorthand = fullString.substring(0,1);

			for(int i = 1; i < fullString.length(); i++)
			{
				if(fullString.charAt(i) == '_')
				{
					shorthand += fullString.charAt(i+1);
				}
			}

			buffShorthand.put(w, shorthand);
			shorthandToBuff.put(shorthand, w);
		}
	}

	public static WorldBuffs whichWorldBuff(Integer id)
	{
		if(id%22817 <=4)	//22819 doesn't exist. It won't be a false positive.
		{
			return DIRE_MAUL_TRIBUTE;
		}

		if(id%23735 <= 4 || id%23766 <= 4)
		{
			return DARKMOON_FAIRE;
		}

		return BY_ID.get(id);
	}

	public static String getShorthand(WorldBuffs worldBuff)
	{
		return buffShorthand.get(worldBuff);
	}
	public static WorldBuffs getBuffByShorthand(String shorthand) { return shorthandToBuff.get(shorthand); }
}
