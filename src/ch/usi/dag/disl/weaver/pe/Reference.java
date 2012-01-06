package ch.usi.dag.disl.weaver.pe;

public class Reference {
	private Object obj;
	private boolean isValid;

	public Reference(Object obj) {
		this.obj = obj;
		this.isValid = (obj != null);
	}

	public Object getObj() {
		return obj;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
}
