package ch.usi.dag.disl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.cbloader.ClassByteLoader;
import ch.usi.dag.disl.classparser.ClassParser;
import ch.usi.dag.disl.exception.DiSLException;
import ch.usi.dag.disl.exception.DiSLIOException;
import ch.usi.dag.disl.exception.DynamicInfoException;
import ch.usi.dag.disl.exception.InitException;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.exclusion.ExclusionSet;
import ch.usi.dag.disl.guard.GuardHelper;
import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.processor.Proc;
import ch.usi.dag.disl.processor.generator.PIResolver;
import ch.usi.dag.disl.processor.generator.ProcGenerator;
import ch.usi.dag.disl.processor.generator.ProcInstance;
import ch.usi.dag.disl.processor.generator.ProcMethodInstance;
import ch.usi.dag.disl.scope.Scope;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.staticcontext.generator.SCGenerator;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.utilinstr.codemerger.CodeMerger;
import ch.usi.dag.disl.utilinstr.tlvinserter.TLVInserter;
import ch.usi.dag.disl.weaver.Weaver;

// TODO javadoc comment all
public class DiSL {

	public final String PROP_DEBUG = "debug";
	private final boolean debug = Boolean.getBoolean(PROP_DEBUG);
	
	// default is that exception handler is inserted
	// this is the reason for double negation in assignment
	private final String PROP_NO_EXCEPT_HANDLER = "disl.noexcepthandler";
	private final boolean exceptHandler = ! Boolean.getBoolean(PROP_NO_EXCEPT_HANDLER);
	
	private final boolean useDynamicBypass;
	
	private final Set<Scope> exclusionSet;
	
	private final List<Snippet> snippets;

	// this method should be called only once
	public DiSL(boolean useDynamicBypass) throws Exception {

		this.useDynamicBypass = useDynamicBypass;
		
		// report DiSL exception within our code
		try {

			List<InputStream> dislClasses = ClassByteLoader.loadDiSLClasses();
			
			if(dislClasses == null) {
				throw new InitException("Cannot load DiSL classes. Please set" +
						" the property " + ClassByteLoader.PROP_DISL_CLASSES
						+ " or supply jar with DiSL classes"
						+ " and proper manifest");
			}

			// prepare exclusion set
			exclusionSet = ExclusionSet.prepare();
			
			// *** parse disl classes ***
			// - create snippets
			// - create static context methods

			ClassParser parser = new ClassParser();

			for (InputStream classIS : dislClasses) {
				parser.parse(classIS);
			}

			// initialize processors
			Map<Type, Proc> processors = parser.getProcessors();
			for (Proc processor : processors.values()) {
				processor.init(parser.getAllLocalVars());
			}

			List<Snippet> parsedSnippets = parser.getSnippets();

			// initialize snippets
			for (Snippet snippet : parsedSnippets) {
				snippet.init(parser.getAllLocalVars(), processors,
						exceptHandler, useDynamicBypass);
			}
			
			// initialize snippets variable
			//  - this is set when everything is ok
			//  - it serves as initialization flag
			snippets = parsedSnippets;

			// TODO put checker here
			// like After should catch normal and abnormal execution
			// but if you are using After (AfterThrowing) with BasicBlockMarker
			// or InstructionMarker that doesn't throw exception, then it is
			// probably something, you don't want - so just warn the user
			// also it can warn about unknown opcodes if you let user to
			// specify this for InstructionMarker
		}
		catch (DiSLException e) {
			
			reportError(e);

			// signal error
			throw e;
		}
	}
	
