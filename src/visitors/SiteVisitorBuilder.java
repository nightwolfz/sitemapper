package visitors;

import url.verifiers.Verifiable;
import main.SiteMapper;

/**
 * 
 * @author mikeecb
 *
 */
public class SiteVisitorBuilder {

	private final VisitorType type;
	private final Verifiable URLVerifier;
	
	public SiteVisitorBuilder(VisitorType type, Verifiable URLVerifier) {
		this.type = type;
		this.URLVerifier = URLVerifier;
	}
	
	public SiteVisitor build(String url, SiteMapper mapper) {		
		switch(type) {
		case LINK:
			return new LinkVisitor(url, mapper, URLVerifier);
		case ASSET:
			return new AssetVisitor(url, mapper, URLVerifier);
		default:
			// Default
			return new LinkVisitor(url, mapper, URLVerifier); 
		}

	}
	
}
