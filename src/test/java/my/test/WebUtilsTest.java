package my.test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import my.web.utils.WebUtils;

public class WebUtilsTest {
	private Path dumpsDirPath = Paths.get("./dumps");
	
	@Before
	public void before() {
		removeDirectory(dumpsDirPath);
	}

	@After
	public void after() {
		removeDirectory(dumpsDirPath);
	}

	@Test
	public void test1() throws IOException {
		String documentTitle = "Title > Sub-title";
		String expectedFilename = "_Title___Sub-title.dump";
		Document doc = Jsoup.parse("<html><head><title>" + documentTitle + "</title></head><body></body></html>");

		WebUtils.dumpDocument(doc);

		List<Path> pathsFound = Files.walk(dumpsDirPath).filter(Files::isRegularFile).collect(Collectors.toList());

		assertThat(pathsFound.size(), is(1));
		assertThat(pathsFound.get(0).getFileName().toString(), endsWith(expectedFilename));
	}

	@Test
	public void test2() throws IOException {
		String documentTitle = "12345678901234567890123456789012345";
		String expectedFilename = "1234567890123456789012345(...).dump";

		WebUtils.dumpDocument(Jsoup.parse("<html><head><title>" + documentTitle + "</title></head><body></body></html>"));

		List<Path> pathsFound = Files.walk(dumpsDirPath).filter(Files::isRegularFile).collect(Collectors.toList());

		assertThat(pathsFound.size(), is(1));
		assertThat(pathsFound.get(0).getFileName().toString(), endsWith(expectedFilename));
	}

	public static void removeDirectory(Path p) {
		removeDirectory(p.toFile());
	}

	public static void removeDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (File aFile : files) {
					removeDirectory(aFile);
				}
			}
			dir.delete();
		} else {
			dir.delete();
		}
	}
}
