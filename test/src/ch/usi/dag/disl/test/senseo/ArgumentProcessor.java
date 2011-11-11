package ch.usi.dag.disl.test.senseo;

import ch.usi.dag.disl.dislclass.annotation.Processor;

@Processor
public class ArgumentProcessor {
    public static void objPM(int pos, int n, Object o) {
        DiSLClass.thisAnalysis.profileArgument(o);//==null) ? Null.class : o.getClass());//, pos);
    }

//    public static void booleanPM(int pos, int n, boolean b) {
//        DiSLClass.thisAnalysis.profileArgument(Boolean.class, pos);
//    }
//
//    public static void bytePM(int pos, int n, byte b) {
//        DiSLClass.thisAnalysis.profileArgument(Byte.class, pos);
//    }
//
//    public static void charPM(int pos, int n, char c) {
//        DiSLClass.thisAnalysis.profileArgument(Char.class, pos);
//    }
//
//    public static void doublePM(int pos, int n, double d) {
//        DiSLClass.thisAnalysis.profileArgument(Double.class, pos);
//    }
//
//    public static void floatPM(int pos, int n, float f) {
//        DiSLClass.thisAnalysis.profileArgument(Float.class, pos);
//    }
//
//    public static void intPM(int pos, int n, int i) {
//        DiSLClass.thisAnalysis.profileArgument(Int.class, pos);
//    }
//
//    public static void longPM(int pos, int n, long l) {
//        DiSLClass.thisAnalysis.profileArgument(Long.class, pos);
//    }
//
//    public static void shortPM(int pos, int n, short s) {
//        DiSLClass.thisAnalysis.profileArgument(Short.class, pos);
//    }
}
