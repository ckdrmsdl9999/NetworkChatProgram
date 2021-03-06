package kokushozero.clientside;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.UIManager;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import org.eclipse.wb.swing.FocusTraversalOnArray;

import kokushozero.serverside.User;

import java.awt.Component;

import javax.swing.JLabel;

import java.awt.SystemColor;
import java.awt.Font;

/**
 * Main chat client GUI and client side logic for chat server.
 * @author Kokushozero
 *
 */
public class Client extends JFrame {

	private JPanel contentPane;
	private InetAddress IPAddress;
	private DatagramSocket socket;
	private String userName;
	private JTextField textChatField;
	private JTextArea textHistory;
	private int serverPort;
	private User user;
	private int userPort;
	private boolean running;
	private InetAddress userIP;
	private DatagramSocket aliveListener;
	private String uniqueID;
	private JLabel lblConUsers;
	private ArrayList<String> usersList;
	private JTextArea connectedUserTxt;

	/*
	 * Creates a user object, converts it to a byte array and sends it to the Server. This allows the server to
	 * determine when individual users have logged in to the client.
	 */
	private void login(){
		
		user = new User(this.userName, this.userIP, this.userPort);
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ObjectOutput output = null;
		
		try {
			output = new ObjectOutputStream(outStream);
			output.writeObject(user);
			byte[] objectData = outStream.toByteArray();
			outStream.close();
			output.close();
			
			DatagramPacket packet = new DatagramPacket(objectData, objectData.length, IPAddress, serverPort+1);
			socket.send(packet);
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Posts string to the main JTextArea for the user to see.
	 * @param String message
	 */
	private void postToConsole(String message){	
		
		textHistory.append(message+"\n");
		textHistory.setCaretPosition(textHistory.getDocument().getLength());
	}
	
	/**
	 * Posts user typed string to server, then deletes written text from user input text-box.
	 * @param String message
	 */
	private void post(String message){
		postToServer(userName+": "+message);
		textChatField.setText("");
		textChatField.requestFocusInWindow();
	}
	
	/**
	 * Main method of receiving data from server. Data is analysed and processed depending on its content.
	 */
	private void receive(){
		
		byte[] payload = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(payload, payload.length);
		try {
			socket.receive(udpPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		byte[] data = udpPacket.getData();
		String receivedMessage = new String(data);
		receivedMessage = receivedMessage.trim();
		
		// Received string is a text communication for user.
		if (receivedMessage.substring(0, 3).equals("txt")){
			postToConsole(receivedMessage.substring(3, receivedMessage.length()));
		}
		
		// Received string is a identifier for client.
		else if (receivedMessage.substring(0, 3).equals("uid")){
			uniqueID = receivedMessage.substring(3, receivedMessage.length());
			System.out.println("received unique ID from server :"+getUniqueID());
		}
		
		// Received string is actually data for the client userlist.
		else if (receivedMessage.substring(0, 3).equals("uli")){
			populateUserList(data);
		}
		
	}
	
	/**
	 * Method is called upon receipt of a userList sync message from server. Populates the userList JTextArea with 
	 * the current connected userList.
	 * @param byte[] data
	 */
	private void populateUserList(byte[] data){
		
		int stringArrayLength = "uli".getBytes().length;
		byte[] objectData = new byte[data.length - stringArrayLength];
		int incrementCounter = 0;
		
		// removes prefixed string from byte array to isolate object data.
		for (int i = stringArrayLength; i < data.length; i++){			
			objectData[incrementCounter++] = data[i];
		}
		
		
		
		ByteArrayInputStream byteStream = new ByteArrayInputStream(objectData);
		try {
			ObjectInputStream instream = new ObjectInputStream(byteStream);
			usersList = (ArrayList<String>) instream.readObject();
			connectedUserTxt.setText(null);
			for (int i = 0; i < usersList.size(); i++){
				connectedUserTxt.append(usersList.get(i)+"\n");
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		
		
	}
	
	/**
	 * Sends string to server to be sent to all connected clients.
	 * @param String message
	 */
	private void postToServer(String message){
		
		byte[] payload = message.getBytes();
		DatagramPacket udpPacket = new DatagramPacket(payload, payload.length, IPAddress, serverPort);
		try {
			socket.send(udpPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Returns the client's assigned Unique ID number in String format.
	 * @return String uniqueID
	 */
	private String getUniqueID(){
		return this.uniqueID;
	}
	
	/**
	 * Responds to server packet enquiries to ensure server remains aware of client.
	 */
	private void isAlive(){
		Thread thread = new Thread("isAlive"){
			public void run(){
				
				try {
					aliveListener = new DatagramSocket(userPort+1); // Listens on current user port + 1.
				} catch (SocketException e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				
				while (running){
										
					byte[] payload = new byte[1024];
					DatagramPacket alivePacket = new DatagramPacket(payload, payload.length);
					try {
						aliveListener.receive(alivePacket);
						String ack = "uid" + getUniqueID();
						alivePacket = new DatagramPacket(ack.getBytes(), ack.getBytes().length, alivePacket.getAddress(), alivePacket.getPort());
						aliveListener.send(alivePacket);
					} catch (IOException e) {
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
					
					
				}
			}
		};
		thread.start();
		
	}
	
	/**
	 * Main loop thread for receiving new packets from server.
	 */
	private void listen(){
		Thread thread = new Thread("Listen"){
			public void run(){
				
				running = true;
				
				while(running){
					receive();
				}}
		};
		thread.start();
		
	}
	
	/**
	 * Default constructor, parameters are populated from Login instance.
	 * @param INetAddress ip
	 * @param DatagramSocket socket
	 * @param String username
	 * @param int port
	 * @param int userPort
	 * @param InetAddress userIP
	 */
	public Client(InetAddress ip, DatagramSocket socket, String username, int port, int userPort, InetAddress userIP) {
		setResizable(false);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		this.IPAddress = ip;
		this.socket = socket;
		this.userName = username;
		this.serverPort = port;
		this.userPort = userPort;
		this.userIP = userIP;
		this.uniqueID = "";
		usersList = new ArrayList<String>();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1000, 500);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 764, 404);
		contentPane.add(scrollPane);
		
		textHistory = new JTextArea();
		textHistory.setWrapStyleWord(true);
		textHistory.setLineWrap(true);
		textHistory.setEditable(false);
		scrollPane.setViewportView(textHistory);
		textHistory.setColumns(10);
		
		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (textChatField.getText().length() != 0){
					post(textChatField.getText());
				}
			}
		});
		btnSend.setBounds(685, 426, 89, 23);
		contentPane.add(btnSend);

		textChatField = new JTextField();
		textChatField.setBounds(10, 426, 651, 23);
		contentPane.add(textChatField);
		textChatField.setColumns(10);
		
		connectedUserTxt = new JTextArea();
		connectedUserTxt.setWrapStyleWord(true);
		connectedUserTxt.setFont(new Font("Modern No. 20", Font.PLAIN, 13));
		connectedUserTxt.setLineWrap(true);
		connectedUserTxt.setBackground(SystemColor.controlHighlight);
		connectedUserTxt.setEditable(false);
		connectedUserTxt.setBounds(784, 32, 200, 383);
		contentPane.add(connectedUserTxt);
		
		lblConUsers = new JLabel("Connected Users");
		lblConUsers.setBounds(848, 7, 89, 14);
		contentPane.add(lblConUsers);
		setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{textChatField, btnSend}));
		textChatField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER){
					if (textChatField.getText().length() != 0){
						post(textChatField.getText());
					}
				}
			}
		});
		login();
		listen();
		isAlive();
		textChatField.requestFocusInWindow();
		
		
	}
}
