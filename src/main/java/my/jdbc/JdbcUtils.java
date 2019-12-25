package my.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;

import org.apache.commons.io.IOUtils;

@Slf4j
public enum JdbcUtils {
    ;

    /**
     * 
     * Execute UTF-8 inputstream script.
     * 
     * @param conn
     * @param is
     * @param charset
     * @throws IOException
     * @throws SQLException
     */
    public static void executeScript(Connection conn, InputStream is) throws SQLException {
        executeScript(conn, is, StandardCharsets.UTF_8);
    }

    public static void executeScript(Connection conn, InputStream is, Charset charset) throws SQLException {
        try {
            executeScript(conn, IOUtils.toString(is, charset));
        } catch (IOException e) {
            throw new ApplicationRuntimeException(e);
        }
    }

    /**
     * 
     * @param conn
     * @param sqlScript
     *            One or multiple semi-colon separated sql orders .
     * @throws SQLException
     */
    public static void executeScript(Connection conn, String sqlScript) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sqlScript)) {
            ps.executeUpdate();
        }
    }

    public static void closeQuietly(AutoCloseable ... autoCloseables) {
        for (AutoCloseable ac : autoCloseables) {
            if (ac != null) {
                try {
                    ac.close();
                } catch (Exception e) {
                    log.warn("", e);
                }
            }
        }
    }
}
