package my.web.proxy.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import my.web.proxy.Proxy;
import my.web.proxy.DefaultProxy;

public class DefaultProxyTest {

	@Test
	public void test1() {
		Proxy p1 = new DefaultProxy("127.0.0.1", 8080);
		p1.setProxyProviderName("foo");

		Proxy p2 = new DefaultProxy("127.0.0.1", 8080);
		p2.setProxyProviderName("bar");

		int p1HashCode = p1.hashCode();
		assertThat(p1HashCode == p2.hashCode(), is(true));
		assertThat(p1.equals(p2), is(true));
		assertThat(p1.compareTo(p2) == 0, is(true));

		p1.setProxyProviderName("bar");
		assertThat(p1HashCode == p1.hashCode(), is(true));
		assertThat(p1.equals(p2), is(true));
		assertThat(p1.compareTo(p2) == 0, is(true));
	}
}
