package my.webdriver.test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import my.webdriver.EnhancedWebDriverOptions;



import org.junit.Test;

public class EnhancedWebDriverOptionsTest {
	
	@Test
	public void test() {
		EnhancedWebDriverOptions options = EnhancedWebDriverOptions.prepareWith() //
				.chromeDriverVersion("0.00") //
				.userDataDir("./userDataDir") //
				.headless(true) //
				.andBuild();

		assertThat(options.getExtensionFilter(), is(notNullValue()));
	}
}
