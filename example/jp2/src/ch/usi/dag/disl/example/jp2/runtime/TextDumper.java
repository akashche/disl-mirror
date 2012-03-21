package ch.usi.dag.disl.example.jp2.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;



/**
 * @author Aibek Sarimbekov
 * @author Philippe Moret
 * @author Walter Binder
 * @author Andreas Sewe
 */
public class TextDumper extends AbstractTextDumper {
	public static long methodCounter=0; //total number of nodes
	public static long execmethodCounter=0; //total number of nodes

    public static long bbCounts=0; //total number of executed basic blocks
    public static long bytecodes=0; //total number of executed bytecodes
    public static Set<String>methods = new TreeSet<String>(); //Set containing the unique nodes
    
    public TextDumper(String outputFilePrefix) throws IOException {
        final FileOutputStream fos = new FileOutputStream(outputFilePrefix + ".output.gz");
        final GZIPOutputStream gzfos = new GZIPOutputStream(fos);
        out = new PrintStream(new BufferedOutputStream(gzfos), false, "UTF-8");
    }
    
    public void dump(CCTNode node) {
        out.println('{');
        
        dumpCallees(1, node.getCallees());
        out.println('}');
        out.flush();
        out.close();
    }
    
    @Override
    public String getMethodEntry(CCTNode n) {
        if (n.getExecutionCount()!=0 || n.getMID().contains("[NATIVE]")) 
            methodCounter++;
        execmethodCounter+=n.getExecutionCount();
        List<Long> bbs = n.getBasicBlockExecutionCounts();

        StringBuffer buf = new StringBuffer(n.getMID());
        if (n.getExecutionCount()!=0 || n.getMID().contains("[NATIVE]")) 
        	addUniqueNode(n.getMID());
        buf.append(" = ");
        buf.append(n.getExecutionCount());
        buf.append(", ");
        buf.append(" @");
        buf.append(n.getBytecodeIndex());
        buf.append(" [");

        for (int i=0; i < bbs.size(); i++){
        	buf.append(bbs.get(i));
        	buf.append("x" + n.getBBSize(i));
        	if(i != bbs.size() - 1)
        		buf.append(", ");
        }
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
