package my.web.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.DateUtils;
import my.exceptions.ApplicationRuntimeException;

/**
 * 
 * 
 * @author stephan
 *
 * 
 */
@Slf4j
public enum WebUtils {
    ;

    // FIXME: Add more RANDOM and ACTUAL user agent strings
    public static String getUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.101 Safari/537.36";
        // "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)
        // Chrome/49.0.2623.112 Safari/537.36"
        // return
        // "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko)
        // Chrome/42.0.2311.135 Safari/537.36";
    }

    private enum FilenameSpecialCharacterRemoverHolder {
        ;

        private static Matcher instance = Pattern.compile("(?i)[^a-z0-9._-]").matcher("");
    }

    public static void dumpDocument(Document d) {
        dumpDocument(d, DumpExtension.DEFAULT);
    }

    public static void dumpDocument(Document d, DumpExtension extension) {
        if (d == null) {
            throw new IllegalArgumentException("The passed document MUST NOT be null.");
        }

        try {
            String prefix = DateUtils.getPathTimeFormatter().format(LocalDateTime.now());

            String title = d.title();
            if (title.length() == 0) {
                title = "--No_title_set--";
            } else {
                title = sanitizeTitle(title);
            }

            File f = FileUtils.getFile("dumps/" + prefix + "_" + title + extension.toString());

            log.info("Dumping document {} to {}", d.location(), f.getAbsolutePath());
            FileUtils.write(f, d.outerHtml(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to dump to disk: " + d.location(), e);
        }
    }

    public static Document createEmptyDocument() {
        return Jsoup.parse("<html/>");
    }

    private static String sanitizeTitle(String title) {
        StringBuilder sb = new StringBuilder(FilenameSpecialCharacterRemoverHolder.instance.reset(title).replaceAll("_"));

        // Limit the title length
        final int titleMaxLength = 30;
        final String replacer = "(...)";
        int len = sb.length();
        if (len > titleMaxLength) {
            sb.delete(titleMaxLength - replacer.length(), len);
            sb.append(replacer);
        }

        return sb.toString();
    }

    public static void checkElement(String name, Element elem) {
        if (elem == null) {
            throw new ApplicationRuntimeException("Unable to find [" + name + "]");
        }
    }

    public static String encodeQueryStringParameter(String parameter) {
        try {
            return URLEncoder.encode(parameter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationRuntimeException(e);
        }
    }

    public static String format(String url) {
        Objects.requireNonNull(url);

        String tmp = url.trim();

        if (!tmp.toLowerCase().startsWith("http")) {
            tmp = "http://" + tmp;
        }

        return tmp;
    }

    @AllArgsConstructor
    public enum DumpExtension {
        DEFAULT(".dump"), HTML(".html");

        private String extension;

        @Override
        public String toString() {
            return extension;
        }
    }
}
