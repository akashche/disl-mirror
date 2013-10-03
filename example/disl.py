#!/usr/bin/env python

import argparse
import itertools
import os
import platform
import time
from subprocess import *


######################################################################
# CONSTANTS 
######################################################################
# default disl_home value, relative to the script
DEFAULT_DISL_HOME = "../../"
# string to be substituted by the actual value of DISL_HOME in paths
VARIABLE_DISL_HOME = "${DISL_HOME}"


######################################################################
# DISL_HOME
#
# @return
#	DISL_HOME to be used in paths. Either default or from env. 
######################################################################
def disl_home():
	if os.getenv("DISL_HOME") is None:
		return DEFAULT_DISL_HOME
	else:
		return os.getenv("DISL_HOME")


######################################################################
# GENERAL_PARSER 
######################################################################
def general_parser(parser):
	group = parser.add_argument_group("GENERAL")	
	
	# positional variant of i for simplicity
	# both cannot be set at once	
	group.add_argument("instr", 
		default=None, 
		nargs="?",
		help="path to jar containing disl instrumentation code, same as '-i'")
	
	group.add_argument("-i", 
		dest="instrumentation",
		default=None, 
		metavar="PATH",
		help="path to jar containing disl instrumentation code, same as 'instr'")

	group.add_argument("-t",
		dest="test_dir",
		default=None,
		metavar="PATH",
		help="directory to run in, default=current")

	return
	

######################################################################
# CLIENT_PARSER 
######################################################################
def client_parser(parser):
	parser.add_argument("-c",
		action="store_true",
		default=False,
		dest="client",
		help="set to start the server")
	
	group = parser.add_argument_group("CLIENT")

	# positional variant of c_app for simplicity
	# both cannot be set at once	
	group.add_argument("app", 
		default=None, 
		nargs="*",
		help="client jar or class and its arguments, same as '-c_app'")

	group.add_argument("-c_opts",
		action="append",
		default=[],
		metavar="A",
		nargs="+",
		help="java options of the client application")
	
	group.add_argument("-c_app",
		action="append",
		default=None,
		metavar="A",
		nargs="+",
		help="client jar or class and its arguments, same as 'app'")
	
	if platform.system() == "Darwin":	
		group.add_argument("-c_cagent", 
			default=VARIABLE_DISL_HOME+"/build/libdislagent.jnilib",
			metavar="PATH",
			help="path to c-agent library")
	else:
		group.add_argument("-c_cagent", 
			default=VARIABLE_DISL_HOME+"/build/libdislagent.so",
			metavar="PATH",
			help="path to c-agent library")	
	
	group.add_argument("-c_jagent", 
		default=VARIABLE_DISL_HOME+"/build/disl-agent.jar", 
		metavar="PATH",
		help="path to java-agent library")
	
	group.add_argument("-c_out", 
		default=None, 
		metavar="PATH",
		help="file to save client stdout to")
	
	group.add_argument("-c_err", 
		default=None, 
		metavar="PATH",
		help="file to save client stderr to")
	
	return


######################################################################
# SERVER_PARSER 
######################################################################
def server_parser(parser):
	parser.add_argument("-s",
		action="store_true",
		default=False, 
		dest="server",
		help="set to start the to be instrumented client application")

	group = parser.add_argument_group("SERVER")
	
	group.add_argument("-s_opts",
		action="append",
		default=[],
		metavar="A",
		nargs="+",
		help="java options of the server")

	group.add_argument('-s_jar',
		default=VARIABLE_DISL_HOME+"/build/disl-server.jar", 
		metavar="PATH",
		help="path to disl server jar")

	group.add_argument("-s_args",
		action="append",
		default=[],
		metavar="A",
		nargs="+",
		help="arguments to the server application")

	group.add_argument("-s_debug", 
		action="store_true",
		default=False,
		help="enable debug output")

	group.add_argument("-s_noexcepthandler", 
		action="store_true",
		default=False,
		help="does not instrument exception handler (improves performance but does not protect from errors within instrumentation)")

	group.add_argument("-s_exclusionlist", 
		default=None,
		metavar="PATH",
		help="path to exclusion list")

	group.add_argument("-s_instrumented", 
		default=None,	
		metavar="PATH",
		help="dumps instrumented classes into specified directory")
	
	group.add_argument("-s_uninstrumented", 
		default=None,	
		metavar="PATH",
		help="dumps uninstrumented classes into specified directory")

	group.add_argument("-s_port", 
		default=None,	
		metavar="PORT",
		help="listening network port")

	return
	

