package ch.usi.dag.disl.dislclass.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.processor.Processor;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.exception.SnippetParserException;
import ch.usi.dag.disl.exception.StaticAnalysisException;

public class ClassParser {

	SnippetParser snippetParser = new SnippetParser();
	ProcessorParser processorParser = new ProcessorParser();
	
	public void parse(byte[] classAsBytes) throws SnippetParserException,
			ReflectionException, ScopeParserException, StaticAnalysisException {
		// TODO ! processor - use snippet parser and processor parser
		//  decide according to processor annotation at class
		snippetParser.parse(classAsBytes);
	}
	
	public LocalVars getAllLocalVars() {
		return snippetParser.getAllLocalVars();
	}
	
	public List<Snippet> getSnippets() {
		return snippetParser.getSnippets();
	}
	
	public Map<Class<?>, Processor> getProcessors() {
		// TODO ! processor
		return new HashMap<Class<?>, Processor>();
	}
}