	private void reportError(DiSLException e) {

		// error report for user (input) errors
		System.err.println("DiSL error: " + e.getMessage());
		Throwable cause = e.getCause();
		while (cause != null) {
			System.err.println("  Inner error: " + cause.getMessage());
			cause = cause.getCause();
		}
		
		if(debug) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Instruments a method in a class.
	 * 
	 * NOTE: This method changes the classNode argument
	 * 
	 * @param classNode
	 *            class that will be instrumented
	 * @param methodNode
	 *            method in the classNode argument, that will be instrumented
	 * @param staticContextInstances
	 *
	 */
	private boolean instrumentMethod(ClassNode classNode, MethodNode methodNode)
			throws ReflectionException, StaticContextGenException,
			ProcessorException, DynamicInfoException {

		// skip abstract methods
		if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0) {
			return false;
		}
		
		// skip native methods
		if ((methodNode.access & Opcodes.ACC_NATIVE) != 0) {
			return false;
		}

		String className = classNode.name;
		String methodName = methodNode.name;
		String methodDesc = methodNode.desc;
		
		// evaluate exclusions
		for (Scope exclScope : exclusionSet) {

			if (exclScope.matches(className, methodName, methodDesc)) {

				if (debug) {
					System.out.println("Excluding method: " + className
							+ Constants.CLASS_DELIM + methodName + "("
							+ methodDesc + ")");
				}

				return false;
			}
		}
		
		// *** match snippet scope ***

		List<Snippet> matchedSnippets = new LinkedList<Snippet>();

		for (Snippet snippet : snippets) {

			// snippet matching
			if (snippet.getScope().matches(className, methodName, methodDesc)) {
				matchedSnippets.add(snippet);
			}
		}

		// if there is nothing to instrument -> quit
		// just to be faster out
		if (matchedSnippets.isEmpty()) {
			return false;
		}

		// *** create shadows ***

		// shadows maped to snippets - for viewing
		Map<Snippet, List<Shadow>> snippetMarkings =
			new HashMap<Snippet, List<Shadow>>();

		for (Snippet snippet : matchedSnippets) {

			// marking
			List<Shadow> shadows = 
					snippet.getMarker().mark(classNode, methodNode, snippet);

			// select shadows according to snippet guard
			List<Shadow> selectedShadows =
				selectShadowsWithGuard(snippet.getGuard(), shadows);
			
			// add to map
			if(! selectedShadows.isEmpty()) {
				snippetMarkings.put(snippet, selectedShadows);
			}
		}
		
		// *** compute static info ***

		// prepares SCGenerator class (computes static context)
		SCGenerator staticInfo = new SCGenerator(snippetMarkings);

		// *** used synthetic local vars in snippets ***
		// weaver needs list of synthetic locals that are actively used in
		// selected (matched) snippets

		Set<SyntheticLocalVar> usedSLVs = new HashSet<SyntheticLocalVar>();
		for (Snippet snippet : snippetMarkings.keySet()) {
			usedSLVs.addAll(snippet.getCode().getReferencedSLVs());
		}

		// *** prepare processors ***

		PIResolver piResolver = new ProcGenerator().compute(snippetMarkings);

		// *** used synthetic local vars in processors ***
		
		// include SLVs from processor methods into usedSLV
		for(ProcInstance pi : piResolver.getAllProcInstances()) {
			
			for(ProcMethodInstance pmi : pi.getMethods()) {
				
				usedSLVs.addAll(pmi.getCode().getReferencedSLVs());
			}
		}

		// *** viewing ***

		Weaver.instrument(classNode, methodNode, snippetMarkings,
				new LinkedList<SyntheticLocalVar>(usedSLVs), staticInfo,
				piResolver);

		if(debug) {
			System.out.println("Instumenting method: " + className
					+ Constants.CLASS_DELIM + methodName + "(" + methodDesc
					+ ")");
		}
		
		return true;
	}

	private List<Shadow> selectShadowsWithGuard(Method guard,
			List<Shadow> marking) {
		
		if(guard == null) {
			return marking;
		}

		List<Shadow> selectedMarking = new LinkedList<Shadow>();

		// check guard for each shadow
		for(Shadow shadow : marking) {

			if(GuardHelper.guardApplicable(guard, shadow)) {

				selectedMarking.add(shadow);
			}
		}
		
		return selectedMarking;
	}

	private static class InstrumentedClass {
		
		private ClassNode classNode;
		private Set<String> changedMethods;
		
		public InstrumentedClass(ClassNode classNode,
				Set<String> changedMethods) {
			super();
			this.classNode = classNode;
			this.changedMethods = changedMethods;
		}

		public ClassNode getClassNode() {
			return classNode;
		}

		public Set<String> getChangedMethods() {
			return changedMethods;
		}
	}
	
