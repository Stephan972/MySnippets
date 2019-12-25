package my.web.proxy.test;

import static my.web.proxy.AnonimityType.ANONYMOUS;
import static my.web.proxy.AnonimityType.ELITE;
import static my.web.proxy.AnonimityType.TRANSPARENT;
import static my.web.proxy.AnonimityType.UNKNOWN;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.TreeSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import my.web.proxy.AnonimityType;
import my.web.proxy.Proxy;
import my.web.proxy.DefaultProxy;

public class DefaultProxyComparisonTest {

	private int port;

	@Before
	public void resetPortNumber() {
		port = 0;
	}

	private Proxy createProxy(AnonimityType at, double latencyInMs) {
		port++;

		Proxy p = new DefaultProxy("127.0.0.1", port);
		p.setAnonimityType(at);
		p.setLatency(latencyInMs);

		return p;
	}

	@Test
	public void testOrderByAnonymityType() {
		TreeSet<Proxy> set = new TreeSet<>();
		set.add(createProxy(ELITE, 1));
		set.add(createProxy(ANONYMOUS, 1));
		set.add(createProxy(TRANSPARENT, 1));
		set.add(createProxy(UNKNOWN, 1));

		assertThat(
				//
				set.toString(), //
				is("[UNKNOWN / HTTP @ 127.0.0.1:1(ELITE, 1.000), UNKNOWN / HTTP @ 127.0.0.1:2(ANONYMOUS, 1.000), UNKNOWN / HTTP @ 127.0.0.1:3(TRANSPARENT, 1.000), UNKNOWN / HTTP @ 127.0.0.1:4(UNKNOWN, 1.000)]") //
		);
	}

	@Test
	public void testOrderByMedianLatency() {
		TreeSet<Proxy> set = new TreeSet<>();
		set.add(createProxy(ELITE, 4));
		set.add(createProxy(ELITE, 2));
		set.add(createProxy(ELITE, 5));
		set.add(createProxy(ELITE, 1));
		set.add(createProxy(ELITE, 3));

		assertThat(
				//
				set.toString(), //
				is("[UNKNOWN / HTTP @ 127.0.0.1:1(ELITE, 4.000), UNKNOWN / HTTP @ 127.0.0.1:2(ELITE, 2.000), UNKNOWN / HTTP @ 127.0.0.1:3(ELITE, 5.000), UNKNOWN / HTTP @ 127.0.0.1:4(ELITE, 1.000), UNKNOWN / HTTP @ 127.0.0.1:5(ELITE, 3.000)]") //
		);
	}

	@Test
	public void testOrderBySuccessfulAttemptUse() {
		TreeSet<Proxy> set = new TreeSet<>();

		Proxy elite = createProxy(ELITE, 1);
		elite.incrementSuccessfulAttemptUse();
		set.add(elite);

		set.add(createProxy(ANONYMOUS, 1));
		set.add(createProxy(TRANSPARENT, 1));
		set.add(createProxy(UNKNOWN, 1));

		assertThat(
				//
				set.toString(), //
				is("[UNKNOWN / HTTP @ 127.0.0.1:1(ELITE, 1.000), UNKNOWN / HTTP @ 127.0.0.1:2(ANONYMOUS, 1.000), UNKNOWN / HTTP @ 127.0.0.1:3(TRANSPARENT, 1.000), UNKNOWN / HTTP @ 127.0.0.1:4(UNKNOWN, 1.000)]") //
		);
	}

	@Ignore
	public void testEquals1() {
		Proxy p1 = createProxy(ELITE, 1);
		Proxy p2 = createProxy(ELITE, 1);

		assertThat(p1.equals(p2), is(true));
	}

	@Ignore
	public void testEquals2() {
		resetPortNumber();

		Proxy p1 = createProxy(ELITE, 1);
		Proxy p2 = createProxy(ELITE, 1);

		assertThat(p2.equals(p1), is(true));
	}

	@Test
	public void testEquals3() {
		resetPortNumber();

		Proxy p1 = createProxy(ELITE, 1);
		Proxy p2 = createProxy(ELITE, 1);

		assertThat(p2.compareTo(p1), is(1));
	}

	@Test
	public void testIfTwoProxiesAreEqualThenTheyHaveSameHashCode() {
		resetPortNumber();
		Proxy p1 = createProxy(ELITE, 1);
		
		resetPortNumber();
		Proxy p2 = createProxy(ELITE, 1);

		assertThat(p1.equals(p2) && (p1.hashCode() == p2.hashCode()), is(true));
	}
}
