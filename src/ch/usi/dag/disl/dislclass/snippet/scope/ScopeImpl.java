package ch.usi.dag.disl.dislclass.snippet.scope;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.util.Constants;

public class ScopeImpl implements Scope {

	private final String PARAM_BEGIN = "(";
	private final String PARAM_END = ")";
	private final String PARAM_DELIM = ",";
	private final String METHOD_DELIM = ".";
	private final String PACKAGE_DELIM = ".";
	private final String PARAM_MATCH_REST = "..";
	
	private String classWildCard;
	private String methodWildCard;
	private String returnWildCard;
	private List<String> paramsWildCard;
	
	private int lastWhitespace(String where) {
		
		char[] whereCharArray = where.toCharArray();
		
		for (int i = whereCharArray.length - 1; i >= 0; --i) {
		    
			if (Character.isWhitespace(whereCharArray[i])) {
		       return i;
		    }
		}
		
		return -1;
	}
	
	// thx - http://stackoverflow.com/questions/4067809/how-to-check-space-in-string
	private boolean containsWhiteSpace(String toCheck) {
		
		for (char c : toCheck.toCharArray()) {
		    if (Character.isWhitespace(c)) {
		       return true;
		    }
		}
		
		return false;
	}
	
	public ScopeImpl(String scopeExpression) throws ScopeParserException {
		
		// -- parse the scope into parts - trim whitespace everywhere --

		// parse it from the end
		// its better because you can easier identify return type
		// otherwise you don't know if the first empty space doesn't mean
		// something else 
		
		String restOfExpr = scopeExpression;
		
		// -- method parameters --
		int paramBegin = restOfExpr.lastIndexOf(PARAM_BEGIN);
		if(paramBegin != -1) {
			
			// + 1 - don't include PARAM_BEGIN
			String paramsStr = restOfExpr.substring(paramBegin + 1);
			restOfExpr = restOfExpr.substring(0, paramBegin);
			
			// remove whitespace
			paramsStr = paramsStr.trim();
			
			// PARAM_END check
			if(! paramsStr.endsWith(PARAM_END)) {
				throw new ScopeParserException("Scope \"" + scopeExpression
						+ "\" should end with \"" + PARAM_END + "\"");
			}
			
			// remove PARAM_END
			int paramEnd = paramsStr.lastIndexOf(PARAM_END);
			paramsStr = paramsStr.substring(0, paramEnd);
			
			paramsWildCard = new LinkedList<String>();
			
			// test for emptiness
			if(! paramsStr.trim().isEmpty()) {

				// separate params ant trim them again
				String[] params = paramsStr.split(PARAM_DELIM);
				for(String param : Arrays.asList(params)) {
					
					param = param.trim();
					
					if(param.isEmpty()) {
						throw new ScopeParserException("Scope \""
								+ scopeExpression
								+ " has bad parameter definition");
					}
					
					paramsWildCard.add(param);
				}
			}
		}
		
		// -- method name --
		int methodDelim = restOfExpr.lastIndexOf(METHOD_DELIM);
		if(methodDelim != -1) {
			// + 1 - don't include METHOD_DELIM
			methodWildCard = restOfExpr.substring(methodDelim + 1);
			restOfExpr = restOfExpr.substring(0, methodDelim);
		}
		else {
			methodWildCard = restOfExpr;
			restOfExpr = null;
		}
		
		// remove whitespace
		methodWildCard = methodWildCard.trim();
		
		if(methodWildCard.isEmpty()) {
			throw new ScopeParserException("Scope \"" + scopeExpression
					+ "\" should have defined method at least as \"*\"");
		}
		
		// -- full class name --
		if(restOfExpr != null) {

			// remove whitespace
			restOfExpr = restOfExpr.trim();
			
			if(! restOfExpr.isEmpty()) {

				int classDelim = lastWhitespace(restOfExpr);
				if(classDelim != -1) {
					// + 1 - don't include whitespace
					classWildCard = restOfExpr.substring(classDelim + 1);
					restOfExpr = restOfExpr.substring(0, classDelim);
				}
				else {
					classWildCard = restOfExpr;
					restOfExpr = null;
				}
				
				// if there is no package specified - allow any
				if(classWildCard.indexOf(PACKAGE_DELIM) == -1
						&& ! classWildCard.startsWith(WildCard.WILDCARD_STR)) {
					classWildCard = 
						WildCard.WILDCARD_STR + PACKAGE_DELIM + classWildCard;
				}
			}
		}

		// -- method return type --
		
		// remove whitespace for next parsing
		if(restOfExpr != null) {
		
			restOfExpr = restOfExpr.trim();
		
			if(! restOfExpr.isEmpty()) {
	
				// no whitespace in restOfExpr
				if(containsWhiteSpace(restOfExpr)) {
					throw new ScopeParserException("Cannot parse scope \""
							+ scopeExpression + "\"");
				}
				
				returnWildCard = restOfExpr;
			}
		}
	}

	public boolean matches(String className, MethodNode method) {
		
		// -- match class --
		
		// replace delimiters for matching
		className = className.replace(
				Constants.PACKAGE_ASM_DELIM, Constants.PACKAGE_STD_DELIM);
		
		if(classWildCard != null
				&& ! WildCard.match(className, classWildCard)) {
			return false;
		}
		
		// -- match method name --
		
		if(methodWildCard != null
				&& ! WildCard.match(method.name, methodWildCard)) {
			return false;
		}
		
		// -- match parameters --
		
		if(paramsWildCard != null) {

			// get parameters and match one by one
			Type[] parameters = Type.getArgumentTypes(method.desc);
			
			// get last param
			String lastParamWC = null;
			if(! paramsWildCard.isEmpty()) {
				lastParamWC = paramsWildCard.get(paramsWildCard.size() - 1);
			}
			
			// if the last param is not PARAM_MATCH_REST then test for equal size
			if(! PARAM_MATCH_REST.equals(lastParamWC) &&
					parameters.length != paramsWildCard.size()) {
				return false;
			}
			
			for(int i = 0; i < parameters.length; ++i) {
				
				String paramWC = paramsWildCard.get(i);
				
				// if there is PARAM_MATCH_REST then stop
				if(paramWC.equals(PARAM_MATCH_REST)) {
					break;
				}
				
				String typeName = parameters[i].getClassName();
						
				if(! WildCard.match(typeName, paramWC)) {
					return false;
				}
			}
		}
		
		// -- match return type --
		
		if(returnWildCard != null) {
			Type returnType = Type.getReturnType(method.desc);
			String typeName = returnType.getClassName();
			
			if(! WildCard.match(typeName, returnWildCard)) {
				return false;
			}
		}
		
		return true;
	}

}
