package ch.usi.dag.dislserver;

public class ClassAsBytes {
    
	private byte[] name;
    private byte[] code;

    public ClassAsBytes(byte[] name, byte[] code) {
        this.name = name;
        this.code = code;
    }

	public byte[] getName() {
		return name;
	}

	public byte[] getCode() {
		return code;
	}
}
