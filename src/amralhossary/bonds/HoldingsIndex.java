package amralhossary.bonds;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.biojava.nbio.structure.PdbId;
import org.biojava.nbio.structure.align.util.UserConfiguration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * What the PDB archive holds, entry by entry, read from the wwPDB holdings file.
 * <p>
 * The archive publishes an inventory naming every file of every entry. It answers two questions
 * that otherwise cost either a wrong answer or a lot of network:
 * <ul>
 * <li><b>Whether an X-ray map can exist at all.</b> A density server computes its map from the
 * deposited structure factors, so an entry without them has nothing to serve. Roughly a quarter
 * of the archive is in that position, and a batch that does not know it discovers each one by
 * making a request and being refused.</li>
 * <li><b>What a chosen file format actually covers.</b> Every entry has an mmCIF file; not every
 * entry has a PDB-format one, because some are too large for it. A scan in PDB format therefore
 * passes over thousands of structures, and nothing in the output says so.</li>
 * </ul>
 * <p>
 * The inventory describes the <i>archive</i>, never the local mirror, so it is used to explain
 * and to skip impossible work - never to decide what to parse. A local scan still walks the
 * disk.
 * <p>
 * Entries not mentioned in the inventory are treated as though they have structure factors. The
 * conservative direction is to attempt a fetch that fails rather than to skip one that would
 * have worked, so anything unrecognised - a brand new entry, an obsolete one, or an identifier
 * spelled differently from the inventory's keys - is left alone.
 *
 * @author Amr ALHOSSARY
 */
public class HoldingsIndex {

	/** The inventory of the current archive. */
	public static final String HOLDINGS_URL =
			"https://files.wwpdb.org/pub/pdb/holdings/current_file_holdings.json.gz";

	/**
	 * The same inventory for the archive that replaces it on 21 July 2027.
	 * <p>
	 * Not used automatically. It is here because it exists, so that the move is a change of one
	 * constant rather than a search.
	 */
	public static final String BETA_HOLDINGS_URL =
			"https://files-beta.wwpdb.org/pub/wwpdb/pdb/holdings/current_file_holdings.json.gz";

	/** Where the downloaded copy is kept, under the working folder. */
	public static final String CACHE_SUBFOLDER = "pdb-holdings";

	/** The archive is rebuilt weekly, so a copy older than this is refetched. */
	private static final long MAX_AGE_MILLIS = 8L * 24 * 60 * 60 * 1000;

	/** Keys the inventory uses for the things asked about here. */
	private static final String KEY_STRUCTURE_FACTORS = "structure_factors";
	private static final String KEY_MMCIF = "mmcif";
	private static final String KEY_PDB = "pdb";

	private static HoldingsIndex loaded;
	private static boolean loadAttempted;

	/**
	 * The entries with <em>no</em> structure factors, rather than the ones with them.
	 * <p>
	 * It is the smaller set by a factor of three, and it is the one actually asked about.
	 */
	private final Set<String> withoutStructureFactors;
	private final int totalEntries;
	private final int mmcifEntries;
	private final int pdbEntries;

	private HoldingsIndex(Set<String> withoutStructureFactors, int totalEntries,
			int mmcifEntries, int pdbEntries) {
		this.withoutStructureFactors = withoutStructureFactors;
		this.totalEntries = totalEntries;
		this.mmcifEntries = mmcifEntries;
		this.pdbEntries = pdbEntries;
	}

	/**
	 * The inventory, read from the cached copy or fetched if permitted.
	 * <p>
	 * Read at most once per run, and a failure is remembered so that a machine with no network
	 * does not retry on every call. Returns <code>null</code> when it is unavailable, which
	 * every caller must treat as "carry on as before": nothing here is required for correct
	 * behaviour, it only avoids waste and explains gaps.
	 *
	 * @param settings for the working folder
	 * @param mayDownload whether fetching a missing or stale copy is allowed
	 * @return the index, or <code>null</code> if it could not be read
	 */
	public static synchronized HoldingsIndex get(SettingsManager settings, boolean mayDownload) {
		if (loadAttempted) {
			return loaded;
		}
		loadAttempted = true;
		try {
			File file = cacheFile(settings);
			if (! isUsable(file) && mayDownload) {
				download(file);
			}
			if (isUsable(file)) {
				loaded = parse(file);
				System.out.println("PDB holdings: " + loaded.totalEntries + " entries, "
						+ loaded.withoutStructureFactors.size() + " without structure factors");
			}
		} catch (IOException e) {
			System.err.println("Could not read the PDB holdings inventory: " + e.getMessage());
			System.err.println("  Continuing without it; nothing depends on it being present.");
		}
		return loaded;
	}

	/** Forgets the loaded copy, so the next {@link #get} reads it again. Test seam. */
	static synchronized void forget() {
		loaded = null;
		loadAttempted = false;
	}

	/** @return where the downloaded inventory is kept */
	public static File cacheFile(SettingsManager settings) {
		return new File(new File(settings.getWorkingFolder(), CACHE_SUBFOLDER),
				"current_file_holdings.json.gz");
	}

	private static boolean isUsable(File file) {
		return file.isFile() && file.length() > 0
				&& System.currentTimeMillis() - file.lastModified() < MAX_AGE_MILLIS;
	}

