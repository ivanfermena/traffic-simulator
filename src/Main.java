
import java.awt.*;
import java.io.*;

import error.ParseException;
import error.SimulationError;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import control.Controller;
import model.TrafficSimulator;
import util.ReportsPanelStream;
import view.MainConsole;
import view.MainWindow;

import javax.swing.*;


public class Main {


	private final static Integer timeLimitPorDefecto = 10;
	private static Integer timeLimit = null;
	private static String ficheroEntrada = null;
	private static String ficheroSalida = null;
	private static String directoryPath = null;
	private static String checkPath = null;
	private static String testPath = null;
	private static String commands = null;
	private static String mode = "batch";


	private static void ParseArguments(String[] args) throws ParseException {

		if(args.length == 0)
			throw new ParseException("Main received no arguments\n");

		// define the valid command line options
		//
		Options opcionesLineaComandos = Main.buildOptions();

		// parse the command line as provided in args
		//
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(opcionesLineaComandos, args);
			commands = "";
			parseHelpOption(line);
			parseDirectoryOption(line);
			parseTestOption(line);
			parseInputOption(line);
			parseOutputOption(line);
			parseStepsOption(line);
			parseCheckOption(line);
			parseModeOption(line);

			// if there are some remaining arguments, then something wrong is
			// provided in the command line!
			//
			String[] resto = line.getArgs();
			if (resto.length > 0) {
				String error = "Illegal arguments:";
				for (String o : resto)
					error += (" " + o);
				throw new error.ParseException(error);
			}

		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(1);
		}

	}

	private static Options buildOptions() {
		Options commandLine = new Options();
		commandLine.addOption(Option.builder("d").longOpt("directory").hasArg().desc("Launchs every valid .ini found in a given directory (path)").build());
		commandLine.addOption(Option.builder("h").longOpt("help").desc("Shows every command's description").build());
		commandLine.addOption(Option.builder("i").longOpt("input").hasArg().desc("Events single input file should follow this option").build());
		commandLine.addOption(Option.builder("t").longOpt("test").hasArg().desc("Test every file from the directory path given").build());
		commandLine.addOption(Option.builder("c").longOpt("check").hasArg().desc("Check every file from the directory path given").build());
		commandLine.addOption(Option.builder("o").longOpt("output").hasArg().desc("Output file where the reports are written (console by default)").build());
		commandLine.addOption(Option.builder("s").longOpt("steps").hasArg().desc("Number of simulation steps should follow this option (default ticks number: " + Main.timeLimitPorDefecto + ").").build());
		commandLine.addOption(Option.builder("m").longOpt("mode").hasArg().desc("Graphic mode to be executed should follow this option (batch by default").build());

		return commandLine;
	}

	private static void parseHelpOption(CommandLine line) {
		if (line.hasOption("h")) {
			commands += "h";
		}
	}

	private static void parseInputOption(CommandLine line) throws error.ParseException {
		Main.ficheroEntrada = line.getOptionValue("i");
		if(Main.ficheroEntrada != null)
			commands += "i";
	}

	private static void parseOutputOption(CommandLine line) throws error.ParseException {
		Main.ficheroSalida = line.getOptionValue("o");
		if(Main.ficheroSalida != null)
			commands += "o";
	}

	private static void parseStepsOption(CommandLine line) throws error.ParseException { String t = line.getOptionValue("s", Main.timeLimitPorDefecto.toString());
		try {
			Main.timeLimit = Integer.parseInt(t);
			assert (Main.timeLimit < 0);
			if(line.hasOption("s"))
				commands += "s";
		} catch (Exception e) {
			throw new error.ParseException("Valor invalido para el limite de tiempo: " + t);
		}
	}
	private static void parseDirectoryOption(CommandLine line)throws error.ParseException {
		Main.directoryPath = line.getOptionValue("d");
		if(Main.directoryPath != null)
			commands += "d";
	}
	private static void parseTestOption(CommandLine line) throws error.ParseException{
		Main.testPath = line.getOptionValue("t");
		if(Main.testPath != null)
			commands += "t";
	}
	private static void parseCheckOption(CommandLine line) throws error.ParseException{
		Main.checkPath = line.getOptionValue("c");
		if(Main.checkPath != null)
			commands += "c";
	}

	private static void parseModeOption(CommandLine line) {
		if (line.hasOption("m")) {
			mode = line.getOptionValue("m");
			commands += "m";
		}
	}


	private static void initStandardMode() throws IOException {
		InputStream is = (Main.ficheroEntrada == null ? null : new FileInputStream(new File(Main.ficheroEntrada)));
		OutputStream os = (Main.ficheroSalida == null ? System.out : new FileOutputStream(new File(Main.ficheroSalida)));
		TrafficSimulator sim = new TrafficSimulator();
		Controller ctrl = new Controller(sim, Main.timeLimit, is, os);
		MainConsole mainConsole = new MainConsole(ctrl, is);
		mainConsole.init();
		if (is != null)
			is.close();
		//System.out.println("File " + Main.ficheroEntrada + " has been executed !\n");
	}
	private static void initGraphicMode() throws IOException{
		TrafficSimulator simulator = new TrafficSimulator();
		OutputStream outputStream = (Main.ficheroSalida == null ? System.out : new FileOutputStream(new File(Main.ficheroSalida)));
		Controller controller = new Controller(simulator, Main.timeLimit, null, outputStream);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					new MainWindow(Main.ficheroEntrada, controller);
				} catch (SimulationError simulationError) {
					simulationError.printStackTrace();
				}
			}
		});
	}
	private static void executeFiles(String path) throws IOException {

		File dir = new File(path);

		if ( !dir.exists() ) {
			throw new FileNotFoundException(path);
		}

		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".ini");
			}
		});

		System.out.println("Number of files found to execute: " + files.length);

		for (File file : files) {
			Main.ficheroEntrada = file.getAbsolutePath();
			Main.ficheroSalida = file.getAbsolutePath() + ".out";
			Main.initStandardMode();
		}

	}
	private static  void execute() throws IOException {
		switch (Main.commands){
			case "i": case"m": case"io": case"is": case"ios":case "im": case"imo": case"ims": case"imos": case"iom": case"ism": case"ioms": case"imso":{
				switch(mode){
					case"batch":{
						Main.initStandardMode();
					} break;
					case"gui":{
						Main.initGraphicMode();
					} break;
					default:{
						System.out.println("mode argument not valid");
					}
				}
			} break;
			case "d": case"ds":{
				switch(mode){
					case"batch":{
						Main.executeFiles(Main.directoryPath);
					} break;
					case"gui":{
						Main.initGraphicMode();
					} break;
					default:{
						System.out.println("mode argument not valid");
					}
				}
			} break;
			case "t": case"ts":{
				Main.executeFiles(Main.testPath);
				Check.test(Main.testPath);
			} break;
			case "c":{
				Check.test(Main.checkPath);
			} break;
			case "h":{
				/*HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(Main.class.getCanonicalName(), opcionesLineaComandos, true);*/
				System.out.print("Available commands:\n-f <file path> (-o <file path>)(-s positive-int)\n-t <directory path> (-s positive-int)\n-d <directory path> (-s positive-int)\n-c <directory path>\n");
			} break;
			default:{
				switch(mode){
					case"batch":{
						Main.executeFiles(Main.directoryPath);
					} break;
					case"gui":{
						Main.initGraphicMode();
					} break;
					default:{
						System.out.println("program arguments not valid");
					}
				}
			}
		}
	}

	public static void main(String[] args) throws IOException {

		// example command lines:
		//
		// GUI:
		//	-i examples-out3/cruces_2.ini -m gui
		// ONE FILE:
		//	-i examples-out2\advanced\15_misc.ini
		// DIRECTORY:
		//	-d examples-out3
		// CHECK:
		//	-c examples-out/basic
		// TEST:
		//	-t examples-out3 -s 120
		//
		try {
			Main.ParseArguments(args);
			Main.execute();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

}
