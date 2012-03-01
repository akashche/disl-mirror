package ch.usi.dag.dislreserver.tree;

import java.util.ArrayList;
import java.util.List;

public class ClassRef {

	public static final int CLASS = 7;
	public static final int FIELD = 9;
	public static final int METH = 10;
	public static final int IMETH = 11;
	public static final int STR = 8;
	public static final int INT = 3;
	public static final int FLOAT = 4;
	public static final int LONG = 5;
	public static final int DOUBLE = 6;
	public static final int NAME_TYPE = 12;
	public static final int UTF8 = 1;
	public static final int MTYPE = 16;
	public static final int HANDLE = 15;
	public static final int INDY = 18;
	public static final int HANDLE_BASE = 20;

	private String name;
	private String superName;

	private List<InterfaceRef> interfaces;
	private List<MethodRef> methods;

	public ClassRef(byte[] b) {

		int cpcount = readUnsignedShort(b, 8);
		int[] items = new int[cpcount];

		int offset = 10;

		for (int i = 1; i < cpcount; ++i) {
			items[i] = offset + 1;
			int size;
			switch (b[offset]) {
			case FIELD:
			case METH:
			case IMETH:
			case INT:
			case FLOAT:
			case NAME_TYPE:
			case INDY:
				size = 5;
				break;
			case LONG:
			case DOUBLE:
				size = 9;
				++i;
				break;
			case UTF8:
				size = 3 + readUnsignedShort(b, offset + 1);
				break;
			case HANDLE:
				size = 4;
				break;
			// case CLASS:
			// case STR:
			// case MTYPE
			default:
				size = 3;
				break;
			}

			offset += size;
		}

		name = readClass(b, items, readUnsignedShort(b, offset + 2));
		superName = readClass(b, items, readUnsignedShort(b, offset + 4));

		int interfaces_count = readUnsignedShort(b, offset + 6);

		interfaces = new ArrayList<InterfaceRef>(interfaces_count);

		for (offset += 8; interfaces_count > 0; interfaces_count--, offset += 2) {

			interfaces.add(new InterfaceRef(readClass(b, items,
					readUnsignedShort(b, offset))));
		}

		// skip fields
		int fields_count = readUnsignedShort(b, offset);
		offset += 2;

		for (; fields_count > 0; fields_count--) {
			int c = readUnsignedShort(b, offset + 6);
			offset += 8;
			for (; c > 0; --c) {
				offset += 6 + readInt(b, offset + 2);
			}
		}

		int methods_count = readUnsignedShort(b, offset);
		offset += 2;

		methods = new ArrayList<MethodRef>(methods_count);

		for (; methods_count > 0; methods_count--) {

			String method_name = readUTF8(b,
					items[readUnsignedShort(b, offset + 2)]);
			String method_desc = readUTF8(b,
					items[readUnsignedShort(b, offset + 4)]);
			methods.add(new MethodRef(method_name, method_desc));

			int c = readUnsignedShort(b, offset + 6);
			offset += 8;
			for (; c > 0; --c) {
				offset += 6 + readInt(b, offset + 2);
			}
		}
	}

	private static int readUnsignedShort(byte[] b, final int offset) {
		return ((b[offset] & 0xFF) << 8) | (b[offset + 1] & 0xFF);
	}

	private static int readInt(byte[] b, final int offset) {
		return ((b[offset] & 0xFF) << 24) | ((b[offset + 1] & 0xFF) << 16)
				| ((b[offset + 2] & 0xFF) << 8) | (b[offset + 3] & 0xFF);
	}

	private static String readClass(byte[] b, int[] items, final int index) {

		if (index == 0) {
			return null;
		}

		int name_idx = readUnsignedShort(b, items[index]);
		int name_off = items[name_idx];
		return readUTF8(b, name_off);
	}

	private static String readUTF8(byte[] b, int offset) {

		int length = readUnsignedShort(b, offset);
		char[] name = new char[length];

		offset += 2;

		int end = offset + length;
		int strLen = 0;
		int c;
		int st = 0;
		char cc = 0;

		while (offset < end) {
			c = b[offset++];
			switch (st) {
			case 0:
				c = c & 0xFF;
				if (c < 0x80) { // 0xxxxxxx
					name[strLen++] = (char) c;
				} else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
					cc = (char) (c & 0x1F);
					st = 1;
				} else { // 1110 xxxx 10xx xxxx 10xx xxxx
					cc = (char) (c & 0x0F);
					st = 2;
				}
				break;

			case 1: // byte 2 of 2-byte char or byte 3 of 3-byte char
				name[strLen++] = (char) ((cc << 6) | (c & 0x3F));
				st = 0;
				break;

			case 2: // byte 2 of 3-byte char
				cc = (char) ((cc << 6) | (c & 0x3F));
				st = 1;
				break;
			}
		}

		return new String(name, 0, strLen);
	}

	public String getName() {
		return name;
	}

	public String getSuperName() {
		return superName;
	}

	public List<InterfaceRef> getInterfaces() {
		return interfaces;
	}

	public List<MethodRef> getMethods() {
		return methods;
	}

}
