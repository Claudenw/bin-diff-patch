/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xenei.diffpatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xenei.diffpatch.Diff;
import org.xenei.diffpatch.Patch;
import org.xenei.spanbuffer.Factory;

/**
 * 
 * An application that generates a patch file from two input files. use -h for
 * more help
 *
 */
public class Differ {

	/**
	 * Get the options.
	 * 
	 * @return the options
	 */
	private static Options getOptions() {
		Options options = new Options();
		options.addRequiredOption("l", "left-input", true, "Left input file");
		options.addRequiredOption("r", "right-input", true, "Right input file");
		options.addOption("p", "patch", true, "The patch output file, defualts to stdout");
		options.addOption("h", "help", false, "This help");
		return options;
	}

	/**
	 * Display the help.
	 */
	public static void doHelp() {
		HelpFormatter formatter = new HelpFormatter();
		String header = "Create a patch from two input files.";
		String footer = "";
		formatter.printHelp(Differ.class.getCanonicalName(), header, getOptions(), footer, true);
	}

	/**
	 * Entry point
	 * 
	 * @param args the arguments
	 * @throws FileNotFoundException if an input file is missing.
	 * @throws IOException           on IO exception.
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(getOptions(), args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			doHelp();
			System.exit(1);
		}

		if (cmd.hasOption("h")) {
			doHelp();
		}

		File left = new File(cmd.getOptionValue("l"));
		if (!left.exists()) {
			System.err.println(String.format("Left input file %s does not exist.", cmd.getOptionValue("l")));
		}

		File right = new File(cmd.getOptionValue("r"));
		if (!right.exists()) {
			System.err.println(String.format("Right input file %s does not exist.", cmd.getOptionValue("r")));
		}

		Diff.Builder builder = new Diff.Builder();
		Diff diff = builder.build(Factory.wrap(left), Factory.wrap(right));
		Patch p = new Patch(diff);

		if (cmd.hasOption("o")) {
			File o = new File(cmd.getOptionValue("o"));
			try (OutputStream out = new FileOutputStream(o)) {
				p.write(out);
			}
		} else {
			p.write(System.out);
		}
	}

}
