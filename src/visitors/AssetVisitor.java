package visitors;

import url.verifiers.Verifiable;
import main.SiteMapper;

/**
 * Similar to LinkVisitor except it stores other resources such as <img> sources,
 * scripts etc to the pageToAssets HashMap in SiteMapper. It does not 'visit' them
 * as they are not HTML documents
 * @author mikeecb
 *
 */
public class AssetVisitor extends SiteVisitor {

	public AssetVisitor(String URL, SiteMapper mapper, Verifiable URLVerifier) {
		super(URL, mapper, URLVerifier);
	}

	@Override
	protected void addURLsToVisitAndStoreURLAssets(String URL, String html) {
		System.out.println("AssetVisitor not implement yet");
		System.out.println("Exiting...");
		System.exit(1);
	}

}
