package my;

import static my.ApplicationLauncher.run;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationException;
import my.exceptions.ApplicationRuntimeException;
import my.jdbc.DAOFactory;
import my.jdbc.GenericDAO;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

@Slf4j
public abstract class ConsoleApplication extends ProgramUnit implements UncaughtExceptionHandler {

	private DAOFactory daoFactory;

	/**
	 * 
	 * @param appClazz
	 * @param args
	 * 
	 */
	protected static <T extends ConsoleApplication> void init(Class<T> appClazz, String[] args) {
		System.exit(run(appClazz, args, true).getValue());
	}

	@Override
	public final void uncaughtException(Thread thread, Throwable t) {
		ApplicationLauncher.uncaughtException(thread, t);
	}

	protected final void initDaoFactory() {
		log.info("Init DAO factory...");
		DataSource ds = getDataSource();

		if (ds == null) {
			throw new ApplicationRuntimeException("No datasource ready. Did #initDatabase was called before calling #initDaoFactory?");
		}

		daoFactory = new DAOFactory(ds);
		log.info("DAO Factory init SUCCESSFUL.");
	}

	protected void initParametersWith(String... args) {
		initParametersWith(Collections.emptyMap(), args);
	}

	protected Command initParametersWith(Map<String, Command> commands, String[] args) {
		JCommander commander = new JCommander(this);
		commander.setProgramName(getName());

		for (Map.Entry<String, Command> e : commands.entrySet()) {
			commander.addCommand(e.getKey(), e.getValue());
		}

		try {
			commander.parse(args);

			Command parsedCommand = EmptyCommand.INSTANCE;
			if (!commands.isEmpty()) {
				parsedCommand = commands.get(commander.getParsedCommand());

				if (parsedCommand == null) {
					throw new ParameterException("Unable to determine command from\n" + args);
				}
			}

			return parsedCommand;
		} catch (ParameterException pe) {
			StringBuilder out = new StringBuilder();
			out.append("\n");
			commander.usage(out);
			log.info(out.toString());
			throw pe;
		}
	}

	public DAOFactory getDaoFactory() {
		if (daoFactory == null) {
			throw new ApplicationRuntimeException("No datafactory ready. Did #initDaoFactory was called before calling #getDaoFactory?");
		}

		return daoFactory;
	}

	/* no modifier here intentionaly */final void freeDaoFactory() {
		if (daoFactory != null) {
			daoFactory.freeAllOpenedDAOs();
		}
	}

	protected <T extends GenericDAO> T getDAO(Class<T> clazz) {
		return daoFactory.getDAO(clazz);
	}

	protected void closeSilentlyDAO(GenericDAO dao) {
		daoFactory.closeSilently(dao);
	}

	protected final String getFileContent(Path p, Charset c) throws IOException {
		return new String(Files.readAllBytes(p), c);
	}

	/**
	 * 
	 * Loads a resource from classpath like below:<br>
	 * {@code Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)}
	 *
	 * 
	 * The resource is implicitly considered as UTF-8 encoded.
	 * 
	 * @param resourceName
	 * @return
	 * @throws IOException
	 */
	protected final String getResourceAsString(String resourceName) throws IOException {
		return Utils.read(resourceName);
	}

	protected abstract ExitCode go(String[] args) throws ApplicationException;

	protected abstract void shutdown() throws ApplicationException;

	/**
	 * 
	 * Indicate if the given argument need to be hidden when displayed in a log.
	 * 
	 * This is useful when this ConsoleApplication may receive passwords or any
	 * sensitive data via command line.
	 * 
	 * The default implementation is very restrictive as it assumes ALL
	 * arguments must be protected.
	 * 
	 * @param argument
	 * @return
	 */
	protected boolean needProtection(String argument) {
		return true;
	}

	protected static void enableHeadlessMode() {
		System.setProperty("java.awt.headless", "true");
	}

	public interface Command {
		void checkParameters();
	}

	private enum EmptyCommand implements Command {
		INSTANCE;

		@Override
		public void checkParameters() {
			// Nothing to check...
		}
	}
}
