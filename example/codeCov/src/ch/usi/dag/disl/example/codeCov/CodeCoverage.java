package ch.usi.dag.disl.example.codeCov;

import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;

public class CodeCoverage extends RemoteAnalysis {
    public void onConstructor(Class<?> c) {
        System.out.println("[ANALYSIS] onConstructor");//:\t" + c.getName());
    }

    public void onBB(String fullMethodName, int bbID) {
        System.out.println("[ANALYSIS] onBB:\t" + fullMethodName + "\t" + bbID);
    }

    @Override
    public void atExit() {
        // TODO Auto-generated method stub
    }

    @Override
    public void objectFree(NetReference netRef) {
        System.out.println("[ANALYSIS] Object free for id " + netRef.getObjectId());
    }
}
