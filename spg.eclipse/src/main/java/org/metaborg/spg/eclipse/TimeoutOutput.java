package org.metaborg.spg.eclipse;

public class TimeoutOutput implements ProcessOutput {
	protected String output;
	protected String error;
	
	public TimeoutOutput(String output, String error) {
		this.output = output;
		this.error = error;
	}
	
	public String getOutput() {
		return output;
	}
	
	public String getError() {
		return error;
	}
}