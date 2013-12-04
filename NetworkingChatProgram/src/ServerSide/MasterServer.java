package ServerSide;
/*
 * Run through command line with a single parameter of application port. Code can be altered to run multiple servers if required.
 */
public class MasterServer {

	private Server server;
	
	
	public MasterServer(int port){
		
		server = new Server(port);
		
	}
	
	public static void main(String[] args) {
		
		if (args.length != 1){
			System.out.println("Error: only accepts one port number parameter");
		}
		else{
			new MasterServer(Integer.decode(args[0]));
		}
		
	}

}