	private static void download(File target) throws IOException {
		target.getParentFile().mkdirs();
		System.out.println("Fetching the PDB holdings inventory from " + HOLDINGS_URL);
		URLConnection connection = new URL(HOLDINGS_URL).openConnection();
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(120000);
		File part = new File(target.getPath() + ".part");
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new BufferedInputStream(connection.getInputStream());
			out = new FileOutputStream(part);
			byte[] buffer = new byte[65536];
			long total = 0;
			int read;
			while ((read = in.read(buffer)) > 0) {
				out.write(buffer, 0, read);
				total += read;
			}
			out.close();
			out = null;
			//Only now does it replace what was there, so an interrupted download never leaves a
			//truncated inventory looking like a good one.
			if (target.exists() && ! target.delete()) {
				throw new IOException("could not replace " + target);
			}
			if (! part.renameTo(target)) {
				throw new IOException("could not rename " + part + " to " + target);
			}
			System.out.println("   " + (total / (1024 * 1024)) + " MB, cached at " + target);
		} finally {
			close(in);
			close(out);
			part.delete();
		}
	}

	/**
	 * Streams the inventory rather than holding it as a tree.
	 * <p>
	 * It is around 350 MB once decompressed, which a document model turns into gigabytes; read
	 * as a token stream it costs only the sets built here.
	 */
	private static HoldingsIndex parse(File file) throws IOException {
		Set<String> withoutStructureFactors = new HashSet<String>();
		int total = 0;
		int mmcif = 0;
		int pdb = 0;
		JsonParser parser = null;
		InputStream in = null;
		try {
			in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)), 65536);
			parser = new JsonFactory().createParser(in);
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				throw new IOException("holdings inventory does not start with an object");
			}
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String id = parser.getCurrentName();
				parser.nextToken();   //the entry's own object
				boolean hasSf = false;
				boolean hasMmcif = false;
				boolean hasPdb = false;
				while (parser.nextToken() == JsonToken.FIELD_NAME) {
					String key = parser.getCurrentName();
					parser.nextToken();
					boolean present = hasContent(parser);
					if (present && KEY_STRUCTURE_FACTORS.equals(key)) {
						hasSf = true;
					} else if (present && KEY_MMCIF.equals(key)) {
						hasMmcif = true;
					} else if (present && KEY_PDB.equals(key)) {
						hasPdb = true;
					}
				}
				total++;
				if (hasMmcif) { mmcif++; }
				if (hasPdb) { pdb++; }
				if (! hasSf) { withoutStructureFactors.add(id.toUpperCase()); }
			}
		} finally {
			if (parser != null) {
				try { parser.close(); } catch (IOException ignored) { /* closing */ }
			}
			close(in);
		}
		return new HoldingsIndex(withoutStructureFactors, total, mmcif, pdb);
	}

	/**
	 * Whether the value the parser is positioned on holds anything, consuming it either way.
	 * <p>
	 * A file type is listed as an array of paths. An entry that has none of a type usually
	 * omits the key, but an empty array means the same thing and must not be counted.
	 */
	private static boolean hasContent(JsonParser parser) throws IOException {
		if (parser.currentToken() != JsonToken.START_ARRAY) {
			parser.skipChildren();
			return true;
		}
		boolean any = parser.nextToken() != JsonToken.END_ARRAY;
		if (any) {
			//already inside the first element; finish the array off
			parser.skipChildren();
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				parser.skipChildren();
			}
		}
		return any;
	}

	private static void close(java.io.Closeable c) {
		if (c != null) {
			try { c.close(); } catch (IOException ignored) { /* closing */ }
		}
	}

	/**
	 * Whether the archive holds structure factors for an entry, and so whether a density server
	 * has anything to compute a map from.
	 *
	 * @param pdbId the entry
	 * @return <code>false</code> only when the inventory positively says there are none
	 */
	public boolean hasStructureFactors(PdbId pdbId) {
		if (pdbId == null) {
			return true;
		}
		return ! withoutStructureFactors.contains(pdbId.getId(true).toUpperCase());
	}

	/** @return how many entries the archive holds */
	public int getTotalEntries() {
		return totalEntries;
	}

	/** @return how many entries have a file in the given format */
	public int getEntriesInFormat(String fileFormat) {
		return UserConfiguration.PDB_FORMAT.equals(fileFormat) ? pdbEntries : mmcifEntries;
	}

	/**
	 * What a scan in this format cannot reach, in words, or <code>null</code> when it reaches
	 * everything.
	 *
	 * @param fileFormat the format a scan is about to use
	 * @return a sentence to show the user, or <code>null</code>
	 */
	public String coverageShortfall(String fileFormat) {
		int covered = getEntriesInFormat(fileFormat);
		int missing = totalEntries - covered;
		if (missing <= 0) {
			return null;
		}
		return "The " + fileFormat + " format covers " + covered + " of the archive's "
				+ totalEntries + " entries. " + missing + " entries have no " + fileFormat
				+ " file - they are too large for it - and no scan in this format can see them."
				+ " Use mmCIF to reach the whole archive.";
	}
}
