package my.webdriver;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import my.exceptions.ApplicationRuntimeException;
import my.web.WebClient;
import my.web.WebClientConfigurationFactory;

import org.apache.commons.io.IOUtils;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

// FIXME: The method handleAnyAlert may need to be called in other methods in this class...
@Slf4j
public class EnhancedWebDriver implements WebDriver, JavascriptExecutor, TakesScreenshot {
	private static final Pattern JS_CODE_WHITE_CHAR_REMOVER_PATTERN = Pattern.compile("([\\r\\n])");

	private RemoteWebDriver remoteWebDriver;

	private Matcher jsCodeWhiteCharRemover = JS_CODE_WHITE_CHAR_REMOVER_PATTERN.matcher("");

	private boolean alertsAlreadyAutoAccepted = false;

	private WebDriverWait wait = null;

	private WebClient webClient;

	private NetworkSniffer currentNetworkSniffer;

	private EnhancedWebDriverOptions options;

	public EnhancedWebDriver(RemoteWebDriver remoteWebDriver) {
		this(remoteWebDriver, EnhancedWebDriverOptions.prepareWith().andBuild());
	}

	/**
	 * 
	 * @param remoteWebDriver
	 *            The actual webdriver to enhance.
	 * 
	 * @param options
	 */
	public EnhancedWebDriver(RemoteWebDriver remoteWebDriver, EnhancedWebDriverOptions options) {
		if (remoteWebDriver == null) {
			throw new IllegalArgumentException("Remote webdriver cannot be null.");
		}
		this.remoteWebDriver = remoteWebDriver;

		if (options == null) {
			throw new IllegalArgumentException("Options cannot be null.");
		}
		this.options = options;

		wait = new WebDriverWait( //
				remoteWebDriver, //
				options.getWaitTimeoutInSeconds(), //
				options.getIntervalDurationBetweenTwoWaits() //
		);

		webClient = new WebClient(WebClientConfigurationFactory.getConfigurationWithoutPauseBetweenFetches());
	}

	public void press(CharSequence key) {
		switchTo().activeElement().sendKeys(key);
	}

	public EnhancedWebDriver write(String text, String cssSelector) {
		prepareElement(cssSelector).sendKeys(text);
		return this;
	}

	private WebElement prepareElement(String cssSelector) {
		WebElement webElement = findElement(By.cssSelector(cssSelector));
		webElement.clear();
		return webElement;
	}

	/**
	 * 
	 * The current thread sleeps until the given {@code expectedCondition} is
	 * met.
	 * 
	 * @param expectedCondition
	 * @return A value of type defined by the {@code expectedCondition} or null
	 *         if timeout expires.
	 * @see {@link EnhancedWebDriverOptions#DEFAULT_TIMEOUT_SEC},
	 *      {@link EnhancedWebDriverOptions#DEFAULT_INTERVAL_MILLIS}
	 */
	public <T> T waitUntil(ExpectedCondition<T> expectedCondition) {
		T t;

		try {
			handleAnyAlert();
			t = wait.until(expectedCondition);
		} catch (TimeoutException te) {
			log.warn("", te);
			t = null;
		}

		return t;
	}

	/**
	 * 
	 * 
	 * @see #get(String, boolean)
	 */
	@Override
	public void get(String url) {
		get(url, true);
	}

	/**
	 * 
	 * NOTA: The javascript injection may slow down the calling java code.
	 * Enable ajax hook only if necessary.
	 * 
	 * @param url
	 * @param enableAjaxHook
	 *            <ul>
	 *            <li>TRUE => inject javascript for intercepting ajax calls</li>
	 *            <li>FALSE=> no javascript injected</li>
	 *            </ul>
	 */
	public void get(String url, boolean enableAjaxHook) {
		handleAnyAlert();
		remoteWebDriver.get(url);
		resetAlertHandling();

		if (enableAjaxHook) {
			injectScript(loadResource("wendu.ajaxhook.min.js"), false);
			injectScript(loadResource("install.generic.hooks.min.js"), false);
		}
	}

	@Override
	public String getCurrentUrl() {
		handleAnyAlert();
		return remoteWebDriver.getCurrentUrl();
	}

	@Override
	public String getTitle() {
		handleAnyAlert();
		return remoteWebDriver.getTitle();
	}

	@Override
	public List<WebElement> findElements(By by) {
		handleAnyAlert();
		return remoteWebDriver.findElements(by);
	}

	@Override
	public WebElement findElement(By by) {
		handleAnyAlert();
		return remoteWebDriver.findElement(by);
	}

