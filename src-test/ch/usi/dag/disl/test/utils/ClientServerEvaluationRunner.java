package ch.usi.dag.disl.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ClientServerEvaluationRunner extends Runner {

    private Job __client;

    private Job __server;

    private Job __shadow;

    private boolean clientOutNull;

    private boolean clientErrNull;

    private boolean shadowOutNull;

    private boolean shadowErrNull;

    private boolean serverOutNull;

    private boolean serverErrNull;


    public ClientServerEvaluationRunner (final Class <?> testClass) {
        super (testClass);

        clientOutNull = true;
        clientErrNull = true;
        shadowOutNull = true;
        shadowErrNull = true;
        serverOutNull = true;
        serverErrNull = true;
    }

    private Job __startServer (final File testInstrJar) throws IOException {
        final List <String> serverCommand = newLinkedList (
            _JAVA_COMMAND_,
            "-cp", makeClassPath (testInstrJar, _DISL_SERVER_JAR_)
        );

        serverCommand.addAll (propertiesStartingWith ("dislserver."));
        serverCommand.addAll (propertiesStartingWith ("disl."));
        serverCommand.add (_DISL_SERVER_MAIN_CLASS_.getName ());

        //

        final Job result = new Job (serverCommand);
        result.start ();
        return result;
    }


    private Job __startShadow (final File testInstrJar) throws IOException {
        final List <String> shadowCommand = newLinkedList (
            _JAVA_COMMAND_, "-Xms1G", "-Xmx2G",
            "-cp", makeClassPath (testInstrJar, _DISL_RE_SERVER_JAR_)
        );

        shadowCommand.addAll (propertiesStartingWith ("dislreserver."));
        shadowCommand.add (_DISL_RE_SERVER_MAIN_CLASS_.getName ());

        //

        final Job result = new Job (shadowCommand);
        result.start ();
        return result;
    }


    private Job __startClient (
        final File testInstrJar, final File testAppJar
    ) throws IOException {
        final List <String> clientCommand = newLinkedList (
            _JAVA_COMMAND_,
            String.format ("-agentpath:%s", _DISL_AGENT_LIB_),
            String.format ("-agentpath:%s", _DISL_RE_AGENT_LIB_),
            String.format ("-javaagent:%s", _DISL_AGENT_JAR_),
            String.format ("-Xbootclasspath/a:%s", makeClassPath (
                _DISL_AGENT_JAR_, testInstrJar, _DISL_RE_DISPATCH_JAR_
            ))
        );

        clientCommand.addAll (propertiesStartingWith ("disl."));
        clientCommand.addAll (Arrays.asList (
            "-jar", testAppJar.toString ()
        ));

        //

        final Job result = new Job (clientCommand);
        result.start ();
        return result;
    }


    @Override
    protected void _start (
        final File testInstrJar, final File testAppJar
    ) throws IOException {
        __server = __startServer (testInstrJar);
        __shadow = __startShadow (testInstrJar);

        _INIT_TIME_LIMIT_.softSleep ();

        if (! __server.isRunning ()) {
            throw new IOException ("server failed: "+ __server.getError ());
        }

        if (! __shadow.isRunning ()) {
            throw new IOException ("shadow failed: "+ __shadow.getError ());
        }

        //

        __client = __startClient (testInstrJar, testAppJar);
    }


    @Override
    protected boolean _waitFor (final Duration duration) {
        boolean finished = true;
        finished = finished & __client.waitFor (duration);
        finished = finished & __server.waitFor (duration);
        finished = finished & __shadow.waitFor (duration);
        return finished;
    }


    public void assertIsStarted () {
        assertTrue ("client not started", __client.isStarted ());
        assertTrue ("server not started", __server.isStarted ());
        assertTrue ("shadow not started", __shadow.isStarted ());
    }


    public void assertIsFinished () {
        assertTrue ("client not finished", __client.isFinished ());
        assertTrue ("server not finished", __server.isFinished ());
        assertTrue ("shadow not finished", __shadow.isFinished ());
    }


    public void assertIsSuccessful () {
        assertTrue ("client failed", __client.isSuccessful ());
        assertTrue ("server failed", __server.isSuccessful ());
        assertTrue ("shadow failed", __shadow.isSuccessful ());
    }


    public void destroyIfRunningAndFlushOutputs () throws IOException {
        _destroyIfRunningAndDumpOutputs (__client, "client");
        _destroyIfRunningAndDumpOutputs (__server, "server");
        _destroyIfRunningAndDumpOutputs (__shadow, "shadow");
    }


    public void assertClientOut (final String fileName) throws IOException {
        clientOutNull = false;
        assertEquals (
            "client out does not match",
            _readResource (fileName), __client.getOutput ()
        );
    }


    public void assertClientOutNull () throws IOException {
        clientOutNull = false;
        assertEquals ("client out does not match", "", __client.getOutput ());
    }


    public void assertClientErr (final String fileName) throws IOException {
        clientErrNull = false;
        assertEquals (
            "client err does not match",
            _readResource (fileName), __client.getError ()
        );
    }


    public void assertClientErrNull () throws IOException {
        clientErrNull = false;
        assertEquals ("client err does not match", "", __client.getError ());
    }


    public void assertShadowOut (final String fileName) throws IOException {
        shadowOutNull = false;
        assertEquals (
            "shadow out does not match",
            _readResource (fileName), __shadow.getOutput ()
        );
    }


    public void assertShadowOutNull () throws IOException {
        shadowOutNull = false;
        assertEquals ("shadow out does not match", "", __shadow.getOutput ());
    }


    public void assertShadowErr (final String fileName) throws IOException {
        shadowErrNull = false;
        assertEquals (
            "shadow err does not match",
            _readResource (fileName), __shadow.getError ()
        );
    }


    public void assertShadowErrNull () throws IOException {
        shadowErrNull = false;
        assertEquals ("shadow err does not match", "", __shadow.getError ());
    }


    public void assertServerOut (final String fileName) throws IOException {
        serverOutNull = false;
        assertEquals (
            "server out does not match",
            _readResource (fileName), __server.getOutput ()
        );
    }


    public void assertServerOutNull ()
    throws IOException {
        serverOutNull = false;
        assertEquals ("server out does not match", "", __server.getOutput ());
    }


    public void assertServerErr (final String fileName) throws IOException {
        serverErrNull = false;
        assertEquals (
            "server err does not match",
            _readResource (fileName), __server.getError ()
        );
    }


    public void assertServerErrNull () throws IOException {
        serverErrNull = false;
        assertEquals ("server err does not match", "", __server.getError ());
    }


    public void assertRestOutErrNull () throws IOException {
        if (clientOutNull) {
            assertClientOutNull ();
        }

        if (clientErrNull) {
            assertClientErrNull ();
        }

        if (shadowOutNull) {
            assertClientOutNull ();
        }
        if (shadowErrNull) {
            assertClientErrNull ();
        }

        if (serverOutNull) {
            assertServerOutNull ();
        }
        if (serverErrNull) {
            assertServerErrNull ();
        }
    }


    public void destroy () {
        if (__client != null) {
            __client.destroy ();
        }
        if (__shadow != null) {
            __shadow.destroy ();
        }
        if (__server != null) {
            __server.destroy ();
        }
    }
}
