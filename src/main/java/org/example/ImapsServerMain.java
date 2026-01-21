package org.example;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class ImapsServerMain {

	public static void main(String[] args) throws InterruptedException {
		// Use command-line arguments if provided, otherwise use defaults
		String keystoreFile = (args.length > 1 && args[0] != null && !args[0].isBlank()) ? args[0]
			: ImapsServerMain.class.getClassLoader().getResource("keystore.jks").getPath();
		String keystorePassword = (args.length > 1 && args[1] != null && !args[1].isBlank()) ? args[1]
			: "qwe";

		System.setProperty("greenmail.tls.keystore.file", keystoreFile);
		System.setProperty("greenmail.tls.keystore.password", keystorePassword);

		// Configure IMAPS server
		ServerSetup imaps = new ServerSetup(143, "0.0.0.0", ServerSetup.PROTOCOL_IMAPS);

		GreenMail greenMail = new GreenMail(imaps);
		greenMail.withConfiguration(
			GreenMailConfiguration.aConfig().withUser("user", "password")
		);

		// Start server
		greenMail.start();
		System.out.println("IMAPS server started on port 143");
		System.out.println("Press Ctrl+C to stop");

		// Keep running
		Thread.currentThread().join();
	}
}
