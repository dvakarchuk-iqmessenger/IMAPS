# IMAP Server - TLS/SSL Test Tool

A lightweight standalone IMAP server for testing email client functionality with support for both STARTTLS (TLS) and
direct SSL/TLS (IMAPS) connections.

## Features

- **Dual Mode Support**: STARTTLS (IMAP with TLS upgrade) or direct SSL (IMAPS) connection
- **Configurable SSL Certificates**: Use custom keystores for testing certificate validation
- **IMAP4rev1 Protocol**: Basic IMAP commands for testing email client connectivity
- **Multi-client Support**: Handles concurrent connections
- **Authentication**: Supports both LOGIN and AUTHENTICATE PLAIN (SASL)
- **Empty Mailbox**: Returns empty INBOX for connection testing without message management
- Single test user account pre-configured (**you can change username and password in the code**)

## Requirements

- Java 21
- Network access to port 143
- SSL keystore file (optional, default provided)

## Default Configuration

- **Port:** 143 (both modes use this port by default)
- **Host:** 0.0.0.0 (binds to all network interfaces)
- **Username:** `user`
- **Password:** `password`
- **Default Mode:** TLS (STARTTLS)
- **Default Keystore:** `keystore.jks` (bundled in JAR resources)
- **Default Keystore Password:** `qwe`

## Usage

### Basic Usage - TLS Mode (Default)

Starts IMAP server with STARTTLS support using bundled certificate:

```bash
java -jar imap-server-tls-ssl.jar
```

### SSL Mode

Starts IMAPS server with immediate SSL connection:

```bash
java -jar imap-server-tls-ssl.jar SSL
```

### With Custom Keystore

Use your own SSL certificate and keystore:

```bash
# TLS mode with custom keystore
java -jar imap-server-tls-ssl.jar TLS /path/to/keystore.p12 your_password

# SSL mode with custom keystore
java -jar imap-server-tls-ssl.jar SSL /path/to/keystore.p12 your_password

```

### Command-Line Arguments

```bash
java -jar imap-server-tls-ssl.jar [MODE] [KEYSTORE_PATH] [KEYSTORE_PASSWORD]
```
