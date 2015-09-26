package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import url.verifiers.SameDomainURLVerifier;
import visitors.SiteVisitorBuilder;
import visitors.VisitorType;

/**
 * 
 * @author mikeecb
 *
 */
public class SiteMapper {

	// A HashMap of a page to it's assets
	private HashMap<String, List<String>> pageToAssets;
	// A queue of pages to visit
	private LinkedList<String> pagesToVisit;
	// A fast way to lookup and check if a page has been visited
	private static BloomFilter<String> visitedPages;
	
	private final String domain;
	private final String domainKey;
	private final SiteVisitorBuilder siteVisitorBuilder;
	private final Keyable URLToKey;
	
	private ExecutorService threadPool;
	private int liveProducers;
	
	public SiteMapper(String domain, SiteVisitorBuilder visitor, Keyable URLToKey) {
		pageToAssets = new HashMap<String, List<String>>();
		pagesToVisit = new LinkedList<String>();
		visitedPages = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()) , 1000);
		threadPool = Executors.newFixedThreadPool(10);
		
		this.siteVisitorBuilder = visitor;
		this.URLToKey = URLToKey;
		
		if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
			domain = "http://" + domain;
		} 
		this.domain = domain;
		this.domainKey = URLToKey.key(domain);
		
		String resource = "/";
		String domainWithoutProtocol = this.getKey(domain);
		if (domainWithoutProtocol.contains("/")) {
			resource = domain.substring(domainWithoutProtocol.indexOf("/"));
		}
		
		/*
		 *  Don't want resources ending in "/" in the queue and we might think
		 *  they are different to similar ones not ending in "/". We do this by 
		 *  default later on by trimming trailing "/" before adding it
		 */
		if (resource.equals("/")) {
			pagesToVisit.add(domain);
		} else {
			pagesToVisit.add(domain + resource);
		}
	}
	
	private synchronized void map() {
		long startTime = System.currentTimeMillis();
		
		while (liveProducers > 0 || !pagesToVisit.isEmpty()) {
			/*
			 * Do not busy wait. Pause the main thread to save CPU and it is
			 * only notified when pages are added to pagesToVisit
			 */
			while (liveProducers > 0 && pagesToVisit.isEmpty()) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					continue;
				}
			}
			
			/*
			 * If the thread is awaken when pagesToVisit is still empty but
			 * there are no more liveProducers (ie. when it is about to finish)
			 * exit.
			 */
			String URL = null;
			try {
				URL = pagesToVisit.pop();
			} catch (NoSuchElementException e) {
				continue;
			}

			liveProducers++;
			threadPool.execute(siteVisitorBuilder.build(URL, this));
		}			
		
		long timeTaken = (System.currentTimeMillis() - startTime);
		System.out.println(pageToAssets.keySet().size() + " pages visited in " + timeTaken + " ms");
	}
	
	public synchronized void visitorExited() {
		liveProducers--;
		this.notify();
	}
	
	public synchronized void storePageAssets(String URL, List<String> URLs) {
		pageToAssets.put(getKey(URL), URLs);
	}
	
	public synchronized void addPagesToVisit(List<String> URLs) {
		pagesToVisit.addAll(URLs);
	}
	
	/**
	 * Must be synchronized so that if another SiteVisitor job can't check
	 * if visited until another thread has marked their URL as visited. 
	 * This prevents thread A from checking it is visited and continuing 
	 * and thread B marking visited just after A checks, which leads to both
	 * threads visiting the page. 
	 * 
	 * Returns true if the URL has not been visited in the past and then marks
	 * 
	 * @param URL
	 */
	public synchronized boolean ifShouldVisitMarkVisited(String URL) {
		String key = getKey(URL);
		if (alreadyVisited(key)) {
			return false;
		}
		/*
		 *  Mark the current URL as 'visited' by storing it in the BloomFilter
		 *  so that no other thread continues further than this
		 */
		markVisited(key);
		return true;
	}
	
	/**
	 * Marks a page visited by entering it in the BloomFilter. It also enters it
	 * to pageToAssets and stores the value as an empty list. This is overriden 
	 * later on when the true value is calculated. This is needed because the 
	 * alreadyVisted function checks if pageToAssets contains the key if it might
	 * be in the BloomFilter
	 * @param URL
	 */
	public void markVisited(String key) {
		visitedPages.put(key);
		pageToAssets.put(key, new ArrayList<String>());
	}
	
	/**
	 * Checks if a URL has already been visited. It does this by looking it up in
	 * a BloomFilter. If it 'might' be in the BloomFilter, it checks if it is in the
	 * pageToAssets HashMap. We want to check this because this will be faster then
	 * another HTTP call.
	 * 
	 * @param URL
	 * @return
	 */
	public boolean alreadyVisited(String key) {
		if (visitedPages.mightContain(key)) {
			return pageToAssets.containsKey(key);
		}
		return false;
	}
	
	public String getKey(String URL) {
		return URLToKey.key(URL);
	}
	
	public String getDomain() {
		return this.domain;
	}
	
	public String getDomainKey() {
		return this.domainKey;
	}
	
	private void writeToFile(File file) {
		try {
			PrintWriter writer = new PrintWriter(file, "UTF-8");
			
			for (String URL : pageToAssets.keySet()) {
				writer.println(URL);
				for (String assetURL : pageToAssets.get(URL)) {
					writer.println("\t" + assetURL);
				}
				writer.println("");
			}
			
			writer.close();
		} catch (FileNotFoundException e) {
			printUsageError();
		} catch (UnsupportedEncodingException e) {
			printUsageError();
		}
	}
	
	private static void printUsageError() {
		System.out.println("Usage: java -jar SiteMapper.jar URL FILE [LINK | ASSET]");
		System.exit(1);
	}
	
	public static void main(String[] args) {
		System.err.println("Starting...");
		
		if (args.length != 2 && args.length != 3) {
			printUsageError();
			return;
		}
		
		String domain = args[0];
		String filename = args[1];
		
		// Default
		VisitorType visitorType = VisitorType.LINK;
		if (args.length == 3) {
			visitorType = VisitorType.valueOf(args[2]);
		} 
		
		SiteMapper mapper = new SiteMapper(
			domain, 
			new SiteVisitorBuilder(visitorType, new SameDomainURLVerifier()),
			new Keyable() {
				@Override
				public String key(String input) {
					return input.replaceAll("http://", "")
							    .replaceAll("https://", "")
							    .replaceAll("www.", "");
				}
			}
		);
		
		mapper.map();
		
		System.out.println("Writing to file...");
		mapper.writeToFile(new File(filename));
		
		System.out.println("Exiting...");
		System.exit(0);
	}
}
