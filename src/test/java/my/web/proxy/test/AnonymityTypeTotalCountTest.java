package my.web.proxy.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import my.web.proxy.AnonimityType;

import org.junit.Test;

public class AnonymityTypeTotalCountTest {

	@Test
	public void test() {
		assertThat(AnonimityType.values().length, is(4));
	}
}
