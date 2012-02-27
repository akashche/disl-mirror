package ch.usi.dag.disl.example.codeCov;

public class TargetClass {
    private TargetClass() {
        System.out.println("New instance of TargetClass");
    }

    public static void test(int size) {
        for(int i = 0; i < size; i++) {
            new TargetClass();
        }
    }

    public static void main(String[] args) {
        test(10);
    }
}
