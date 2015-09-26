README
======

This is my, Michael Cypher, solution to the GoCardless web crawler problem. Thank you for taking the time to look at it. A jar has been provided to run the code and the code has also been provided.

Usage
=====

To run, navigate to this directory and type the following:

java -jar SiteMapper.jar URL FILE [LINK | ASSET]

For example:

java -jar SiteMapper.jar www.gocardless.com example_output.txt

If LINK or ASSET are not provided, it uses LINK by default. LINK is explained more further on. ASSET is not implemented yet as explained further on.


Design Decisions
================

I decided to focus on speed and structure as some of the key aspects to try and excel in. I would throughly test this with unit tests and integration tests with more time. 

Another design decision was that although page assets could by anything from images, to javascript files, I limited it to only other links to other pages under the same domain. This was because adding a search for the source of images etc was trivial (performance wise) compared to speeding up visiting many different pages. There exists an unimplemented class `AssetVisitor` which would also look at these other assets however the main one used is PageVisitor. Both implement an interface SiteVisitor. 

Lastly, I assumed that requests using HTTP and HTTPS returned the same HTML page and therefore should be treated as the same request and not repeated. This was implemented by converting URLs to a ‘key’ before inserting them into the BloomFilter and HashMap (explained later on). The function that does this was extracted out into an interface that was passed in to the SiteMapper class and for the time being is just an anonymous inner class:

	new Keyable() {
		@Override
		public String key(String input) {
			return input.replaceAll("http://", "")
				    .replaceAll("https://", "")
				    .replaceAll("www.", "");
			}
		}
	}

This can be changed trivially.

Interesting / Challenging
=========================

What I found most interesting and challenging was actually quickly retrieving the HTML page from a link from a HTTP request, searching it for links and then repeating for the given links.

Initially I used a OOP, recursive approach which but I found this quickly became to slow. Furthermore, it is very messy to try and execute this task in different threads recursively, especially if each thread then recursively ‘visited’ (requested the HTML) a link and then spawned more threads.

My second approach which is the one currently being used is using a producer / consumer approach without any recursion. The URL of pages to be visited are added to a queue, the main thread reads these off and spawns new threads. These new threads read the HTML by doing a GET request to that URL. They then find all the links on that page in an <a> tag and add these to the queue. Furthermore, this is added to a HashMap with the key being the URL and the value being a List of URLs found on that page.

To keep track off which pages have been ‘visited’ before so we don’t ‘visit’ them again (or we’d end up in an endless loop) I initially just checked to see if the URL existed in the HashMap. This worked but I wanted to speed this up further for better performance, so I used a BloomFilter to check instead. Because a BloomFilter returns a No or a Maybe, if a Maybe is returned, we also check if it exists in the HashMap as doing this check is much faster than ‘visiting’ a page twice (I/O pfft) and therefore is worth the check.


Performance
===========

On my machine, I manage to visit about 831 pages in 62457 ms. I profiled it using JVM Monitor and also noticed that almost all of the time is waiting on I/O which is quite nice.

Additionally, 20 threads have been used in the thread pool which is very arbitrary and is something to look into optimizing in the future.

Robustness
==========

At the moment, if given a bad URL to start with, it will return nothing instead of an error. This is because to deal with bad URLSs on different pages (which there are a few of), it does not ‘visit’ it any further but still considers it an asset. Furthermore, we don’t want tons of errors because of broken links. A solution to this problem in the future would be to store all broken links and give it back to the user but this is not currently done.

