#!/usr/bin/env python

import argparse
import datetime
import filecmp
import glob
import itertools
import os
import platform
import shutil
import sys
import time
import subprocess


######################################################################
# CONSTANTS
######################################################################
DISL		= os.path.realpath("./disl.py")
TESTS_DIR	= os.path.realpath("./src-test/ch/usi/dag/disl/test")
RUN_DIR		= "test/"+str(int(time.time()))
TIMEOUT		= 60


######################################################################
# SETTINGS
######################################################################
class Settings:
	verbose	= False

######################################################################
# PROCESS_WAIT
######################################################################
def process_wait(subprocess, timeout):
	start = time.time()
	
	while (time.time() - start) < timeout:
		if subprocess.poll() is not None:
			return True
	
	return False
		

######################################################################
# CLOCK
######################################################################
class Clock:
	def __init__(self):
		self.__start__ = None
		self.__end__ = None
		return

	def start(self):
		self.__start__ = time.clock()
		return

	def stop(self):
		self.__end__ = time.clock()
		return

	def time(self):
		if self.__start__ is None:
			return None
		if self.__end__ is None:
			return None
		return str(int((self.__end__ - self.__start__)*1000))+"ms"


######################################################################
# COMMAND
######################################################################
class Command:
	def __init__(
			self, 
			name,
			cmd = None,
			cwd = ".",
			dir = ".",
			returncode = None,
			stdout = None,	
			stderr = None,
			compare = None):
		self.fork = None
		self.name = name
		self.cmd = cmd
		self.cwd = cwd
		self.dir = dir
		self.returncode = returncode
		self.stdout = stdout
		self.stderr = stderr
		self.compare = compare

		self.outf = os.path.realpath(dir+"/"+name+".out")
		self.errf = os.path.realpath(dir+"/"+name+".err")
		self.pidf = os.path.realpath(dir+"/"+name+".pid")
		self.retf = os.path.realpath(dir+"/"+name+".ret")
		
		self.nofailure = True
		self.errorlist = list()
		self.messagelist = list()

	def run(self):
		of = open(self.outf, "w")
		ef = open(self.errf, "w")

		with open(self.pidf, "w") as f:
			self.fork = subprocess.Popen(
					self.cmd,
					cwd=self.cwd,
					stdout=of,
					stderr=ef,
					shell=False)
			f.write(str(self.fork.pid))
		return

	def write_returncode_(self):
		with open(self.retf, "w") as f:
			f.write(str(self.fork.returncode))
		return

	def check_returncode_(self):
		if Settings.verbose:
			self.messagelist.append("checking returncode")
		if self.returncode is not None:
			if self.returncode != self.fork.returncode:
				self.nofailure = False
				self.errorlist.append("error: returncode does not match")
		
	def check_stdout_(self):
		if self.stdout is not None:
			if Settings.verbose:
				self.messagelist.append("checking stdout")
			if not os.path.isfile(self.stdout):
				self.nofailure = False
				self.errorlist.append("error: stdout file does not exists")
			if not filecmp.cmp(self.stdout, self.outf):
				self.nofailure = False
				self.errorlist.append("error: stdout and stdout file does not match")
	
	def check_stderr_(self):
		if self.stderr is not None:
			if Settings.verbose:
				self.messagelist.append("checking stderr")
			if not os.path.isfile(self.stderr):
				self.nofailure = False
				self.errorlist.append("error: stderr file does not exists")
			if not filecmp.cmp(self.stderr, self.errf):
				self.nofailure = False
				self.errorlist.append("error: stderr and stderr file does not match")
							
	def check_compare_(self):
		if self.compare is None:
			return

		for pair in self.compare:
			if Settings.verbose:
				self.messagelist.append("checking compare files")
			if not os.path.isfile(pair[0]):
				self.nofailure = False
				self.errorlist.append("error: not exists " +pair[0])
			if not os.path.isfile(pair[1]):
				self.nofailure = False
				self.errorlist.append("error: not exists " +pair[1])
			
			emsg = "error: compare failed on " +pair[0]+ " ~ " +pair[1]
			if os.path.isfile(pair[0]) and os.path.isfile(pair[1]):
				if not filecmp.cmp(pair[0], pair[1]):
					self.nofailure = False
					self.errorlist.append(emsg)
	
	def report_out_err_(self):
		if not self.nofailure:
			self.errorlist.append("returncode: " +str(self.fork.returncode))
			self.errorlist.append("stdout:")
			with open(self.outf) as f:
				for l in f:
					self.errorlist.append(l[:-1])
			self.errorlist.append("stderr:")
			with open(self.errf) as f:
				for l in f:
					self.errorlist.append(l[:-1])

	def __print_messagelist__(self):
		for message in self.messagelist:
			print "[           ] " + message
	
	# returns (bool, str[]) - (nofailure, errorlist)
	def wait(self, timeout):
		if self.fork is None:
			raise RuntimeError()
		
		if not process_wait(self.fork, timeout):
			self.nofailure = False
			self.errorlist.append("error: timeout")
			self.fork.kill()
			return (self.nofailure, self.errorlist)
	
		self.write_returncode_()
		self.check_returncode_()
		self.check_stdout_()
		self.check_stderr_()
		self.check_compare_()
		self.report_out_err_()
		self.__print_messagelist__()

		return (self.nofailure, self.errorlist)


