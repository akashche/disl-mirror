#ifndef _CLASSPARSER_H
#define _CLASSPARSER_H

#define CPCOUNT_OFFSET 8
#define CPTABLE_OFFSET 10

#define _CONSTANT_Class					7
#define _CONSTANT_Fieldref				9
#define _CONSTANT_Methodref				10
#define _CONSTANT_InterfaceMethodref	11
#define _CONSTANT_String				8
#define _CONSTANT_Integer				3
#define _CONSTANT_Float					4
#define _CONSTANT_Long					5
#define _CONSTANT_Double				6
#define _CONSTANT_NameAndType			12
#define _CONSTANT_Utf8					1
#define _CONSTANT_MethodType			16
#define _CONSTANT_MethodHandle			15
#define _CONSTANT_InvokeDynamic			20

// read short in big-endian bytes
static unsigned short read_short(const unsigned char * class_data,
		unsigned int index) {

	return (unsigned short) (((class_data[index] & 0xFF) << 8)
			| ((class_data[index + 1] & 0xFF)));
}

// construct constant pool from raw bytes, and returns 10 + cpsize
// constant pool table 'cp' should be allocate before passing to this function
static int get_constant_pool(const unsigned char * class_data,
		unsigned int * cp, unsigned short cpcount) {

	// cp item index start from 1
	unsigned int index = CPTABLE_OFFSET, count = 1;

	// converted from ASM
	while (count < cpcount) {

		cp[count] = index;

		switch (class_data[index]) {

		case _CONSTANT_Fieldref:
		case _CONSTANT_Methodref:
		case _CONSTANT_InterfaceMethodref:
		case _CONSTANT_Integer:
		case _CONSTANT_Float:
		case _CONSTANT_NameAndType:
		case _CONSTANT_InvokeDynamic:
			index += 5;
			break;

		case _CONSTANT_Long:
		case _CONSTANT_Double:
			index += 9;
			count++;
			break;

		case _CONSTANT_Utf8:
			index += 3 + read_short(class_data, index + 1);
			break;

		case _CONSTANT_MethodHandle:
			index += 4;
			break;

			// case _CONSTANT_Class:
			// case _CONSTANT_String:
			// case _CONSTANT_MethodType:
		default:
			index += 3;
			break;
		}

		count++;
	}

	return index;
}

#endif
