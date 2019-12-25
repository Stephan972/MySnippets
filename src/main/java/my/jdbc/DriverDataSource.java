package my.jdbc;

import java.util.Properties;

import org.flywaydb.core.api.FlywayException;

public class DriverDataSource extends org.flywaydb.core.internal.util.jdbc.DriverDataSource {

	public DriverDataSource(String url, String user, String password, String... initSqls) throws FlywayException {
		super(Thread.currentThread().getContextClassLoader(), null, url, user, password, new Properties(), initSqls);
	}
}
