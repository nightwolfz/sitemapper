package url.verifiers;

/**
 * 
 * @author mikeecb
 *
 */
public interface Verifiable {

	public boolean valid(String URL, String domainWithoutProtocol);
	
}
