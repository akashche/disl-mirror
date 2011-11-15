package ch.usi.dag.disl.guard;

import ch.usi.dag.disl.snippet.Shadow;

public interface SnippetGuard {

	public boolean isApplicable(Shadow shadow);
}
