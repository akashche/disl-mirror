package ch.usi.dag.disl.scope;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.util.Constants;

/**
 * Class filters methods based on class name, method name and method parameters.
 * 
 * Name of the method is specified as follows:
 * 
 * returnparam packagename.classname.methodname(parameters)
 *
 * returnparam is the returning parameter of the method. Specified with fully
 * qualified name of the class or basic type (defined as in java source code).
 * If the returnparam is missing, it matches all the return types.
 * 
 * packagename is the name of the package where the class containing the method
 * resides. The separator for inner packages is ".". Part of the package can be
 * substituted with "*" ("*" matches also "." in package name).
 * If the packagename is missing and the class is matched in all packages.
 * The default package can be matched with "[default]".
 * If the packagename is specified class name has to be specified also.
 * 
 * classname is the name of the class where the matched method resides. Part of
 * the classname can be substituted with "*".
 * If the classname is missing also the packagename and the separating "."
 * should not be specified. The missing classname is matching every class in
 * every package.
 * 
 * methodname is mandatory part of the method name. Part of the methodname can
 * be substituted with "*".
 * 
 * parameters are specified as returnparam and separated by ",". Part of the
 * parameter can be substituted with "*".
 * ".." can be supplied instead of last parameter specification and matches
 * all remaining method parameters.
 */
public class ScopeImpl implements Scope {

	private final String PARAM_BEGIN = "(";
	private final String PARAM_END = ")";
	private final String PARAM_DELIM = ",";
	private final String METHOD_DELIM = ".";
	private final String PARAM_MATCH_REST = "..";
	private final String DEFAULT_PKG = "[default]";
	
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

				// separate params and trim them again
				String[] params = paramsStr.split(PARAM_DELIM);
				for(String param : Arrays.asList(params)) {
					
					param = param.trim();
					
					if(param.isEmpty()) {
						throw new ScopeParserException("Scope \""
								+ scopeExpression
								+ "\" has bad parameter definition");
					}
					
					paramsWildCard.add(param);
				}
			}
			
			int pmrIndex = paramsWildCard.indexOf(PARAM_MATCH_REST);
			
			// if the index is valid, the first occurrence of PARAM_MATCH_REST
			// should be at the end of the parameters
			if(pmrIndex != -1 && pmrIndex != paramsWildCard.size() - 1) {
				throw new ScopeParserException("Scope \""
						+ scopeExpression
						+ "\" should have \"" + PARAM_MATCH_REST + "\""
						+ " only as last parameter");
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
				if(classWildCard.indexOf(Constants.PACKAGE_STD_DELIM) == -1
						&& ! classWildCard.startsWith(WildCard.WILDCARD_STR)) {
					classWildCard = WildCard.WILDCARD_STR + 
							Constants.PACKAGE_STD_DELIM + classWildCard;
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

	public boolean matches(String className, String methodName,
			String methodDesc) {
		
		// -- match class (with package) --

		// replace delimiters for matching
		className = className.replace(
				Constants.PACKAGE_INTERN_DELIM, Constants.PACKAGE_STD_DELIM);
		
		// if className has default package (nothing), add our default package
		// reasons:
		// 1) we can restrict scope on default package by putting our default
		//    package into scope
		// 2) default package would not be matched if no package was specified
		//    in the scope (because of substitution made)
		if(className.indexOf(Constants.PACKAGE_STD_DELIM) == -1) {
			className = DEFAULT_PKG + Constants.PACKAGE_STD_DELIM + className;
		}
		
		if(classWildCard != null
				&& ! WildCard.match(className, classWildCard)) {
			return false;
		}
		
		// -- match method name --
		
		if(methodWildCard != null
				&& ! WildCard.match(methodName, methodWildCard)) {
			return false;
		}
		
		// -- match parameters --
		
		if(paramsWildCard != null) {

			// get parameters and match one by one
			Type[] parameters = Type.getArgumentTypes(methodDesc);
			
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
				// works even if there is no additional parameter
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
			Type returnType = Type.getReturnType(methodDesc);
			String typeName = returnType.getClassName();
			
			if(! WildCard.match(typeName, returnWildCard)) {
				return false;
			}
		}
		
		return true;
	}

}
