package my.webdriver.test;

import static org.easymock.EasyMock.expect;
import my.webdriver.EnhancedWebDriver;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.powermock.api.easymock.PowerMock;

public class EnhancedWebDriverTest {

	@Test
	public void testMonitoredAjaxResponseNotReceivedOnTime() {
		RemoteWebDriver remoteWebDriver = PowerMock.createMock(RemoteWebDriver.class);

		expect(remoteWebDriver.switchTo()).andThrow(new NoAlertPresentException());
		expect(remoteWebDriver.executeScript("window.alert = window.confirm = window.prompt = window.onbeforeunload = null;")).andReturn("");
		expect(remoteWebDriver.executeScript("return startMonitoringAjaxRequest('GET', 'http://example.com/foo')")).andReturn("ajax_response_1234");
		expect(remoteWebDriver.findElements(By.cssSelector("#ajax_response_1234"))).andThrow(new TimeoutException());

		PowerMock.replayAll();

		EnhancedWebDriver driver = new EnhancedWebDriver(remoteWebDriver);
		String id = driver.startMonitoringAjaxRequest("GET", "http://example.com/foo");
		driver.waitUntilAjaxRequestIsCompleted(id);
		// driver.stopMonitoringAjaxRequest(id);

		PowerMock.verifyAll();
	}
}
