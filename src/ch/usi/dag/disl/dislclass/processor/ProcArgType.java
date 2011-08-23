package ch.usi.dag.disl.dislclass.processor;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.DiSLFatalException;

public enum ProcArgType {

	BOOLEAN,
	BYTE,
	CHAR,
	DOUBLE,
	FLOAT,
	INT,
	LONG,
	SHORT,
	OBJECT,
	STRING;
	
	public Type getASMType() {
		
		switch(this) {
		case BOOLEAN:
			return Type.BOOLEAN_TYPE;
		case BYTE:
			return Type.BYTE_TYPE;
		case CHAR:
			return Type.CHAR_TYPE;
		case DOUBLE:
			return Type.DOUBLE_TYPE;
		case FLOAT:
			return Type.FLOAT_TYPE;
		case INT:
			return Type.INT_TYPE;
		case LONG:
			return Type.LONG_TYPE;
		case SHORT:
			return Type.SHORT_TYPE;
		case OBJECT:
			return Type.getType(Object.class);
		case STRING:
			return Type.getType(String.class);
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
		
		throw new DiSLFatalException("Conversion from " + type.getClassName() +
				" not defined");
	}
}
