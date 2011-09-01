package ch.usi.dag.disl.dislclass.parser;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.util.ReflectionHelper;

// package visible
public abstract class ParserHelper {

	public static Object getGuard(Type guardType) throws ReflectionException {
		
		if(guardType == null) {
			return null;
		}
		
		Class<?> guardClass = ReflectionHelper.resolveClass(guardType);

		return ReflectionHelper.createInstance(guardClass);
	}
	
	public static <T> T parseAnnotation(AnnotationNode annotation,
			T parsedDataObject) {

		try {
		
			if(annotation.values == null) {
				return parsedDataObject;
			}
				
			Iterator<?> it = annotation.values.iterator();
	
			while (it.hasNext()) {
	
				// get attribute name
				String name = (String) it.next();
	
				// find correct field
				Field attr = parsedDataObject.getClass().getField(name);
				
				if (attr == null) {
	
					throw new DiSLFatalException("Unknow attribute "
							+ name
							+ " in annotation "
							+ Type.getType(annotation.desc).toString()
							+ ". This may happen if annotation class is changed"
							+ "  but parser class is not.");
				}
	
				// set attribute value into the field
				attr.set(parsedDataObject, it.next());
			}
		
		} catch (Exception e) {
			throw new DiSLFatalException(
					"Reflection error wihle parsing annotation", e);
		}
		
		return parsedDataObject;
	}
}
