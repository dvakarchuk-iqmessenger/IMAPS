package org.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public class ImapServerTlsOrSslMain {

	private static final String DEFAULT_USERNAME = "user";
	private static final String DEFAULT_PASSWORD = "password";

	public static void main(String[] args) throws Exception {
		// Parse arguments: [TYPE] [KEYSTORE_PATH] [KEYSTORE_PASSWORD]
		String serverType = (args.length > 0 && args[0] != null && !args[0].isBlank())
			? args[0].toUpperCase()
			: "TLS";

		String keystoreFile = (args.length > 1 && args[1] != null && !args[1].isBlank())
			? args[1]
			: null;  // null means use default from resources

		String keystorePassword = (args.length > 2 && args[2] != null && !args[2].isBlank())
			? args[2]
			: null;  // null means use default password

		if (!serverType.equals("TLS") && !serverType.equals("SSL")) {
			System.err.println("Invalid server type. Use 'TLS' or 'SSL'");
			System.exit(1);
		}

		// Load SSL context
		SSLContext sslContext = createSSLContext(keystoreFile, keystorePassword);

		// Determine port and protocol
		boolean useStartTls = serverType.equals("TLS");

		// Start server
		ServerSocket serverSocket = new ServerSocket(143, 50, InetAddress.getByName("0.0.0.0"));
		String protocol = useStartTls ? "IMAP with STARTTLS" : "IMAPS (SSL)";
		System.out.println(protocol + " server started on port 143");
		System.out.println("User: " + DEFAULT_USERNAME + " / Password: " + DEFAULT_PASSWORD);
		System.out.println("Press Ctrl+C to stop");

		while (true) {
			Socket client = serverSocket.accept();
			new Thread(() -> handleClient(client, sslContext, useStartTls)).start();
		}
	}

	private static SSLContext createSSLContext(String keystorePath, String password) throws Exception {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		InputStream keyStoreIS;
		char[] keyStorePassphrase;

		if (keystorePath == null) {
			// Use default keystore from resources
			keyStoreIS = ImapServerTlsOrSslMain.class.getClassLoader()
				.getResourceAsStream("keystore.jks");
			if (keyStoreIS == null) {
				throw new IllegalStateException("Default keystore not found in resources: keystore.jks");
			}
			keyStorePassphrase = "qwe".toCharArray();
			System.out.println("Loading default keystore from resources");
		} else {
			// Use provided keystore file path
			keyStoreIS = new FileInputStream(keystorePath);
			keyStorePassphrase = password.toCharArray();
			System.out.println("Loading keystore from: " + keystorePath);
		}

		try {
			keyStore.load(keyStoreIS, keyStorePassphrase);
		} finally {
			keyStoreIS.close();
		}

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, keyStorePassphrase);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(), null, null);

		return sslContext;
	}

	private static void handleClient(Socket client, SSLContext sslContext, boolean useStartTls) {
		try {
			BufferedReader in;
			PrintWriter out;

			if (useStartTls) {
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out = new PrintWriter(client.getOutputStream(), true);
			} else {
				SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory()
					.createSocket(client, null, client.getPort(), true);
				sslSocket.setUseClientMode(false);
				sslSocket.startHandshake();

				in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
				out = new PrintWriter(sslSocket.getOutputStream(), true);
			}

			// Send greeting
			out.println("* OK [CAPABILITY IMAP4rev1 STARTTLS AUTH=PLAIN] Server Ready");

			String line;
			boolean authenticated = false;

			while ((line = in.readLine()) != null) {
				System.out.println("Client: " + line);
				String[] parts = line.split(" ", 3);
				String tag = parts[0];
				String command = parts.length > 1 ? parts[1].toUpperCase() : "";

				switch (command) {
					case "CAPABILITY":
						out.println("* CAPABILITY IMAP4rev1 STARTTLS AUTH=PLAIN");
						out.println(tag + " OK CAPABILITY completed");
						break;

					case "STARTTLS":
						if (!useStartTls) {
							out.println(tag + " BAD Already using SSL");
							break;
						}
						out.println(tag + " OK Begin TLS negotiation now");
						out.flush();

						SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory()
							.createSocket(client, null, client.getPort(), true);
						sslSocket.setUseClientMode(false);
						sslSocket.startHandshake();

						in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
						out = new PrintWriter(sslSocket.getOutputStream(), true);
						System.out.println("TLS handshake completed");
						break;

					case "AUTHENTICATE":
						if (parts.length < 3) {
							out.println(tag + " BAD AUTHENTICATE requires mechanism");
							break;
						}
						String mechanism = parts[2].toUpperCase();

						if ("PLAIN".equals(mechanism)) {
							out.println("+");
							out.flush();

							String credentialsBase64 = in.readLine();
							System.out.println("Received credentials (base64): " + credentialsBase64);

							try {
								byte[] decoded = java.util.Base64.getDecoder().decode(credentialsBase64);
								String credentials = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
								String[] credParts = credentials.split("\0");

								String username = credParts.length > 1 ? credParts[1] : "";
								String password = credParts.length > 2 ? credParts[2] : "";

								if (DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password)) {
									authenticated = true;
									out.println(tag + " OK AUTHENTICATE completed");
									System.out.println("Authentication successful for user: " + username);
								} else {
									out.println(tag + " NO AUTHENTICATE failed");
									System.out.println("Authentication failed for user: " + username);
								}
							} catch (Exception e) {
								out.println(tag + " BAD Invalid credentials format");
								System.err.println("Error decoding credentials: " + e.getMessage());
							}
						} else {
							out.println(tag + " NO AUTHENTICATE mechanism not supported");
						}
						break;

					case "LOGIN":
						if (parts.length < 3) {
							out.println(tag + " BAD LOGIN requires username and password");
							break;
						}
						String[] loginCreds = parts[2].split(" ", 2);
						String username = loginCreds[0].replace("\"", "");
						String password = loginCreds.length > 1 ? loginCreds[1].replace("\"", "") : "";

						if (DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password)) {
							authenticated = true;
							out.println(tag + " OK LOGIN completed");
						} else {
							out.println(tag + " NO LOGIN failed");
						}
						break;

					case "LIST":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						if (parts.length < 3) {
							out.println(tag + " BAD LIST requires reference and mailbox");
							break;
						}

						String mailboxPattern = parts[2].contains(" ")
							? parts[2].substring(parts[2].lastIndexOf(" ") + 1).replace("\"", "").trim()
							: parts[2].replace("\"", "");

						if (mailboxPattern.isEmpty() || mailboxPattern.equals("*") ||
							mailboxPattern.equalsIgnoreCase("Inbox") || mailboxPattern.equals("%")) {
							out.println("* LIST (\\HasNoChildren) \".\" \"INBOX\"");
						}
						out.println(tag + " OK LIST completed");
						break;

					case "LSUB":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						out.println(tag + " OK LSUB completed");
						break;
					case "SEARCH":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						// Return empty search results (no messages in mailbox)
						out.println("* SEARCH");
						out.println(tag + " OK SEARCH completed");
						break;

					case "CLOSE":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						// Close the currently selected mailbox
						out.println(tag + " OK CLOSE completed");
						break;

					case "EXPUNGE":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						// No messages to expunge
						out.println(tag + " OK EXPUNGE completed");
						break;

					case "UID":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						// Handle UID commands (UID SEARCH, UID FETCH, etc.)
						if (parts.length > 2) {
							String subCommand = parts[2].split(" ")[0].toUpperCase();
							if ("SEARCH".equals(subCommand)) {
								out.println("* SEARCH");
								out.println(tag + " OK UID SEARCH completed");
							} else if ("FETCH".equals(subCommand)) {
								out.println(tag + " OK UID FETCH completed");
							} else {
								out.println(tag + " BAD UID subcommand not recognized");
							}
						} else {
							out.println(tag + " BAD UID requires subcommand");
						}
						break;

					case "FETCH":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						// No messages to fetch
						out.println(tag + " OK FETCH completed");
						break;

					case "SELECT":
					case "EXAMINE":
						if (!authenticated) {
							out.println(tag + " NO Not authenticated");
							break;
						}
						out.println("* 0 EXISTS");
						out.println("* 0 RECENT");
						out.println("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
						out.println("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft \\*)]");
						out.println("* OK [UIDVALIDITY 1]");
						out.println("* OK [UIDNEXT 1]");
						out.println(tag + " OK [READ-WRITE] " + command + " completed");
						break;

					case "LOGOUT":
						out.println("* BYE Server logging out");
						out.println(tag + " OK LOGOUT completed");
						return;

					case "NOOP":
						out.println(tag + " OK NOOP completed");
						break;

					default:
						out.println(tag + " BAD Command not recognized");
				}

			}
		} catch (Exception e) {
			System.err.println("Client error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException ignored) {
			}
		}
	}

}
