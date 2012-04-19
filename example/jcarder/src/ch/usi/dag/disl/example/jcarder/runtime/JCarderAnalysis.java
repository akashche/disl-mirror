package ch.usi.dag.disl.example.jcarder.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import ch.usi.dag.disl.example.jcarder.runtime.InstrumentConfig;
import ch.usi.dag.disl.example.jcarder.runtime.util.logging.AppendableHandler;
import ch.usi.dag.disl.example.jcarder.runtime.util.logging.Handler;
import ch.usi.dag.disl.example.jcarder.runtime.EventListener;
import ch.usi.dag.disl.example.jcarder.runtime.StaticEventListener;
import ch.usi.dag.disl.example.jcarder.runtime.util.logging.Logger;




public class JCarderAnalysis {
	private static final String DUMP_PROPERTY = "jcarder.dump";
	private static final String LOGLEVEL_PROPERTY = "jcarder.loglevel";
	private static final String LOG_FILENAME = "jcarder.log";
	private static final String OUTPUTDIR_PROPERTY = "jcarder.outputdir";
	private static Logger mLogger;
	private static File mOutputDir;
    private static PrintWriter mLogWriter;
    private static Logger.Level mLogLevel;
    private static final InstrumentConfig mConfig = new InstrumentConfig();
   
    static {
    	instanceOfJA = new JCarderAnalysis();
    	try {
    		
    		handleDumpProperty();
            handleLogLevelProperty();
            handleOutputDirProperty();
    		initLogger();
    		EventListener listener;
    		listener = EventListener.create(mLogger, mOutputDir);
    		StaticEventListener.setListener(listener);
    	}
    	catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    }

    private static void initLogger() {
        File logFile = new File(mOutputDir, LOG_FILENAME);
        if (logFile.exists()) {
            logFile.delete();
        }
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(logFile);
        } catch (IOException e) {
            System.err.println("Failed to open log file \""
                               + logFile + "\": " + e.getMessage());
            return;
        }
        mLogWriter = new PrintWriter(new BufferedWriter(fileWriter));
        AppendableHandler fileHandler = new AppendableHandler(mLogWriter);
        AppendableHandler consoleHandler =
            new AppendableHandler(System.err,
                                  Logger.Level.INFO,
                                  "{message}\n");

        Thread hook = new Thread() {
            public void run() {
                mLogWriter.flush();
            }
        };
        Runtime.getRuntime().addShutdownHook(hook);

        Collection<Handler> handlers = new ArrayList<Handler>();
        handlers.add(fileHandler);
        handlers.add(consoleHandler);
        mLogger = new Logger(handlers, mLogLevel);
    }
    private static void handleDumpProperty() {
        mConfig.setDumpClassFiles(Boolean.getBoolean(DUMP_PROPERTY));
    }

    private static void handleLogLevelProperty() {
        String logLevelValue = System.getProperty(LOGLEVEL_PROPERTY, "fine");
        Logger.Level logLevel = Logger.Level.fromString(logLevelValue);
        if (logLevel != null) {
            mLogLevel = logLevel;
        } else {
            System.err.print("Bad loglevel; should be one of ");
            System.err.println(Logger.Level.getEnumeration());
            System.err.println();
            System.exit(1);
        }
    }

    private static void handleOutputDirProperty() throws IOException {
        String property = System.getProperty(OUTPUTDIR_PROPERTY, ".");
        property = property.replace(
            "@TIME@", String.valueOf(System.currentTimeMillis()));
        mOutputDir = new File(property).getCanonicalFile();
        if (!mOutputDir.isDirectory()) {
            mOutputDir.mkdirs();
        }
    }
	private static final JCarderAnalysis instanceOfJA;
	
	public static JCarderAnalysis instanceOf() {
		return instanceOfJA;
	}
	
	public void onSynchronizedMethodEntry() {
		
	}
	
	public void onMonitorEnter(Object monitor, String classAndMethodName, boolean lockOnThis) {
		try {
			String lockOnObject = monitor.getClass().getName();
//			System.err.println("on monitor enter!");
			if (lockOnThis)
				lockOnObject = "this";
			StaticEventListener.beforeMonitorEnter(monitor, lockOnObject, classAndMethodName);
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public void onMonitorExit(Object monitor, String classAndMethodName, boolean lockOnThis) {
		try {
//			System.err.println("on monitor exit!");
			StaticEventListener.beforeMonitorExit(monitor, "2",  classAndMethodName);
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}
}
