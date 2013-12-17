package kokushozero.clientside;

import java.awt.EventQueue;

/**
 * Main Runnable Class for the Chat Server Client. Execute to start Client program.
 * @author Kokushozero
 *
 */
public class Driver {

	public static void main(String[] args) {
		new Driver();

	}

	public Driver(){

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Login frame = new Login();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
