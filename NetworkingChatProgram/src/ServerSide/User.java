package ServerSide;

import java.io.Serializable;
import java.net.InetAddress;

public class User implements Serializable{

	private String userName;
	private InetAddress ip;
	private int port;
	private String uniqueID;
	public boolean active;
	private int timeDead;
	private int pingsFailed;
	
	public User(String name, InetAddress userIp, int port){
		
		this.userName = name;
		this.ip = userIp;
		this.port = port;
		timeDead = 0;
		pingsFailed = 0;
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
	
	public void setUniqueID(String id){
		this.uniqueID = id;
	}
	
	public String getUniqueID(){
		return this.uniqueID;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getTimeDead() {
		return timeDead;
	}

	public void setTimeDead(int timeDead) {
		this.timeDead = timeDead;
	}

	public int getPingsFailed() {
		return pingsFailed;
	}

	public void setPingsFailed(int pingsFailed) {
		this.pingsFailed = pingsFailed;
	}
	
}
