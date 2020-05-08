package com.loktarogar.helpers;

import org.json.JSONObject;

public class JSONResponse
{
	private int httpCode;
	private JSONObject messageText;

	public JSONResponse(int code, JSONObject message)
	{
		messageText = message;
		httpCode = code;
	}

	public int getHttpCode()
	{
		return httpCode;
	}

	public void setHttpCode(int httpCode)
	{
		this.httpCode = httpCode;
	}

	public JSONObject getMessageText()
	{
		return messageText;
	}

	public void setMessage(JSONObject messageText)
	{
		this.messageText = messageText;
	}
}
