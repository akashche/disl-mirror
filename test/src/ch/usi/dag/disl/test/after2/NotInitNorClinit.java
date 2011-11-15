package ch.usi.dag.disl.test.after2;

import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.snippet.Shadow;

public class NotInitNorClinit implements SnippetGuard {
    @Override
    public boolean isApplicable(Shadow shadow) {
        return (shadow.getMethodNode().name.endsWith("init>")) ? false : true;
    }
}
