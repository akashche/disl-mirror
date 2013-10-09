package ch.usi.dag.disl.test.suite.after.app;

public class TargetClass {

    public void print(boolean flag) {
        try {
            System.out.println("app: TargetClass.print(..) - try:begin");

            if (flag) {
                String float_one = "1.0";
                Integer int_one = Integer.valueOf(float_one);
                System.out.println("app: UNREACHABLE " + int_one);
            }

            System.out.println("app: TargetClass.print(..) - try:end");
        } finally {
            System.out.println("app: TargetClass.print(..) - finally");
        }
    }

    // FIXME
    // this main breaks the test on class not found exception of this class
    /*
     * public static void main(String[] args) { try { TargetClass t = new
     * TargetClass();
     * System.out.println("app: TargetClass.main(..) - .print(false)");
     * t.print(false);
     * System.out.println("app: TargetClass.main(..) - .print(true)");
     * t.print(true); } catch (Throwable e) {
     * System.out.println("app: TargetClass.main(..) - catch"); } }
     */

    public static void main(String[] args) {
        TargetClass t = new TargetClass();
        System.out.println("app: TargetClass.main(..) - .print(false)");
        t.print(false);
        System.out.println("app: TargetClass.main(..) - .print(true)");
        t.print(true);
    }
}
