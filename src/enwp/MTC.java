package enwp;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import jwiki.core.ColorLog;
import jwiki.core.Req;
import jwiki.core.WTask;
import jwiki.core.Wiki;
import jwiki.util.FError;
import jwiki.util.FL;
import jwiki.util.FString;
import jwiki.util.WikiGen;
import util.FCLI;
import util.WTool;

/**
 * CLI utility to assist with transfer of files from enwp to Commons.
 * 
 * @author Fastily
 *
 */
public final class MTC
{
	/**
	 * The URL to post to.
	 */
	private static final String url = "http://tools.wmflabs.org/commonshelper/index.php";

	/**
	 * The template text to post to the wmflabs tool.
	 */
	private static final String posttext = "language=en&project=wikipedia&image=%s&newname="
			+ "&ignorewarnings=1&doit=Get+text&test=%%2F";

	/**
	 * Files with these categories should not be transferred.
	 */
	private static final ArrayList<String> blacklist = FL.toSAL(
			"Category:Wikipedia files on Wikimedia Commons for which a local copy has been requested to be kept",
			"Category:Wikipedia files not suitable for Commons", "Category:Wikipedia files of no use beyond Wikipedia",
			"Category:All non-free media", "Category:All Wikipedia files with unknown source",
			"Category:All Wikipedia files with unknown copyright status", "Category:Candidates for speedy deletion",
			"Category:All possibly unfree Wikipedia files", "Category:Wikipedia files for discussion", "Category:All free in US media",
			"Category:Files deleted on Wikimedia Commons", "Category:All Wikipedia files with the same name on Wikimedia Commons",
			"Category:All Wikipedia files with a different name on Wikimedia Commons", "Category:Wikipedia files with disputed copyright information");

	/**
	 * Matches GFDL-disclaimers templates
	 * 
	 * @see #selfAttribCheck(String, String)
	 */
	private static final Pattern gfdlDiscl = Pattern.compile("(?i)\\{\\{GFDL\\-user\\-(w|en)\\-(with|no)\\-disclaimers");

	/**
	 * The Wiki objects
	 */
	private static Wiki enwp, com;

	/**
	 * The directory pointing to the location for file downloads
	 */
	private static final String fdump = "filedump/";

	/**
	 * Creates the regular expression matching Copy to Wikimedia Commons
	 */
	private static String tRegex;

	/**
	 * Main driver
	 * 
	 * @param args Program args
	 */
	public static void main(String[] args) throws Throwable
	{
		CommandLine l = FCLI.gnuParse(makeOptList(), args, "MTC [-help] [-u <user>|-f <file>] [<titles>]");

		init();
		if (l.hasOption('u'))
			procList(enwp.getUserUploads(l.getOptionValue('u')));
		else if (l.hasOption('f'))
			procList(new ArrayList<>(Files.readAllLines(Paths.get(l.getOptionValue('f')))));
		else
			procList(FL.toSAL(l.getArgs()));
	}

	/**
	 * Attempts to move files to Commons
	 * 
	 * @param titles The titles to try and move.
	 */
	private static void procList(ArrayList<String> titles)
	{
		int i = 0, total = titles.size();
		for (String s : titles)
		{
			System.err.printf("Doing item %d of %d%n", ++i, total);
			if (canTransfer(s) && doTransfer(s))
				enwp.edit(s, "{{subst:ncd}}\n" + enwp.getPageText(s).replaceAll(tRegex, ""), "ncd");

		}
	}

	/**
	 * Transfers a file to Commons.
	 * 
	 * @param title The title to transfer.
	 */
	private static boolean doTransfer(String title)
	{
		String baseFN = enwp.nss(title), localFN = fdump + baseFN;
		String text = null;

		ColorLog.fyi("Generating description page for " + title);
		try
		{
			String rawhtml = FString
					.inputStreamToString(Req.genericPOST(new URL(url), null, Req.urlenc, String.format(posttext, FString.enc(baseFN))));
			text = selfAttribCheck(cleanupText(rawhtml.substring(rawhtml.indexOf("{{Info"), rawhtml.indexOf("</textarea>"))), title);

		}
		catch (Throwable e)
		{
			e.printStackTrace();
			System.err.println("Skipping " + title);
		}

		return text != null && WTask.downloadFile(title, localFN, enwp)
				? com.upload(Paths.get(localFN), title, text, String.format("Transferred from [[w:%s|enwp]]", title)) : false;

	}