	@Override
	public String getPageSource() {
		handleAnyAlert();
		return remoteWebDriver.getPageSource();
	}

	@Override
	public void close() {
		handleAnyAlert();
		remoteWebDriver.close();
	}

	@Override
	public void quit() {
		handleAnyAlert();
		remoteWebDriver.quit();
	}

	@Override
	public Set<String> getWindowHandles() {
		handleAnyAlert();
		return remoteWebDriver.getWindowHandles();
	}

	@Override
	public String getWindowHandle() {
		handleAnyAlert();
		return remoteWebDriver.getWindowHandle();
	}

	@Override
	public TargetLocator switchTo() {
		// No alert handling needed ...
		return remoteWebDriver.switchTo();
	}

	@Override
	public Navigation navigate() {
		// No alert handling needed ...
		return remoteWebDriver.navigate();
	}

	@Override
	public Options manage() {
		// No alert handling needed ...
		return remoteWebDriver.manage();
	}

	public boolean click(WebElement webElement) {
		return clickGiven(elementToBeClickable(webElement));
	}

	public boolean click(By by) {
		return clickGiven(elementToBeClickable(by));
	}

	private boolean clickGiven(ExpectedCondition<WebElement> ec) {
		boolean ret = false;
		WebElement we = waitUntil(ec);

		if (we != null) {
			we.click();
			ret = true;
		}

		return ret;
	}

	private String loadResource(String name) {
		try {
			return IOUtils.toString(EnhancedWebDriver.class.getResourceAsStream(name));
		} catch (IOException ioe) {
			throw new ApplicationRuntimeException(ioe);
		}
	}

	/**
	 * 
	 * Install a javascript hook that dump responses of any XHR call matching
	 * the HTTP method ({@code method}) and the given url ({@code url}).
	 * 
	 * @param method
	 *            HTTP Method (GET, POST etc)
	 * @param url
	 *            The url looked for
	 * @return A monitoring id to use for later cancelling.
	 * 
	 * @see #stopMonitoringAjaxRequest(String)
	 */
	public String startMonitoringAjaxRequest(String method, String url) {
		String addAjaxListenerClause = String.format("return startMonitoringAjaxRequest('%s', '%s')", method, url);
		return executeScript(addAjaxListenerClause).toString();
	}

	/**
	 * 
	 * Wait for an ajax response for given monitoring id {@code id}.
	 * 
	 * @param id
	 *            Monitoring id to wait for.
	 * @return An ajax response for the given {@code id}.
	 * 
	 */
	public AjaxResponse waitUntilAjaxRequestIsCompleted(String id) {
		return new AjaxResponse(waitUntilPresenceOfTextLocated(By.cssSelector("#" + id)));
	}

	private String waitUntilPresenceOfTextLocated(By by) {
		WebElement element = waitUntil(presenceOfElementLocated(by));

		String body = "";
		if (element != null) {
			body = element.getAttribute("innerHTML");
		}

		return body;
	}

	public void stopMonitoringAjaxRequest(String id) {
		executeScript("stopMonitoringAjaxRequest('" + id + "')");
	}

	/**
	 * 
	 * Minimize {@code jsCode} then inject its minimization in current page.
	 * 
	 * @param jsCode
	 */
	public void injectScript(String jsCode) {
		injectScript(jsCode, true);
	}

	public void injectScript(String jsCode, boolean minimizeScript) {
		String finalJsCode;
		if (minimizeScript) {
			finalJsCode = minimizeScript(jsCode);
		} else {
			finalJsCode = jsCode;
		}

		log.debug(finalJsCode);
		executeScript(finalJsCode);
	}

	private String minimizeScript(String jsCode) {
		List<SourceFile> externs = Collections.emptyList();

		List<SourceFile> inputs = Arrays.asList(SourceFile.fromCode(System.currentTimeMillis() + ".js", jsCode));

		CompilerOptions options = new CompilerOptions();
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
		options.lineBreak = false;

		Compiler compiler = new Compiler();
		Result result = compiler.compile(externs, inputs, options);
		if (!result.success) {
			throw new ApplicationRuntimeException("Unable to minimize script:\n\nWarnings:\n" + result.warnings + "\n\nErrors:\n" + result.errors);
		}

		return jsCodeWhiteCharRemover.reset(compiler.toSource()).replaceAll("");
	}

	@Override
	public Object executeAsyncScript(String script, Object... args) {
		handleAnyAlert();
		return remoteWebDriver.executeAsyncScript(script, args);
	}

