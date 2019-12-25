package my.webdriver;

import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Paths.get;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;
import my.web.WebClient;
import my.web.WebClientConfiguration;
import my.windows.Reg;

import org.apache.commons.exec.OS;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;
import org.hjson.Stringify;
import org.jsoup.Connection.Response;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

@Slf4j
public enum EnhancedWebDriverFactory {
	;

	private static final String ORIGINAL_KEY = "HKCU\\Software\\Google\\Chrome\\TriggeredReset";
	private static final String ORIGINAL_KEY_BACKUP = ORIGINAL_KEY + "Backup";
	private static final String DEFAULT_LATEST_CHROME_DRIVER_VERSION = "2.33";

	private static String latestChromeDriverVersionLastModified = null;
	private static Lock latestChromeDriverVersionLastModifiedLock = new ReentrantLock();
	private static String latestChromeDriverVersion = DEFAULT_LATEST_CHROME_DRIVER_VERSION;

	// Empty string mean that there are no extensions to load.
	private static List<String> extensionsLocation = new ArrayList<>();
	static {
		deployExtensionsIfNecessary();
	}

	/**
	 * 
	 * Create a new headless Chrome driver instance.
	 * 
	 * @param chromeDriverVersion
	 * @param userDataDir
	 * @return
	 * 
	 * @see #newChromeDriver(String, String, boolean)
	 * @see #newChromeDriver(EnhancedWebDriverOptions)
	 */
	public static EnhancedWebDriver newChromeDriver(String chromeDriverVersion, String userDataDir) {
		return newChromeDriver(chromeDriverVersion, userDataDir, true);
	}

	/**
	 * 
	 * Start a new Chrome Driver.
	 * 
	 * @param chromeDriverVersion
	 *            can be null. If null the latest chrome driver version will be
	 *            downloaded and used.
	 * @param userDataDir
	 *            Absolute path of directory containing user profile.
	 * @param headless
	 *            TRUE => don't show any window , FALSE=> show browser window
	 * 
	 * @return a ChromeDriver instance wrapped in an EnhancedWebDriver instance.
	 */
	// TODO: Handle (SOCKS) proxy settings
	// See: http://stackoverflow.com/a/28891213/363573
	// See:_http://www.chromium.org/developers/design-documents/network-stack/socks-proxy
	// @see https://sites.google.com/a/chromium.org/chromedriver/getting-started
	public static EnhancedWebDriver newChromeDriver(String chromeDriverVersion, String userDataDir, boolean headless) {
		EnhancedWebDriverOptions options = EnhancedWebDriverOptions.prepareWith() //
				.chromeDriverVersion(chromeDriverVersion) //
				.userDataDir(userDataDir) //
				.headless(headless) //
				.andBuild();

		return newChromeDriver(options);
	}

