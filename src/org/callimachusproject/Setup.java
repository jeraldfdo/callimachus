/*
 * Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.setup.CallimachusConf;
import org.callimachusproject.setup.CallimachusSetup;
import org.callimachusproject.util.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for setting up a new repository.
 * 
 * @author James Leigh
 * 
 */
public class Setup {
	public static final String NAME = Version.getInstance().getVersion();

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.option("b", "basedir").arg("directory")
				.desc("Base directory used for local storage");
		commands.option("u", "user").optional("name")
				.desc("Create the given user");
		commands.option("n", "name").arg("name")
				.desc("If creating a new user use this full name");
		commands.option("e", "email").arg("addr")
				.desc("If creating a new user use this email address");
		commands.option("s", "silent").desc(
				"If the repository is already setup exit successfully");
		commands.option("h", "help").desc("Print help (this message) and exit");
		commands.option("V", "version").desc(
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			Setup setup = new Setup();
			setup.init(args);
			setup.start();
			setup.stop();
			setup.destroy();
		} catch (ClassNotFoundException e) {
			System.err.print("Missing jar with: ");
			System.err.println(e.toString());
			System.exit(5);
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	private static void println(Throwable e) {
		Throwable cause = e.getCause();
		if (cause == null && e.getMessage() == null) {
			e.printStackTrace(System.err);
		} else if (cause != null) {
			println(cause);
		}
		System.err.println(e.toString());
		e.printStackTrace();
	}

	private final Logger logger = LoggerFactory.getLogger(Setup.class);
	private boolean silent;
	private File basedir;
	private String name;
	private String email;
	private String username;
	private char[] password;

	public void init(String[] args) {
		try {
			Command line = commands.parse(args);
			if (line.isParseError()) {
				line.printParseError();
				System.exit(2);
				return;
			} else if (line.has("help")) {
				line.printHelp();
				System.exit(0);
				return;
			} else if (line.has("version")) {
				line.printCommandName();
				System.exit(0);
				return;
			} else {
				silent = line.has("silent");
				if (line.has("basedir")) {
					basedir = new File(line.get("basedir")).getCanonicalFile();
				} else {
					basedir = new File("").getCanonicalFile();
				}
				if (line.has("user") || line.has("email")) {
					this.name = line.get("name");
					this.email = line.get("email");
					String u = line.get("user");
					if (u != null && u.contains(":")) {
						username = u.substring(0, u.indexOf(':'));
						password = u.substring(u.indexOf(':') + 1).toCharArray();
						CallimachusSetup.validateName(username);
					}
					Console console = System.console();
					if (username == null || username.length() < 1) {
						if (u != null && u.length() > 0 && !u.contains(":")) {
							username = u;
						} else if (console == null) {
							Reader reader = new InputStreamReader(System.in);
							username = new BufferedReader(reader).readLine();
						} else {
							username = console.readLine("Enter a username: ");
						}
						CallimachusSetup.validateName(username);
					}
					if (email == null || email.length() < 1) {
						if (console == null) {
							Reader reader = new InputStreamReader(System.in);
							email = new BufferedReader(reader).readLine();
						} else {
							email = console.readLine("Enter an email: ");
						}
						CallimachusSetup.validateEmail(email);
					}
				}
			}
		} catch (IllegalArgumentException e) {
			println(e);
			System.exit(1);
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	public void start() throws Exception {
		boolean changed = false;
		File repositoryConfig = SystemProperties.getRepositoryConfigFile();
		File webappCar = SystemProperties.getWebappCarFile();
		CallimachusConf conf = new CallimachusConf();
		String configString = readContent(repositoryConfig.toURI().toURL());
		CallimachusSetup setup = new CallimachusSetup(basedir, configString);
		try {
			if (webappCar.canRead()) {
				for (String origin : conf.getCallimachusWebappOrigins()) {
					changed |= setup.clearCallimachusWebapp(origin);
				}
			}
			for (String origin : conf.getCallimachusWebappOrigins()) {
				changed |= setup.createWebappOrigin(origin);
			}
			if (webappCar.canRead()) {
				for (String origin : conf.getCallimachusWebappOrigins()) {
					changed |= setup.importCallimachusWebapp(webappCar.toURI().toURL(), origin);
				}
			}
			if (email != null && email.length() > 0) {
				for (String origin : conf.getCallimachusWebappOrigins()) {
					changed |= setup.createAdmin(email, username, name, null, origin);
					if (password == null || password.length < 1) {
						for (String url : setup.getUserRegistrationLinks(username, email, origin)) {
							System.err.println("Use this URL to assign a password");
							System.err.println();
							System.out.println(url);
							System.err.println();
						}
					} else {
						changed |= setup.changeUserPassword(email, username, password, origin);
					}
				}
			}
			for (String origin : conf.getCallimachusWebappOrigins()) {
				changed |= setup.finalizeWebappOrigin(origin);
			}
		} finally {
			setup.shutDown();
		}
		if (changed || silent) {
			System.exit(0);
		} else {
			logger.warn("Repository is already setup");
			System.exit(166); // already setup
		}
	}

	public void stop() throws Exception {
		// do nothing
	}

	public void destroy() throws Exception {
		// do nothing
	}

	private String readContent(URL config) throws IOException {
		InputStream in = config.openStream();
		try {
			return new Scanner(in).useDelimiter("\\Z").next();
		} finally {
			in.close();
		}
	}

}
