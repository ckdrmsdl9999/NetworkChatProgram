package kokushozero.serverside;

/**
 * Main runnable server instantiator. Allows multiple servers to be started simultaneously on any number of pre-defined ports.
 * @author Kokushozero
 *
 */
public class MasterServer {

	private Server server;
	
	/**
	 * Default constructor
	 * @param int port
	 */
	public MasterServer(int port){
		
		server = new Server(port);
		
	}
	
	/**
	 * Run this class with individual string arguments representing a port number for each server instance intended to be run.
	 * @param String args
	 */
	public static void main(String[] args) {
		
		if (args.length == 0){
			System.out.println("Error: only accepts at least one port number parameter");
		}
		else{
			for (int i = 0; i < args.length; i++){
				new MasterServer(Integer.decode(args[0]));
			}
			
		}
		
	}

}
