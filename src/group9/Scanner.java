package group9;

import java.util.NoSuchElementException;

public class Scanner {
	String string;
	int index;
	public Scanner(String string) {
		this.string = new String(string);
		index = 0;
	}
	
	public int nextInt() {
		String intString = "";
		int prevIndex = index;
		if (index == string.length()) {
			throw new NoSuchElementException();
		}
		while (string.charAt(index) == ' ' && index < string.length()) { 
			index++;
		}
	
		while (string.charAt(index) >= '0' && string.charAt(index) <= '9' && index < string.length()) {
			intString += string.charAt(index);
			index++;
		}
		if (intString.length() == 0) {
			index = prevIndex;
			throw new NoSuchElementException();
		}
		return Integer.parseInt(intString);
	}
	
	public char nextChar() {
		if (index == string.length()) {
			throw new NoSuchElementException();
		}
		return string.charAt(index++);
	}
	
	public boolean hasNextChar(char chr) {
		return index < string.length() && string.charAt(index) == chr;
	}
	public boolean hasNextChar() {
		return index < string.length();
	}
}