######################################################################
# PROPERTIES
######################################################################
def read_properties(file_name):
	compare = list()
	source = None
	
	if not os.path.isfile(file_name):
		print "[           ] properties not found"
		return compare
		
	with open(file_name) as file:
		for line in file:
			if source is None:
				source = line[:-1]	
			else:
				compare.append((source, line[:-1]))
				source = None
	if source is not None:
		raise RuntimeError("compare list is odd")
	return compare


######################################################################
# TESTS REGISTER
######################################################################
class Test:
	def __init__(self, name, dir):
		self.__name__ = name
		self.__dir__ = dir
		return

	def name(self):
		return self.__name__
	
	def dir(self):
		return self.__dir__


class TestsRegister:
	def __init__(self):
		self.__tests__ = list()
		return
	
	def add_test(self, test):
		self.__tests__.append(test)
		return

	def tests(self):
		return iter(self.__tests__)


######################################################################
# TESTS REPORTER
######################################################################
class TestReporter:
	def __init__(self, test):
		self.test = test
		self.clock = Clock()
		self.nofailure = True
		return

	def start(self):
		self.clock.start()
		print "[-----------]"
		print "[  Run      ] Test : " +self.test.name()
		return

	def __print_messagelist__(self, messagelist):
		for message in messagelist:
			print "[           ] " + message
		
	def __print_errorlist__(self, errorlist):
		for error in errorlist:
			print error

	def success(self, messagelist=[]):
		self.clock.stop()
		self.__print_messagelist__(messagelist)
		print "[       Ok  ] in " +self.clock.time()
		return

	def failure(self, errorlist=[], messagelist=[]):
		self.clock.stop()
		self.nofailure = False 
		self.__print_messagelist__(messagelist)
		print "[  FAILURE  ] in " +self.clock.time()
		self.__print_errorlist__(errorlist)
		return


class TestsReporter:
	def __init__(self, tests_register):
		self.tests_register = tests_register
		self.test_reporter_list = list()
		self.clock = Clock()
		self.nofailure = True
		self.errorlist = list()	
		return
	
	def tests_start(self):
		self.clock.start()
		print "[===========] Tests started."
		return

	def new_reporter(self, test):
		test_reporter = TestReporter(test)
		self.test_reporter_list.append(test_reporter)
		return test_reporter

	def tests_failure(self, errorlist=[]):
		self.clock.stop()
		self.nofailure = False
		self.errorlist = errorlist 
		print "[===========] Tests failed."
		for error in errorlist:
			print error
		return

	def __success_count__(self):
		count = 0
		for tr in self.test_reporter_list:
			if tr.nofailure:
				count += 1
		return count

	def __failure_count__(self):
		count = 0
		for tr in self.test_reporter_list:
			if not tr.nofailure:
				count += 1
		return count

	def tests_end(self):
		self.clock.stop()
		success = self.__success_count__()
		failure = self.__failure_count__()
		if (
				self.nofailure and
				failure == 0):
			print "[-----------]"
			print "[===========]"
			print "[= SUCCESS =]" 
			print "[===========] " +str(success)+ " tests successfull. "
			print "[===========] in " +self.clock.time()
		else:
			print "[-----------]"
			print "[===========]"
			print "[= FAILURE =]"
			print "[===========] " +str(success)+ " tests successfull. "
			print "[===========] " +str(failure)+ " tests failed. "
			print "[===========] in " +self.clock.time()	


