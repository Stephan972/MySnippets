package my.console.test;

import static my.console.ProgressFormatter.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class ProgressFormatterTest {

    @Test
    public void testNullCurrent() {
        assertThat(formatEstimatedRemainingTime(0, 1000, 0, 10), is("Infinity"));
    }

    @Test
    public void test1() {
        assertThat(formatEstimatedRemainingTime(0, 1000, 1, 10), is("0 h 00 mn 09 s 000 ms"));
    }

    @Test
    public void test2() {
        assertThat(formatEstimatedRemainingTime(1000, 0, 1, 10), is("0 h 00 mn 09 s 000 ms"));
    }

    @Test
    public void test3() {
        assertThat(formatEstimatedRemainingTime(1000, 1000, 1, 10), is("0 h 00 mn 00 s 000 ms"));
    }

    @Test
    public void test4() {
        assertThat(format(1, 10), is("1/10 (10,00 %)"));
    }

    @Test
    public void test5() {
        assertThat(format(0, 10), is("0/10 (0,00 %)"));
    }

    @Test
    public void test6() {
        assertThat(format(0, 0), is("0/0 (0,00 %)"));
    }

    @Test
    public void test7() {
        assertThat(format(0, -4), is("0/-4 (0,00 %)"));
    }

    @Test
    public void test8() {
        assertThat(formatEstimatedTimeOfArrival(1570312799000L), is("05/10/2019 23:59"));
    }
}
