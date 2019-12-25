package my.test;

import java.io.IOException;

import my.webdriver.EnhancedWebDriver;
import my.webdriver.EnhancedWebDriverFactory;
import my.webdriver.EnhancedWebDriverOptions;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.junit.Ignore;
import org.junit.Test;

public class dummy {

	@Ignore
	public void t() throws IOException {
		Response r = Jsoup.connect("http://httpbin.org/get").proxy("138.68.24.145", 3128).ignoreContentType(true).execute();
		System.out.println(r.body());

		r = Jsoup.connect("http://httpbin.org/get").proxy("176.15.163.199", 53281).ignoreContentType(true).execute();
		System.out.println(r.body());
	}

	@Test
	public void t2() {
		EnhancedWebDriver driver = null;

		try {
			EnhancedWebDriverOptions options = EnhancedWebDriverOptions.prepareWith() //
					.chromeDriverVersion("2.35") //
					.userDataDir("C:/Users/stephan/AppData/Local/Temp/1517527699822-0") //
					.headless(false) //
					.disableImageLoading(true) //
					.waitTimeoutInSeconds(20) //
					.andBuild();

			driver = EnhancedWebDriverFactory.newChromeDriver(options);
			String testedUrl = "http://abandonedography.com/";

			System.out.println(driver.getHttpResponse(testedUrl));
		} finally {
			if (driver != null) {
				driver.close();
				driver.quit();
			}
		}
	}
}
