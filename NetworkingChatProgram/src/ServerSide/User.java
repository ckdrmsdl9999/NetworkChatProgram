package ServerSide;

import java.io.Serializable;
import java.net.InetAddress;

public class User implements Serializable{

	private String userName;
	private InetAddress ip;
	private int port;
	
	public User(String name, InetAddress userIp, int port){
		
		this.userName = name;
		this.ip = userIp;
		this.port = port;
	}

	public String getUserName() {
		return userName;
	}

	public InetAddress getIp() {
		return ip;
	}
	public int getPort(){
		return port;
	}
	
}
