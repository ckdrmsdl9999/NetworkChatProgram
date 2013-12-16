import java.awt.EventQueue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.UIManager;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import org.eclipse.wb.swing.FocusTraversalOnArray;

import ServerSide.User;

import java.awt.Component;


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
	
	
	private void postToConsole(String message){	
		
		textHistory.append(message+"\n");
		textHistory.setCaretPosition(textHistory.getDocument().getLength());
	}
	
	private void post(String message){
		postToServer(userName+": "+message);
		textChatField.setText("");
		textChatField.requestFocusInWindow();
	}
	
	private void receive(){
		
		byte[] payload = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(payload, payload.length);
		try {
			socket.receive(udpPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		String receivedMessage = new String(udpPacket.getData());
		if (receivedMessage.substring(0, 3).equals("txt")){
			postToConsole(receivedMessage.substring(3, receivedMessage.length()));
		}
		else if (receivedMessage.substring(0, 3).equals("uid")){
			uniqueID = receivedMessage.substring(3, receivedMessage.length());
			System.out.println("received unique ID from server :"+getUniqueID());
		}
		
	}
	
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
	
	private String getUniqueID(){
		return this.uniqueID;
	}
	
	/*
	 * Responds to server inquiries to ensure server remains aware of client.
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
	
	/*
	 * Thread listens for new messages from the server and posts them to the textpane in the GUI
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
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 500);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 764, 404);
		contentPane.add(scrollPane);
		
		textHistory = new JTextArea();
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
