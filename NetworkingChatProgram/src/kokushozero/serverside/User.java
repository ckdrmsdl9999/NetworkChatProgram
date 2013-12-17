package kokushozero.serverside;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Each instance of this class represents an individual connected user\client.
 * @author Kokushozero
 *
 */
public class User implements Serializable{

	private String userName;
	private InetAddress ip;
	private int port;
	private String uniqueID;
	
	/**
	 * Represents whether the Client has an active communicative relationship with Server.
	 */
	public boolean active;
	
	/**
	 * Value counts how many times the server has attempted to contact the client unsuccessfully after being disconnected.
	 */
	private int timeDead;
	
	/**
	 * Value counts how many times the server has attempted to contact the client unsuccessfully before being disconnected.
	 */
	private int pingsFailed;
	
	/**
	 * Default constructor
	 * @param String name
	 * @param InetAddress userIp
	 * @param int port
	 */
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