	public static EnhancedWebDriver newChromeDriver(EnhancedWebDriverOptions enhancedWebDriverOptions) {
		log.info("Starting Chrome...");

		log.info(" # Preparing webdriver options");
		DesiredCapabilities capabilities = new DesiredCapabilities();

		// Add other webdriver options here...

		log.info(" # Preparing Chrome specific options");
		ChromeOptions options = new ChromeOptions();
		String userDataDir = enhancedWebDriverOptions.getUserDataDir();
		if (userDataDir != null) {
			log.info(" * With user data dir: {}", userDataDir);
			options.addArguments("--user-data-dir=" + userDataDir);

			disableSessionRestoreWarning(userDataDir);

			// suspendTriggeredResetApiIfNeeded();
		}

		options.setHeadless(enhancedWebDriverOptions.isHeadless());

		String userAgent = enhancedWebDriverOptions.getUserAgent();
		if (userAgent != null) {
			options.addArguments("--user-agent=" + userAgent);
		}

		Map<String, Object> prefs = new HashMap<>();
		HashMap<String, Object> images = new HashMap<>();
		if (enhancedWebDriverOptions.disableImageLoading()) {
			images.put("images", EnhancedWebDriverOptions.CONTENT_SETTING_BLOCK);
			options.addArguments("--blink-settings=imagesEnabled=false");
		} else {
			images.put("images", EnhancedWebDriverOptions.CONTENT_SETTING_ALLOW);
			options.addArguments("--blink-settings=imagesEnabled=true");
		}
		prefs.put("profile.default_content_setting_values", images);

		prefs.put("session.restore_on_startup", 5);

		// Sort arguments by theme and in alphabetic order inside a given theme
		// ## Arguments references :
		// https://superuser.com/a/1009683/104863
		// https://peter.sh/experiments/chromium-command-line-switches
		// https://forum.clb.heroes-online.com/threads/1645-Solution-to-fix-lag-on-Google-Chrome?s=49c6c2faf25a4ed54b3b55119485dabc&p=30249&viewfull=1#post30249
		// ## Misc. switches
		options.addArguments("--disable-infobars");
		options.addArguments("--disable-print-preview");
		options.addArguments("--disable-session-crashed-bubble");
		options.addArguments("--disable-sync");
		options.addArguments("--fast");
		options.addArguments("--fast-start");
		options.addArguments("--load-extension=" + getExtensionsLocation(enhancedWebDriverOptions.getExtensionFilter()));
		options.addArguments("--no-default-browser-check");
		options.addArguments("--no-first-run");
		options.addArguments("--start-maximized");

		// ## Cache switches
		options.addArguments("--aggressive-cache-discard");
		options.addArguments("--disk-cache-size=268435456"); // 256 MB
		options.addArguments("--media-cache-size=134217728"); // 128 MB

		// FIXME: Should we use the two --v8-* cache related switches below ?
		// --v8-cache-options
		// --v8-cache-strategies-for-cache-storage

		log.info(" # Instanciating Chrome driver");
		options //
		.merge(capabilities) //
				.setExperimentalOption("prefs", prefs) //
		;

		ChromeDriverService service = new ChromeDriverService.Builder() //
				.usingDriverExecutable(getChromeDriverLocation(enhancedWebDriverOptions)) //
				.usingAnyFreePort() //
				.build();

		EnhancedWebDriver driver = new EnhancedWebDriver( //
				new ChromeDriver(service, options), //
				enhancedWebDriverOptions //
		);

		// restoreTriggeredResetApiIfNeeded();

		return driver;
	}

	private static String getExtensionsLocation(ExtensionFilter extensionFilter) {
		StringJoiner joiner = new StringJoiner(",");

		for (String extensionLocation : extensionsLocation) {
			if (extensionFilter.accept(extensionLocation)) {
				joiner.add(extensionLocation);
			} else {
				log.info("Extension rejected: {}", extensionLocation);
			}
		}

		return joiner.toString();
	}

	private static void deployExtensionsIfNecessary() {
		try {
			final String extensions = "extensions";
			URL extensionsUrl = EnhancedWebDriverFactory.class.getResource(extensions);
			if (extensionsUrl != null) {
				File temp = Files.createTempDirectory("ewdf-extensions-").toFile();
				temp.deleteOnExit();
				String canonicalTempPath = temp.getCanonicalPath();

				if (!my.FileUtils.copyResourcesRecursively(extensionsUrl, temp)) {
					throw new ApplicationRuntimeException("Unable to deploy extensions.");
				}

				File[] extensionsDirectories = Paths //
						.get(canonicalTempPath) //
						.resolve(extensions) //
						.toFile() //
						.listFiles(File::isDirectory) //
				;

				extensionsLocation = new ArrayList<>();
				if (extensionsDirectories.length != 0) {
					log.info("{} extension(s) successfully deployed to {}", extensionsDirectories.length, canonicalTempPath);
					for (File extensionsDirectory : extensionsDirectories) {
						log.info(" - {}", extensionsDirectory.getName());
						extensionsLocation.add(extensionsDirectory.getCanonicalPath());
					}
				} else {
					log.info("No extensions found to deploy in {}.", extensionsUrl.toString());
				}
			} else {
				log.info("No extensions found as resources.");
			}
		} catch (IOException e) {
			throw new ApplicationRuntimeException(e);
		}
	}

	/**
	 *
	 * Restore TriggeredApi (if any) set in the registry key.
	 *
	 * The algorithm followed by this method can be resumed like below: <br>
	 * Original Key Exits; Backup Key Exists => action(s)
	 *
	 * <ul>
	 * <li>false;false => No action performed
	 * <li>false;true => No action performed
	 * <li>true;false => No action performed.
	 * <li>true;true => Delete backup key. Goto next action.
	 * </ul>
	 *
	 * @see suspendTriggeredResetApiIfNeeded
	 */
	private static void restoreTriggeredResetApiIfNeeded() {
		if (isWindows() && Reg.exists(ORIGINAL_KEY_BACKUP)) {
			if (!Reg.exists(ORIGINAL_KEY)) {
				Reg.copy(ORIGINAL_KEY_BACKUP, ORIGINAL_KEY);
			}

			Reg.delete(ORIGINAL_KEY_BACKUP);
		}
	}

