package ch.usi.dag.disl.guard;

import ch.usi.dag.disl.processorcontext.ArgumentContext;

// used for guard invocation - reduced visibility
class ArgumentContextImpl implements ArgumentContext {

	private int position;
	private String typeDescriptor;
	private int totalCount;
	
	public ArgumentContextImpl(int position, String typeDescriptor,
			int totalCount) {
		super();
		this.position = position;
		this.typeDescriptor = typeDescriptor;
		this.totalCount = totalCount;
	}

	public int position() {
		return position;
	}

	public String typeDescriptor() {
		return typeDescriptor;
	}

	public int totalCount() {
		return totalCount;
	}

}
