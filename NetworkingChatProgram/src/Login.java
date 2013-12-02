import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class Login extends JFrame {

	private JPanel contentPane;
	private JTextField txtUsername;
	private JTextField txtIpaddress;
	private JTextField txtPort;
	private int port;
	private boolean loginSuccessful;
	private JLabel lblError;
	private InetAddress IPAddress;
	private DatagramSocket socket;
	
	private boolean openConnection(String ipAddress, int port){
		
		try {
			IPAddress = InetAddress.getByName(ipAddress);
			socket = new DatagramSocket(port);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			return false;
		} 
		
		return true;
		
	}

	
	private void userLogin(){
		lblError.setText("");
		if (txtUsername.getText().length() == 0 || txtIpaddress.getText().length() == 0 || txtPort.getText().length() == 0){
			lblError.setText("Error: Figures you have entered are invalid");
			return;
		}
		else{
			loginSuccessful = false;
			loginSuccessful = openConnection(txtIpaddress.getText(), Integer.decode(txtPort.getText()));
			if (loginSuccessful){
				//TODO login to main gui component sending arguments txtUsername, IPAddress, socket
				dispose();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							Client frame = new Client(IPAddress, socket, txtUsername.getText());
							frame.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
			else{
				lblError.setText("Error: Connection could not be established");
				return;
			}
		}
		
	}
	
	/**
	 * Create the frame.
	 */
	public Login() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
		setTitle("Login");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 300, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		setLocationRelativeTo(null);

		JLabel lblUsername = new JLabel("UserName");
		lblUsername.setBounds(119, 48, 56, 14);
		contentPane.add(lblUsername);

		txtUsername = new JTextField();
		txtUsername.setBounds(73, 73, 147, 20);
		contentPane.add(txtUsername);
		txtUsername.setColumns(10);
		txtUsername.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER){
					userLogin();
				}
			}
		});

		JLabel lblServerIp = new JLabel("Server IP");
		lblServerIp.setBounds(122, 106, 50, 14);
		contentPane.add(lblServerIp);

		txtIpaddress = new JTextField();
		txtIpaddress.setColumns(10);
		txtIpaddress.setBounds(73, 131, 147, 20);
		contentPane.add(txtIpaddress);
		txtIpaddress.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER){
					userLogin();
				}
			}
		});

		JLabel lblPort = new JLabel("Port");
		lblPort.setBounds(133, 162, 28, 14);
		contentPane.add(lblPort);

		txtPort = new JTextField();
		txtPort.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER){
					userLogin();
				}
			}
		});
		txtPort.setColumns(10);
		txtPort.setBounds(73, 187, 147, 20);
		contentPane.add(txtPort);

		JButton btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				userLogin();
			}
		});
		btnLogin.setBounds(104, 263, 89, 23);
		contentPane.add(btnLogin);

		lblError = new JLabel("");
		lblError.setBounds(28, 338, 237, 14);
		contentPane.add(lblError);
	}

}
