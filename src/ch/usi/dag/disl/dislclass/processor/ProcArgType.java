package ch.usi.dag.disl.dislclass.processor;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.DiSLFatalException;

public enum ProcArgType {

	BOOLEAN,
	BOOLEAN_ARRAY,
	BYTE,
	BYTE_ARRAY,
	CHAR,
	CHAR_ARRAY,
	DOUBLE,
	DOUBLE_ARRAY,
	FLOAT,
	FLOAT_ARRAY,
	INT,
	INT_ARRAY,
	LONG,
	LONG_ARRAY,
	SHORT,
	SHORT_ARRAY,
	OBJECT,
	OBJECT_ARRAY,
	STRING,
	STRING_ARRAY;
	
	final static String ARRAY_SUFFIX = "_ARRAY";
	
	public Type getASMType() {
		
		switch(this) {
		case BOOLEAN:
			return Type.BOOLEAN_TYPE;
		case BOOLEAN_ARRAY:
			return Type.getType(boolean[].class);
		case BYTE:
			return Type.BYTE_TYPE;
		case BYTE_ARRAY:
			return Type.getType(byte[].class);
		case CHAR:
			return Type.CHAR_TYPE;
		case CHAR_ARRAY:
			return Type.getType(char[].class);
		case DOUBLE:
			return Type.DOUBLE_TYPE;
		case DOUBLE_ARRAY:
			return Type.getType(double[].class);
		case FLOAT:
			return Type.FLOAT_TYPE;
		case FLOAT_ARRAY:
			return Type.getType(float[].class);
		case INT:
			return Type.INT_TYPE;
		case INT_ARRAY:
			return Type.getType(int[].class);
		case LONG:
			return Type.LONG_TYPE;
		case LONG_ARRAY:
			return Type.getType(long[].class);
		case SHORT:
			return Type.SHORT_TYPE;
		case SHORT_ARRAY:
			return Type.getType(short[].class);
		case OBJECT:
			return Type.getType(Object.class);
		case OBJECT_ARRAY:
			return Type.getType(Object[].class);
		case STRING:
			return Type.getType(String.class);
		case STRING_ARRAY:
			return Type.getType(String[].class);
		default:
			throw new DiSLFatalException("Conversion from "
					+ this.getClass().toString() + " to asm Type not defined");
		}
	}
	
	public static ProcArgType valueOf(Type type) {
		
		if(type == null) {
			throw new DiSLFatalException("Conversion from null not defined");
		}
		
		if(Type.BOOLEAN_TYPE.equals(type)) {
			return BOOLEAN;
		}
		
		if(Type.BYTE_TYPE.equals(type)) {
			return BYTE;
		}
		
		if(Type.CHAR_TYPE.equals(type)) {
			return CHAR;
		}
		
		if(Type.DOUBLE_TYPE.equals(type)) {
			return DOUBLE;
		}
		
		if(Type.FLOAT_TYPE.equals(type)) {
			return FLOAT;
		}
		
		if(Type.INT_TYPE.equals(type)) {
			return INT;
		}
		
		if(Type.LONG_TYPE.equals(type)) {
			return LONG;
		}
		
		if(Type.SHORT_TYPE.equals(type)) {
			return SHORT;
		}
		
		if(Type.getType(String.class).equals(type)) {
			return STRING;
		}
		
		if(Type.OBJECT == type.getSort()) {
			return OBJECT;
		}
		
		if(Type.ARRAY == type.getSort()) {
			
			// TRICK :)
			// get element type value
			ProcArgType elementType = valueOf(type.getElementType());
			// use element type and valueof(String) function to get the result 
			ProcArgType arrayType = 
				valueOf(elementType.toString() + ARRAY_SUFFIX);
			return arrayType;
		}
		
		throw new DiSLFatalException("Conversion from " + type.getClassName() +
				" not defined");
	}
}
