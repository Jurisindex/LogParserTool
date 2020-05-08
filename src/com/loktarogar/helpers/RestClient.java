package com.loktarogar.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestClient
{
	private static Logger logger = Logger.getInstance(Logger.LogLevel.WARNING);

	public HttpResponse get(String url, String urlParams, String userAgent)
	{
		HttpURLConnection httpURLConnection;
		try
		{
			//Combine the URL and params, properly format, and open a connection.
			String fullPath = url + urlParams;
			//String formatedPath = URLEncoder.encode(fullPathPreFormat, "UTF-8");
			httpURLConnection = getHTTPConnection(fullPath, userAgent);
		}
		catch (IOException e)
		{
			logger.error("URL Encoding failed:");
			logger.error(e.getLocalizedMessage());
			return null;
		}

		return processRequest(httpURLConnection);
	}

	private HttpResponse processRequest(HttpURLConnection httpURLConnection)
	{
		StringBuffer response = new StringBuffer();
		HttpResponse httpResponse;
		int responseCode;

		try
		{
			responseCode = httpURLConnection.getResponseCode();
			logger.debug(httpURLConnection.getHeaderFields().toString());

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(httpURLConnection.getInputStream()));
			String output;

			while ((output = reader.readLine()) != null) {
				response.append(output);
				response.append('\n');
			}
			reader.close();
		}
		catch (IOException e)
		{
			response = new StringBuffer();
			response.append("{\"status\": \"SOCKET_EXCEPTION\"" + "}");
			responseCode = 503;
		}

		logger.debug(response.toString());
		String responseString = response.toString();
		return new HttpResponse(responseString, responseCode);
	}

	private HttpURLConnection getHTTPConnection(String formatedPath, String userAgent) throws IOException
	{
		URL obj = new URL(formatedPath);
		HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();

		httpURLConnection.setRequestProperty("Content-Type", "application/json");
		httpURLConnection.setRequestProperty("Accept", "application/json");
		httpURLConnection.setRequestProperty("user-agent", userAgent);
		httpURLConnection.setConnectTimeout(5000);
		httpURLConnection.setReadTimeout(10000);
		httpURLConnection.setUseCaches(true);
		httpURLConnection.setRequestMethod("GET");

		return httpURLConnection;
	}
}