	// this method is thread safe after initialization
	private InstrumentedClass instrument(ClassNode classNode)
			throws DiSLException {

		// report DiSL exception within our code
		try {

			// NOTE that class can be changed without changing any method
			//  - adding thread local fields
			boolean classChanged = false;

			// track changed methods for code merging
			Set<String> changedMethods = new HashSet<String>();

			// instrument all methods in a class
			for (MethodNode methodNode : classNode.methods) {

				boolean methodChanged = instrumentMethod(classNode, methodNode);
				
				// add method to the set of changed methods
				if(methodChanged) {
					changedMethods.add(methodNode.name + methodNode.desc);
					classChanged = true;
				}
			}
			
			// instrument thread local fields
			if (Type.getInternalName(Thread.class).equals(classNode.name)) {
				
				Set<ThreadLocalVar> insertTLVs = new HashSet<ThreadLocalVar>();
				
				// dynamic bypass
				if(useDynamicBypass) {
					
					// prepare dynamic bypass thread local variable
					ThreadLocalVar tlv = new ThreadLocalVar(null, "bypass",
							Type.getType(boolean.class), false);
					tlv.setDefaultValue(0);
					insertTLVs.add(tlv);
				}
				
				// get all thread locals in snippets
				for(Snippet snippet : snippets) {
					insertTLVs.addAll(snippet.getCode().getReferencedTLVs());
				}

				if(! insertTLVs.isEmpty()) {
				
					// instrument fields
					ClassNode cnWithFields = new ClassNode(Opcodes.ASM4);
					classNode.accept(new TLVInserter(cnWithFields, insertTLVs));
	
					// replace original code with instrumented one
					classNode = cnWithFields;
					classChanged = true;
				}
			}
			
			// we have changed some methods
			if(classChanged) {
				return new InstrumentedClass(classNode, changedMethods);
			}
			
			return null;
		}
		catch (DiSLException e) {
			
			reportError(e);

			// signal error
			throw e;
		}
	}

	public byte[] instrument(byte[] classAsBytes) throws DiSLException {
	
		// output bytes into the file
		try {
			
			if(debug) {
				String errFile = "err.class";
				FileOutputStream fos = new FileOutputStream(errFile);
				fos.write(classAsBytes);
				fos.close();
			}
		}
		catch (IOException e) {
			throw new DiSLIOException(e);
		}
		
		// create class reader
		ClassReader classReader = new ClassReader(classAsBytes);
		
		// AfterInitBodyMarker uses AdviceAdapter
		//  - classNode with API param is required by ASM 4.0 guidelines
		ClassNode classNode = new ClassNode(Opcodes.ASM4);

		// TODO jb - try without SKIP_DEBUG
		classReader.accept(classNode, ClassReader.SKIP_DEBUG
				| ClassReader.EXPAND_FRAMES);
		
		InstrumentedClass instrClass = instrument(classNode);
		
		if(instrClass == null) {
			return null;
		}
		
		ClassNode instrCN = instrClass.getClassNode();
		
		// if dynamic bypass is enabled use code merger
		if(useDynamicBypass) {
			
			ClassReader origCR = new ClassReader(classAsBytes);
			ClassNode origCN = new ClassNode();

			// TODO jb - try without SKIP_DEBUG
			origCR.accept(origCN, ClassReader.SKIP_DEBUG
					| ClassReader.EXPAND_FRAMES);

			// origCN and instrCN are destroyed during the merging
			instrCN = CodeMerger.mergeClasses(origCN, instrCN,
					instrClass.getChangedMethods());
		}
		
		// DiSL uses some instructions available only in higher versions
		final int REQUIRED_VERSION = Opcodes.V1_5;
		final int MAJOR_V_MASK = 0xFFFF;
		
		int requiredMajorVersion = REQUIRED_VERSION & MAJOR_V_MASK;
		int classMajorVersion = instrCN.version & MAJOR_V_MASK;
		
		if (classMajorVersion < requiredMajorVersion) {
			instrCN.version = REQUIRED_VERSION;
		}
		
		// Use compute frames for newer classes
		// It is required for >= 1.7
		final int COMPUTEFRAMES_VERSION = Opcodes.V1_6;
		ClassWriter cw = null;
		
		if(classMajorVersion >= COMPUTEFRAMES_VERSION) {
			cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		}
		else {
			cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		}

		// return as bytes
		instrCN.accept(cw);
		return cw.toByteArray();
	}
	
	public void terminate() {
		
	}
}
