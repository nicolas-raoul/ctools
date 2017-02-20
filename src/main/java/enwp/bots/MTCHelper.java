package enwp.bots;

import java.util.ArrayList;
import java.util.HashSet;

import ctools.util.Toolbox;
import ctools.util.WikiX;
import enwp.WPStrings;
import enwp.WTP;
import fastily.jwiki.core.NS;
import fastily.jwiki.core.Wiki;

/**
 * Find and fix tags for files tagged for transfer to Commons which have already transferred.
 * 
 * @author Fastily
 *
 */
public final class MTCHelper
{
	/**
	 * The Wiki to use
	 */
	private static Wiki wiki = Toolbox.getFastilyBot();

	/**
	 * Creates the regular expression matching Copy to Wikimedia Commons
	 */
	private static String tRegex = WTP.mtc.getRegex(wiki);

	/**
	 * The list of pages transcluding {@code Template:Now Commons}
	 */
	private static HashSet<String> nowCommons = WTP.ncd.getTransclusionSet(wiki, NS.FILE);

	/**
	 * The ncd template to fill out
	 */
	private static String ncdT = WPStrings.makeNCDBotTemlpate(wiki.whoami());

	/**
	 * Main driver
	 * 
	 * @param args Not used - program does not accept arguments
	 */
	public static void main(String[] args)
	{
		HashSet<String> l = Toolbox.fetchLabsReportListAsFiles(wiki, "wpDupes");
		l.retainAll(WTP.mtc.getTransclusionSet(wiki, NS.FILE));
		l.removeAll(WTP.keeplocal.getTransclusionList(wiki, NS.FILE)); // lots of in-line tags

		WikiX.getFirstOnlySharedDuplicate(wiki, new ArrayList<>(l)).forEach((k, v) -> {
			if (nowCommons.contains(k))
				wiki.replaceText(k, tRegex, "BOT: File has already been copied to Commons");
			else
			{
				String o_text = wiki.getPageText(k);
				String n_text = o_text.replaceAll(tRegex, "");
				if (o_text.equals(n_text)) // avoid in-line tags
					return;

				wiki.edit(k, String.format(ncdT, v) + n_text, "BOT: File is available on Commons");
			}
		});
	}
}