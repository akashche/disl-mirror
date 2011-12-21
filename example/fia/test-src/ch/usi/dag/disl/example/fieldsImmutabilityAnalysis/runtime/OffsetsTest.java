package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;



import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;



import org.junit.BeforeClass;
import org.junit.Test;

public class OffsetsTest {

	protected static class TestClassA { public int[] a; }

	protected static class TestClassB extends TestClassA { public int b; }

	protected static class TestClassC extends TestClassB { public int b; }

	protected static class TestClassD extends TestClassC { public int a; }

	protected static class TestClassE extends TestClassD {public int[] array;  }

	@BeforeClass
	public static void setUpBeforeClass() {
		Offsets.registerIfNeeded(TestClassE.class); // Register the entire test class hierarchy.
	}

	@Test
	public void testGetNumberOfFields() {
		assertThat(Offsets.getNumberOfFields(Object.class), is(0));

		assertThat(Offsets.getNumberOfFields(TestClassA.class), is(1));

		assertThat(Offsets.getNumberOfFields(TestClassB.class), is(2));

		assertThat(Offsets.getNumberOfFields(TestClassC.class), is(3));

		assertThat(Offsets.getNumberOfFields(TestClassD.class), is(4));

		assertThat(Offsets.getNumberOfFields(TestClassE.class), is(4));
	}

	@Test
	public void testGetCanonicalFieldIDs() {
		assertThat(Offsets.getCanonicalFieldIDs(TestClassA.class), is(array(equalTo(Offsets.getFieldId(TestClassA.class, "a", Integer.TYPE)))));

		assertThat(Offsets.getCanonicalFieldIDs(TestClassB.class), is(array(
				equalTo(Offsets.getFieldId(TestClassA.class, "a", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassB.class, "b", Integer.TYPE)))));

		assertThat(Offsets.getCanonicalFieldIDs(TestClassC.class), is(array(
				equalTo(Offsets.getFieldId(TestClassA.class, "a", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassB.class, "b", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassC.class, "b", Integer.TYPE)))));

		assertThat(Offsets.getCanonicalFieldIDs(TestClassD.class), is(array(
				equalTo(Offsets.getFieldId(TestClassA.class, "a", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassB.class, "b", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassC.class, "b", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassD.class, "a", Integer.TYPE)))));

		assertThat(Offsets.getCanonicalFieldIDs(TestClassE.class), is(array(
				equalTo(Offsets.getFieldId(TestClassA.class, "a", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassB.class, "b", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassC.class, "b", Integer.TYPE)),
				equalTo(Offsets.getFieldId(TestClassD.class, "a", Integer.TYPE)))));
	}

	@Test
	public void testGetFieldOffset() {
		assertThat(getFieldOffset(TestClassA.class, "a", Integer.TYPE), is(0));

		assertThat(getFieldOffset(TestClassB.class, "a", Integer.TYPE), is(0));
		assertThat(getFieldOffset(TestClassB.class, "b", Integer.TYPE), is(1));

		assertThat(getFieldOffset(TestClassC.class, "a", Integer.TYPE), is(0));
		assertThat(getFieldOffset(TestClassC.class, "b", Integer.TYPE), is(2));

		assertThat(getFieldOffset(TestClassD.class, "a", Integer.TYPE), is(3));
		assertThat(getFieldOffset(TestClassD.class, "b", Integer.TYPE), is(2));

		assertThat(getFieldOffset(TestClassE.class, "a", Integer.TYPE), is(3));
		assertThat(getFieldOffset(TestClassE.class, "b", Integer.TYPE), is(3));
	}

	private int getFieldOffset(Class<?> owner, String name, Class<?> type) {
		return Offsets.getFieldOffset(Offsets.getFieldId(owner, name, type));
	}
}