	@Override
	public Object executeScript(String script, Object... args) {
		handleAnyAlert();
		return remoteWebDriver.executeScript(script, args);
	}

	/**
	 *
	 * Check if an alert box is present. If so , accept it.
	 *
	 * @see Alert
	 *
	 */
	private void handleAnyAlert() {
		if (options.isAlertHandling() && !alertsAlreadyAutoAccepted) {
			try {
				remoteWebDriver.switchTo().alert().accept();
			} catch (NoAlertPresentException e) {
				// Do nothing...
			} finally {
				// Disabling any function that would raise an unexpected box.
				remoteWebDriver.executeScript("window.alert = window.confirm = window.prompt = window.onbeforeunload = null;");
				setAlertHandling();
			}
		}
	}

	private void resetAlertHandling() {
		alertsAlreadyAutoAccepted = false;
	}

	private void setAlertHandling() {
		alertsAlreadyAutoAccepted = true;
	}

	/**
	 * 
	 * Look for HTTP response received for {@code originalUrl} in the
	 * performance logs.
	 * 
	 * @param url
	 * @return
	 * @throws ApplicationRuntimeException
	 *             if response headers can't be found.
	 */
	public String getHttpResponse(String url) {
		enableNetworkAnalysis();
		try {
			// Load url
			get(url);

			// Get HTTP response
			State currentState = State.FIND_REQUEST_ID;
			String requestId = "";
			String headersText = "";
			boolean rawHttpResponseFound = false;
			boolean responseHasBody = false;
			int attempts = 2;

			// # Find start line and headers
			while (!rawHttpResponseFound && (attempts > 0)) {
				String currentEntryMessage = currentNetworkSniffer.grabNextMessage();

				if (currentEntryMessage != null) {
					switch (currentState) {
					case FIND_REQUEST_ID:
						if (currentEntryMessage.contains("\"method\":\"Network.requestWillBeSent\"")
								&& currentEntryMessage.contains("\"url\":\"" + url.toLowerCase())) {
							requestId = fromParamsOf(currentEntryMessage).get("requestId").asString();
							currentState = State.FIND_RESPONSE;
						}
						break;

					case FIND_RESPONSE:
						if (currentEntryMessage.contains("\"method\":\"Network.responseReceived\"")
								&& currentEntryMessage.contains("\"requestId\":\"" + requestId + "\"")) {
							JsonObject responseObject = fromParamsOf(currentEntryMessage).get("response").asObject();
							JsonValue headersTextObject = responseObject.get("headersText");

							if (headersTextObject == null) {
								StringBuilder sb = new StringBuilder("HTTP/1.1 " + responseObject.get("status") + " TODO_GET_CODE\r\n");
								JsonValue xTumblrUser = responseObject.get("headers").asObject().get("x-tumblr-user");
								if (xTumblrUser != null) {
									sb.append("X-Tumblr-User: ");
									sb.append(xTumblrUser.toString().replace("\"", ""));
									sb.append("\r\n");
								}
								sb.append("\r\n");
								headersText = sb.toString();
							} else {
								headersText = headersTextObject.asString();
							}

							currentState = State.FIND_BODY_OR_END;
						}
						break;

					case FIND_BODY_OR_END:
						if (currentEntryMessage.contains("\"requestId\":\"" + requestId + "\"")) {
							if (currentEntryMessage.contains("\"method\":\"Network.dataReceived\"")) {
								responseHasBody = true;
							} else if (currentEntryMessage.contains("\"method\":\"Network.loadingFinished\"")) {
								rawHttpResponseFound = true;
							} else {
								throw new ApplicationRuntimeException("Unexpected message:\n" + currentEntryMessage);
							}
						}
						break;

					default:
						throw new ApplicationRuntimeException("Unknown state: " + currentState);
					}
				} else {
					attempts--;
				}
			}

			if (!rawHttpResponseFound) {
				headersText = "HTTP/1.1 " + WebClient.FAILED_REQUEST + " " + url + "\r\n";
			}

			if (responseHasBody) {
				// TODO: Handle body here if ever needed...
			}

			return headersText;
		} finally {
			disableNetworkAnalysis();
		}
	}

