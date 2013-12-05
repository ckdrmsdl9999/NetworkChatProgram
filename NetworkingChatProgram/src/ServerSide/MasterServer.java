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
