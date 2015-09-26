package url.verifiers;


/**
 * Only urls from the same domain are valid. Also links to other divs on the 
 * same page by means of '#' are not valid
 * @author mikeecb
 *
 */
public class SameDomainURLVerifier implements Verifiable {
	
	public boolean valid(String URL, String domainWithoutProtocol) {		
		if (URL.startsWith("http://") || URL.startsWith("https://")) {
			if (!URL.contains(domainWithoutProtocol)) {
				return false;
			}
			String URLWithoutProtocol = URL.replace("http://", "")
										   .replace("https://", "")
										   .replace("www.", "")
										   .split("/")[0];
			return URLWithoutProtocol.contains(domainWithoutProtocol);
		}
		return !URL.startsWith("#");
	}
}
