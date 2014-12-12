package com.textserv.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;


public class ScriptServer {
	protected Hashtable<String, ScriptMessageHandler> messageHandlers = new Hashtable<String, ScriptMessageHandler>();

	protected int port;
	protected String name = "ScriptServer";

	protected ServerSocket serverSocket = null;
	protected boolean keepOpen = true;

	protected static String hello = "Hello";
	protected static String goodBye = "GoodBye";
	protected static String exit = "Exit";
	protected static String done = "Done";
	protected static String helpShort = "?";
	protected static String help = "Help";
	protected static String waitForServerLogon = "WaitForLogon";
	protected static String shutdown = "Shutdown";
	protected static byte[] buffer = new byte[512];

	protected boolean serverLoggedOn = false;
	protected Properties serverProperties = null;

	public ScriptServer(String name, boolean interactive) throws IOException {
		this(name, 61067, interactive);
	}

	public ScriptServer( String name, int port, boolean interactive ) throws IOException {
		this.name = name;
		this.port = port;
		setServerProperties();
		openServerSocket();
		Thread thread = new Thread( new SocketListener() );
		thread.setDaemon(true);
		thread.start();
		if ( interactive ) {
			System.out.println(name + " running. Interactive mode.");
			Thread threadI = new Thread( new InteractiveListener() );
			threadI.setDaemon(true);
			threadI.start();			
		} else {
			System.out.println(name + " running. You can control using ScriptClient connecting to port " + port);
		}
	}

	private void setServerProperties() {
		serverProperties = new Properties();
		serverProperties.setProperty("name", name);
		serverProperties.setProperty("port", String.valueOf(port));
	}

	public Properties getServerProperties() {
		return serverProperties;
	}

	public void loggedOnToServer() {
		loggedOnToServer(true);
	}

	public void loggedOnToServer(boolean serverLoggedOn) {
		this.serverLoggedOn = serverLoggedOn;
	}

	public void openServerSocket() throws IOException {
		if(serverSocket == null) {
			serverSocket = new ServerSocket(port);
		}
	}

	public void close() {
		keepOpen = false;
		try {
			serverSocket.close();
		} catch ( Exception e ) {
		}
	}

	public void registerMessageHandler(String message, ScriptMessageHandler messageHandler) {
		messageHandlers.put(message, messageHandler);
	}

	class InteractiveListener implements Runnable {
		
		public void run() {
			boolean active = true;
			while (active) {
				String message = ""; 
				showPrompt();
				try {
					message = readInput();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (message != null && (message.length() > 0)) {
					try {
						PrintWriter writer = new PrintWriter(System.out);
						processInput(message, writer );
						writer.flush();
						writer.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (message != null && message.equals("Exit")) {
//					exit();
					active = false;
				}
			}	
		}
	}
	
	public void showPrompt() {
		System.out.print(name + "> ");
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
	
	class SocketListener implements Runnable {
		public void run() {
			try {
				openServerSocket();
				while( keepOpen == true ) {
						Socket clientSocket = serverSocket.accept();
						Thread thread = new Thread(new SocketHandler( clientSocket ));
						thread.setDaemon(true);
						thread.start();
				}
			} catch ( Exception e ) {
			} finally {
				try {
					serverSocket.close();
				} catch ( Exception e ) {
				}
				serverSocket = null;
			}
		}
	}

	class SocketHandler implements Runnable {
		protected Socket clientSocket = null;

		public SocketHandler( Socket clientSocket ) {
			this.clientSocket = clientSocket;
		}

		public void run() {
			PrintWriter out = null;
			BufferedReader in = null;
			try {
				out = new PrintWriter(clientSocket.getOutputStream(), true );
				in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()) );

				//initiate
				String inputLine;

				out.println(name);
				while((inputLine = in.readLine()) != null) {
					if( processInput(inputLine, out) ) {
						break;
					}
				}
			} catch(Exception e ) {
				e.printStackTrace();
			} finally {
				try {
					out.close();
					in.close();
					clientSocket.close();
				} catch ( Exception e ) {
				}
			}
		}
	}

	protected boolean processInput( String inputLine, PrintWriter out ) throws Exception{
		boolean doExit = false;

//		System.out.println("ClientSays: " + inputLine );

		StringTokenizer tokenizer = new StringTokenizer(inputLine);
		String message = tokenizer.nextToken().toLowerCase();
		String[] params = inputLine.substring(message.length()).trim().split(" ");

		if ( message.equals(exit) ) {
			out.println(goodBye);
			doExit = true;
		} else if ( message.equals(waitForServerLogon) ) {
			try {
				do{
					Thread.sleep(100);
				} while (serverLoggedOn == false);
				out.println(done);
			} catch ( InterruptedException ie ) {
			}
		} else if ( message.equals("name") ) {
			out.println(name);
			out.println(done);
		} else if ( message.equals(helpShort) || message.equals(help ) ) {
			out.println("The following commands are currently supported, all commands are case sensitive");
			out.println("?, prints this message");
			out.println("Name, prints the server name");
			out.println("Exit, logs off the server");
			out.println("WaitForLogon, waits until the server says it has logged on");
			out.println("Shutdown, requests the ScriptServer to shutdown");
			for ( Enumeration<String> e = messageHandlers.keys(); e.hasMoreElements(); ) {
				String theMessage = e.nextElement();
				ScriptMessageHandler handler = messageHandlers.get(theMessage);
				handler.describeMessage(theMessage, out);
			}
			out.println(done);
		} else if (message.equals(shutdown) ) {
			//call anyone who is interested
			ScriptMessageHandler handler = messageHandlers.get(message);
			if ( handler != null ) {
				handler.processMessage(message, params, out);
			}
			out.println(goodBye);
			doExit = true;
			close();
		} else {
			ScriptMessageHandler handler = messageHandlers.get(message);
			if ( handler != null ) {
				handler.processMessage(message, params, out);
				out.println(done);
			} else {
				out.println("Unrecognized command - " + inputLine);
				out.println(done);
			}
		}
		return doExit;
	}
}
