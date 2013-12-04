package ServerSide;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

/*
 * Single instance of Network Chat Server.
 */
public class Server implements Runnable{

	private int port;
	private DatagramSocket socket, sendSocket;
	private Thread run, users, post, listen;
	private boolean ServerRunning;
	private ArrayList<User> userList;
	private ObjectInputStream inStream;
	private ByteArrayInputStream byteInStream;


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


	/*
	 * Listens for incoming logins from Clients and records them in an ArrayList.
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
						byteInStream.close();
						inStream.close();
						if (!userList.contains(user)){
							userList.add(user);
						}
						System.out.println("**CONNECTED USERS**");
						for (int i = 0; i < userList.size(); i++){
							System.out.println(userList.get(i).getUserName()+" on "+ userList.get(i).getIp().getHostAddress()+"\n**********************");
						}
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
					System.out.println(message);
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
	}




}
