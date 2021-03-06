package enwp.tools;

import java.util.ArrayList;
import java.util.regex.Pattern;

import ctools.tplate.ParsedItem;
import ctools.util.TParse;
import ctools.util.Toolbox;
import enwp.WPStrings;
import enwp.WTP;
import fastily.jwiki.core.MQuery;
import fastily.jwiki.core.NS;
import fastily.jwiki.core.Wiki;

/**
 * Deletes the current day's PROD'd files. CAVEAT: Be sure to check each file before using.
 * 
 * @author Fastily
 *
 */
public class SimpleFPROD
{
	/**
	 * The Wiki object to use
	 */
	private static Wiki wiki = Toolbox.getFastily();

	/**
	 * Main driver
	 * 
	 * @param args Program arguments, not used.
	 */
	public static void main(String[] args)
	{
		Pattern filePRODRegex = Pattern.compile(WTP.fprod.getRegex(wiki));
		ArrayList<String> ftl = new ArrayList<>();

		MQuery.getPageText(wiki, wiki
				.getCategoryMembers("Category:Proposed deletion as of " + Toolbox.dateAsDMY(Toolbox.getUTCofNow().minusDays(8)), NS.FILE))
				.forEach((k, v) -> {
					try
					{
						String s = ParsedItem.parse(wiki, k, TParse.extractTemplate(filePRODRegex, v)).tplates.get(0).get("concern")
								.toString().trim();

						if (!s.isEmpty() && wiki.delete(k, "Expired [[WP:PROD|PROD]], concern was: " + s))
							ftl.add(wiki.convertIfNotInNS(wiki.nss(k), NS.FILE_TALK));
					}
					catch (Throwable e)
					{
						e.printStackTrace();
					}
				});

		for (String s : MQuery.exists(wiki, true, ftl))
			wiki.delete(s, WPStrings.csdG8talk);
	}
}