	/**
	 * Removes garbage from the text generated by CommonsHelper.
	 * 
	 * @param text The text to remove garbage from.
	 * @return A garbage free file description page.
	 */
	private static String cleanupText(String text)
	{
		String t = text.replaceAll(
				"(?si)\\{\\{(Green|Red|Yesno|Center|Own|Section link|Trademark|PD\\-logo|Bad JPEG|OTRS permission|Spoken article entry)\\}\\}\n?",
				"");
		t = t.replaceAll("(?si)\\{\\{(\\QCc-by-sa-3.0-migrated\\E|Copy to Commons).*?\\}\\}\n?", "");
		t = t.replaceAll("\\Q<!--\\E.*?\\Q-->\\E\n?", "");
		t = t.replaceAll("(?i)\\|(Permission)\\=.*?\n", "|Permission=\n");
		t = t.replaceAll("(?i)\\|(Source)\\=(Transferred from).*?\n", "|Source={{Transferred from|en.wikipedia}}\n");
		t = t.replace("{{subst:Unc}}", "").replaceAll("__NOTOC__\n?", "");
		t = t.replace("&times;", "×");

		if (!t.contains("int:filedesc"))
			t = "== {{int:filedesc}} ==\n" + t;

		return t;
	}

	/**
	 * Checks for GFDL-disclaimers templates in generated file description pages and applies the <code>user</code>
	 * parameter if needed.
	 * 
	 * @param text The text generated for <code>title</code>
	 * @param title The title of the file being transferred.
	 * @return A String with properly configured GFDL-disclaimers templates.
	 */
	private static String selfAttribCheck(String text, String title)
	{
		Matcher m = gfdlDiscl.matcher(text);

		if (m.find())
			try
			{
				return String.format("%s|1=%s%s", text.substring(0, m.end()), enwp.getRevisions(title, 1, true).get(0).user,
						text.substring(m.end()));
			}
			catch (Throwable e)
			{
				e.printStackTrace();
			}

		return text;
	}

	/**
	 * Performs checks to determine if a file can be transfered to Commons.
	 * 
	 * @param title The title to check
	 * @return True if the file can <ins>probably</ins> be transfered to Commons.
	 */
	private static boolean canTransfer(String title)
	{
		if (enwp.getSharedDuplicatesOf(title).size() > 0 || com.exists(title))
			return false;

		for (String s : enwp.getCategoriesOnPage(title))
			if (blacklist.contains(s))
				return false;

		return true;
	}

	/**
	 * Makes the list of CLI options.
	 * 
	 * @return The list of Command line options.
	 */
	private static Options makeOptList()
	{
		Options ol = FCLI.makeDefaultOptions();
		ol.addOptionGroup(FCLI.makeOptGroup(FCLI.makeArgOption("u", "Transfer eligible files uploaded by a user", "user"),
				FCLI.makeArgOption("f", "Transfer titles listed in a text file", "file")));
		return ol;
	}

	/**
	 * Initializes environment and sets up variables
	 */
	private static void init()
	{
		if (!Files.isDirectory(Paths.get(fdump)))
			try
			{
				Files.createDirectory(Paths.get(fdump));
			}
			catch (IOException e)
			{
				FError.errAndExit(e, "Please manually remove " + fdump);
			}

		com = WikiGen.wg.get("FastilyClone");
		enwp = com.getWiki("en.wikipedia.org");
		tRegex = WTool.makeTRegex(enwp, "Template:Copy to Wikimedia Commons");
	}
}