package com.loktarogar.helpers;

public class HttpResponse
{
	private String responseText;
	private Integer responseCode;
	private Boolean is2xx;

	HttpResponse(String responseText, Integer responseCode)
	{
		this.responseCode = responseCode;
		this.responseText = responseText;
		if(responseCode/100 == 2)
		{
			is2xx = true;
		}
		else
		{
			is2xx = false;
		}
	}

	public String getResponseText()
	{
		return responseText;
	}

	public Integer getResponseCode()
	{
		return responseCode;
	}

	public Boolean is2xx()
	{
		return is2xx;
	}
}
