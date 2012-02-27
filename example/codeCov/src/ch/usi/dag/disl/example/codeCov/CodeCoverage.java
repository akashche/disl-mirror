package ch.usi.dag.disl.example.codeCov;

import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;

public class CodeCoverage extends RemoteAnalysis {
    private final AtomicLong cCounter = new AtomicLong();
    private final AtomicLong bbCounter = new AtomicLong();

    public void onConstructor(Class<?> c, int classId) {
        cCounter.incrementAndGet();
    }

    public void onBB(String fullMethodName, int bbID) {
        bbCounter.incrementAndGet();
    }

    @Override
    public void atExit() {
        System.out.println("[ANALYSIS] executed constructors: " + cCounter.get());
        System.out.println("[ANALYSIS] executed basic-blocks: " + bbCounter.get());
    }

    @Override
    public void objectFree(NetReference netRef) {
        System.out.println("[ANALYSIS] Object free for id " + netRef.getObjectId());
    }
}
