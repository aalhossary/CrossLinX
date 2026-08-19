package amralhossary.bonds;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.biojava.nbio.structure.ExperimentalTechnique;
import org.biojava.nbio.structure.PDBHeader;
import org.biojava.nbio.structure.PdbId;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.align.gui.jmol.JmolPanel;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.density.DensityCacheLayout;
import org.biojava.nbio.structure.io.density.DensityFileFormat;
import org.biojava.nbio.structure.io.density.DensityMapCache;
import org.biojava.nbio.structure.io.density.DensityMapKind;
import org.biojava.nbio.structure.io.density.DensityMapSource;

/**
 * The decisions behind fetching a density map, kept apart from the interface that shows one.
 * <p>
 * Everything here is a pure function of a structure and the settings, so it can be checked
 * without a display; {@link ParsingUI} owns the thread, the queue and the drawing.
 *
 * @see ParsingUI
 */
public class DensityService {

	/**
	 * What is known about a structure's density map, as shown against its row in the
	 * structures list.
	 * <p>
	 * {@link #NOT_FETCHED} describes the file, not our knowledge of it: probing the cache
	 * costs nothing and always answers, so "unknown" would never survive a moment, and
	 * "pending" would promise an arrival that is not coming while auto-fetch is off.
	 */
	public enum DensityState {
		/** Cached, and drawn if the viewer is showing this structure. */
		AVAILABLE,
		/** Waiting for the fetch thread. */
		QUEUED,
		/** Being downloaded now. */
		FETCHING,
		/** Not cached, and auto-fetch is off, so nothing is coming unless asked for. */
		NOT_FETCHED,
		/** This entry has no density map - an NMR ensemble, or every source said no. */
		NO_DENSITY,
		/** A download was attempted and the transport failed. Worth retrying. */
		FAILED
	}

	private DensityService() {
		//decisions only; nothing to instantiate
	}

	/**
	 * Which kind of map, if any, a structure could have.
	 * <p>
	 * The experimental method decides, never the model count: an NMR ensemble has no map
	 * however few models it has, and a multi-model crystal structure still has one. It is
	 * also never inferred from the resolution, which BioJava reads incorrectly for some
	 * cryo-EM entries (biojava#1000).
	 *
	 * @param structure the structure on screen
	 * @param configuredKind the user's preferred kind, a {@link DensityMapKind} file token
	 * @return the kind to ask for, or null when this entry cannot have a density map
	 */
	public static DensityMapKind kindFor(Structure structure, String configuredKind) {
		Set<ExperimentalTechnique> techniques = techniquesOf(structure);

		//Nothing recorded: ask for whatever exists rather than guess. Refusing to look is
		//the one answer that cannot be corrected by looking.
		if (techniques == null || techniques.isEmpty()) {
			return DensityMapKind.AUTO;
		}
		if (ExperimentalTechnique.isNmr(techniques)) {
			return null;
		}
		if (techniques.contains(ExperimentalTechnique.ELECTRON_MICROSCOPY)) {
			return DensityMapKind.EM;
		}
		//Electron crystallography deliberately falls through to AUTO rather than being
		//treated as cryo-EM: it is crystallography, its entries carry structure factors
		//rather than an EMDB volume, so a 2Fo-Fc map is the likelier answer - but not
		//reliably enough to name one outright.
		if (ExperimentalTechnique.isCrystallographic(techniques)
				&& ! techniques.contains(ExperimentalTechnique.ELECTRON_CRYSTALLOGRAPHY)) {
			return xrayKind(configuredKind);
		}
		return DensityMapKind.AUTO;
	}

	/** @return the user's kind if it is one an X-ray entry can serve, otherwise AUTO */
	private static DensityMapKind xrayKind(String configuredKind) {
		DensityMapKind kind = kindForToken(configuredKind);
		if (kind == DensityMapKind.TWO_FO_FC || kind == DensityMapKind.FO_FC) {
			return kind;
		}
		//The user asked for EM, or for something unrecognisable, on a crystal structure.
		return DensityMapKind.AUTO;
	}

	/**
	 * @param token a {@link DensityMapKind} file token such as {@code 2fofc}
	 * @return the matching kind, or {@link DensityMapKind#AUTO} when the token is unknown
	 */
	public static DensityMapKind kindForToken(String token) {
		if (token != null) {
			for (DensityMapKind kind : DensityMapKind.values()) {
				if (token.equalsIgnoreCase(kind.getFileToken())) {
					return kind;
				}
			}
		}
		return DensityMapKind.AUTO;
	}

	/** @return the techniques recorded for {@code structure}, or null if none are */
	private static Set<ExperimentalTechnique> techniquesOf(Structure structure) {
		if (structure == null) {
			return null;
		}
		PDBHeader header = structure.getPDBHeader();
		return header == null ? null : header.getExperimentalTechniques();
	}

