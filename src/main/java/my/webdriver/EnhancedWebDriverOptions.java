package my.webdriver;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Accessors(fluent = false)
@Getter
@Slf4j
public class EnhancedWebDriverOptions {
	// See:-https://chromium.googlesource.com/chromium/src/+/7e762c1f17514a29834506860961ba2f24e7e6e5/components/content_settings/core/common/content_settings.h
	public static final int CONTENT_SETTING_ALLOW = 1;
	public static final int CONTENT_SETTING_BLOCK = 2;

	// Waits
	private static final long DEFAULT_INTERVAL_MILLIS = 500;
	private static final long DEFAULT_TIMEOUT_SEC = 120; // 2 minutes

	private String chromeDriverVersion = "2.35";

	private String userDataDir = null;

	private boolean headless = false;

	@Accessors(fluent = true)
	private boolean disableImageLoading = false;

	// Maximum duration in seconds reserved for any ExpectedCondition
	private long waitTimeoutInSeconds = DEFAULT_TIMEOUT_SEC;

	// Interval in milliseconds between each check of the ExpectedCondition
	private long intervalDurationBetweenTwoWaits = DEFAULT_INTERVAL_MILLIS;

	private String userAgent = null;

	private ExtensionFilter extensionFilter = AcceptAllExtensionFilter.INSTANCE;

	private boolean alertHandling = true;

	public static EnhancedWebDriverOptionsBuilder prepareWith() {
		return new EnhancedWebDriverOptionsBuilder();
	}

	public static class EnhancedWebDriverOptionsBuilder {
		Map<String, Object> definedProperties = new HashMap<>();

		private EnhancedWebDriverOptionsBuilder() {

		}

		public EnhancedWebDriverOptionsBuilder chromeDriverVersion(String chromeDriverVersion) {
			definedProperties.put("chromeDriverVersion", chromeDriverVersion);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder userDataDir(String userDataDir) {
			definedProperties.put("userDataDir", userDataDir);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder headless(boolean headless) {
			definedProperties.put("headless", headless);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder disableImageLoading(boolean disableImageLoading) {
			definedProperties.put("disableImageLoading", disableImageLoading);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder waitTimeoutInSeconds(long waitTimeoutInSeconds) {
			definedProperties.put("waitTimeoutInSeconds", waitTimeoutInSeconds);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder intervalDurationBetweenTwoWaits(long intervalDurationBetweenTwoWaits) {
			definedProperties.put("intervalDurationBetweenTwoWaits", intervalDurationBetweenTwoWaits);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder userAgent(String userAgent) {
			definedProperties.put("userAgent", userAgent);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder extensionFilter(ExtensionFilter extensionFilter) {
			definedProperties.put("extensionFilter", extensionFilter);
			return this;
		}

		public EnhancedWebDriverOptionsBuilder alertHandling(boolean alertHandling) {
			definedProperties.put("alertHandling", alertHandling);
			return this;
		}

		public EnhancedWebDriverOptions andBuild() {
			EnhancedWebDriverOptions enhancedWebDriverOptions = new EnhancedWebDriverOptions();

			for (Map.Entry<String, Object> e : definedProperties.entrySet()) {
				try {
					Field declaredField = EnhancedWebDriverOptions.class.getDeclaredField(e.getKey());
					boolean accessible = declaredField.isAccessible();

					declaredField.setAccessible(true);
					declaredField.set(enhancedWebDriverOptions, e.getValue());

					declaredField.setAccessible(accessible);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException t) {
					log.warn("", t);
				}
			}

			return enhancedWebDriverOptions;
		}
	}

	// TODO: Allow passing custom command line switches
	// TODO: Allow passing custom preferences
	// TODO: Allow passing custom capabilities
}
