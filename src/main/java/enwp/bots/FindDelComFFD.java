package enwp.bots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import ctools.tplate.ParsedItem;
import ctools.util.TParse;
import ctools.util.Toolbox;
import enwp.WTP;
import fastily.jwiki.core.MQuery;
import fastily.jwiki.core.NS;
import fastily.jwiki.core.Wiki;
import fastily.jwiki.util.FL;

/**
 * Finds local enwp files transferred to Commons which have then been deleted on Commons.
 * 
 * @author Fastily
 *
 */
public final class FindDelComFFD
{
	/**
	 * The Wiki objects to use
	 */
	private static Wiki enwp = Toolbox.getFastilyBot(), com = Toolbox.getCommons(enwp);

	/**
	 * A Pattern representation of {@code Template:Nominated for deletion on Commons}
	 */
	private static final Pattern nomDelTemplPattern = Pattern.compile(WTP.nomDelOnCom.getRegex(enwp));

	/**
	 * The Map of file names and page texts on enwp to work with.
	 */
	private static final HashMap<String, String> pageTexts = MQuery.getPageText(enwp,
			enwp.whatTranscludesHere(WTP.nomDelOnCom.title, NS.FILE));

	/**
	 * Main driver
	 * 
	 * @param args Program arguments, not used.
	 */
	public static void main(String[] args)
	{
		HashMap<String, String> comPairs = new HashMap<>();
		pageTexts.forEach((k, v) -> {
			try
			{
				String comFile = ParsedItem.parse(enwp, k, TParse.extractTemplate(nomDelTemplPattern, v)).tplates.get(0).get("1")
						.getString();
				if (comFile != null)
					comPairs.put(k, enwp.convertIfNotInNS(comFile, NS.FILE));
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}
		});

		FL.toHM(MQuery.exists(com, false, new ArrayList<>(comPairs.keySet())).stream()
				.filter(s -> !com.getLogs(comPairs.get(s), null, "delete", 1).isEmpty()), Function.identity(), comPairs::get)
				.forEach((k, v) -> enwp.edit(k,
						pageTexts.get(k).replaceAll(WTP.nomDelOnCom.getRegex(enwp), String.format("{{Deleted on Commons|%s}}", enwp.nss(v))),
						"BOT: Adding note that file has been deleted on Commons"));
	}
}