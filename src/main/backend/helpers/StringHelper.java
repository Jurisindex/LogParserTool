package helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringHelper
{
	private static Map<String,Integer> letterToIndexMap = new HashMap<>();
	private static Map<Integer,String> indexToLetterMap = new HashMap<>();

	public static StringHelper getInstance()
	{
		return StringHelper.StringHelperSingletonHelper.INSTANCE;
	}

	private static class StringHelperSingletonHelper
	{
		private static final StringHelper INSTANCE = new StringHelper();
	}

	private StringHelper()
	{
		Character letter = 'A';
		for(int i = 1; i <= 26; i++)
		{
			letterToIndexMap.put(String.valueOf(letter), i);
			indexToLetterMap.put(i, String.valueOf(letter));
			letter++;
		}
		indexToLetterMap.put(0,"Z");
	}

	public String getRangeCalculation(List<List<String>> data, String startingCell)
	{
		String[] arr = startingCell.split("\\d+", 2);
		String columnLetters = arr[0].trim();
		String rowNumbers = startingCell.substring(columnLetters.length()).trim();
		Integer numberInts = Integer.parseInt(rowNumbers);
		Long columnAdditions = data.get(0).size()-1+0L;
		Integer rowAdditions = data.size()-1;
		String endColumn = columnLetters;
		if(columnAdditions > 0)
		{
			String columnWidthAsString = convertIntegerToBase26Alphabet(columnAdditions);
			endColumn = addTwo26AlphabetStrings(columnWidthAsString, columnLetters);
		}
		Integer endRow = numberInts+rowAdditions;

		String result = startingCell + ":" + endColumn + String.valueOf(endRow);
		return result;
	}

	public String addTwo26AlphabetStrings(String columnWidthAsString, String letters)
	{
		Long columnsInWidth = convertBase26AlphabetToInteger(columnWidthAsString);
		Long currentColumns = convertBase26AlphabetToInteger(letters);
		Long newColumnRange = columnsInWidth + currentColumns;
		return convertIntegerToBase26Alphabet(newColumnRange);
	}

	public Long convertBase26AlphabetToInteger(String letters)
	{
		//Where A => 0
		Integer biggestPower = letters.length();
		Long result = 0L;
		while(biggestPower > 1)
		{
			String charAtFirst = letters.substring(0,1);
			letters = letters.substring(1);
			Integer value = letterToIndexMap.get(charAtFirst);
			Integer basePower = (int) Math.pow(26, biggestPower-1);
			value = value * basePower;
			result += value;
			biggestPower--;
		}
		result += letterToIndexMap.get(letters);

		return result;
	}

	public String convertIntegerToBase26Alphabet(Long input)
	{
		if(input <= 0)
		{
			return "A";
		}
		String result = "";

		while(input >= 26)
		{
			Long remainder = input%26;
			Long quotient = input/26;
			String letterToAdd = indexToLetterMap.get(remainder.intValue());
			result += letterToAdd;
			if(remainder == 0)
			{
				quotient--;
			}

			input = quotient;
		}

		if(input == 0L)
		{
			return reverseString(result);
		}
		else
		{
			Long index = input%26;
			result += indexToLetterMap.get(index.intValue());
		}

		return reverseString(result);
	}

	public String reverseString(String s)
	{
		String reversedString = "";
		//reverse the string
		for (int i = s.length()-1; i >= 0; i--)
		{
			char c = s.charAt(i);
			//Process char
			reversedString += c;
		}
		return reversedString;
	}
}
