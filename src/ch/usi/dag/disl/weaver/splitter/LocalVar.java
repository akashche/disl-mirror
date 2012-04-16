package ch.usi.dag.disl.weaver.splitter;

import java.util.HashSet;
import java.util.Set;

public class LocalVar {

	public int index;
	public Set<SuperBlock> defines;
	public Set<SuperBlock> uses;

	public LocalVar(int index) {
		this.index = index;
		this.defines = new HashSet<SuperBlock>();
		this.uses = new HashSet<SuperBlock>();
	}

	public void addDefine(SuperBlock sb) {
		defines.add(sb);
	}

	public void addUse(SuperBlock sb) {
		uses.add(sb);
	}

}
