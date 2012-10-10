package ch.usi.dag.dislreserver.netreference;

import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfo;
import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfoResolver;

public class NetReference {

	final private long netRefeference;
	final private long objectId;
	final private int classId;
	final private short spec;

	public NetReference(long netReference) {
		super();
		this.netRefeference = netReference;
		
		// initialize inner fields
		this.objectId = net_ref_get_object_id(netReference);
		this.classId = net_ref_get_class_id(netReference);
		this.spec = net_ref_get_spec(netReference);
	}
	
	public long getNetRefeferenceRawNum() {
		return netRefeference;
	}

	public long getObjectId() {
		return objectId;
	}

	public int getClassId() {
		return classId;
	}

	public short getSpec() {
		return spec;
	}
	
	public ClassInfo getClassInfo() {
		return ClassInfoResolver.getClass(classId);
	}

	// only object id considered
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (objectId ^ (objectId >>> 32));
		return result;
	}

	// only object id considered
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NetReference other = (NetReference) obj;
		if (objectId != other.objectId)
			return false;
		return true;
	}

	// ************* special bit mask handling methods **********
	
	// NOTE names of the methods are unusual for reason
	// you can find almost identical methods in agent
	
	// should be in sync with net_reference functions on the client
	
	// format of net reference looks like this
	// HIGHEST (1 bit spec, 23 bits class id, 40 bits object id)
	// bit field not used because there is no guarantee of alignment

	private final short OBJECT_ID_POS = 0;
	private final short CLASS_ID_POS = 40;
	private final short SPEC_POS = 63;
	
	private final long OBJECT_ID_MASK = 0xFFFFFFFFFFL;
	private final long CLASS_ID_MASK = 0x7FFFFFL;
	private final long SPEC_MASK = 0x1L;

	// get bits from "from" with pattern "bit_mask" lowest bit starting on position
	// "low_start" (from 0)
	private long get_bits(long from, long bit_mask, short low_start) {

		// shift it
		long bits_shifted = from >> low_start;

		// mask it
		return bits_shifted & bit_mask;
	}

	private long net_ref_get_object_id(long net_ref) {

		return get_bits(net_ref, OBJECT_ID_MASK, OBJECT_ID_POS);
	}

	private int net_ref_get_class_id(long net_ref) {

		return (int)get_bits(net_ref, CLASS_ID_MASK, CLASS_ID_POS);
	}

	private short net_ref_get_spec(long net_ref) {

		return (short)get_bits(net_ref, SPEC_MASK, SPEC_POS);
	}
}
