package com.textserv.script;

import java.io.PrintWriter;

public interface ScriptMessageHandler {
	public void processMessage( String message, String[] arguments, PrintWriter out );
	
	public void describeMessage( String message, PrintWriter out );
}
	