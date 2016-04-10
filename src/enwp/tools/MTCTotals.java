package enwp.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import jwiki.core.NS;
import jwiki.core.Wiki;
import jwiki.util.FL;
import jwiki.util.Tuple;
import jwikix.util.WikiGen;

/**
 * Report which totals MTC counts by user
 * @author Fastily
 *
 */
public class MTCTotals
{
	/**
	 * The Wiki object to use
	 */
	private static final Wiki wiki = WikiGen.wg.get("FastilyBot", "en.wikipedia.org");

	/**
	 * Main driver
	 * @param args Program arguments; not used.
	 */
	public static void main(String[] args)
	{
		HashMap<String, Integer> m = new HashMap<>();
		for(String s : wiki.getCategoryMembers("Category:Copy to Wikimedia Commons reviewed by a human", NS.FILE))
		{
			try
			{
				String user = wiki.getRevisions(s, 1, true, null, null).get(0).user;
				if(user == null)
					continue;
				
				if(m.containsKey(user))
					m.put(user, m.get(user) + 1);
				else
					m.put(user, 1);
			}
			catch(Throwable e)
			{
				e.printStackTrace();
			}
		}
		
		ArrayList<Tuple<String, Integer>> l = FL.mapToList(m);
		Collections.sort(l, (o1, o2) -> o2.y.compareTo(o1.y)); // G -> L
		
		String x = "Updated ~~~~~\n\n";
		for(Tuple<String, Integer> e : l)
			x += String.format("# [[Special:ListFiles/%s|%s]] - %d%n", e.x, e.x, e.y);
		
		wiki.edit("User:FastilyBot/Sandbox2", x, "Update report");
		
	}
}