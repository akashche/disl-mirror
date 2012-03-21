package ch.usi.dag.disl.example.jp2.runtime;

import java.io.PrintStream;



public abstract class AbstractTextDumper implements Dumper {
    
    protected PrintStream out;
    
    public void dumpCallees(int level, CCTNode n) {
        if (n != null) {
            printScope(out, level);
            
            out.print(getMethodEntry(n));
            
            if (n.getCallees() == null) {
                out.println();
            } else {
                out.println(" {");
                dumpCallees(level + 1, n.getCallees());
                printScope(out, level);
                out.println("}"); 
            }
            
            dumpCallees(level, n.getLeft());
            dumpCallees(level, n.getRight());
        }
    }
    
    public void printScope(PrintStream out, int level) {
        while (level-- > 0)
            out.print("    ");
    }
    
    public abstract String getMethodEntry(CCTNode n);
}
