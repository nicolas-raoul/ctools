package enwp.bots;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;

import ctools.util.Toolbox;
import ctools.util.WikiX;
import enwp.WPStrings;
import enwp.WTP;
import fastily.jwiki.core.NS;
import fastily.jwiki.core.Wiki;
import fastily.jwiki.util.FL;
import fastily.jwiki.util.MultiMap;

/**
 * Checks daily deletion categories on enwp and notifies users if they have not been notified.
 * 
 * @author Fastily
 *
 */
public class DDNotifier
{
	/**
	 * The Wiki object to use
	 */
	private static Wiki wiki = Toolbox.getFastilyBot();

	/**
	 * The root configuration page.
	 */
	private static String baseConfig = String.format("User:%s/Task6/", wiki.whoami());
	
	/**
	 * The start of today, and the start of yesterday (target date)
	 */
	private static final ZonedDateTime targetDT = ZonedDateTime.of(LocalDate.now(ZoneOffset.UTC), LocalTime.of(0, 0), ZoneOffset.UTC)
			.minusDays(1);

	/**
	 * Time stamps, used to select talk page revisions which were made in the past day only.
	 */
	private static final Instant start = Instant.from(targetDT), end = Instant.now();

	/**
	 * The dated-category suffix used to select daily deletion categories to process.
	 */
	private static final String targetDateStr = String.format("%d %s %d", targetDT.getDayOfMonth(),
			targetDT.getMonth().getDisplayName(TextStyle.FULL, Locale.US), targetDT.getYear());

	/**
	 * The title blacklist; the bot will not edit any page transcluding {{bots}}
	 */
	private static final HashSet<String> talkPageBL = WTP.nobots.getTransclusionSet(wiki, NS.USER_TALK);

	/**
	 * The list of files with templates that trigger the bot unnecessarily.
	 */
	private static final HashSet<String> idkL = FL.toSet(wiki.getLinksOnPage(baseConfig + "Ignore", NS.TEMPLATE).stream()
			.flatMap(s -> wiki.whatTranscludesHere(s, NS.FILE).stream()));

	/**
	 * Main driver
	 * 
	 * @param args Program arguments; not used.
	 */
	public static void main(String[] args)
	{
		Toolbox.fetchPairedConfig(wiki, baseConfig + "Rules").forEach((k, v) -> procPair(k, v));
	}

	/**
	 * Processes a rules-set pair.
	 * 
	 * @param rootCat The root category to look for applicable categories in
	 * @param templ The Template which will be used to notify users.
	 */
	private static void procPair(String rootCat, String templ)
	{		
		Optional<String> cat = wiki.getCategoryMembers(rootCat, NS.CATEGORY).stream().filter(s -> s.endsWith(targetDateStr)).findAny();
		if (!cat.isPresent())
			return;

		MultiMap<String, String> ml = new MultiMap<>();
		wiki.getCategoryMembers(cat.get(), NS.FILE).forEach(s -> {
			if (idkL.contains(s))
				return;
			
			String author = WikiX.getPageAuthor(wiki, s);
			if(author != null)
				ml.put(wiki.convertIfNotInNS(author, NS.USER_TALK), s);
		});
		
		ml.l.forEach((k, v) -> {
			if (talkPageBL.contains(k))
				return;

			ArrayList<String> notifyList = WikiX.detLinksInHist(wiki, k, v, start, end);
			if (notifyList.isEmpty())
				return;

			String x = String.format("%n{{subst:%s|1=%s}}%n", templ, notifyList.get(0));
			if (notifyList.size() > 1)
				x += Toolbox.listify("\nAlso:\n", notifyList.subList(1, notifyList.size()), true);

			wiki.addText(k, x + WPStrings.botNote, "BOT: Notify user of possible file issue(s)", false);
		});
	}
}