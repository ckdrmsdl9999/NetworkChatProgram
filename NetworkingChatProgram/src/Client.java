import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

import java.awt.Component;


public class Client extends JFrame {

	private JPanel contentPane;
	private InetAddress IPAddress;
	private DatagramSocket socket;
	private String userName;
	private JTextField textChatField;
	private JTextArea textHistory;
	private DefaultCaret caret;
	
	private void postToConsole(String message){	
		textHistory.append(message+"\n");
		textHistory.setCaretPosition(textHistory.getDocument().getLength());
	}
	
	private void post(String message){
		
		postToConsole(userName+": "+message);
		textChatField.setText("");
		textChatField.requestFocusInWindow();
	}
	
	private String receive(){
		
		byte[] payload = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(payload, payload.length);
		try {
			socket.receive(udpPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return new String(udpPacket.getData());
	}
	
	private void postToServer(String message){
		
		byte[] payload = message.getBytes();
		DatagramPacket udpPacket = new DatagramPacket(payload, payload.length, IPAddress, socket.getPort());
		try {
			socket.send(udpPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
	
	public Client(InetAddress ip, DatagramSocket socket, String username) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
		
		this.IPAddress = ip;
		this.socket = socket;
		this.userName = username;
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
		textChatField.requestFocusInWindow();
	}
}
