package com.braintribe.gm.cli.posix.parser;

public class CliArgument {
	
	private int num;
	private String input;
	private Object value;
	
	public CliArgument(int num, String input) {
		super();
		this.num = num;
		this.input = input;
	}
	
	void setValue(Object value) {
		this.value = value;
	}
	
	public int getNum() {
		return num;
	}
	
	public String getInput() {
		return input;
	}
	
	public Object getValue() {
		return value;
	}
	
	public String getDesc() {
		return "argument #" + num + " [" + input + "]";
	}
	
	@Override
	public String toString() {
		return getDesc();
	}
	
}
