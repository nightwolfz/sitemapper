package visitors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import main.SiteMapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import url.verifiers.Verifiable;

/**
 * Only visits links down in <a> tags. Does not visit or add any other asset
 * such as images, scripts, styles etc.
 * @author mikeecb
 *
 */
public class LinkVisitor extends SiteVisitor {

	public LinkVisitor(String URL, SiteMapper mapper, Verifiable URLVerifier) {
		super(URL, mapper, URLVerifier);
	}

	protected void addURLsToVisitAndStoreURLAssets(String URL, String html) {
		Document doc = Jsoup.parse(html);
		Elements aLinks = doc.select("a[href]");
		List<String> URLs = new ArrayList<String>();
		
		for (Element aLink : aLinks) {
			String aURL = aLink.attr("href");
			if (aURL.contains("#")) {
				aURL = aURL.substring(0, aURL.indexOf("#"));
			}
			if (aURL.endsWith("/")) {
				aURL = aURL.substring(0, aURL.length() - 1);
			}
			if (URLVerifier.valid(aURL, mapper.getDomainKey())) {
				if (!aURL.startsWith("http")) {
					aURL = mapper.getDomain() + aURL;
				}
		        URLs.add(aURL);
			}
		}
		
		while (URLs.contains(URL)) {
			URLs.remove(URL);
		}
		// Remove duplicates
		URLs = new ArrayList<String>(new HashSet<String>(URLs));
		
		visit(URLs);
		storeAssets(URL, URLs);
	}
	
}
