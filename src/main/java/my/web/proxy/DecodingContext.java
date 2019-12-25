package my.web.proxy;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.nodes.Document;

/**
 * 
 * A class acting as a medium for exchanging any information between Java code
 * and a proxy provider specific Javascript code.
 *
 */
@Slf4j
public class DecodingContext {

	private static final String JS_CODE_LOGS_PREFIX = "JsCode >>";

	@Getter
	@Setter
	private Document proxiesPage;

	private boolean decodingNeeded;

	public DecodingContext(boolean decodingNeeded) {
		this.decodingNeeded = decodingNeeded;
	}

	public boolean decodingIsNeeded() {
		return decodingNeeded;
	}

	public void debug(String format, Object... arguments) {
		log.debug(JS_CODE_LOGS_PREFIX + format, arguments);
	}

	public void warn(String msg) {
		log.warn(JS_CODE_LOGS_PREFIX + "{}", msg);
	}

	public void info(String msg) {
		log.info(JS_CODE_LOGS_PREFIX + "{}", msg);
	}
}
