package ServerSide;

import java.util.ArrayList;
import java.util.Random;

/**
 * Offers unique integer identifiers to connecting clients without the possibility of the same random numbers. 
 * Allows the server to differentiate between clients who share the same public IP and port.
 *
 * @author Kokushozero
 *
 */
public class ServerRNG {

	private ArrayList<String> numberPool;
	private Random rng;
	
	public ServerRNG(){
		
		numberPool = new ArrayList<String>();
		rng = new Random();
		
		for (int i = 0; i < 100000; i++){
			numberPool.add(new String(""+i));
		}
	}
	
	public String getNumber(){
		
		int randomNum = rng.nextInt(numberPool.size());
		return numberPool.get(randomNum);
	}	
}
