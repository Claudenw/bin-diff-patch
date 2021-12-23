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
import java.io.FileInputStream;
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
import org.apache.commons.io.IOUtils;
import org.xenei.diffpatch.Patch.ApplyResult;
import org.xenei.spanbuffer.Factory;

/**
 * An application that applies a patch to a file.
 *
 */
public class Patcher {

    private static Options getOptions() {
        Options options = new Options();
        options.addRequiredOption("i", "input", true, "Input file");
        options.addRequiredOption("p", "patch", true, "Patch file");
        options.addOption("o", "output", true, "Output file, defualts to stdout");
        options.addOption("r", "reverse", false, "Reverse patch");
        options.addOption("h", "help", false, "This help");
        return options;
    }

    public static void doHelp() {
        HelpFormatter formatter = new HelpFormatter();
        String header = "Apply a patch to an input file";
        String footer = "";
        formatter.printHelp(Patcher.class.getCanonicalName(), header, getOptions(), footer, true);
    }

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

        File in = new File(cmd.getOptionValue("i"));
        if (!in.exists()) {
            System.err.println(String.format("Input file %s does not exist.", cmd.getOptionValue("i")));
        }

        File patch = new File(cmd.getOptionValue("p"));
        if (!patch.exists()) {
            System.err.println(String.format("Patch File %s does not exist.", cmd.getOptionValue("p")));
        }

        Patch p = new Patch(new FileInputStream(patch));
        if (cmd.hasOption("r")) {
            p = p.reverse();
        }
        ApplyResult result = p.apply(Factory.wrap(in));

        if (cmd.hasOption("o")) {
            File o = new File(cmd.getOptionValue("o"));
            try (OutputStream out = new FileOutputStream(o)) {
                IOUtils.copyLarge(result.getResult().getInputStream(), out);
            }
        } else {
            IOUtils.copyLarge(result.getResult().getInputStream(), System.out);
        }
    }

}
