package ch.usi.dag.disl.weaver.splitter;

import org.objectweb.asm.tree.analysis.Value;

public class FlagValue implements Value {

	public class FlagReference {

		boolean rFlag;

		public FlagReference(boolean rFlag) {

			this.rFlag = rFlag;
		}
	}

	private int size;
	private boolean flag;
	private FlagReference ref;

	public FlagValue(int size) {
		this.size = size;
		this.flag = true;
		this.ref = new FlagReference(true);
	}

	public FlagValue(int size, boolean flag) {
		this.size = size;
		this.flag = flag;
		this.ref = null;
	}

	@Override
	public int getSize() {
		return size;
	}

	public boolean getFlag() {

		if (ref == null) {
			return flag;
		} else {
			return ref.rFlag;
		}
	}

	public void setFlag(boolean flag) {

		if (ref == null) {
			this.flag = flag;
		} else {
			ref.rFlag = flag;
		}
	}

	public FlagValue clone() {

		FlagValue clone = new FlagValue(size, flag);
		clone.ref = ref;
		return clone;
	}

}