######################################################################
# TESTS RUNNER
######################################################################
class TestRunner:
	def __init__(self, test, test_reporter):
		self.test = test
		self.test_reporter = test_reporter
		self.wd = os.path.realpath(RUN_DIR+"/"+self.test.name())
		return

	def prepare_(self):
		cmd = [
			"ant",
			"unsafe-package-test",
			"-Dtest.name="+self.test.name()]
		
		command = Command(
				name="init",
				cmd=cmd,
				cwd=".",
				dir=self.wd,
				returncode=0)
		command.run()
		(nofailure, errorlist) = command.wait(TIMEOUT)	
		
		if not nofailure:
			self.test_reporter.failure(errorlist)
			return False
		return True

	def perform_(self):
		disl_dir	= os.path.realpath(".") 
		instr 		= os.path.realpath("./build/disl-instr-"+self.test.name()+".jar")
		cp 		= os.path.realpath("./bin/")
		target 		= "ch.usi.dag.disl.test."+self.test.name()+".TargetClass"

		cmd = [
			DISL,
			"-d="+disl_dir,
			"-t="+self.wd, 
			"--",
			instr, "-cp", cp, target]

		stdout = None
		stderr = None
	
		if os.path.isfile(self.test.dir()+"/test.out"):
			stdout = self.test.dir()+"/test.out"
		if os.path.isfile(self.test.dir()+"/test.err"):
			stderr = self.test.dir()+"/test.err"

		command = Command(
				name="test",
				cmd=cmd,
				cwd=".",
				dir=self.wd,
				stdout=stdout,
				stderr=stderr,
				returncode=0)
		command.run()
		(nofailure, errorlist) = command.wait(TIMEOUT)	
		
		if not nofailure:
			self.test_reporter.failure(errorlist)
			return False
		return True
	
	def cleanup_(self):
		return True

	def run(self):
		self.test_reporter.start()
		os.mkdir(self.wd)
		
		if not self.prepare_():
			self.cleanup_()
			return False
		if not self.perform_():
			self.cleanup_()
			return False
		if not self.cleanup_():
			return False
		
		self.test_reporter.success()
		return True


class TestsRunner:
	def __init__(self, tests_register, tests_reporter):
		self.tests_register = tests_register
		self.tests_reporter = tests_reporter
		return

	def prepare_(self):
		command = Command(
				name="init",
				cmd=["ant"],
				cwd=".",
				dir=RUN_DIR,
				returncode=0)
		command.run()
		(nofailure, errorlist) = command.wait(3*TIMEOUT)
		
		if not nofailure:
			self.tests_reporter.tests_failure(errorlist)
			return False
		return True

	def perform_(self):
		for test in self.tests_register.tests():
			test_reporter = self.tests_reporter.new_reporter(test)
			test_runner = TestRunner(test, test_reporter)
			test_runner.run()

	def cleanup_(self):
		return True

	def run_(self):
		if not self.prepare_():
			self.cleanup_()
			return False
		if not self.perform_():
			self.cleanup_()
			return False
		if not self.cleanup_():
			return False
		return True		

	def run(self):
		self.tests_reporter.tests_start()
		result = self.run_()
		self.tests_reporter.tests_end()
		return result


######################################################################
# REGISTER_TESTS
######################################################################
def register_tests(tests_register, parser, args):
	test_dirs = sorted(glob.glob(TESTS_DIR+"/"+args.t))

	for i in range(len(test_dirs)):
		test_dirs[i] = os.path.realpath(test_dirs[i])		

	for test_dir in test_dirs:
		test_name = os.path.basename(test_dir)
		test = Test(test_name, test_dir)
		tests_register.add_test(test)
	return


######################################################################
# PARSER UTILS 
######################################################################
def strip(line):
	count = 0
	for char in line:
		if char == "\t":
			count+=1
		else:
			return count, line[count:] 
	
	return count, string[count:] 


######################################################################
# MAKE_PARSER
######################################################################
def make_parser():
	parser = argparse.ArgumentParser()
	
	parser.add_argument("-t",
		default="**",
		help="test pattern, default='**'")

	parser.add_argument("-v",
		default=False,
		action="store_true",
		help="verbose")	

	return parser


######################################################################
# PARSE_ARGUMENTS
######################################################################
def parse_arguments(parser):
	args = parser.parse_args()
	Settings.verbose = args.v
	return args


######################################################################
# MAIN
######################################################################
def main():
	parser = make_parser()
	args = parse_arguments(parser)

	tests_register = TestsRegister()
	register_tests(tests_register, parser, args)

	tests_reporter = TestsReporter(
		tests_register)

	os.makedirs(RUN_DIR)

	tests_runner = TestsRunner(
		tests_register,
		tests_reporter)

	return tests_runner.run()


######################################################################
# ENTRY_POINT
######################################################################
if __name__ == "__main__":
	main()

