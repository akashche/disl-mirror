package ch.usi.dag.dislreserver.objectid;

public class ObjectId {

	int id;

	public ObjectId(int id) {
		super();
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectId other = (ObjectId) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
