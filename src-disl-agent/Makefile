# Import local variable definitions
-include Makefile.local

INCLUDE_DIR ?= ../src-jdk-include


ifeq ($(OS),Windows_NT)
    ifeq (,$(findstring /cygdrive/,$(PATH)))
        $(error Building on the Windows platform is only supported under Cygwin)
    endif

    HOST_ENV := Cygwin
else
    HOST_ENV := $(shell uname -s)
    HOST_CPU := $(shell uname -m)
endif


TARGET_ENV ?= $(HOST_ENV)
TARGET_CPU ?= $(HOST_CPU)

ifneq ($(HOST_ENV),$(TARGET_ENV))
    ifneq ($(TARGET_ENV),MinGW)
        $(error Cross-compiling is only supported for TARGET_ENV=MinGW)
    else
        $(warning The MinGW port for the Windows platform is experimental!)
    endif
endif

ifneq (,$(filter MinGW Cygwin,$(TARGET_ENV)))
    LIB_PREFIX :=
    LIB_SUFFIX := .dll
    JDK_TARGET := windows

    ifeq (MinGW,$(TARGET_ENV))
        CC := $(TARGET_CPU)-w64-mingw32-gcc

        CFLAGS += -DMINGW -mthreads
        CFLAGS_LD += -Wl,--kill-at
        LIBS += -lws2_32
    endif

else ifeq (Darwin,$(TARGET_ENV))
    LIB_PREFIX := lib
    LIB_SUFFIX := .jnilib
    JDK_TARGET := macosx

else ifeq (Linux,$(TARGET_ENV))
    LIB_PREFIX := lib
    LIB_SUFFIX := .so
    JDK_TARGET := linux
else
    $(error Target environment $(TARGET_ENV) is not supported)
endif


# Source and object files needed to create the library
SOURCES = bytecode.c common.c jvmtiutil.c connection.c \
	connpool.c msgchannel.c network.c dislagent.c

HEADERS = $(wildcard *.h) codeflags.h
OBJECTS = $(SOURCES:%.c=%.o)

LIBRARY = $(LIB_PREFIX)dislagent$(LIB_SUFFIX)


# Base options depending on the build
ifeq (,$(DEBUG))
    CFLAGS += -O3 -DNDEBUG -fomit-frame-pointer
else
    CFLAGS += -g3 -DDEBUG
endif

# Tell the linker to create a shared library.
CFLAGS_LD += -shared

#
# For non-MinGW targets, use position independent code
# and dynamically link to the pthread library. When
# building the library as a whole, tell GCC to build
# a shared library as well.
#
ifneq (MinGW,$(TARGET_ENV))
    CFLAGS += -fPIC
    LIBS += -lpthread
endif

# JNI and JVMTI includes
CFLAGS += -I$(INCLUDE_DIR) -I$(INCLUDE_DIR)/$(JDK_TARGET)

# Source code options
CFLAGS += -std=gnu99 -W -Wall -Wextra -Wno-unused-parameter

# Fix-up quirks before first target
-include Makefile.quirks


all: $(LIBRARY)


# Build native library
ifneq (,$(WHOLE))

CFLAGS += -DWHOLE -flto

$(LIBRARY): $(HEADERS) $(SOURCES)
	$(CC) $(CFLAGS) $(TARGET_ARCH) $(CFLAGS_LD) $(SOURCES) $(LIBS) $(OUTPUT_OPTION)

else

$(LIBRARY): $(HEADERS) $(OBJECTS)
	$(CC) $(CFLAGS) $(TARGET_ARCH) $(CFLAGS_LD) $(OBJECTS) $(LIBS) $(OUTPUT_OPTION)

endif


# Generated files

include Makefile.bypass

$(BYPASS_FILES):
	ant -f ../build.xml compile-disl-bypass

bytecode.c: $(BYPASS_FILES) Makefile.bypass bin2cdef
	./bin2cdef \
		$(foreach BPC,$(BYPASS_FILES),$(BPC) $(subst /$(BYPASS_PKG_PATH)/,_,$(subst $(BYPASS_DIR)/,,$(BPC:.class=)))) \
		> $@


codeflags.h:
	ant -f ../build.xml prepare-disl-agent-codeflags


# Cleanup targets

clean:
	-rm -f $(OBJECTS)

cleanall: clean
	-rm -f $(LIBRARY)

pristine: cleanall
	-rm -f Makefile.build
	-rm -f codeflags.h bytecode.c


# Convenience targets

debug:
	$(MAKE) DEBUG=1

whole:
	$(MAKE) WHOLE=1
