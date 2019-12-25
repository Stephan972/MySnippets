package my.web.proxy;

import java.time.Clock;

import lombok.Setter;

public enum ProxyClock {
	;

	@Setter
	private static Clock clock = Clock.systemUTC();

	public static long now() {
		return clock.millis();
	}
}
