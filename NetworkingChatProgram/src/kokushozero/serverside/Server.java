package kokushozero.serverside;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Server instance. Contains all code to operate a Single chat server.
 * @author Kokushozero
 *
 */
public class Server implements Runnable{

	private int port;
	private DatagramSocket socket, sendSocket;
	private Thread run, users, listen, check, userListSend;
	private boolean ServerRunning;
	private ArrayList<User> userList;
	private ObjectInputStream inStream;
	private ByteArrayInputStream byteInStream;
	private DatagramSocket udpPing;
	private ServerRNG rng;
	private Date date;
	
	/**
	 * Default constructor. Port parameter passed from MasterServer class
	 * @param int port
	 */
	public Server(int port){
		rng = new ServerRNG();
		this.port = port;
		ServerRunning = false;
		try {
			socket = new DatagramSocket(port);
			sendSocket = new DatagramSocket();
		} catch (Exception e) {
			System.out.println(date.toString()+": "+ e.getMessage());
			e.printStackTrace();
		}
		run = new Thread(this, "Server");
		run.start();
	}

	/**
	 * Polls the userList, sends UDP packets to all clients at a regular interval to determine if any clients have since disconnected.
	 */
	private void checkWhoIsStillAlive(){
		check = new Thread("Check"){
			public void run(){
										
				String responseFromClient = "";
				boolean evaluateID = false;
				
				try {
					udpPing = new DatagramSocket();									
				} catch (Exception e1) {
					System.out.println(date.toString()+": "+e1.getMessage());
					e1.printStackTrace();
				}
											
				while (ServerRunning){
					
					// Delay the thread by 1 seconds per cycle to ameliorate network congestion.
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						System.out.println(date.toString()+": "+e1.getMessage());
						e1.printStackTrace();
					}
					
					for (int i = 0; i < userList.size();i++){
						byte[] payload = new byte[1024];
						String expectedResponse = "uid"+userList.get(i).getUniqueID(); // String expected from client of this User instance.
						DatagramPacket packet = new DatagramPacket(payload, payload.length, userList.get(i).getIp(), userList.get(i).getPort()+1); // Creates a packet with user port num + 1.
						try {
							udpPing.send(packet);
							payload = new byte[expectedResponse.getBytes().length]; // byte array should only be the length of the expected response string.
							packet = new DatagramPacket(payload, payload.length);
							udpPing.setSoTimeout(3000); // Will throw a SocketTimeoutException if the socket does not receive a packet within 3 seconds.
							udpPing.receive(packet);
							responseFromClient = new String(packet.getData());
							evaluateID = true;
						} catch (SocketTimeoutException e) {
							evaluateID = false;
							
							// Will allow no reply from client 3 times before evicting them from the client list to ameliorate errors from packet loss via UDP.
							userList.get(i).setPingsFailed(userList.get(i).getPingsFailed()+1);
							if (userList.get(i).getPingsFailed() > 2 && userList.get(i).active){
								System.out.println("\n"+date.toString()+": "+e.getMessage());
								System.out.println(date.toString()+": "+userList.get(i).getUserName()+" has disconnected"+"\n");
								sendToClients(userList.get(i).getUserName()+" has disconnected");
								userList.get(i).setActive(false);
								userList.get(i).setPingsFailed(0);
								break;
							}
							// counts how many times the User instance has been attempted to contact.
							else if (userList.get(i).active == false){
								userList.get(i).setTimeDead(userList.get(i).getTimeDead()+1);															
								
								// if still no response after 20 ticks, remove the User from userList.
								if (userList.get(i).getTimeDead() > 20){
									System.out.println(date.toString()+": "+"removing "+userList.get(i).getUserName()+" from userList"+"\n");
									userList.remove(i);
									break;
								}
							}
						} catch (Exception e1){
							System.out.println(date.toString()+": "+e1.getMessage());
							e1.printStackTrace();
							break;
						}						
						
						// All successful acknowledgements from client are set to be evaluated. If User instance matches but ID is different (two computers on the same lan
						// with the same port numbers).
						if (!responseFromClient.equals(expectedResponse) && userList.get(i).active == true && evaluateID){
							System.out.println(date.toString()+": "+"Clients unique ID does not match their IP and port number registration");
							System.out.println(date.toString()+": "+userList.get(i).getUserName()+" has disconnected"+"\n");
							userList.get(i).setActive(false);
							sendToClients(userList.get(i).getUserName()+" has disconnected");
						}
						
						// Previously disconnected client has re-appeared with the same unique ID, network drop likely.
						else if (responseFromClient.equals(expectedResponse) && userList.get(i).active == false){
							userList.get(i).setActive(true);
							sendToClients(userList.get(i).getUserName()+" has re-connected");
							userList.get(i).setTimeDead(0);
						}
					}
					
				}
			}
		};
		check.start();

	}

	/**
	 * Listens for incoming logins from Clients and records them and the Clients parameters in an ArrayList.
	 */
	private void manageUsers(){
		users = new Thread("Users"){
			public void run(){

				try {
					DatagramSocket socket = new DatagramSocket(port+1);
					userList = new ArrayList<User>();
					while (ServerRunning){
						byte[] payload = new byte[1024];
						DatagramPacket packet = new DatagramPacket(payload, payload.length);
						socket.receive(packet);
						byteInStream = new ByteArrayInputStream(payload);
						inStream = new ObjectInputStream(byteInStream);
						User user = (User) inStream.readObject();
						
						// Checks if the user's IP address, port number and Unique ID has already been registered as a client. If it is not currently active it will be added as
						// a valid client.
						boolean inList = false;
						for (int i = 0; i < userList.size(); i++){
							if (user.getIp().equals(userList.get(i).getIp()) && user.getPort() == (userList.get(i).getPort()) && user.getUniqueID().equals(userList.get(i).getUniqueID())){
								inList = true;
							}
						}
						if (inList == false){
							user.setUniqueID(rng.getNumber()); // attach unique identifier to client instance
							user.active = true;
							userList.add(user);
							sendClientID(user); // sends new identifier to newly registered client.
							sendToClients(user.getUserName()+" has connected");
						}
						
						
						byteInStream.close();
						inStream.close();
						System.out.println("**********************\n"+"**CONNECTED USERS**");
						for (int i = 0; i < userList.size(); i++){
							if (userList.get(i).active){
							System.out.println(userList.get(i).getUserName()+" on "+ userList.get(i).getIp().getHostAddress() + "ID:"+userList.get(i).getUniqueID());
						}}
						System.out.println("\n**********************");
					}
					socket.close();
				} catch (Exception e) {
					System.out.println(date.toString()+": "+e.getMessage());
					e.printStackTrace();
				}




			}
		};
		users.start();
	}

	/**
	 * Once the client has been registered and given a unique id, the id is sent to the client as a string to be
	 * decoded by the client.
	 * @param kokushozero.serverside.User user
	 */
	private void sendClientID(User user){
		
		String message = "uid"+user.getUniqueID();
		byte[] payload = message.getBytes();
		DatagramPacket packet = new DatagramPacket(payload, payload.length, user.getIp(), user.getPort());
		try {
			sendSocket.send(packet);
		} catch (Exception e) {
			System.out.println(date.toString()+": "+e.getMessage());
			e.printStackTrace();
		}
	}
			
	/**
	 * Sends string message to all active clients in the userList ArrayList of connected clients.
	 * @param String message
	 */
	private void sendToClients(String message){

		message = "txt"+message;

		for (int i = 0; i < userList.size(); i++){
			if (userList.get(i).active){
				
				byte[] payload = message.getBytes();
				DatagramPacket packet = new DatagramPacket(payload, payload.length, userList.get(i).getIp(), userList.get(i).getPort());
				try {
					sendSocket.send(packet);
				} catch (Exception e) {
					System.out.println(date.toString()+": "+e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Sends an ArrayList<String> object to all active clients containing a list of connected user's usernames.
	 */
	private void sentToClientsUserlist(){
		userListSend = new Thread("userListSend"){
			public void run(){
				DatagramSocket userListSocket = null; // had to initialise to null to prevent uninitialised error below.
				try {
					userListSocket = new DatagramSocket();
				} catch (SocketException e1) {
					System.out.println(date.toString()+": "+e1.getMessage());
					e1.printStackTrace();
				}

				while(ServerRunning){

					// sleep for 2 seconds per cycle
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						System.out.println(date.toString()+": "+e.getMessage());
						e.printStackTrace();
					}

					ArrayList<String> usernames = new ArrayList<String>();
					
					// all user's usernames are stored in a new arraylist as strings.
					for (int i = 0; i < userList.size(); i++){

						if (userList.get(i).isActive()){
						usernames.add(userList.get(i).getUserName());}					
					}
	
					ByteArrayOutputStream outstream = new ByteArrayOutputStream();
					ObjectOutputStream objOutstream;
					try {
						objOutstream = new ObjectOutputStream(outstream);
						objOutstream.writeObject(usernames);
						
						
						
						byte[] string = "uli".getBytes(); //string prefix so the client can identify the message type. "uli" = userlist
						byte[] tempArray = outstream.toByteArray(); // stores the object usernames as a byte array to be sent.
						byte[] payload = new byte[string.length+tempArray.length]; // empty byte array sized to fit string + usernames object
						
						// loop fills the byte array with the string and the object data.
						for (int i = 0; i < payload.length; i++){
							if (i < 3){
								payload[i] = string[i];
							}
							else{
								payload[i] = tempArray[i-3];
							}
						}
						
						
						// iterates though all connected users and sends them their individual packet.
						for (int i = 0; i < userList.size(); i++){

							DatagramPacket packet = new DatagramPacket(payload, payload.length, userList.get(i).getIp(), userList.get(i).getPort());
							try {
								userListSocket.send(packet);
							} catch (IOException e) {
								System.out.println(date.toString()+": "+e.getMessage());
								e.printStackTrace();
							}
						}
						objOutstream.close();
						outstream.close();					
					} catch (Exception e) {
						System.out.println(date.toString()+": "+e.getMessage());
						e.printStackTrace();
					}



				}}
		};
		userListSend.start();
	}
	
	/**
	 * Starts a "Listen" thread which listens for incoming packet communications from clients.
	 * Determines whether packet is either chat message or username request.
	 */
	private void listen(){
		listen = new Thread("Listen"){

			public void run(){
				while (ServerRunning){

					byte[] payload = new byte[1024];
					DatagramPacket packet = new DatagramPacket(payload, payload.length);
					try {
						socket.receive(packet);
					} catch (IOException e) {
						System.out.println(date.toString()+": "+e.getMessage());
						e.printStackTrace();
					}
					String message = new String((packet.getData()));
					System.out.println(date.toString()+": "+message);
					
					if (message.substring(0, 3).equals("chk")){
						System.out.println(date.toString()+": "+"Received username availability check... client asking for :"+message.substring(3, message.length()));
						boolean isAvailable = checkUsernameAvail(message.substring(3,message.length()));
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
							System.out.println(date.toString()+": "+e1.getMessage());
							e1.printStackTrace();
						}
						if (isAvailable){
							boolean reply = true;
							byte[] replyPayload = new byte[]{ (byte) (reply?1:0)}; // convert the boolean to a byte array.
							DatagramPacket response = new DatagramPacket(replyPayload, replyPayload.length, packet.getAddress(), packet.getPort());
							try {
								socket.send(response);
							} catch (IOException e) {
								System.out.println(date.toString()+": "+e.getMessage());
								e.printStackTrace();
							}
						}
						else{
							boolean reply = false;
							byte[] replyPayload = new byte[]{ (byte) (reply?1:0)}; // convert the boolean to a byte array.
							DatagramPacket response = new DatagramPacket(replyPayload, replyPayload.length, packet.getAddress(), packet.getPort());
							try {
								socket.send(response);
							} catch (IOException e) {
								System.out.println(date.toString()+": "+e.getMessage());
								e.printStackTrace();
							}
						}
					}
					else{
					sendToClients(message);
					}
				}
			}
		};
		listen.start();
	}

	/**
	 * Checks to see whether the requested username string has already been taken by another Client user.
	 * String is case insensitive. Returns boolean response to query.
	 * @param String string
	 * @return boolean
	 */
	private boolean checkUsernameAvail(String string){
		string = string.trim();
		System.out.println("\n"+date.toString()+": "+"checking to see if "+string+" is already taken.");
		for (int i = 0; i < userList.size(); i++){
			if (userList.get(i).getUserName().equalsIgnoreCase(string)){
				System.out.println(date.toString()+": "+string+" is already in service."+"\n");
				return false;
			}
		}
		System.out.println(date.toString()+": "+string+" is available."+"\n");
		return true;
	}
	
	/**
	 * Main runnable thread. Is executed automatically from constructor.
	 */
	@Override	
	public void run() {
		this.ServerRunning = true;
		date = new Date();
		System.out.println(date.toString()+": "+"Server running on port "+this.port);
		manageUsers();
		listen();
		checkWhoIsStillAlive();
		sentToClientsUserlist();
	}

}
