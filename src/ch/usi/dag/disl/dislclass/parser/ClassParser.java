package ch.usi.dag.disl.dislclass.parser;

import java.util.List;

import ch.usi.dag.disl.dislclass.processor.Processor;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.exception.SnippetParserException;
import ch.usi.dag.disl.exception.StaticAnalysisException;

public class ClassParser {

	public void parse(byte[] classAsBytes) throws SnippetParserException,
			ReflectionException, ScopeParserException, StaticAnalysisException {
		// TODO ! processor - use snippet parser and processor parser
		//  decide according to processor annotation at class
	}
	
	public List<Snippet> getSnippets() {
		// TODO ! processor
		return null;
	}
	
	public List<Processor> getProcessors() {
		// TODO ! processor
		return null;
	}
}
