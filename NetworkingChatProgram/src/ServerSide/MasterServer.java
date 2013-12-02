package ServerSide;
/*
 * Run through command line with a single parameter of application port. Code can be altered to run multiple servers if required.
 */
public class MasterServer {

	public static void main(String[] args) {
		
		if (args.length != 1){
			System.out.println("Error: only accepts one port number parameter");
		}
		else{
			new Server(Integer.decode(args[0]));
		}
		
	}

}