######################################################################
# EVALUATION_PARSER 
######################################################################
def evaluation_parser(parser):
	parser.add_argument("-e", 
		action="store_true",
		default=False,
		dest="evaluation",
		help="set to start the remote instrumentation evaluation server")

	group = parser.add_argument_group("EVALUATION")
	
	group.add_argument("-e_opts",
		action="append",
		default=[],
		nargs="+",
		metavar="A", 
		help="java options of the evaluation server")
	
	group.add_argument("-e_args",
		action="append",
		default=[],
		nargs="+",
		metavar="A", 
		help="arguments to the evaluation server application")

	group.add_argument("-e_debug", 
		action="store_true",
		default=False,
		help="enable debug output")

	group.add_argument("-e_port", 
		default=None,	
		metavar="PORT",
		help="listening network port")

	return


######################################################################
# DOCUMENTATION_PARSER 
######################################################################
def documentation_parser(parser):

	parser.formatter_class=argparse.RawTextHelpFormatter

	parser.description = """
This script is a DiSL client application, server and evaluation server starter.

By default a client application that will be instrumented and the server that 
will instrument the application will be started. To specify what should be 
started switch '-c', -'s and '-e' options. If any of these is switched then 
default values are ignored and only specified module is started.

To pass option like arguments (starting with dash) one must either use equal 
sign or positional variant of the argument if it's present. For example 
'-c_app="-jar"' instead of '-c_app "-jar"'. The positional version must be 
preceded with '--' as in the example.

The '-d' option specifies where the DiSL framework is installed. In some cases 
it might work thanks to default relative path. In other cases one must either 
specify the correct location at the command line or set 'DISL_HOME' system 
variable. 
"""

	parser.epilog = """
EXAMPLES:

To execute the example application run following:
	./disl.py -s -c -i=instr/build/disl-instr.jar -c_app=-jar c_app=app/build/example-app.jar
or	
	./disl.py -- instr/build/disl-instr.jar -jar app/build/example-app.jar
"""

######################################################################
# MAKE_PARSER 
######################################################################
def make_parser():
	parser = argparse.ArgumentParser()
	
	parser.add_argument("-p",
		action="store_true",
		default=False,
		dest="print_namespace",
		help="show all set options and default values")
	
	parser.add_argument("-d", 
		default=disl_home(),
		dest="disl_home",
		help="disl home directory")

	general_parser(parser)
	client_parser(parser)
	server_parser(parser)	
	evaluation_parser(parser)	
	documentation_parser(parser)
	
	return parser


######################################################################
# FLATTEN_ALL
#	Makes a list from nested lists or even a single non list
#	argument. Does not slice strings.
######################################################################
def flatten_all(object):
	if object is None:
		return None

	if isinstance(object, basestring):
		return [object]

	result = []
	for x in object:
		if hasattr(x, "__iter__") and not isinstance(x, basestring):
			result.extend(flatten_all(x))
		else:
			result.append(x)
	return result

	#return list(itertools.chain.from_iterable(object))


