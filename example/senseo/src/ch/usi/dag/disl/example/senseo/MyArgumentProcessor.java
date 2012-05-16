package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.ArgumentProcessor;

@ArgumentProcessor
public class MyArgumentProcessor {
    public static void objPM(Object obj) {
        DiSLClass.thisAnalysis.profileArgument(obj);
    }
}
