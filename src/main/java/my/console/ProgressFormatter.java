package my.console;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProgressFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public static String format(long current, long total) {
        return String.format("%d/%d (%.2f %%)", current, total, (total <= 0) ? 0 : 100 * ((double) current / total));
    }

    /**
     * 
     * This method is a shortcut for the method
     * {@link ProgressFormatter#formatEstimatedRemainingTime(long, long, long, long)}
     * .
     * 
     * The end time is implicit and is calculated as the current time in
     * milliseconds returned by {@link System#currentTimeMillis}.
     * 
     * @param start
     * @param current
     * @param total
     * @return
     * 
     * @see #formatEstimatedRemainingTime(long, long, long, long)
     */
    public static String formatEstimatedRemainingTime(long start, long current, long total) {
        return formatEstimatedRemainingTime(start, System.currentTimeMillis(), current, total);
    }

    /**
     * 
     * @param start
     *            Start time in milliseconds
     * @param end
     *            End time in milliseconds
     * @param current
     *            Actions performed count
     * @param total
     *            Actions to perform count
     * @return A formatted estimated remaining time.
     */
    public static String formatEstimatedRemainingTime(long start, long end, long current, long total) {
        String formatEstimatedRemainingTime = "Infinity";

        if (current != 0) {
            long estimatedRemainingTimeInMs = calculateEstimatedRemainingTime(start, end, current, total);
            formatEstimatedRemainingTime = formatEstimatedRemainingTime(estimatedRemainingTimeInMs);
        }

        return formatEstimatedRemainingTime;
    }

    public static long calculateEstimatedRemainingTime(long start, long current, long total) {
        return calculateEstimatedRemainingTime(start, System.currentTimeMillis(), current, total);
    }

    /**
     * 
     * @param start
     * @param end
     * @param current
     * @param total
     * @return
     * @throws IllegalArgumentException
     *             If current is zero
     *
     */
    public static long calculateEstimatedRemainingTime(long start, long end, long current, long total) {
        if (current == 0) {
            throw new IllegalArgumentException("Unable to calculate estimated remaining time if current is zero.");
        }

        long elapsedTimeInMs = end - start;
        return Math.abs((elapsedTimeInMs * (total - current)) / current);
    }

    public static String formatEstimatedRemainingTime(long estimatedRemainingTime) {
        return StandardDateFormatter.formatDuration(estimatedRemainingTime);
    }

    /**
     * 
     * @param estimatedRemainingTime
     *            An instant of time from 1970-01-01 at midnight UTC
     * @return An ETA with system default zone id.
     */
    public static String formatEstimatedTimeOfArrival(long estimatedRemainingTime) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(estimatedRemainingTime), ZoneId.systemDefault());

        return DATE_TIME_FORMATTER.format(localDateTime);
    }
}