	/**
	 * 
	 * On Windows specifically, Chrome exposes an API (via registry key)
	 * enabling a third party tool to be notified when a change in user data is
	 * detected. The following code disables (again via registry key) this API
	 * if found.
	 *
	 * The algorithm followed by this method can be resumed like below: <br>
	 * Original Key Exits; Backup Key Exists => action(s)
	 *
	 * <ul>
	 * <li>false;false => No action performed
	 * <li>false;true => No action performed
	 * <li>true;true=> Delete backup key. Goto next action.
	 * <li>true;false=> Create backup key from original key and Delete original
	 * key. No more actions performed.
	 * </ul>
	 * 
	 * @see {@link restoreTriggeredResetApiIfNeeded},
	 *      https://www.chromium.org/developers/triggered-reset-api
	 *
	 */
	private static void suspendTriggeredResetApiIfNeeded() {
		if (isWindows() && Reg.exists(ORIGINAL_KEY)) {
			Reg.copy(ORIGINAL_KEY, ORIGINAL_KEY_BACKUP, true);
			Reg.delete(ORIGINAL_KEY);
		}

		// reg query "HKCU\Software\Google\Chrome\TriggeredReset" /v
		// Timestamp
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	/**
	 * 
	 * With Chrome webdriver 2.27+ and exiting it with {@link WebDriver#quit()},
	 * on next launch with an existing userDataDir, Chrome prompts a session
	 * restore warning bubble.
	 * 
	 * This method ensures that Chrome won't show this warning by modifying user
	 * preferences located in {@code userDataDir} if it exists.
	 * 
	 * @param userDataDir
	 * @see https://stackoverflow.com/a/38876225/363573
	 */
	private static void disableSessionRestoreWarning(String userDataDir) {
		log.info("* -- Disabling session restore warning");

		Path userDataDirPath = Paths.get(userDataDir);
		// Java 8's "Files.exists" should not be used (squid:S3725)
		if (!userDataDirPath.toFile().exists()) {
			return;
		}

		Path userDefaultPreferencesPath = userDataDirPath.resolve("Default").resolve("Preferences");
		// Java 8's "Files.exists" should not be used (squid:S3725)
		if (!userDefaultPreferencesPath.toFile().exists()) {
			return;
		}

		try {
			JsonObject json = JsonValue.readHjson( //
					new String(Files.readAllBytes(userDefaultPreferencesPath), StandardCharsets.UTF_8) //
					).asObject();

			JsonValue profile = json.get("profile");
			if (profile == null) {
				log.warn("Profile object not found in user preferences.");
				return;
			}

			JsonObject profileObject = profile.asObject();
			profileObject.set("exit_type", "None");
			profileObject.set("exited_cleanly", true);

			Files.write( //
					userDefaultPreferencesPath, //
					json.toString(Stringify.PLAIN).getBytes(StandardCharsets.UTF_8), //
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE //
			);
		} catch (IOException | ParseException e) {
			log.warn("", e);
		}
	}

	private static File getChromeDriverLocation(EnhancedWebDriverOptions enhancedWebDriverOptions) {
		try {
			String chromeDriverVersion = enhancedWebDriverOptions.getChromeDriverVersion();

			ChromeDriverDetails currentChromeDriverDetails;
			if (OS.isFamilyWindows()) {
				currentChromeDriverDetails = ChromeDriverDetails.WINDOWS;
			} else {
				throw new ApplicationRuntimeException("Current running OS isn't supported.");
			}

			WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
			webClientConfiguration.makePauseBetweenFetches(false);

			WebClient webClient = new WebClient(webClientConfiguration);
			String latestChromeDriverVersion = getLatestChromeDriverVersionWith(webClient);

			if ((chromeDriverVersion == null) && latestChromeDriverVersion.isEmpty()) {
				throw new ApplicationRuntimeException(
						"Unable to determine latest chrome driver version and no version (ie null value received) was specified.");
			}

			String effectiveChromeDriverVersion = chromeDriverVersion;
			if ((effectiveChromeDriverVersion == null) && !latestChromeDriverVersion.isEmpty()) {
				effectiveChromeDriverVersion = latestChromeDriverVersion;
			}

			String chromeDriverExecutableName = currentChromeDriverDetails.getExecutableName();
			log.info("Locating {}...", chromeDriverExecutableName);

			Path chromeDriverPath = get("./webdrivers", "chrome", effectiveChromeDriverVersion, chromeDriverExecutableName);
			// Java 8's "Files.exists" should not be used (squid:S3725)
			if (!chromeDriverPath.toFile().exists()) {
				log.info("Downloading {} {}", chromeDriverExecutableName, effectiveChromeDriverVersion);

				String chromeDriverZip = format( //
						"https://chromedriver.storage.googleapis.com/%s/chromedriver_%s.zip", //
						effectiveChromeDriverVersion, //
						currentChromeDriverDetails.getZipSuffix() //
				);

				byte[] chromeDriverZipData = webClient.download(chromeDriverZip);
				if (chromeDriverZipData.length > 0) {
					// TODO: Fetch the XML document at
					// https://chromedriver.storage.googleapis.com/ and assert
					// "official" size of the zip file.
					createDirectories(chromeDriverPath.getParent());
					saveChromeDriver(chromeDriverPath, chromeDriverZipData);
				} else {
					log.warn("Invalid data size(0). Nothing written to disk.");
				}
			}

			File ret = chromeDriverPath.toFile();
			log.info("Found at: {}", ret.getAbsolutePath());

			return ret;
		} catch (IOException ioe) {
			throw new ApplicationRuntimeException(ioe);
		}
	}

	private static String getLatestChromeDriverVersionWith(WebClient webClient) {
		latestChromeDriverVersionLastModifiedLock.lock();
		try {
			Map<String, String> headers = new HashMap<>();
			if (latestChromeDriverVersionLastModified != null) {
				headers.put("If-Modified-Since", latestChromeDriverVersionLastModified);
			}

			Response r = webClient.get("https://chromedriver.storage.googleapis.com/LATEST_RELEASE", headers);
			latestChromeDriverVersionLastModified = r.header("Last-Modified");

			int statusCode = r.statusCode();
			String message;
			switch (statusCode) {
			case WebClient.OK:
				latestChromeDriverVersion = r.body().replaceAll("\\s+", "");
				message = "New Chrome Driver version found: " + latestChromeDriverVersion;
				break;

			case WebClient.NOT_MODIFIED:
				message = "No new Chrome Driver version found! Keeping: " + latestChromeDriverVersion;
				break;

			default:
				latestChromeDriverVersion = DEFAULT_LATEST_CHROME_DRIVER_VERSION;
				message = String.format( //
						"Unexpected HTTP code returned (%d %s). Fallback to default version (%s)", //
						statusCode, //
						r.statusMessage(), //
						latestChromeDriverVersion //
						);
				break;
			}

			log.info(message);
			return latestChromeDriverVersion;
		} finally {
			latestChromeDriverVersionLastModifiedLock.unlock();
		}
	}

	private static void saveChromeDriver(Path chromeDriverPath, byte[] chromeDriverZipAsBytes) {
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(chromeDriverZipAsBytes))) {
			String chromeDriverFilename = chromeDriverPath.getFileName().toString();

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().toLowerCase().endsWith(chromeDriverFilename)) {
					break;
				}
			}

			if (entry == null) {
				throw new ApplicationRuntimeException("Unable do find " + chromeDriverFilename);
			}

			int read = 0;
			final int bufferSize = 4096;
			byte[] buffer = new byte[bufferSize];

			try (OutputStream os = new BufferedOutputStream(new FileOutputStream(chromeDriverPath.toFile()))) {
				while ((read = zis.read(buffer, 0, bufferSize)) >= 0) {
					os.write(buffer, 0, read);
				}
			}
		} catch (IOException e) {
			throw new ApplicationRuntimeException(e);
		}
	}

	@AllArgsConstructor
	@Getter
	private enum ChromeDriverDetails {
		WINDOWS("chromedriver.exe", "win32");

		private String executableName;
		private String zipSuffix;
	}
}
