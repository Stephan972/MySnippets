package my.test;

import java.io.IOException;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.junit.Ignore;

public class dummy {

	@Ignore
	public void t() throws IOException {
		Response r = Jsoup.connect("http://httpbin.org/get").proxy("138.68.24.145", 3128).ignoreContentType(true).execute();
		System.out.println(r.body());

		r = Jsoup.connect("http://httpbin.org/get").proxy("176.15.163.199", 53281).ignoreContentType(true).execute();
		System.out.println(r.body());
	}

}