######################################################################
# PARSE_ARGUMENTS 
######################################################################
def parse_arguments(parser):
	args = parser.parse_args()

	# substite ${DISL_HOME}
	if args.c_cagent.startswith(VARIABLE_DISL_HOME):
		args.c_cagent = args.c_cagent.replace(VARIABLE_DISL_HOME, args.disl_home)
	
	if args.c_jagent.startswith(VARIABLE_DISL_HOME):
		args.c_jagent = args.c_jagent.replace(VARIABLE_DISL_HOME, args.disl_home)
	
	if args.s_jar.startswith(VARIABLE_DISL_HOME):
		args.s_jar = args.s_jar.replace(VARIABLE_DISL_HOME, args.disl_home)

	args.c_opts = flatten_all(args.c_opts)
	args.c_app = flatten_all(args.c_app)
	args.app = flatten_all(args.app)

	args.s_opts = flatten_all(args.s_opts)
	args.s_args = flatten_all(args.s_args)
	if args.s_debug is True:
		args.s_opts+= ["-Ddebug=true"]
	if args.s_port is not None:
		args.s_opts+= ["-Ddislserver.port="+args.s_port]
	if args.s_noexcepthandler is True:
		args.s_opts+= ["-Ddisl.noexcepthandler"]
	if args.s_exclusionlist is not None:
		args.s_opts+= ["-Ddisl.exclusionList="+args.s_exclusionlist]
	if args.s_instrumented is not None:
		args.s_opts+= ["-Ddislserver.instrumented="+args.s_instrumented]
	if args.s_uninstrumented is not None:
		args.s_opts+= ["-Ddislserver.uninstrumented="+args.s_uninstrumented]

	args.e_opts = flatten_all(args.e_opts)
	args.e_args = flatten_all(args.e_args)
	if args.e_debug is True:
		args.e_opts+= ["-Ddebug=true"]
	if args.e_port is not None:
		args.e_opts+= ["-Ddislreserver.port="+args.e_port]

	# supply instrumentation from positional instr if set
	if args.instrumentation is not None and args.instr is not None:
		parser.error("-i and instr set both")	
	if args.instr is not None:
		args.instrumentation = args.instr

	# supply c_app from positional app if set
	if args.c_app is not None and args.app is not None:
		parser.error("-c_app and app set both")	
	if args.app is not None:
		args.c_app = args.app

	# by default run client and server
	if args.client is False and args.server is False and args.evaluation is False:
		args.client = True
		args.server = True
	
	if args.print_namespace:
		for key in args.__dict__:
			value = args.__dict__[key]
			print key + "=" + str(value)

	return args


######################################################################
# RUN_SERVER
######################################################################
def run_server(args, parser):
	if args.instrumentation is None:
		parser.error("argument instr (-i) is required to run the client")	

	try:
		with open(".server.pid", "r") as pid_file:
			pid = pid_file.readline()
			kill = Popen(["kill", pid], stdout=PIPE, shell=False)
	except IOError:
		pass


	s_class = "ch.usi.dag.dislserver.DiSLServer" 

	s_cmd = ["java"]
	s_cmd+= args.s_opts
	s_cmd+= ["-cp", args.instrumentation + ":" + args.s_jar]
	s_cmd+= [s_class]
	s_cmd+= args.s_args

	#print s_cmd

	server = Popen(s_cmd, shell=False)
	
	with open(".server.pid", "w") as pid_file:
		pid_file.write(str(server.pid))

	time.sleep(3)
	
	return


######################################################################
# RUN_CLIENT
######################################################################
def run_client(args, parser):
	if args.c_app is None:
		parser.error("argument app (-c_app) is required to run the client")	
	
	if args.instrumentation is None:
		parser.error("argument instr (-i) is required to run the client")	

	c_cmd = ["java"]
	c_cmd+= args.c_opts
	c_cmd+= ["-agentpath:"+args.c_cagent]
	c_cmd+= ["-javaagent:"+args.c_jagent]
	c_cmd+= ["-Xbootclasspath/a:"+args.c_jagent+":"+args.instrumentation]
	c_cmd+= args.c_app

	#print c_cmd

	c_out_f = None
	c_err_f = None	
	if args.c_out is not None:
		c_out_f = open(args.c_out, "w")
	if args.c_err is not None:
		c_err_f = open(args.c_err, "w")
	
	client = Popen(c_cmd, stdout=c_out_f, stderr=c_err_f, shell=False)	
	
	client.wait()
	
	try:
		with open(".server.pid", "r") as pid_file:
			pid = pid_file.readline()
			kill = Popen(["kill", pid], stdout=PIPE, shell=False)
	except IOError:
		pass

	os.remove(".server.pid")

	return


######################################################################
# MAIN
######################################################################
def main():
	parser = make_parser()
	args = parse_arguments(parser)

	if args.test_dir is not None:
		os.chdir(args.test_dir)

	if args.server == True:
		run_server(args, parser)

	if args.client == True:
		run_client(args, parser)

	return


######################################################################
# ENTRY_POINT
######################################################################
if __name__ == "__main__":
	main()

