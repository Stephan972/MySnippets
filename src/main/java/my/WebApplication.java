package my;

import java.io.File;

import javax.servlet.ServletException;

import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.startup.Tomcat;

@Slf4j
public class WebApplication extends ConsoleApplication {

	private static final int DEFAULT_HTTP_PORT = 8080;

	private Tomcat tomcat;

	@Override
	protected final ExitCode go(String[] args) throws ApplicationException {
		try {
			tomcat = new Tomcat();
			tomcat.setPort(getPort());
			tomcat.getConnector();
			tomcat.addWebapp(getContextPath(), new File("src/main/webapp").getAbsolutePath());
			tomcat.setSilent(true);
			tomcat.start();

			Server server = tomcat.getServer();
			log.info("Server accessible at http://{}:{}{}", server.getAddress(), getPort(), getContextPath());

			server.await();

			return ExitCode.SUCCESS;
		} catch (LifecycleException e) {
			throw new ApplicationException(e);
		}
	}

	protected String getContextPath() {
		return "";
	}

	@Override
	protected final void shutdown() throws ApplicationException {
		try {
			if (tomcat != null) {
				tomcat.stop();
			}
		} catch (LifecycleException e) {
			throw new ApplicationException(e);
		}
	}

	protected int getPort() {
		// FIXME: Find a way to select a free port for listening automatically
		return DEFAULT_HTTP_PORT;
	}
}
