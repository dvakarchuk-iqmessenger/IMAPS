# IMAP Server Test Tool

A standalone IMAPS server for testing email reception functionality using GreenMail.

## Features

- IMAPS (IMAP over SSL) server on port 143
- Configurable SSL certificate via keystore
- Single test user account pre-configured (you can change username and password in the code)
- Runs indefinitely until manually stopped

## Requirements

- Java 21
- Network access to port 143
- SSL keystore file (optional, default provided)

## Default Configuration

- **Port:** 143
- **Protocol:** IMAPS (SSL)
- **Host:** 0.0.0.0 (binds to all network interfaces)
- **User:** `user`
- **Password:** `password`
- **Default Keystore:** `keystore.jks` (bundled in JAR)
- **Default Keystore Password:** `qwe`

## Usage

### Run with Default Settings

Uses the bundled keystore from the JAR resources:

```bash
java -jar imap-server-1.0-SNAPSHOT.jar
```

### Run with Custom Keystore

Provide your own keystore file and password:
```bash
java -jar imap-server-1.0-SNAPSHOT.jar /path/to/your/keystore.jks your_password
```

### Command-Line Arguments

```bash
java -jar imap-server-1.0-SNAPSHOT.jar [KEYSTORE_PATH] [KEYSTORE_PASSWORD]
```

### Stopping the Server
Press `Ctrl+C` in the terminal to stop the server.
