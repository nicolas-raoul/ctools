package enwp;

import java.util.ArrayList;

import jwiki.core.Wiki;
import jwiki.extras.WikiGen;
import jwiki.util.FL;
import util.WTool;

/**
 * Removes Copy to Wikimedia Commons on enwp files that may be ineligible for transfer to Commons.
 * 
 * @author Fastily
 *
 */
public class RemoveBadMTC
{
	/**
	 * The Wiki object to use
	 */
	private static final Wiki wiki = WikiGen.wg.get("FastilyBot", "en.wikipedia.org");
	
	/**
	 * The Copy to Wikimedia Commons template title
	 */
	private static final String mtc = "Template:Copy to Wikimedia Commons";

	/**
	 * Creates the regular expression matching Copy to Wikimedia Commons
	 */
	private static final String tRegex = WTool.makeTRegex(wiki, mtc);
	
	/**
	 * The list of files transcluding Copy to Wikimedia Commons.
	 */
	private static final ArrayList<String> mtcFiles = wiki.whatTranscludesHere(mtc);
	
	/**
	 * Main driver
	 * 
	 * @param args No args, not used.
	 */
	public static void main(String[] args)
	{
		ArrayList<String> fails = new ArrayList<>();
		
		for(String blt : wiki.getLinksOnPage("User:FastilyBot/Task2Blacklist"))
			 for(String x : FL.toAL(wiki.whatTranscludesHere(blt).parallelStream().filter(s -> mtcFiles.contains(s))))
			 {
				 String oText = wiki.getPageText(x);
				 String newText = oText.replaceAll(tRegex, "");
				 
				 if(oText.equals(newText))
					 fails.add(x);
				 else
					 wiki.edit(x, newText, "BOT: Remove {{Copy to Wikimedia Commons}}; the file may not be eligible for Commons");
			 }
		
		wiki.edit("User:FastilyBot/Task2Borked", WTool.listify(fails, true), "Update list");
	}
}