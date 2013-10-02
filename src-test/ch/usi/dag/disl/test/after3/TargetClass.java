package ch.usi.dag.disl.test.after3;

public class TargetClass {

    public static void main(String[] args) {
        System.out.println("hi");

        try {
            ethrowing1();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ereturning1();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ethrowing2();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ereturning2();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ethrowing1() throws Exception {
        try {
            if ("a".equals("a")) {
                throw new Exception();
            }
            return;
        } catch (Exception e) {
            throw e;
        }
    }

    public static void ereturning1() throws Exception {
        try {
            if ("a".equals("a")) {
                // throw new Exception();
            }
            return;
        } catch (Exception e) {
            throw e;
        }
    }

    public static void ethrowing2() throws Exception {
        throw new Exception();
    }

    public static void ereturning2() throws Exception {
        // throw new Exception();
    }
}
