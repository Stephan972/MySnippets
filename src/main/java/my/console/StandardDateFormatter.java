package my.console;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

// FIXME: Can StandardDateFormatter and DateUtils classes may be merged together ?
public final class StandardDateFormatter {
	public static final DateTimeFormatter instance = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public static String formatDuration(long durationInMs) {
		DateFormat fmt = new SimpleDateFormat("' h 'mm' mn 'ss' s 'SSS' ms'");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		return (durationInMs / 3600000/* hours */) + fmt.format(new Date(durationInMs));
	}
}
