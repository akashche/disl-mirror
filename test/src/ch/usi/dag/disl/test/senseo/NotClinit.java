package ch.usi.dag.disl.test.senseo;

import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.snippet.Shadow;

public class NotClinit implements SnippetGuard {
    @Override
    public boolean isApplicable(Shadow shadow) {
        return (shadow.getMethodNode().name.equals("<clinit>")) ? false : true;
    }
}
