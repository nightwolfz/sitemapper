package visitors;

import java.io.IOException;
import java.util.List;

import main.SiteMapper;

import org.jsoup.Jsoup;

import url.verifiers.Verifiable;

/**
 * 
 * @author mikeecb
 *
 */
public abstract class SiteVisitor implements Runnable {

	protected final String URL;
	protected final SiteMapper mapper;
	protected final Verifiable URLVerifier;
	
	public SiteVisitor(String URL, SiteMapper mapper, Verifiable URLVerifier) {
		this.URL = URL;
		this.mapper = mapper;
		this.URLVerifier = URLVerifier;
	}
	
	@Override
	public void run() {
		try {
			if (!mapper.ifShouldVisitMarkVisited(URL)) {
				return;
			}
			
			System.out.println("Visiting " + URL);
			
			String html = null;
			try {
				html = getHTML(URL);
			} catch (IOException e) {
				System.err.println("Error visiting url " + URL);
				return;
			}
			
			addURLsToVisitAndStoreURLAssets(URL, html);
		} finally {
			mapper.visitorExited();
		}
	}
	
	protected String getHTML(String URL) throws IOException {
		return Jsoup.connect(URL)
					.timeout(60000)
				    .get()
				    .html(); 
	}
	
	protected void visit(List<String> URLs) {
		try {
			mapper.addPagesToVisit(URLs);
		} catch (Exception e) {
			return;
		}
	}
	
	protected void storeAssets(String URL, List<String> URLs) {
		mapper.storePageAssets(URL, URLs);
	}
	
	protected abstract void addURLsToVisitAndStoreURLAssets(String URL, String html);

}
