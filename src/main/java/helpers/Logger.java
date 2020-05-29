package helpers;

public class Logger
{
	public static Logger getInstance(LogLevel logLevelInput)
	{
		logLevel = logLevelInput;
		return LoggerSingletonHelper.INSTANCE;
	}

	private static class LoggerSingletonHelper
	{
		private static final Logger INSTANCE = new Logger(logLevel);
	}

	public enum LogLevel
	{
		DEBUG(0),
		INFO(1),
		WARNING(2),
		ERROR(3),
		NONE(4);

		private Integer severity;

		LogLevel(int severity)
		{
			this.severity = severity;
		}

		public boolean isAtLeast(LogLevel level)
		{
			return this.severity <= level.severity;
		}
	}

	private static LogLevel logLevel;

	private Logger(LogLevel logLevel)
	{
		this.logLevel = logLevel;
	}

	private void log(String s, LogLevel logLevel)
	{

		if(this.logLevel.isAtLeast(logLevel))
		{
			System.out.println(s);
		}
	}

	public void debug(String s) { log(s, LogLevel.DEBUG); }

	public void info(String s)
	{
		log(s, LogLevel.INFO);
	}

	public void warn(String s)
	{
		log(s, LogLevel.WARNING);
	}

	public void error(String s)
	{
		log(s, LogLevel.ERROR);
	}
}
