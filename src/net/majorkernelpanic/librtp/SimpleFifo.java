package net.majorkernelpanic.librtp;

public class SimpleFifo {

	private int length = 0, tail = 0, head = 0;
	private byte[] buffer;
	
	public SimpleFifo(int length) {
		this.length = length;
		buffer = new byte[length];
	}
	
	public void write(byte[] buffer, int offset, int length) {
		
		if (tail+length<this.length) {
			System.arraycopy(buffer, offset, this.buffer, tail, length);
			tail += length;
		}
		else {
			int u = this.length-tail;
			System.arraycopy(buffer, offset, this.buffer, tail, u);
			System.arraycopy(buffer, offset+u, this.buffer, 0, length-u);
			tail = length-u;
		}

	}
	
	public int read(byte[] buffer, int offset, int length) {
		
		length = length>available() ? available() : length;
		
		if (head+length<this.length) {
			System.arraycopy(this.buffer, head, buffer, offset, length);
			head += length;
		}
		else {
			int u = this.length-head;
			System.arraycopy(this.buffer, head, buffer, offset, u);
			System.arraycopy(this.buffer, 0, buffer, offset+u, length-u);
			head = length-u;
		}
		
		return length;
	}
	
	public int available() {
		return (tail>=head) ? tail-head : this.length-(head-tail) ; 
	}
	
}
