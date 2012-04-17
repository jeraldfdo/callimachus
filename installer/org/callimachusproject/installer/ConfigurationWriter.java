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
package org.callimachusproject.installer;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.callimachusproject.installer.helpers.Configure;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.DataValidator;

/**
 * A custom IzPack (see http://izpack.org) validator to validate
 * the status of com.izforge.izpack.panels.CallimachusConfigurationPanel.
 * 
 * @author David Wood (david @ http://3roundstones.com)
 * 
 */
public class ConfigurationWriter implements DataValidator {
	public static AutomatedInstallData automatedInstallData;
	private boolean abort;
	private String warning;
	private String error;

	public boolean getDefaultAnswer() {
		return true;
	}

	public synchronized String getErrorMessageId() {
		if (error == null)
			return ConfigurationReader.ERROR_MSG;
		try {
			return error;
		} finally {
			error = null;
		}
	}

	public synchronized String getWarningMessageId() {
		return warning;
	}

	public synchronized DataValidator.Status validateData(AutomatedInstallData adata) {
		if (abort)
			return Status.ERROR;
		automatedInstallData = adata;
		try {

        	Configure configure = Configure.getInstance(adata.getInstallPath());

			// Write Callimachus callimachus.conf file.
			configure.setServerConfiguration(getServerConfiguration(adata));
			// Write Callimachus mail.properties file.
			configure.setMailProperties(getMailProperties(adata));
			// Write Callimachus logging.properties file
			configure.setLoggingProperties(getLoggingProperties(adata));

			return Status.OK;
		} catch (Exception e) {
			abort = true;
			// This is an unknown error.
			e.printStackTrace();
			return Status.ERROR;
		}
	}

	public Properties getServerConfiguration(AutomatedInstallData adata) throws IOException {
    	Configure configure = Configure.getInstance(adata.getInstallPath());
		String primary = getSingleLine(adata, "callimachus.PRIMARY_ORIGIN");
		String secondary = getSingleLine(adata, "callimachus.SECONDARY_ORIGIN");
		String other = getSingleLine(adata, "callimachus.OTHER_REALM");
		Properties conf = configure.getServerConfiguration();
		conf.setProperty("DAEMON_GROUP", getSingleLine(adata, "callimachus.DAEMON_GROUP"));
		conf.setProperty("DAEMON_USER", getSingleLine(adata, "callimachus.DAEMON_USER"));
		conf.setProperty("PORT", getSingleLine(adata, "callimachus.PORT"));
		conf.setProperty("SSLPORT", getSingleLine(adata, "callimachus.SSLPORT"));
		conf.setProperty("PRIMARY_ORIGIN", primary);
		conf.setProperty("SECONDARY_ORIGIN", secondary);
		conf.setProperty("OTHER_REALM", other);
		conf.setProperty("ALL_SERVICEABLE", getSingleLine(adata, "callimachus.ALL_SERVICEABLE"));
		conf.setProperty("FULLNAME", getSingleLine(adata, "callimachus.FULLNAME"));
		conf.setProperty("EMAIL", getSingleLine(adata, "callimachus.EMAIL"));
		conf.setProperty("USERNAME", getSingleLine(adata, "callimachus.USERNAME"));
		// Set the origin on disk to be the space-separated concatenation of origins
		StringBuilder origin = new StringBuilder();
		origin.append(primary).append(" ");
		origin.append(secondary).append(" ");
		origin.append(other.replaceAll("(://[^/]*)/\\S*", "$1")).append(" ");
		conf.setProperty("ORIGIN", origin.toString().trim());
		// save JAVA_HOME
		String path = adata.getVariable("JDKPath");
		if (conf.getProperty("JAVA_HOME") == null && path != null) {
			File jdk = new File(path);
			File jre = new File(jdk, "jre");
			if (isJavaHome(jre)) {
				conf.setProperty("JAVA_HOME", jre.getAbsolutePath());
			} else if (isJavaHome(jdk)) {
				conf.setProperty("JAVA_HOME", jdk.getAbsolutePath());
			}
			if (isJdkHome(jdk)) {
				conf.setProperty("JDK_HOME", jdk.getAbsolutePath());
			}
		}
		return conf;
	}

	public Properties getMailProperties(AutomatedInstallData adata) throws IOException {
    	Configure configure = Configure.getInstance(adata.getInstallPath());
		Properties mail = configure.getMailProperties();
		if ("true".equals(adata.getVariable("callimachus.later.mail"))) {
			mail.remove("mail.transport.protocol");
		} else {
			mail.setProperty("mail.transport.protocol", getSingleLine(adata, "callimachus.mail.transport.protocol") );
		}
		mail.setProperty("mail.from", getSingleLine(adata, "callimachus.mail.from") );
		mail.setProperty("mail.smtps.host", getSingleLine(adata, "callimachus.mail.smtps.host") );
		mail.setProperty("mail.smtps.port", getSingleLine(adata, "callimachus.mail.smtps.port") );
		mail.setProperty("mail.smtps.auth", getSingleLine(adata, "callimachus.mail.smtps.auth") );
		mail.setProperty("mail.user", getSingleLine(adata, "callimachus.mail.user") );
		mail.setProperty("mail.password", getSingleLine(adata, "callimachus.mail.password") );
		return mail;
	}

	public Properties getLoggingProperties(AutomatedInstallData adata)
			throws IOException {
    	Configure configure = Configure.getInstance(adata.getInstallPath());
		return configure.getLoggingProperties();
	}

	private String getSingleLine(AutomatedInstallData adata, String key) {
		String value = adata.getVariable(key);
		if (value == null)
			return "";
		return value.trim().replaceAll("\\s+", " ");
	}

	private boolean isJavaHome(File jre) {
		File bin = new File(jre, "bin");
		File java = new File(bin, "java");
		File java_exe = new File(bin, "java.exe");
		return java.isFile() || java_exe.isFile();
	}

	private boolean isJdkHome(File jdk) {
		if (new File(new File(jdk, "lib"), "tools.jar").isFile())
			return true;
		return new File(new File(jdk.getParentFile(), "Classes"), "classes.jar").isFile();
	}

}