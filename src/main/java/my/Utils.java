package my;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import my.exceptions.ApplicationRuntimeException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    /**
     * 
     * Make current thread entering a infinite loop. The thread NEVER leave this
     * method once entering it.
     * 
     * This method is useful for visual application that need to stay alive after
     * showing their main frame. <br>
     * <br>
     * Sample call:
     * 
     * <pre>
     * <code>
     * &#064;Override
     * protected ExitCode go(String[] args) throws ApplicationException {
     *    prepareGUI();
     * 
     *    runForEver();
     * 
     *    return ExitCode.SUCCESS;
     * }
     * </code>
     * </pre>
     * 
     */
    public static void runForEver() {
        boolean canGo = true;
        while (canGo) {
            try {
                TimeUnit.MINUTES.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApplicationRuntimeException(e);
            }
        }
    }

    public static String read(String resourceName) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new FileNotFoundException("Not found: " + resourceName);
            }

            return IOUtils.toString(stream, StandardCharsets.UTF_8.name());
        }
    }

    public static String readSilently(String resourceName) {
        try {
            return Utils.read(resourceName);
        } catch (IOException e) {
            throw new ApplicationRuntimeException(e);
        }
    }
}
