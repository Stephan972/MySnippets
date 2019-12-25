package my.web.proxy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import my.exceptions.ApplicationRuntimeException;

import org.hjson.JsonObject;
import org.hjson.JsonObject.Member;
import org.hjson.JsonValue;

@RequiredArgsConstructor
public class ProxyProvider {

	@NonNull
	private JsonObject jsonConfiguration;
	@NonNull
	private Map<String, String> proxyAnonimityTypeMap;

	public static ProxyProvider createFrom(JsonValue jsonValue) throws InvalidJsonValueException {
		if (jsonValue == null) {
			throw new InvalidJsonValueException("null value provided.");
		}

		if (!jsonValue.isObject()) {
			throw newInvalidJsonValueException("Json Object expected but was:\n" + jsonValue.toString());
		}

		JsonObject jsonConfiguration = jsonValue.asObject();

		JsonValue candidateProxyAnonimityTypeMap = jsonConfiguration.get("proxyAnonimityTypeMap");
		if (candidateProxyAnonimityTypeMap == null) {
			throw newInvalidJsonValueException("No proxy anonimity type map defined.\n" + jsonValue.toString());
		}

		Map<String, String> proxyAnonimityTypeMap = new HashMap<>();		
		for(Member m : candidateProxyAnonimityTypeMap.asObject()){
			proxyAnonimityTypeMap.put(m.getName(), m.getValue().asString());
		}

		return new ProxyProvider(jsonConfiguration, proxyAnonimityTypeMap);
	}

	public static class InvalidJsonValueException extends Exception {
		private static final long serialVersionUID = -4794134756599558007L;

		public InvalidJsonValueException(String msg) {
			super(msg);
		}
	}

	public String getStartUrl() {
		return getProperty("startUrl");
	}

	public String getProxyRowsCssQuery() {
		return getProperty("proxyRowsCssQuery");
	}

	private String getProperty(String name) {
		String value = jsonConfiguration.getString(name, null);
		if (value == null) {
			throw new ApplicationRuntimeException(name + " not found.");
		}

		return value;
	}

	public String getProxyHostCellCssQuery() {
		return getProperty("proxyHostCellCssQuery");
	}

	public String getProxyPortCellCssQuery() {
		return getProperty("proxyPortCellCssQuery");
	}

	public String getProxyLatencyValueCssQuery() {
		return getProperty("proxyLatencyValueCssQuery");
	}

	public String getProxyAnonimityTypeValueCssQuery() {
		return getProperty("proxyAnonimityTypeValueCssQuery");
	}

	public Map<String, String> getProxyAnonimityTypeMap() {
		return Collections.unmodifiableMap(proxyAnonimityTypeMap);
	}

	private static InvalidJsonValueException newInvalidJsonValueException(String msg) {
		String finalMsg = msg.replaceAll("\\n", System.lineSeparator());

		return new InvalidJsonValueException(finalMsg);
	}

	public String getName() {
		return getProperty("name");
	}

	public String getDescription() {
		return getProperty("description");
	}

	public Pattern getProxyLatencyValueRegexPattern() {
		return Pattern.compile( getProperty("proxyLatencyValueRegex"));
	}

	public String getNextPageUrlCssQuery() {
		return getProperty("nextPageUrlCssQuery");
	}

	public String getDecodingScript() {
		return getProperty("decodingScript");
	}

	public String getSpamWarningCssQuery() {
		return getProperty("spamWarningCssQuery");
	}
}
