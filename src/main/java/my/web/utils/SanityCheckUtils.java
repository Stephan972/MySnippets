package my.web.utils;

import org.apache.commons.validator.routines.EmailValidator;


public enum SanityCheckUtils {
    ;

    /**
     * 
     * @param value
     * @return
     * 
     * @see WebUtils#sanitize(String, String)
     */
    public static String sanitize(String value) {
        return sanitize(value, "");
    }

    /**
     * 
     * Return trimmed value. If {@code value} is null, {@code defaultValue} is
     * returned.
     * 
     * @param value
     * @param defaultValue
     * @return
     *
     */
    public static String sanitize(String value, String defaultValue) {
        return value == null ? defaultValue : value.trim();
    }

    /**
     * 
     * Turn a String value into an int value.
     * 
     * @param value
     * @return
     */
    public static int s2i(String value) {
        return Integer.parseInt(value);
    }

    public static int s2i(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * 
     * Turn a String value into a double value.
     * 
     * @param value
     * @return
     */
    public static double s2d(String value) {
        return Double.parseDouble(value);
    }

    /**
     * 
     * Convert a string into a boolean value if and only if:<br>
     * <ul>
     * <li>the string is not null</li>
     * <li>equals '0', '1', 'true' or 'false' (case insensitive)</li>
     * </ul>
     * 
     * @param value
     * @return a boolean value
     * 
     * @throws IllegalArgumentException
     *             If value doesn't equal one of the expected value.
     */
    public static boolean s2b(String value) {
        switch (sanitize(value).toLowerCase()) {
        case "0":
        case "false":
            return false;

        case "1":
        case "true":
            return true;

        default:
            throw new IllegalArgumentException("Unexpected value received: " + value);
        }
    }

    /**
     * 
     * @param value
     * @return
     * 
     * @throws IllegalArgumentException
     *             If value is not a valid email.
     */
    public static String s2email(String value) {
        String email = sanitize(value).toLowerCase();

        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException("Invalid email provided: " + value);
        }

        return email;
    }
}
