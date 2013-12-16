package ServerSide;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/*
 * Single instance of Network Chat Server.
 */
public class Server implements Runnable{

	private int port;
	private DatagramSocket socket, sendSocket;
	private Thread run, users, listen, check;
	private boolean ServerRunning;
	private ArrayList<User> userList;
	private ObjectInputStream inStream;
	private ByteArrayInputStream byteInStream;
	private DatagramSocket udpPing;
	private ServerRNG rng;
	
	public Server(int port){
		rng = new ServerRNG();
		this.port = port;
		ServerRunning = false;
		try {
			socket = new DatagramSocket(port);
			sendSocket = new DatagramSocket();
		} catch (Exception e) {
			System.out.println(e.getMessage());
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
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
											
				while (ServerRunning){
					
					// Delay the thread by 1 seconds per cycle to ameliorate network congestion.
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						System.out.println(e1.getMessage());
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
							
							// Will allow no reply from client 5 times before evicting them from the client list to ameliorate errors from packet loss via UDP.
							userList.get(i).setPingsFailed(userList.get(i).getPingsFailed()+1);
							System.out.println("Exception thrown waiting for "+userList.get(i).getUserName());
							if (userList.get(i).getPingsFailed() > 4 && userList.get(i).active){
								System.out.println("\n"+e.getMessage());
								System.out.println(userList.get(i).getUserName()+" has disconnected"+"\n");
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
									System.out.println("removing "+userList.get(i).getUserName()+" from userList"+"\n");
									userList.remove(i);
									break;
								}
							}
						} catch (Exception e1){
							System.out.println(e1.getMessage());
							e1.printStackTrace();
							break;
						}						
						
						// All successful acknowledgements from client are set to be evaluated. If User instance matches but ID is different (two computers on the same lan
						// with the same port numbers).
						if (!responseFromClient.equals(expectedResponse) && userList.get(i).active == true && evaluateID){
							System.out.println("Clients unique ID does not match their IP and port number registration");
							System.out.println(userList.get(i).getUserName()+" has disconnected"+"\n");
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
					System.out.println(e.getMessage());
					e.printStackTrace();
				}




			}
		};
		users.start();
	}

	/*
	 * Once the client has been registered and given a unique id, the id is sent to the client as a string to be 
	 * decoded by the client.
	 */
	private void sendClientID(User user){
		
		String message = "uid"+user.getUniqueID();
		byte[] payload = message.getBytes();
		DatagramPacket packet = new DatagramPacket(payload, payload.length, user.getIp(), user.getPort());
		try {
			sendSocket.send(packet);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/*
	 * Sends string message to all clients in the userList ArrayList of connected clients. Prefixes string with "txt" so client
	 * is aware this is a public message.
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
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * Listens for new message transmissions from clients
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
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
					String message = new String((packet.getData()));
					System.out.println(message);
					
					if (message.substring(0, 3).equals("chk")){
						System.out.println("Received username availability check... client asking for :"+message.substring(3, message.length()));
						boolean isAvailable = checkUsernameAvail(message.substring(3,message.length()));
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						if (isAvailable){
							boolean reply = true;
							byte[] replyPayload = new byte[]{ (byte) (reply?1:0)}; // convert the boolean to a byte array.
							DatagramPacket response = new DatagramPacket(replyPayload, replyPayload.length, packet.getAddress(), packet.getPort());
							try {
								socket.send(response);
							} catch (IOException e) {
								System.out.println(e.getMessage());
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
								System.out.println(e.getMessage());
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

	private boolean checkUsernameAvail(String string){
		string = string.trim();
		System.out.println("\nchecking to see if "+string+" is already taken.");
		for (int i = 0; i < userList.size(); i++){
			if (userList.get(i).getUserName().equalsIgnoreCase(string)){
				System.out.println(string+" is already in service."+"\n");
				return false;
			}
		}
		System.out.println(string+" is available."+"\n");
		return true;
	}
	

	@Override
	public void run() {
		this.ServerRunning = true;
		System.out.println("Server running on port "+this.port);
		manageUsers();
		listen();
		checkWhoIsStillAlive();
	}




}
