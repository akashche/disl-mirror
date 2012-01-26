package ch.usi.dag.disl.exclusion;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import ch.usi.dag.disl.exception.DiSLIOException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.scope.Scope;
import ch.usi.dag.disl.scope.ScopeImpl;

public abstract class ExclusionSet {

	private static final String PROP_EXCLIST = "disl.exclusionList";
	private static final String excListPath = 
			System.getProperty(PROP_EXCLIST, null);
	
	public static Set<Scope> prepare() throws DiSLIOException,
			ScopeParserException {

		Set<Scope> exclSet = defaultExcludes();
		exclSet.addAll(instrumentationJar());
		exclSet.addAll(readExlusionList());
		
		return exclSet;
	}

	private static Set<Scope> defaultExcludes() throws ScopeParserException {
		
		Set<Scope> exclSet = new HashSet<Scope>();
		
		// DiSL agent classes
		exclSet.add(new ScopeImpl("ch.usi.dag.dislagent.*.*"));
		
		// dynamic bypass classes
		exclSet.add(new ScopeImpl("ch.usi.dag.disl.dynamicbypass.*.*"));
		
		// java instrument classes could cause troubles if instrumented
		exclSet.add(new ScopeImpl("sun.instrument.*.*"));
		
		// TODO jb - rally has to be excluded ?
		// finalize method in java.lang.Object can cause problems 
		exclSet.add(new ScopeImpl("java.lang.Object.finalize"));
		
		return exclSet;
	}
	
	private static Set<Scope> instrumentationJar() {
		
		Set<Scope> exclSet = new HashSet<Scope>();
		
		// TODO jb ! add classes from instrumentation jar
		
		return exclSet;
	}
	
	private static Set<Scope> readExlusionList() throws DiSLIOException,
			ScopeParserException {

		final String COMMENT_START = "#";
		
		try {
		
			Set<Scope> exclSet = new HashSet<Scope>();

			// if exclusion list path exits
			if(excListPath != null) {
			
				// read exclusion list line by line
				Scanner scanner = new Scanner(new FileInputStream(excListPath));
				while (scanner.hasNextLine()) {
					
					String line = scanner.nextLine();
					
					if(! line.startsWith(COMMENT_START)) {
						exclSet.add(new ScopeImpl(line));
					}
				}
	
				scanner.close();
			}

			return exclSet;
		
		} catch(FileNotFoundException e) {
			throw new DiSLIOException(e);
		}
	}
}
