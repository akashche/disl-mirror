# if there is Makefile.local file then include it
ifneq (, $(wildcard Makefile.local))
include Makefile.local
endif

BUILDDIR=../build

# Source lists
LIBNAME=dislagent

SOURCES=bytecode.c common.c jvmtiutil.c connection.c \
	connpool.c msgchannel.c network.c dislagent.c

HEADERS=$(wildcard *.h) codeflags.h

# Object files needed to create library
OBJECTS=$(SOURCES:%.c=%.o)

# Library name and options needed to build it
UNAME := $(shell uname)

ifeq ($(UNAME), Linux)
LIBRARY=lib$(LIBNAME).so
endif

ifeq ($(UNAME), Darwin)
LIBRARY=lib$(LIBNAME).jnilib
endif


# Building a shared library
LINK_SHARED=$(LINK.c) -shared -o $@

# GNU Compiler options needed to build it
COMMON_FLAGS=-fPIC -std=gnu99

# Options that help find errors
COMMON_FLAGS+= -W -Wall -Wextra -Wno-unused-parameter -lpthread

CFLAGS += $(COMMON_FLAGS)

CFLAGS += -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux


# add debugging output
ifneq ($(DEBUG),)
CFLAGS += -DDEBUG -g
else
CFLAGS += -DNDEBUG -O3
endif

build: $(LIBRARY)
	cp $(LIBRARY) $(BUILDDIR)

# Build native library
ifneq ($(WHOLE),)
$(LIBRARY): $(HEADERS) $(SOURCES)
	gcc $(CFLAGS) -DWHOLE -fwhole-program -flto -shared -o $@ $(SOURCES)
else
$(LIBRARY): $(HEADERS) $(OBJECTS)
	$(LINK_SHARED) $(OBJECTS) $(LIBRARIES)
endif


BPC_CLASSES := $(wildcard bypass/*/*.class)

bytecode.c: $(BPC_CLASSES) bin2class
	./bin2class \
		$(foreach BPC,$(BPC_CLASSES),$(BPC) $(subst bypass/,bpc_,$(BPC:/BypassCheck.class=))) \
		> $@

codeflags.h: codeflags
	./codeflags


# Cleanup the built bits
clean:
	rm -f $(LIBRARY) $(OBJECTS)

debug:
	$(MAKE) DEBUG=1

whole:
	$(MAKE) WHOLE=1

all: build
