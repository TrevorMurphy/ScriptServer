package com.textserv.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ScriptClient {
	protected int port;
	protected static String hello = "Hello";
	protected static String goodBye = "GoodBye";
	protected static String exit = "Exit";
	protected static String done = "Done";
	protected static String waitForServerLogon = "WaitForLogon";
	protected static byte[] buffer = new byte[512];

	protected Socket socket = null;
	protected PrintWriter out = null;
	protected BufferedReader in = null;
	protected String serverName = null;
	protected String outputPrefix = null;
	protected boolean verbose = false;

	public static void main(String args[]) {
		if ( args.length == 0 ) {
			ScriptClient.printUsage();
		}
		boolean waitForServerLogon = false;
		boolean verbosePrinting = false;
		boolean interactiveSession = false;
		boolean active = false;
		String message = null;
		String params = null;
		String hostDefined = null;
		String portDefined = null;
		ScriptClient client = null;

		for ( int i = 0; i < args.length; i++ ) {
			String arg = args[i];
			if ( arg.equals("-v") ) {
				verbosePrinting = true;
			} else if ( arg.equals("-w") ) {
				waitForServerLogon = true;
			} else if ( arg.equals("-i") ) {
				interactiveSession = true;
			} else if ( arg.startsWith("-host") ) {
				hostDefined = arg.substring(5);
			} else if ( arg.startsWith("-port") ) {
				portDefined = arg.substring(5);
			} else {
				if ( message == null ) {
					message = arg;
				} else {
					message = message + " " + arg;
				}
			}

		}
		if ( portDefined == null ) {
			portDefined = new String("61067");
		}
		if ( hostDefined == null ) {
			hostDefined = new String("localhost");
		}
		if ( message == null && !interactiveSession ) {
			ScriptClient.printUsage();
		}
		else {
			client = new ScriptClient(hostDefined, new Integer(portDefined).intValue(), verbosePrinting);
			if ( waitForServerLogon ) {
				client.waitForServerLogon();
			}
			active = true;
			if (message != null) {
				client.sendMessage(message);
			}
			if (!interactiveSession) {
				client.exit();
				active = false;
			}
		}
		while (active) {
			client.showPrompt();
			try {
				message = client.readInput();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (message != null && (message.length() > 0)) {
				client.sendMessage(message);
			}
			if (message != null && message.equals("Exit")) {
				client.exit();
				active = false;
			}
		}
	}

	public ScriptClient() {
		this(false);
	}

	public ScriptClient(boolean verbose) {
		this("localhost", 61067, verbose);
	}

	public ScriptClient( int port, boolean verbose ) {
		this("localhost", port, verbose);
	}

	public ScriptClient( String hostName, boolean verbose ) {
		this(hostName, 61067, verbose);
	}

	public ScriptClient( String hostName, int port, boolean verbose ) {
		this.verbose = verbose;

		try {
			socket = new Socket( hostName, port );
			out = new PrintWriter( socket.getOutputStream(), true );
			in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
			serverName = in.readLine();		//read server name, must do this, part of my protocol
			if ( verbose ) {
				printSeparator();
				System.out.println("Connected to " + serverName );
				printSeparator();
			}
			outputPrefix = serverName + ": ";
		} catch( Exception e ) {
			System.out.println("Unable to establish working connection to " + hostName + ":" + port);
		}
	}

	public 	static void printUsage() {
		System.out.println("Usage: java com.tradingedge.script.ScriptClient [-hostHostName] [-portPort] [-v] [-w] message [params]");
	}

	public void printSeparator() {
		System.out.println("*****************************");
	}

	public void showPrompt() {
		System.out.print(serverName + "> ");
	}

	public String readInput() throws IOException {
		String input = null;
		int lengthRead = -1;
		try {
			lengthRead = System.in.read(buffer);
		} catch (IOException e) {
			throw e;
		}
		if (!(lengthRead < 0)) {
			input = (new String(buffer, 0, lengthRead)).trim();
		}
		return input;
	}

	public void exit() {
		try {
			out.println(exit);
			String serverSays = in.readLine();
			if ( verbose && serverSays != null ) {
				printSeparator();
				System.out.println(outputPrefix + serverSays);
				printSeparator();
			}
		} catch ( Exception e ) {
		} finally {
			try {
				out.close();
				in.close();
				socket.close();
			} catch( Exception e1 ) {
			}
		}
	}

	public void waitForServerLogon() {
		if ( socket != null && in != null && out != null ) {
			out.println(waitForServerLogon);
			readIt();
		}
	}

	public void sendMessage(String message) {
		if ( socket != null && in != null && out != null ) {
			out.println(message);
			readIt();
		} else {
			if ( verbose ) {
				System.out.println("Unable to send message " + message  + " , since not Connected ");
			}
		}
	}

	public void readIt() {
		String serverSays = null;
		try {
			while( (serverSays = in.readLine()) != null ) {
				if ( serverSays.equals(done) ) {
					break;
				} else {
					if (verbose) {
						System.out.print(outputPrefix);
					}
					System.out.println(serverSays);
				}
			}
		} catch ( Exception e ) {
		}
	}

}