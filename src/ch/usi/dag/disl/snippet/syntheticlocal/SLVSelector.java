package ch.usi.dag.disl.snippet.syntheticlocal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.usi.dag.disl.snippet.Snippet;

public class SLVSelector {

	public static List<SyntheticLocalVar> usedInSnippets(
			Set<Snippet> allSnippets,
			Map<String, SyntheticLocalVar> syntheticLoclaVars) {
		
		List<SyntheticLocalVar> result = new LinkedList<SyntheticLocalVar>();

		// go through all snippets and create list according to vars they uses
		
		for(Snippet snippet : allSnippets) {
			
			for(String slvName : snippet.getLocalVars()) {
				
				// If here is null, that means syntheticLoclaVars
				// does not contain some variable
				//  - but AnnotationParser should not allow this
				//  - error is in AnnotationParser
				result.add(syntheticLoclaVars.get(slvName));
			}
			
		}
		
		return result;
	}
}
