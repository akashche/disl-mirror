ifeq ($(OS),Windows_NT)
	UNAME := Windows

	ifneq (,$(findstring /cygdrive/,$(PATH)))
		UNAME := Cygwin
	endif
else
	UNAME := $(shell uname -s)
endif


.PHONY: all, clean


all: $(UNAME)

Linux:
	@./find.sh		

Darwin:
	@./find.sh		

Windows:
	@echo Windows not yet supported.

Cygwin:
	@echo "Cygwin not yet supported."


clean:
	@rm -f var

