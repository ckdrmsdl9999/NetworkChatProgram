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
	private Thread run, users, post, listen, check;
	private boolean ServerRunning;
	private ArrayList<User> userList;
	private ObjectInputStream inStream;
	private ByteArrayInputStream byteInStream;
	private DatagramSocket udpPing;

	public Server(int port){
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
				
				try {
					udpPing = new DatagramSocket();									
				} catch (Exception e1) {
					System.out.println(e1.getMessage());
					e1.printStackTrace();
				}
											
				while (ServerRunning){

					int timesFailed = 0;
					// Delay the thread by 2 seconds per cycle to ameliorate network congestion.
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						System.out.println(e1.getMessage());
						e1.printStackTrace();
					}
					
					for (int i = 0; i < userList.size();i++){
						byte[] payload = new byte[1024];
						DatagramPacket packet = new DatagramPacket(payload, payload.length, userList.get(i).getIp(), userList.get(i).getPort()+1); // Creates a packet with user port num + 1.
						try {
							udpPing.send(packet);
							payload = new byte[1024];
							packet = new DatagramPacket(payload, payload.length);
							udpPing.setSoTimeout(3000); // Will throw a SocketTimeoutException if the socket does not receive a packet within 3 seconds.
							udpPing.receive(packet);	
						} catch (SocketTimeoutException e) {
							
							// Will allow no reply from client 5 times before evicting them from the client list to ameliorate errors from packet loss via UDP.
							timesFailed += 1;
							if (timesFailed > 4){
								System.out.println(e.getMessage());
								System.out.println(userList.get(i).getUserName()+" has disconnected");
								userList.remove(i);
								timesFailed = 0;
							}
						} catch (Exception e1){
							System.out.println(e1.getMessage());
							e1.printStackTrace();
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
					DatagramSocket socket = new DatagramSocket(8913);
					userList = new ArrayList<User>();
					while (ServerRunning){
						byte[] payload = new byte[1024];
						DatagramPacket packet = new DatagramPacket(payload, payload.length);
						socket.receive(packet);
						byteInStream = new ByteArrayInputStream(payload);
						inStream = new ObjectInputStream(byteInStream);
						User user = (User) inStream.readObject();
						
						// Checks if the user's IP address has already been registered as a client. If it is not currently active it will be added as
						// a valid client.
						boolean inList = false;
						for (int i = 0; i < userList.size(); i++){
							if (user.getIp().equals(userList.get(i).getIp())){
								inList = true;
							}
						}
						if (inList == false){
							userList.add(user);
						}
						
						
						byteInStream.close();
						inStream.close();
						System.out.println("**CONNECTED USERS**");
						for (int i = 0; i < userList.size(); i++){
							System.out.println(userList.get(i).getUserName()+" on "+ userList.get(i).getIp().getHostAddress());
						}
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
	 * Sends string message to all clients in the userList ArrayList of connected clients.
	 */
	private void sendToClients(String message){

		for (int i = 0; i < userList.size(); i++){
			byte[] payload = message.getBytes();
			System.out.println("Sending message to "+ userList.get(i).getIp().getHostAddress()+ " on "+userList.get(i).getPort());
			DatagramPacket packet = new DatagramPacket(payload, payload.length, userList.get(i).getIp(), userList.get(i).getPort());
			try {
				sendSocket.send(packet);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
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
					String message = new String(packet.getAddress()+" on port "+packet.getPort()+": "+ new String(packet.getData()));
					System.out.println(message+"\n");
					sendToClients(message);
				}
			}
		};
		listen.start();
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
