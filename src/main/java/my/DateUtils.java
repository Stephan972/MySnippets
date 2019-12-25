package my;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 
 * @author stephan
 *
 *         Copied from http://stackoverflow.com/a/27323328/363573
 *
 */
// FIXME: Can DateUtils and StandardDateFormatter classes may be merged together
// ?
public class DateUtils {

	public static Date asDate(LocalDate localDate) {
		return asDate(localDate, Clock.systemDefaultZone());
	}

	public static Date asDate(LocalDate localDate, Clock clock) {
		return Date.from(localDate.atStartOfDay().atZone(clock.getZone()).toInstant());
	}

	public static Date asDate(LocalDateTime localDateTime) {
		return asDate(localDateTime, Clock.systemDefaultZone());
	}

	public static Date asDate(LocalDateTime localDateTime, Clock clock) {
		return Date.from(localDateTime.atZone(clock.getZone()).toInstant());
	}

	public static LocalDate asLocalDate(Date date) {
		return asLocalDate(date, Clock.systemDefaultZone());
	}

	public static LocalDate asLocalDate(Date date, Clock clock) {
		return Instant.ofEpochMilli(date.getTime()).atZone(clock.getZone()).toLocalDate();
	}

	public static LocalDateTime asLocalDateTime(Date date) {
		return asLocalDateTime(date, Clock.systemDefaultZone());
	}

	public static LocalDateTime asLocalDateTime(Date date, Clock clock) {
		return Instant.ofEpochMilli(date.getTime()).atZone(clock.getZone()).toLocalDateTime();
	}

	private static class PathTimeFormatterHolder {
		private static DateTimeFormatter PATH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HHmmss");
	}

	/**
	 * 
	 * Returns a DateTimeFormatter that formats a date in the form:
	 * yyyy/MM/dd/HHmmss
	 * 
	 * @return
	 */
	public static DateTimeFormatter getPathTimeFormatter() {
		return PathTimeFormatterHolder.PATH_TIME_FORMATTER;
	}
}