	private void enableNetworkAnalysis() {
		// Find webSocketDebuggerUrl
		JsonArray infoArray = webClient.fetchAsJson("http://localhost:" + getRemoteDebuggingPort() + "/json").asArray();

		String webSocketDebuggerUrl = null;
		for (JsonValue info : infoArray) {
			JsonObject infoObject = info.asObject();
			if (infoObject.getString("type", "").equals("page")) {
				webSocketDebuggerUrl = infoObject.getString("webSocketDebuggerUrl", null);

				if (webSocketDebuggerUrl != null) {
					break;
				}
			}
		}

		if (webSocketDebuggerUrl == null) {
			throw new ApplicationRuntimeException("Unable to find webSocketDebuggerUrl.");
		}

		// Connect to webSocketDebuggerUrl
		currentNetworkSniffer = new NetworkSniffer(webSocketDebuggerUrl);
		currentNetworkSniffer.connectBlocking();
		currentNetworkSniffer.enableNetwork();
	}

	private void disableNetworkAnalysis() {
		currentNetworkSniffer.disableNetwork();
		currentNetworkSniffer.closeBlocking();
		currentNetworkSniffer = null;
	}

	private int getRemoteDebuggingPort() {
		try {
			Process p = Runtime.getRuntime().exec("WMIC path win32_process get Caption,Processid,Commandline");

			try (BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				int remoteDebuggingPort = -1;
				String line;
				while ((line = bri.readLine()) != null) {
					// We assume there is ONE chrome instance only with such
					// switch
					int commandLineSwitchPos = line.indexOf("--remote-debugging-port=");
					if (commandLineSwitchPos >= 0) {
						int equalPos = line.indexOf('=', commandLineSwitchPos);
						int nextSpacePos = line.indexOf(' ', equalPos);
						remoteDebuggingPort = Integer.parseInt(line.substring(equalPos + 1, nextSpacePos));
						break;
					}
				}

				if (remoteDebuggingPort <= 0) {
					log.warn("Unable to find remote debugging port.");
				}

				return remoteDebuggingPort;
			} finally {
				log.info("Waiting end of processes dump tool...");
				p.waitFor();
				log.info("DONE");
			}
		} catch (InterruptedException | IOException e) {
			throw new ApplicationRuntimeException(e);
		}
	}

	private JsonObject fromParamsOf(String entryMessage) {
		return JsonValue.readJSON(entryMessage).asObject() //
				.get("params").asObject();
	}

	private enum State {
		FIND_REQUEST_ID, FIND_RESPONSE, FIND_BODY_OR_END
	}

	private class NetworkSniffer extends WebSocketClient {
		private BlockingQueue<String> messages;

		public NetworkSniffer(String webSocketDebuggerUrl) {
			super(URI.create(webSocketDebuggerUrl));
			messages = new LinkedBlockingQueue<>();
		}

		/**
		 * 
		 * Returns a message sent by Chrome. The method waits no longer than 5
		 * seconds. It returns null if the timeout elapses.
		 * 
		 * @return The next available message or null if this message fails to
		 *         arrive within 5 seconds.
		 */
		public String grabNextMessage() {
			try {
				return messages.poll(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ApplicationRuntimeException(e);
			}
		}

		public void enableNetwork() {
			sendToDevtool("Network.enable");
		}

		public void disableNetwork() {
			sendToDevtool("Network.disable");
		}

		private void sendToDevtool(String method) {
			if (isOpen()) {
				send("{\"id\":1, \"method\": \"" + method + "\"}");
			} else {
				log.warn("Websocket disconnected. Ignoring: {}", method);
			}
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			log.info("Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
		}

		@Override
		public void onError(Exception e) {
			log.warn("", e);
		}

		@Override
		public void onMessage(String message) {
			try {
				messages.put(message);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ApplicationRuntimeException(e);
			}
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			log.info("Connection to remote debugger established.");
		}

		@Override
		public boolean connectBlocking() {
			try {
				return super.connectBlocking();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ApplicationRuntimeException(e);
			}
		}

		@Override
		public void closeBlocking() {
			try {
				super.closeBlocking();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ApplicationRuntimeException(e);
			}
		}
	}

	/**
	 * 
	 * A special overloded version of the method
	 * {@code EnhancedWebDriver#write(String, String)} for typing passwords
	 * securely.
	 * 
	 * @param password
	 * @param cssSelector
	 * @return
	 * 
	 * @see EnhancedWebDriver#write(String, String)
	 */
	public EnhancedWebDriver write(char[] password, String cssSelector) {
		int len = password.length;

		if (len > 0) {
			StringBuilder sb = new StringBuilder(len).append(password);
			prepareElement(cssSelector).sendKeys(sb);
		}

		return this;
	}

	@Override
	public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
		return remoteWebDriver.getScreenshotAs(target);
	}
}
