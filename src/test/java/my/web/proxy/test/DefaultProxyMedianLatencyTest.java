package my.web.proxy.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import my.web.proxy.Proxy;
import my.web.proxy.DefaultProxy;

public class DefaultProxyMedianLatencyTest {

	private Proxy getTestProxy() {
		return new DefaultProxy("0.0.0.0", 0);
	}

	@Test
	public void test1() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(2);
		proxy.setLatency(6);
		proxy.setLatency(1);

		assertThat(proxy.getMedianLatency(), is(2d));
	}

	@Test
	public void test2() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(2);
		proxy.setLatency(4);
		proxy.setLatency(6);
		proxy.setLatency(1);

		assertThat(proxy.getMedianLatency(), is(3d));
	}

	@Test
	public void test3() {
		Proxy proxy = getTestProxy();

		assertThat(proxy.getMedianLatency(), is(0d));
	}

	@Test
	public void test4() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(1);

		assertThat(proxy.getMedianLatency(), is(1d));
	}

	@Test
	public void test5() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(1);
		proxy.setLatency(2);

		assertThat(proxy.getMedianLatency(), is(1.5d));
	}
	
	@Test
	// This test is the same as test5 but with inversed latencies
	public void test6() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(2);
		proxy.setLatency(1);

		assertThat(proxy.getMedianLatency(), is(1.5d));
	}
	
	@Test
	public void test7() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(2);
		proxy.setLatency(2);

		assertThat(proxy.getMedianLatency(), is(2d));
	}
	
	@Test
	public void test9() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(2);
		proxy.setLatency(2);
		proxy.setLatency(2);

		assertThat(proxy.getMedianLatency(), is(2d));
	}
	
	@Test
	public void test10() {
		Proxy proxy = getTestProxy();

		proxy.setLatency(2);
		proxy.setLatency(2);
		proxy.setLatency(2);
		proxy.setLatency(2);

		assertThat(proxy.getMedianLatency(), is(2d));
	}
}
