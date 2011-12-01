package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class OffsetsTest {

	protected static class TestClassA { public int a; }

	protected static class TestClassB extends TestClassA { public int b; }

	protected static class TestClassC extends TestClassB { public int b; }

	protected static class TestClassD extends TestClassC { public int a; }

	protected static class TestClassE extends TestClassD { }

	@Test
	public void testFieldOffsets() {
		Offsets.register(TestClassE.class); // Should register the entire test class hierarchy.

		assertThat(getOffset(TestClassA.class, "a", Integer.TYPE), is((short) 0));

		assertThat(getOffset(TestClassB.class, "a", Integer.TYPE), is((short) 0));
		assertThat(getOffset(TestClassB.class, "b", Integer.TYPE), is((short) 1));

		assertThat(getOffset(TestClassC.class, "a", Integer.TYPE), is((short) 0));
		assertThat(getOffset(TestClassC.class, "b", Integer.TYPE), is((short) 2));

		assertThat(getOffset(TestClassD.class, "a", Integer.TYPE), is((short) 3));
		assertThat(getOffset(TestClassD.class, "b", Integer.TYPE), is((short) 2));

		assertThat(getOffset(TestClassE.class, "a", Integer.TYPE), is((short) 3));
		assertThat(getOffset(TestClassE.class, "b", Integer.TYPE), is((short) 2));
	}

	private short getOffset(Class<?> owner, String name, Class<?> type) {
		return Offsets.getFieldOffset(Offsets.getFieldId(owner, name, type));
	}
}
