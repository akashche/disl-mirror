package ch.usi.dag.dislreserver.netreference;

// should be in sync with net_reference on the client
public class NetReference {

	// TODO re ! rename
	long id;

	public NetReference(long id) {
		super();
		this.id = id;
	}

	public long getObjectId() {
		// TODO re ! should contain only object id
		return id;
	}

	// TODO re ! should hash only object id
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	// TODO re ! should equal only object id
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NetReference other = (NetReference) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
