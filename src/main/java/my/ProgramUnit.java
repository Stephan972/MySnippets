package my;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.flywaydb.core.Flyway;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;
import my.jdbc.DriverDataSource;

@Slf4j
public class ProgramUnit {
	private static final String APPLICATION_PROPERTIES_FILENAME = "/application.properties";
	private static final String DEFAULT_PROGRAM_UNIT_DATABASE_JDBC_URL = "jdbc:sqlite:data.db";

	private Properties applicationInfo = new Properties();

	@Setter
	private DataSource dataSource;

	/**
	 * 
	 * Create on the fly a H2 memory database.
	 * 
	 * H2 has the advantage of being able to load CSV files through the jdbc
	 * url. SQLite-jdbc is unable to do it for now...
	 * 
	 * NOTA: You can use either the database OR the temporayDatabase. Not both.
	 * (TODO: remove this constraint??)
	 * 
	 * @param initSqls
	 *            The (optional) sql statements to execute to initialize a
	 *            connection immediately after obtaining it.
	 * 
	 * @see DriverDataSource
	 */
	protected final void initTemporaryDatabase(String... initSqls) {
		setDataSource( //
		new DriverDataSource( //
				"jdbc:h2:mem:", //
				"sa", //
				"", //
				initSqls //
		));
	}

	/**
	 * 
	 * @param initSqls
	 *            The (optional) sql statements to execute for initializing a
	 *            connection immediately after obtaining it.
	 *
	 * @throws SQLException
	 * @throws IOException1
	 *             On the first initSql file that cannot be read
	 *
	 */
	protected final void initDatabase(String... initSqls) {
		log.info("Performing database init...");
		Flyway flyway = new Flyway();

		String[] sql = completeInitSqls(initSqls);

		if (sql.length > 0) {
			log.info("Init sql:\n{}", String.join("\n", sql));
		}
		flyway.setDataSource(getJdbcUrl(), getUser(), getPassword(), sql);

		flyway.migrate();

		DataSource ds = flyway.getDataSource();
		setDataSource(ds);
	}

	protected String getPassword() {
		return "";
	}

	protected String getUser() {
		return "";
	}

	private String[] completeInitSqls(String[] initSqls) {
		String[] tmp = new String[0];

		if (getJdbcUrl().toLowerCase().startsWith("jdbc:sqlite:")) {
			tmp = new String[] { "pragma encoding=\"UTF-8\";", "pragma busy_timeout=30000;" };
		}

		return ArrayUtils.addAll(tmp, initSqls);
	}

	protected String getJdbcUrl() {
		return DEFAULT_PROGRAM_UNIT_DATABASE_JDBC_URL;
	}

	public DataSource getDataSource() {
		if (dataSource == null) {
			throw new ApplicationRuntimeException(
					"No dataSource ready. Did #initTemporaryDatabase or #initDatabase was called before calling #getDataSource?");
		}

		return dataSource;
	}

	/**
	 * 
	 * @throws ApplicationRuntimeException
	 *             If application properties file can't be loaded.
	 * 
	 */
	protected final void initApplicationProperties() {
		initApplicationProperties(this.getClass());
	}

	public void initApplicationProperties(Class<?> clazz) {
		InputStream in = null;

		try {
			log.info( //
					"[{}] Loading application properties ({})...", //
					clazz.getSimpleName(), //
					ProgramUnit.class.getResource(APPLICATION_PROPERTIES_FILENAME) //
			);
			applicationInfo.clear();

			in = ProgramUnit.class.getResourceAsStream(APPLICATION_PROPERTIES_FILENAME);
			if (in == null) {
				log.warn("No file '" + APPLICATION_PROPERTIES_FILENAME + "' found on classpath.");
			} else {
				applicationInfo.load(in);
			}
		} catch (IOException e) {
			throw new ApplicationRuntimeException(e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	protected final String getName() {
		return getProperty("name", this.getClass().getCanonicalName());
	}

	protected final String getVersion() {
		return getProperty("version", "0.0.0");
	}

	/**
	 * 
	 * Return the value of the property with name {@code name} from the
	 * application.properties file. If property is not found,
	 * {@code defaultValue} is returned.
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 * @see #getProperty(String)
	 */
	public final String getProperty(String name, String defaultValue) {
		return applicationInfo.getProperty(name, defaultValue);
	}

	/**
	 * 
	 * Return the value of the property with name {@code name} from the
	 * application.properties file.
	 * 
	 * @param name
	 * @return The property value if found or null otherwise.
	 * @see ProgramUnit#getProperty(String, String)
	 */
	public final String getProperty(String name) {
		return applicationInfo.getProperty(name);
	}
}
