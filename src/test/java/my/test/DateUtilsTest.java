package my.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;

import my.DateUtils;

import org.junit.Test;

public class DateUtilsTest {

	@Test
	public void asDateTest1() {
		LocalDate localDate = LocalDate.now();

		Date d = DateUtils.asDate(localDate);
		assertThat(d.getTime(), is(Timestamp.valueOf(localDate.atStartOfDay()).getTime()));
	}
}
