
/*
 * Copyright (c) 1998-2002 TIBCO Software Inc.
 * All rights reserved.
 * TIB/Rendezvous is protected under US Patent No. 5,187,787.
 * For more information, please contact:
 * TIBCO Software Inc., Palo Alto, California, USA
 *
 * @(#)AuthenticationFilter.java	1.6
 */

/*
 * This program is designed to act as an intermediary between a HTTP client and an
 * authenticating HTTP proxy server.
 * It was primarily written to illustrate how an application using the HTTP tunneling
 * feature of TIBCO Rendezvous' API for Java can interoperate with an authenticating
 * HTTP proxy server.
 *
 * WARNING: This program is only a proof of concept and should be carefully adapted
 * to fit the requirements of any particular situation.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class AuthenticationFilter {
    private ServerSocket serverSocket;

    private class AuthenticationHandler implements Runnable {
        private Socket socketToClient;
        private StringBuffer requestFromClient;

        public AuthenticationHandler(Socket socketToClient) {
            this.socketToClient = socketToClient;
        }

        public void run() {
            BufferedReader clientBufferedReader = null;
            Socket socketToServer = null;
            OutputStream serverOutputStream = null;
            InputStream serverInputStream = null;
            OutputStream clientOutputStream = null;

            try {
                // Step 1: Read request from client (This step also adds the Proxy-Authorization header field.)
                clientBufferedReader = new BufferedReader(new InputStreamReader(socketToClient.getInputStream()));
                readRequestFromClient(clientBufferedReader);

                // Step 2: Forward request to server
                socketToServer = new Socket(System.getProperty("http.authenticatingProxyHost"),
                                            Integer.parseInt(System.getProperty("http.authenticatingProxyPort")));
                serverOutputStream = socketToServer.getOutputStream();
                forwardRequestToServer(serverOutputStream);

                // Step 3: Read and forward response from server
                serverInputStream = socketToServer.getInputStream();
                clientOutputStream = socketToClient.getOutputStream();
                readAndForwardResponseFromServer(serverInputStream,
                                                 clientOutputStream);

                // Step 5: Clean-up time
                clientOutputStream.close();
                serverInputStream.close();
                serverOutputStream.close();
                socketToServer.close();
                clientBufferedReader.close();
                socketToClient.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        private void readRequestFromClient(BufferedReader clientBufferedReader) throws IOException {
            String line = null;
            int lines = 0;
            boolean methodIsGET = false;
            int emptyLines = 0;
            requestFromClient = new StringBuffer(1024);

            while (null != (line = clientBufferedReader.readLine())) {
                lines++;
                if (1 == lines) {
                    if (-1 != line.indexOf("GET")) {
                        methodIsGET = true;
                    }
                }
                if (line.equals("")) {
                    if (0 == emptyLines) {
                        requestFromClient.append(getPAHeaderField());
                    }
                    emptyLines++;
                }
                requestFromClient.append(line +
                                         "\r\n");
                if (methodIsGET &&
                        1 == emptyLines) {
                    break;
                }
            }
        }

        private String getPAHeaderField() {
            return "Proxy-Authorization: " +
                    System.getProperty("http.authenticatingProxyCredentials") +
                    "\r\n";
        }

        private void forwardRequestToServer(OutputStream serverOutputStream) throws IOException {
            serverOutputStream.write(requestFromClient.toString().getBytes());
        }

        private void readAndForwardResponseFromServer(InputStream serverInputStream,
                                                      OutputStream clientOutputStream) throws IOException {
            int asciiCharacter = -1;

            while (-1 != (asciiCharacter = serverInputStream.read())) {
                clientOutputStream.write((byte) asciiCharacter);
            }
        }
    } // End of AuthenticationHandler

    public static void main(String[] args) {
        int port = -1;
        AuthenticationFilter authenticationFilter = null;

        // Parameters are passed via custom system properties. These properties are not defined by the Java specification!
        Properties properties = System.getProperties();
        properties.put("http.authenticatingProxyHost",
                       "");
        properties.put("http.authenticatingProxyPort",
                       "");

        // Credentials must be provided in a format that will be understood by the authenticating proxy.
        // For instance, if the authenticating proxy uses Basic Authentication, replace "login" and
        // "password" by your login and password in the following statement.
        properties.put("http.authenticatingProxyCredentials",
                       "Basic " + new sun.misc.BASE64Encoder().encode("login:password".getBytes()));

        if (2 == args.length &&
                args[0].equals("-port")) {
            port = Integer.parseInt(args[1]);
        } else {
            AuthenticationFilter.usage();
            System.exit(1);
        }
        if (-1 != port) {
            authenticationFilter = new AuthenticationFilter(port);
            authenticationFilter.start();
        } else {
            AuthenticationFilter.usage();
            System.exit(1);
        }
    }

    public static void usage() {
        System.err.println("java AuthenticationFilter -port <port>");
    }

    public AuthenticationFilter(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void start() {
        Socket socket = null;
        AuthenticationHandler authenticationHandler = null;

        do {
            try {
                socket = serverSocket.accept();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            authenticationHandler = new AuthenticationHandler(socket);
            new Thread(authenticationHandler).start();
        } while (true);
    }
} // End of AuthenticationFilter
