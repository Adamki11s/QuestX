package com.adamki11s.sync.io.exceptions;

public class NullWorldException extends Exception {

	private static final long serialVersionUID = -1330729429787481115L;
	
	public NullWorldException(String worldName){
		super("World '" + worldName + "' was NULL - Failed to load.");
	}

}
