#ifndef _BUFFER_H
#define	_BUFFER_H

#include <stdlib.h>
#include <string.h>

#include "../src-agent-c/jvmtihelper.h"

// initial buffer size
static const size_t INIT_BUFF_SIZE = 512;
// max limit buffer size
static const size_t MAX_BUFF_SIZE = 8192;

typedef struct {
	unsigned char * buff;
	size_t occupied;
	size_t capacity;
	volatile int available;
} buffer;

// ******************* Buffer routines *******************

void buffer_alloc(buffer * b) {

	b->buff = (unsigned char *) malloc(INIT_BUFF_SIZE);
	b->capacity = INIT_BUFF_SIZE;
	b->occupied = 0;
	b->available = TRUE;
}

void buffer_free(buffer * b) {

	free(b->buff);
	b->buff = NULL;
	b->capacity = 0;
	b->occupied = 0;
	b->available = TRUE;
}

void buffer_fill(buffer * b, const void * data, size_t data_length) {

	// not enough free space - extend buffer
	if(b->capacity - b->occupied < data_length) {

		unsigned char * old_buff = b->buff;

		// alloc as much as needed to be able to insert data
		size_t new_capacity = 2 * b->capacity;
		while(new_capacity - b->occupied < data_length) {
			new_capacity *= 2;
		}

		b->buff = (unsigned char *) malloc(new_capacity);

		memcpy(b->buff, old_buff, b->occupied);

		free(old_buff);
	}

	memcpy(b->buff + b->occupied, data, data_length);
	b->occupied += data_length;
}

void buffer_clean(buffer * b) {

	// if capacity is higher then limit "reset" buffer
	// should keep memory consumption in limits
	if(b->capacity > MAX_BUFF_SIZE) {

		buffer_free(b);
		buffer_alloc(b);
	}

	b->occupied = 0;
}

#endif	/* _BUFFER_H */
