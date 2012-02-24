package ch.usi.dag.dislreserver.classinfo;

public class ExtractedClassInfo {

	private byte[] classCodeAsBytes;

	public ExtractedClassInfo(byte[] classCodeAsBytes) {
		super();
		this.classCodeAsBytes = classCodeAsBytes;
	}

	public ExtractedClassInfo(ExtractedClassInfo eci) {
		this.classCodeAsBytes = eci.classCodeAsBytes;
	}

	public byte[] getClassCodeAsBytes() {
		return classCodeAsBytes;
	}
}
