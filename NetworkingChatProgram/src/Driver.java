import java.awt.EventQueue;


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
