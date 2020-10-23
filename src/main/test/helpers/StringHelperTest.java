package helpers;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringHelperTest
{
	private static StringHelper stringHelper = StringHelper.getInstance();

	@Test
	public void getRangeCalculationTest()
	{
		List<List<String>> data = new ArrayList<>();
		int counter = 10;
		String startingCell = "Y41";
		for(int i = 0; i < counter; i++)
		{
			data.add(Arrays.asList("Stuff", "Doesn't Matter", "Just making a 10x3"));
		}
		String range = stringHelper.getRangeCalculation(data, startingCell);
		assert range.equals("Y41:AA50");
	}

	@Test
	public void addTwo26AlphabetStringsTest()
	{
		String first = "AA";
		String second = "A";
		String result = stringHelper.addTwo26AlphabetStrings(first, second);
		assert result.equals("AB");
	}

	@Test
	public void addTwo26AlphabetStringsTestBigValues()
	{
		String first = "DSZZ";
		String second = "ZAZXZ";
		String result = stringHelper.addTwo26AlphabetStrings(first, second);
		assert result.equals("ZFTYZ");
	}

	@Test
	public void addTwo26AlphabetStringsTestSmallerZValues()
	{
		String first = "ZZ";
		String second = "ZX";
		String result = stringHelper.addTwo26AlphabetStrings(first, second);
		assert result.equals("BAX");
	}

	@Test
	public void addTwo26AlphabetStringsTestJustTwoZ()
	{
		String first = "Z";
		String second = "Z";
		String result = stringHelper.addTwo26AlphabetStrings(first, second);
		assert result.equals("AZ");
	}

	@Test
	public void addTwo26AlphabetStringsTestZZPlusZ()
	{
		String first = "ZZ";
		String second = "Z";
		String result = stringHelper.addTwo26AlphabetStrings(first, second);
		assert result.equals("AAZ");
	}

	@Test
	public void addTwo26AlphabetStringsTestWithZOverflows()
	{
		String first = "ZZ";
		String second = "A";
		String result = stringHelper.addTwo26AlphabetStrings(first, second);
		assert result.equals("AAA");
	}

	@Test
	public void convertBase26AlphabetToIntegerTest()
	{
		String alphabetical = "TEST";
		Long result = stringHelper.convertBase26AlphabetToInteger(alphabetical);
		assert result.equals(355414L);
	}

	@Test
	public void convertBase26AlphabetToIntegerTestA()
	{
		String alphabetical = "A";
		Long result = stringHelper.convertBase26AlphabetToInteger(alphabetical);
		assert result.equals(1L);
	}

	@Test
	public void convertBase26AlphabetToIntegerTestZ()
	{
		String alphabetical = "Z";
		Long result = stringHelper.convertBase26AlphabetToInteger(alphabetical);
		assert result.equals(26L);
	}

	@Test
	public void convertBase26AlphabetToIntegerTestInterlacedZAndA()
	{
		String alphabetical = "AZCZA";
		Long result = stringHelper.convertBase26AlphabetToInteger(alphabetical);
		assert result.equals(916657L);
	}

	@Test
	public void convertIntegerToBase26Alphabet()
	{
		Long numeral = 355414L; //17576*20 + 676*5 + 26*19 + 19;
		String result = stringHelper.convertIntegerToBase26Alphabet(numeral);
		assert result.equals("TEST");
	}

	@Test
	public void convertIntegerToBase26AlphabetACase()
	{
		Long numeral = 1L;
		String result = stringHelper.convertIntegerToBase26Alphabet(numeral);
		assert result.equals("A");
	}

	@Test
	public void convertIntegerToBase26AlphabetRepeatingACase()
	{
		Long numeral = 26L;
		String result = stringHelper.convertIntegerToBase26Alphabet(numeral);
		assert result.equals("Z");
	}

	@Test
	public void convertIntegerToBase26AlphabetMixedInACases()
	{
		Long numeral = 676*1 + 26*26 + 1L;
		String result = stringHelper.convertIntegerToBase26Alphabet(numeral);
		assert result.equals("AZA");
	}

	@Test
	public void convertIntegerToBase26AlphabetMixedInZCases()
	{
		Long numeral = 456976*1 + 17576*26 + 676*3 + 26*26 + 1L;
		String result = stringHelper.convertIntegerToBase26Alphabet(numeral);
		assert result.equals("AZCZA");
	}

	@Test
	public void convertIntegerToBase26AlphabetStartBCase()
	{
		Long numeral = 26*2 + 1L;
		String result = stringHelper.convertIntegerToBase26Alphabet(numeral);
		assert result.equals("BA");
	}


	@Test
	public void reverseStringTest()
	{
		String toTest = "SDW";
		String reversed = stringHelper.reverseString(toTest);
		assert reversed.equals("WDS");
	}
}
