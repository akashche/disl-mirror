package ch.usi.dag.disl.test.utils;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import ch.usi.dag.dislreserver.DiSLREServer;
import ch.usi.dag.dislserver.DiSLServer;
import ch.usi.dag.util.Duration;
import ch.usi.dag.util.Strings;

public abstract class Runner {

    protected static final Duration _INIT_TIME_LIMIT_ = Duration.of (3, SECONDS);
    protected static final Duration _TEST_TIME_LIMIT_ = Duration.of (60, SECONDS);

    protected static final String _ENV_JAVA_HOME_ = "JAVA_HOME";
    protected static final String _JAVA_COMMAND_ = __getJavaCommand ();

    protected static final File _DISL_BUILD_DIR_ = new File ("build");
    protected static final File _DISL_AGENT_JAR_ = new File (_DISL_BUILD_DIR_, "disl-agent.jar");
    protected static final File _DISL_AGENT_LIB_ = new File (_DISL_BUILD_DIR_, "libdislagent.so");

    protected static final File _DISL_SERVER_JAR_ = new File (_DISL_BUILD_DIR_, "disl-server.jar");
    protected static final Class <?> _DISL_SERVER_MAIN_CLASS_ = DiSLServer.class;

    protected static final File _DISL_RE_AGENT_LIB_ = new File (_DISL_BUILD_DIR_, "libdislreagent.so");
    protected static final File _DISL_RE_DISPATCH_JAR_ = new File (_DISL_BUILD_DIR_, "dislre-dispatch.jar");

    protected static final File _DISL_RE_SERVER_JAR_ = new File (_DISL_BUILD_DIR_, "dislre-server.jar");
    protected static final Class <?> _DISL_RE_SERVER_MAIN_CLASS_ = DiSLREServer.class;

    protected static final File _TEST_BUILD_DIR_ = new File ("build-test");

    //

    private final String __testName;
    private final Class <?> __testClass;

    //

    Runner (final Class <?> testClass) {
        __testClass = testClass;
        __testName = __extractTestName (testClass);
    }

    private static String __getJavaCommand () {
        final String javaHome = System.getenv (_ENV_JAVA_HOME_);
        if (javaHome != null) {
            return new File (
                new File (new File (javaHome, "jre"), "bin"), "java"
            ).toString ();
        } else {
            return "java";
        }
    }


    private static String __extractTestName (final Class <?> testClass) {
        final String [] packages = testClass.getPackage ().getName ().split ("\\.");
        return packages [packages.length - 2];
    }

    //

    public void start () throws IOException {
        final File testInstrJar = new File (
            _TEST_BUILD_DIR_, String.format ("disl-instr-%s.jar", __testName)
        );

        final File testAppJar = new File (
            _TEST_BUILD_DIR_, String.format ("disl-app-%s.jar", __testName)
        );

        _start (testInstrJar, testAppJar);
    }

    protected abstract void _start (
        final File testInstrJar, final File testAppJar
    ) throws IOException;

    //

    public final boolean waitFor () {
        return _waitFor (_TEST_TIME_LIMIT_);
    }

    protected abstract boolean _waitFor (final Duration duration);

    //

    protected String _loadResource (final String name) throws IOException {
        return Strings.loadFromResource (__testClass, name);
    }

    protected void _destroyIfRunningAndDumpOutputs (
        final Job job, final String prefix
    ) throws IOException {
        if (job.isRunning ()) {
            job.destroy ();
        }

        Strings.storeToFile (
            String.format ("tmp.%s.%s.out.txt", __testName, prefix),
            job.getOutput ()
        );


        Strings.storeToFile (
            String.format ("tmp.%s.%s.err.txt", __testName, prefix),
            job.getError ()
        );
    }

    //

    static List <String> propertiesStartingWith (final String prefix) {
        final List <String> result = new LinkedList <String> ();

        for (final String key : System.getProperties ().stringPropertyNames ()) {
            if (key.startsWith (prefix)) {
                final Object valueObject = System.getProperty (key);
                if (valueObject instanceof String) {
                    final String value = (String) valueObject;
                    if (! value.isEmpty ()) {
                        result.add (String.format ("-D%s=%s", key, value));
                    }
                }
            }
        }

        return result;
    }


    static String classPath (final File ... paths) {
        return Strings.join (File.pathSeparator, (Object []) paths);
    }

}
