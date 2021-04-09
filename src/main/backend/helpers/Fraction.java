package helpers;

public class Fraction//Jackson
{
	private String representation;
	private Integer numerator;
	private Integer denominator;

	public Fraction(String raw)
	{
		representation = raw;
		String[] numbers = raw.split("/");
		numerator = Integer.parseInt(numbers[0]);
		denominator = Integer.parseInt(numbers[1]);
	}

	public Fraction(Integer numerator, Integer denominator)
	{
		this.numerator = numerator;
		this.denominator = denominator;
		updateRepresentation();
	}

	private void updateRepresentation()
	{
		this.representation = numerator + "/" + denominator;
	}

	@Override
	public String toString()
	{
		return representation;
	}

	public Integer getNumerator()
	{
		return numerator;
	}

	public void setNumerator(Integer numerator)
	{
		this.numerator = numerator;
		updateRepresentation();
	}

	public void incrementNumerator()
	{
		this.numerator = numerator+1;
		updateRepresentation();
	}

	public Integer getDenominator()
	{
		return denominator;
	}

	public void setDenominator(Integer denominator)
	{
		this.denominator = denominator;
		updateRepresentation();
	}

	public void incrementDenominator()
	{
		this.denominator = denominator+1;
		updateRepresentation();
	}
}