	/**
	 * Builds a cache configured from the current settings.
	 * <p>
	 * Built per fetch rather than kept: {@code AtomCache} is constructed once in
	 * {@link ProteinParser} and never told about later settings changes, which is why
	 * toggling "Autofetch Files" has no effect (see KNOWN-ISSUES.md). Rebuilding costs
	 * almost nothing next to a download and cannot go stale.
	 *
	 * @param settings the live settings
	 * @return a cache honouring the folder, the fetch permission and the source order
	 */
	public static DensityMapCache newCache(SettingsManager settings) {
		String folder = settings.getDensityCacheFolder();
		DensityMapCache cache = (folder == null || folder.trim().isEmpty())
				? new DensityMapCache()
				: new DensityMapCache(folder);

		//Independent of the coordinate-file autoFetch: a complete local PDB mirror still
		//holds no density maps, and maps are far larger than the coordinates they belong to.
		cache.setFetchBehavior(settings.isAutoFetchElectronDensity()
				? FetchBehavior.FETCH_FILES
				: FetchBehavior.LOCAL_ONLY);

		List<DensityMapSource> xray = SettingsManager.parseSourceChain(
				settings.getDensityXraySources(), DensityMapCache.DEFAULT_XRAY_SOURCE_CHAIN);
		List<DensityMapSource> em = SettingsManager.parseSourceChain(
				settings.getDensityEmSources(), DensityMapCache.DEFAULT_EM_SOURCE_CHAIN);
		cache.setSourceChain(DensityMapKind.TWO_FO_FC, xray);
		cache.setSourceChain(DensityMapKind.FO_FC, xray);
		cache.setSourceChain(DensityMapKind.EM, em);
		cache.setMaxDownloadBytes(settings.getDensityMaxDownloadBytes());

		return cache;
	}

	/** @return where cached maps are written, for reporting to the user */
	public static File cacheRoot(SettingsManager settings) {
		return new File(newCache(settings).getCachePath());
	}

	/**
	 * Which sources can serve a box around a region rather than a whole unit cell.
	 * <p>
	 * This is the difference that decides whether a cross-link at a crystal contact gets any
	 * density at all: a whole-cell map runs from the origin, and coordinates outside that box
	 * have nothing to draw. Only the two Mol* volume servers understand a box request; PDBe's
	 * pre-computed CCP4 files and the wwPDB coefficients are whole-cell only.
	 */
	public static boolean servesBox(DensityMapSource source) {
		return source == DensityMapSource.RCSB_VOLUME_SERVER
				|| source == DensityMapSource.PDBE_VOLUME_SERVER;
	}

	/** @return the base URL of a volume server, or null if that source is not one */
	public static String volumeServerBase(DensityMapSource source) {
		if (source == DensityMapSource.RCSB_VOLUME_SERVER) {
			return "https://maps.rcsb.org/";
		}
		if (source == DensityMapSource.PDBE_VOLUME_SERVER) {
			return "https://www.ebi.ac.uk/pdbe/volume-server/";
		}
		return null;
	}

	/** The sampling detail asked of a volume server; see the note on the cache qualifier. */
	public static final int BOX_DETAIL = 3;

	/**
	 * Builds a box request around a region, in Angstroms.
	 * <p>
	 * The corners are Cartesian and may be negative: the server resolves them against the
	 * crystal symmetry itself, which is exactly why this works where a whole-cell map does not.
	 * Verified on 3ALB, whose cross-links all sit at negative z - 59,797 bytes here against
	 * 5,416,411 for the same entry's cell.
	 *
	 * @param source the volume server to ask
	 * @param pdbId the entry
	 * @param min lower corner
	 * @param max upper corner
	 * @return the URL, or null if this source cannot serve a box
	 */
	public static String boxUrl(DensityMapSource source, PdbId pdbId, double[] min, double[] max) {
		String base = volumeServerBase(source);
		if (base == null) {
			return null;
		}
		return String.format(Locale.US,
				"%sx-ray/%s/box/%.3f,%.3f,%.3f/%.3f,%.3f,%.3f?detail=%d&space=cartesian&encoding=bcif",
				base, pdbId.getId().toLowerCase(),
				min[0], min[1], min[2], max[0], max[1], max[2], BOX_DETAIL);
	}

	/**
	 * Where a box map is cached.
	 * <p>
	 * The qualifier carries the clip radius and the detail level, because both change what was
	 * downloaded. The region itself needs no part in the key: it is always the bounding box of
	 * every interacting atom of the structure, so for a given entry and radius it is the same
	 * box every time. That is what makes caching a box tractable here - there is no need to ask
	 * whether some previously cached box happens to contain the one now wanted.
	 *
	 * @param settings for the cache folder
	 * @param pdbId the entry
	 * @param kind the map kind
	 * @param source the volume server it came from
	 * @param radius the clip radius the box was built with
	 * @return the file, which need not exist
	 */
	public static File boxCacheFile(SettingsManager settings, PdbId pdbId, DensityMapKind kind,
			DensityMapSource source, double radius) {
		File root = cacheRoot(settings);
		String qualifier = String.format(Locale.US, "box-r%.1f-d%d", radius, BOX_DETAIL);
		return DensityCacheLayout.pdbMapFile(root, pdbId, kindToken(kind), source,
				DensityFileFormat.BCIF_VOLUME, qualifier);
	}

	/**
	 * A density-server response carries both the 2Fo-Fc and the Fo-Fc blocks in one file, so
	 * the two kinds share a cache entry rather than downloading the same bytes twice. This
	 * mirrors what BioJava's own volume-server provider does.
	 */
	private static String kindToken(DensityMapKind kind) {
		return (kind == DensityMapKind.TWO_FO_FC || kind == DensityMapKind.FO_FC)
				? DensityCacheLayout.BOTH_KINDS_TOKEN
				: kind.getFileToken();
	}
}
