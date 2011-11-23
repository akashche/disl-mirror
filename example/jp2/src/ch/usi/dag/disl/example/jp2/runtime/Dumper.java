package ch.usi.dag.disl.example.jp2.runtime;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Dumper {
  // DUMP STUFF
    
	public static long methodCounter=0; //total number of nodes
	public static long execmethodCounter=0; //total number of nodes

    public static long bbCounts=0; //total number of executed basic blocks
    public static long bytecodes=0; //total number of executed bytecodes
    public static Set<String>methods = new TreeSet<String>(); //Set containing the unique nodes
    
    static protected PrintStream out;
    

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
    
    public void dump(CCTNode node) {
        out.println('{');
        
        dumpCallees(1, node.getCallees());
        out.println('}');
        out.flush();
        out.close();
    }
    
  
    public String getMethodEntry(CCTNode n) {
        if (n.getExecutionCount()!=0 || n.getMID().contains("[NATIVE]")) 
            methodCounter++;
        execmethodCounter+=n.getExecutionCount();
    	List<Integer> bbSize = n.getBasicBlockSizes();
        List<Long> bbs = n.getBasicBlockExecutionCounts();
        
        StringBuffer buf = new StringBuffer(n.getMID());
        if (n.getExecutionCount()!=0 || n.getMID().contains("[NATIVE]")) 
            addUniqueNode(n.getMID());
        buf.append(" = ");
        buf.append(n.getExecutionCount());
        //bytecodes+=n.getExecutionCount();
        buf.append(", ");
        buf.append(n.getExecutedInstructions());
        buf.append(" @");
        buf.append(n.getBytecodeIndex());
        buf.append(" MYBYTECODEEXECUTED " + n.getExecutedBytecodes());
        buf.append(" [");
        for (int i=0; i < bbs.size(); i++){
            buf.append(bbs.get(i));
            bbCounts+=bbs.get(i);
            buf.append("x" + bbSize.get(i));
            bytecodes+=bbSize.get(i)*bbs.get(i);
            if(i != bbs.size() - 1)
                buf.append(", ");
        }
        buf.append("]");
        // DEBUG
        buf.append("SIZES [");
        buf.append(n.getSizesAsString());
        buf.append("]");
        return buf.toString();
    }
    static void addUniqueNode(String s){
    	if(methods.contains(s))
    		return;
    	else
    		methods.add(s);
    }
}
