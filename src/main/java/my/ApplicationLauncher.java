package my;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;

@Slf4j
// TODO: Add support of argumentsMap with same arguments appearing multiple
// times in run method.
public enum ApplicationLauncher {
	;

	/**
	 * 
	 * @param appClazz
	 * @param argsMap
	 * @return
	 */
	public static final <T extends ConsoleApplication> ExitCode run(Class<T> appClazz, Map<ArgumentName, String[]> argsMap) {
		List<String> argsList = new ArrayList<>();

		for (Map.Entry<ArgumentName, String[]> e : argsMap.entrySet()) {
			ArgumentName currentArgument = e.getKey();
			String[] currentArgumentValues = e.getValue();

			if (currentArgument.hasCommandLineSwitch()) {
				argsList.add(currentArgument.getCommandLineSwitch());
			}

			int len = currentArgumentValues.length;
			for (int i = 0; i < len; i++) {
				argsList.add(currentArgumentValues[i]);
			}
		}

		return run(appClazz, argsList.toArray(new String[argsList.size()]));
	}

	/**
	 * 
	 * Run a ConsoleApplication and return its {@code ExitCode}.
	 * 
	 * You use it like below:
	 * 
	 * <pre>
	 * ApplicationLauncher.run(MyApplication.class, &quot;--foo&quot;, &quot;bar&quot;, &quot;--baz&quot;);
	 * </pre>
	 * 
	 * This method is useful when you want to test a ConsoleApplication or embed
	 * it in another application.
	 * 
	 * NOTA: Auto exception handling is disabled when launching the passed
	 * ConsoleApplication. This method assumes that the calling code will handle
	 * any uncaught exception.
	 * 
	 * @param appClazz
	 * @param args
	 * @return
	 * 
	 * @see #run(Class, String, boolean)
	 */
	public static final <T extends ConsoleApplication> ExitCode run(Class<T> appClazz, String... args) {
		return run(appClazz, args, false);
	}

	public static final <T extends ConsoleApplication> ExitCode run(Class<T> appClazz, String[] args, boolean handleUncaughtExceptions) {
		T app = null;
		ExitCode status = ExitCode.SUCCESS;

		try {
			// Create application
			app = appClazz.newInstance();

			if (handleUncaughtExceptions) {
				Thread.setDefaultUncaughtExceptionHandler(app);
			}

			// Run application
			log.info("\nStarting application: {}\nWith: {}", appClazz.getCanonicalName(), protect(app, args));
			status = app.go(args);

			if (status == null) {
				log.error("Invalid null status code returned.");
				status = ExitCode.ERROR;
			}
		} catch (Throwable t) {
			status = ExitCode.ERROR;

			if (handleUncaughtExceptions) {
				uncaughtException(Thread.currentThread(), t);
			} else {
				throw new ApplicationRuntimeException(t);
			}
		} finally {
			if (app != null) {
				try {
					app.shutdown();
				} catch (Throwable t) {
					log.warn("Exception handled during application shutdown.\n", t);
				} finally {
					app.freeDaoFactory();
				}
			}
		}

		return status;
	}

	private static <T extends ConsoleApplication> String protect(T app, String[] args) {
		int len = args.length;
		StringBuilder protectedArguments = new StringBuilder();
		protectedArguments.append("IMPLEMENT arguments protection to display them !");
		// for (int i = 0; i < len; i += 2) {
		// String arg = args[i];
		//
		// protectedArguments.append(arg).append(" ");
		//
		// if (app.needProtection(arg)) {
		// protectedArguments.append("*****");
		// } else {
		// protectedArguments.append(args[i + 1]);
		// }
		//
		// protectedArguments.append(" ");
		// }

		return protectedArguments.toString().trim();
	}

	public static final void uncaughtException(Thread thread, Throwable t) {
		String handledBy = "";

		if (!Thread.currentThread().equals(thread)) {
			handledBy = " (handled by [" + thread.getName() + "])";
		}

		log.error("Unhandled error{}:", handledBy, t);
	}
}
