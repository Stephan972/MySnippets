package my.console;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtils {

    /**
     * 
     * Set the first character of the String {@code s} uppercase and all remaining
     * characters lowercase.
     *
     * @param s
     *            string to capitalize
     * @return the capitalized string
     */
    public static String capitalize(String s) {
        Objects.requireNonNull(s);

        if (s.length() == 0) {
            return s;
        }

        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
