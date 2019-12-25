package my.web.proxy.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.AllArgsConstructor;
import my.web.proxy.AnonimityType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@AllArgsConstructor
// This test checks that AnonymityType is defined in decreasing natural order.
public class AnonymityTypeDecreasingOrderDefinitionTest {

	@Parameters(name = "{index}: {0} vs {1}")
	public static Collection<Object[]> data() {
		AnonimityType[] anonyAnonimityTypes = AnonimityType.values();
		int len = anonyAnonimityTypes.length;
		List<Object[]> ret = new ArrayList<>();

		for (int i = 0; i < len; i++) {
			for (int j = i + 1; j < len; j++) {
				ret.add(new Object[] { anonyAnonimityTypes[i], anonyAnonimityTypes[j] });
			}
		}

		return ret;
	}

	private AnonimityType left;
	private AnonimityType right;

	@Test
	public void test1() {
		assertThat(left.isGreaterThan(right), is(true));
	}

	@Test
	public void test2() {
		assertThat(right.isLowerThan(left), is(true));
	}
}
