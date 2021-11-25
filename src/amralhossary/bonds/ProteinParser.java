package amralhossary.bonds;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.biojava.nbio.structure.AminoAcid;
import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.AtomImpl;
import org.biojava.nbio.structure.Bond;
import org.biojava.nbio.structure.BondImpl;
import org.biojava.nbio.structure.Calc;
import org.biojava.nbio.structure.Chain;
import org.biojava.nbio.structure.EntityInfo;
import org.biojava.nbio.structure.Group;
import org.biojava.nbio.structure.HetatomImpl;
import org.biojava.nbio.structure.PDBHeader;
import org.biojava.nbio.structure.PdbId;
import org.biojava.nbio.structure.ResidueNumber;
import org.biojava.nbio.structure.Site;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.biojava.nbio.structure.io.FileParsingParameters;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.docopt.Docopt;

import amralhossary.bonds.SettingsManager.SettingListener;



public class ProteinParser implements SettingListener{

	private static final String CYCLO_CYCLIC_LASSO = "cyclo|cyclic|lasso";
	private static final String UBIQ = "ubiq";
	private static final String ADHESIN_ADHESION = "adhesin|adhesion";
	private static final String PILI_PILUS = "pili|pilus";
	private static final String FAILS = "Fails";
	private static final String EMPTY_FILES = "Empty Files";
	private static final String CHAINS_PARSED = "Chains parsed";
	private static final String CHAINS_SKIPPED = "Chains skipped";
	private static final String CHAINS_NOT_FOUND = "Chains NOT FOUND";
	private static final String AMINO_ACIDS_FOUND = "Amino Acids Found";
	private static final String HET_GROUPS_FOUND = "Het Groups Found";
//	private static final String ATOMS_NOT_FOUND = "Atoms NOT Found";
	private static final String AMINO_ACIDS_FAILED = "AminoAcids Failed";
	private static final String TOTAL_STRUCTURES_WITH_INTERACTIONS = "Total structures with interactions";
	private static final String ISOPEPTIDE_BONDS = "IsoPeptide Bonds";
	private static final String NOS_Bonds        = "NOS Bonds";
	private static final String NXS_Bonds        = "NxS Bonds (not known previously)";
	private static final String CSO_WITH_MISSING_O = "CSO with missing O";
	private static final String SUCCESSFULLY_PARSED_STRUCTURE_FILES = "successfully parsed structure files";
	private static final String ATTEMPTED_FILES = "attempted files";
	private static final String START_OF_STATISTICS = "=============== STATISTICS ===============";

	static final String ISOPEPTIDE		= "Isopeptide";
	static final String N_AA			= "N-terminus to C in side chain";
	static final String AA_C			= "N in side chain to C-terminus";
	static final String AA_HET			= "N in side chain to C in ligand";
	static final String HET_AA			= "N in ligand to C in side chain";
	static final String N_HET			= "N-terminus to C in ligand";
	static final String HET_C			= "N in ligand to to C-terminus";
	static final String NOS_BOND		= "NOS Bond";
	static final String NXS_BOND		= "NxS Bond";
	static final String ARG_CYS			= "ARG to CYS";
	static final String ARG_CSO			= "ARG to CSO";
	static final String HIS_CYS			= "HIS to CYS";
	static final String HIS_CSO			= "HIS to CSO";
	static final String ETHER_BOND		= "Ether bond";
	static final String LYS_TYR			= "LYS-TYR";
	static final String ARG_TYR			= "ARG-TYR";
	static final String HIS_TYR			= "HIS-TYR";
	static final String TYR_TYR 		= "TYR-TYR";
	static final String ESTER_BOND		= "Ester bond";
	static final String THIOETHER_BOND	= "ThioEther bond";
	static final String THIOESTER_BOND	= "ThioEster bond";
	static final String TO_C_TERMINUS	= "To C-Terminus";
	static final String INCLUDING_TYR 	= "Including Tyr";

	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public class ExploreTask extends RecursiveAction{
		private static final long serialVersionUID = 1L;
		private static final int CHUNK_SIZE = 5;
		List<String> ids;
		int startInclusive;
		int endExclusive;

		public ExploreTask(List<String> ids, int startInclusive, int endExclusive) {
			this.ids = ids;
			this.startInclusive = startInclusive;
			this.endExclusive = endExclusive;
		}

		public ExploreTask(List<String> ids) {
			this(ids, 0, ids.size());
		}

		@Override
		protected void compute() {
			if (endExclusive - startInclusive <= CHUNK_SIZE) {
				Hashtable<String, ArrayList<GroupOfInterest>> cubes = new Hashtable<String, ArrayList<GroupOfInterest>>();
				StringBuilder logStringBuilder = new StringBuilder();
				for (int i = startInclusive; moreWork && i < endExclusive; i++) {
					cubes.clear();
					logStringBuilder.setLength(0);
					String id = ids.get(i);
					parseStructure(id, cubes, logStringBuilder);
					System.out.println(logStringBuilder.toString());
				}
				System.out.println(String.format("Parsed entries from %d to %d.", startInclusive+1, endExclusive));
			} else {
			    int split = (endExclusive - startInclusive) / 2;
				invokeAll(new ExploreTask(ids, startInclusive, startInclusive + split),
			              new ExploreTask(ids, startInclusive + split, endExclusive));
			}
		}
		
	}

	//source, target, bond type, subtype.
	//This quadruplet structure is not the best option, but it is a fast one.
	static final String[] operations = new String[] {
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___TYR, INCLUDING_TYR, LYS_TYR,
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___TYR, INCLUDING_TYR, ARG_TYR,
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___TYR, INCLUDING_TYR, HIS_TYR,
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___GLU, ISOPEPTIDE, "",
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___ASP, ISOPEPTIDE, "",
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___GLN, ISOPEPTIDE, "",
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___ASN, ISOPEPTIDE, "",
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___GLU, ISOPEPTIDE, "",
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___ASP, ISOPEPTIDE, "",
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___GLN, ISOPEPTIDE, "",
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___ASN, ISOPEPTIDE, "",
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___GLU, ISOPEPTIDE, "",
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___ASP, ISOPEPTIDE, "",
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___GLN, ISOPEPTIDE, "",
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___ASN, ISOPEPTIDE, "",
			
			GroupOfInterest.NAME___1ST, GroupOfInterest.NAME___TYR, ISOPEPTIDE, N_AA+INCLUDING_TYR, // TODO review
			GroupOfInterest.NAME___1ST, GroupOfInterest.NAME___GLU, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME___1ST, GroupOfInterest.NAME___ASP, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME___1ST, GroupOfInterest.NAME___GLN, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME___1ST, GroupOfInterest.NAME___ASN, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___LST, ISOPEPTIDE, AA_C,
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___LST, ISOPEPTIDE, AA_C,
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___LST, ISOPEPTIDE, AA_C,
			GroupOfInterest.NAME___1ST, GroupOfInterest.NAME___HET, ISOPEPTIDE, N_HET, //new
			GroupOfInterest.NAME___HET, GroupOfInterest.NAME___LST, ISOPEPTIDE, HET_C, //new
			
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___HET, ISOPEPTIDE, AA_HET,
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___HET, ISOPEPTIDE, AA_HET,
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___HET, ISOPEPTIDE, AA_HET,
			GroupOfInterest.NAME___HET, GroupOfInterest.NAME___GLU, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME___HET, GroupOfInterest.NAME___ASP, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME___HET, GroupOfInterest.NAME___GLN, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME___HET, GroupOfInterest.NAME___ASN, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME___HET, GroupOfInterest.NAME___TYR, ISOPEPTIDE, HET_AA, //new
			
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___CSO, NOS_BOND, "",
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___CSO, NOS_BOND, ARG_CSO,
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___CSO, NOS_BOND, HIS_CSO,
			GroupOfInterest.NAME___LYS, GroupOfInterest.NAME___CYS, NXS_BOND, "",
			GroupOfInterest.NAME___ARG, GroupOfInterest.NAME___CYS, NXS_BOND, ARG_CYS,
			GroupOfInterest.NAME___HIS, GroupOfInterest.NAME___CYS, NXS_BOND, HIS_CYS,
//			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_SEC, NXS_BOND, "", //We know there should be no results
			//I ordered ester and Thioester before ether and thioether
			//Ester: Thr/Ser/Tyr <-> C-Terminous / Glu/Asp/Gln/Asn/Arg (/Gly ?) (2.1 A)
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___GLU, ESTER_BOND, "",
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___ASP, ESTER_BOND, "",
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___GLN, ESTER_BOND, "",
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___ASN, ESTER_BOND, "",
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___ARG, ESTER_BOND, "",
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___LST, ESTER_BOND, TO_C_TERMINUS,
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___GLU, ESTER_BOND, "",
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___ASP, ESTER_BOND, "",
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___GLN, ESTER_BOND, "",
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___ASN, ESTER_BOND, "",
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___ARG, ESTER_BOND, "",
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___LST, ESTER_BOND, TO_C_TERMINUS,
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___GLU, ESTER_BOND, "",
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___ASP, ESTER_BOND, "",
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___GLN, ESTER_BOND, "",
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___ASN, ESTER_BOND, "",
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___ARG, ESTER_BOND, "",
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___LST, ESTER_BOND, TO_C_TERMINUS,
			//Thioester: Cys <-> C-Terminous / Glu/Asp/Gln/Asn / ((Lys.CA)) (2.3 A)
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___GLU, THIOESTER_BOND, "",
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___ASP, THIOESTER_BOND, "",
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___GLN, THIOESTER_BOND, "",
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___ASN, THIOESTER_BOND, "",
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___ARG, THIOESTER_BOND, "",
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___LST, THIOESTER_BOND, TO_C_TERMINUS,
			
			
			//Ethers: O from the first to C from the second
			//Ether: Thr/Ser/Tyr <-> Any C ?
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___TYR, ETHER_BOND, "",
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___TYR, ETHER_BOND, "",
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___TYR, ETHER_BOND, TYR_TYR, //TODO check this
			GroupOfInterest.NAME___THR, GroupOfInterest.NAME___LST, ETHER_BOND, "",
			GroupOfInterest.NAME___SER, GroupOfInterest.NAME___LST, ETHER_BOND, "",
			GroupOfInterest.NAME___TYR, GroupOfInterest.NAME___LST, ETHER_BOND, "",
//			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_HET, ETHER_BOND, "",
//			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_HET, ETHER_BOND, "",
//			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_HET, ETHER_BOND, "",
			//Thioether (sulfide): Cys <-> any C (incl. Tyr CB/CD1/CD2/CE1/CE2) (2.3 A)
			//						6nef has Cys <-> HEC (heme) some within cutoff and some unrealisticly far
			//						6ef8 (same protein) has all of them within range although worse resolution
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___TYR, THIOETHER_BOND, "",
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___LST, THIOETHER_BOND, "",
			GroupOfInterest.NAME___CYS, GroupOfInterest.NAME___HET, THIOETHER_BOND, "",
			//DAminoAcids 5nf0 has Thioester with Dcy.SG
			
	};

	public void recalculateValues(){
//		HEXAGONAL_RING_RADIUS= settingsManager.getHexagonalRingDiameter() /2;
//		PENTAGONAL_RING_RADIUS= settingsManager.getPentagonalRingDiameter() /2;
//		HEXAGONAL_RING_CUTOFF= settingsManager.getHexagonalRingDiameterToCutoffRatio() * settingsManager.getHexagonalRingDiameter();
//		PENTAGONAL_RING_CUTOFF= settingsManager.getPentagonalRingDiameterToCutoffRatio() * settingsManager.getPentagonalRingDiameter();
//		HEXAGONAL_RING_RISE= HEXAGONAL_RING_CUTOFF/2;
//		PENTAGONAL_RING_RISE= PENTAGONAL_RING_CUTOFF/2;
//		HEXAGONAL_RING_DUMBBELL_RADIUS=Math.sqrt(HEXAGONAL_RING_RISE*HEXAGONAL_RING_RISE+HEXAGONAL_RING_RADIUS*HEXAGONAL_RING_RADIUS);
//		PENTAGONAL_RING_DUMBBELL_RADIUS=Math.sqrt(PENTAGONAL_RING_RISE*PENTAGONAL_RING_RISE+PENTAGONAL_RING_RADIUS*PENTAGONAL_RING_RADIUS);
	}
	
	static final double CUBE_SIDE = 3.1; //2.8; //9.2;//4.6;
	
	//	String token=null;
	FileParsingParameters parameters = new FileParsingParameters();

	private long attemptedPDBFiles;
	private long successfullyParsedStructures;
	private long failedToParseStructure;
	private long failedToParseAminoAcids;
	private long emptyFiles;
	private long chainsParsed;
	private long chainsSkipped;
	private long chainsNotFound;
	private long foundAminoAcids;
	private long foundHetGroups;
//	private long processedAtoms;
	
//	static long totalFoundHetGroupsOfInterest;
	       long totalFoundStructuresWithInteractions;

	private long isopeptideBonds;
	private long NOSBonds;
	private long NxSBonds;
	
//	private long missingAtoms;//TODO complete it

	private long startTime;
	
	static SettingsManager settingsManager = null; 
	AtomCache atomCache = new AtomCache(SettingsManager.getSettingsManager().getUserConfiguration());

//	{
//		atomCache.setUseMmCif(false);
//		atomCache.setUseMmtf(false);
//	}
	private ProteinParsingGUI gui;
	static boolean moreWork;
	private PrintStream out;
	private PrintStream tsvOut;
	PrintStream log= System.out;
	void initialize() {
		moreWork = true;
		recalculateValues();
		this.attemptedPDBFiles=0;
		this.successfullyParsedStructures=0;

		
		this.failedToParseAminoAcids=0;
		this.failedToParseStructure=0;
		this.emptyFiles=0;
		this.chainsParsed=0;
		this.chainsSkipped=0;
		this.chainsNotFound=0;
		this.foundAminoAcids=0;
		this.foundHetGroups=0;
//		this.missingAtoms=0;

//		totalFoundHetGroupsOfInterest = 0;
		totalFoundStructuresWithInteractions = 0;

		this.isopeptideBonds = 0;
		this.NOSBonds = 0;
		this.NxSBonds = 0;
		
//		cubes.clear();
	}
	
	public static void main (String [] args) throws IOException {
		
	    String version = getVersion();
		InputStream docStream = ProteinParser.class.getResourceAsStream("/doc.txt");
		Docopt docopt = new Docopt(docStream);
		Map<String, Object> options = docopt.withVersion(version).parse(args);
		
//		Set<Entry<String,Object>> entrySet = options.entrySet();
//		for (Entry<String, Object> entry : entrySet) {
//			String key = entry.getKey();
//			Object value = entry.getValue();
//			String valueName = value != null? value.getClass().getName() : "NULL";
//			System.out.format("%s:\t(%s)\t%s\n", key, valueName, value);
//		}
//		System.out.println(options);
		
		boolean all = (boolean) options.get("all");
		boolean fromFile = (boolean) options.get("from-file");
		boolean fromList = (boolean) options.get("from-list");
		boolean noGui = (boolean) options.get("--no-gui");

		
		settingsManager = SettingsManager.getSettingsManager();
		
		
		
		Scanner scanner = null;
		String pdbDir = (String) options.get("--pdb-dir");
		if(pdbDir != null) {
			settingsManager.setPdbFilePath(pdbDir);
		}
		String homeDir = (String) options.get("--home-dir");
		if(homeDir != null) {
			settingsManager.setWorkingFolder(homeDir);
		}

		if(all && (fromFile || fromList)) {
			System.err.println("all can not be used with any of from-file or from-list");
			System.exit(-2);
		}
		
		if (noGui) {
			moreWork=true;
			if(all || (! fromFile && ! fromList)) {
				System.out.println("ALL");
				boolean listOnly = (boolean) options.get("--list-only");
				File listFile = ResultManager.prepareFilesList(!listOnly);
				if(listOnly) {
					System.exit(0);
				}
				scanner = new Scanner(new FileReader(listFile));
			}else if (fromFile) {
				System.out.println("FROM FILE");
				scanner = new Scanner(new FileReader((String) options.get("<FILE>")));
			}else if (fromList) {
				System.out.println("FROM LIST");
				String list = (String) options.get("<LIST>");
				scanner = new Scanner(list);
			}

			ProteinParser proteinParser = new ProteinParser();
			proteinParser.startParsing(scanner);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ParsingUI application = new ParsingUI();
					application.getJFrame().setVisible(true);
				}
			});
		}
	}

	public static String getVersion() {
		final String ERROR = "ERROR";
		try {
			if ((new File("pom.xml")).exists()) {
				MavenXpp3Reader reader = new MavenXpp3Reader();
				Model model = reader.read(new FileReader("pom.xml"));
				return model.getVersion();
			}
			else {
				final String versionFileName = "version.properties";
				final String versionPropertyName = "app.ver";
				Properties properties=new Properties();
				InputStream res = ClassLoader.getSystemResourceAsStream(versionFileName);
				properties.load(res);
				return properties.containsKey(versionPropertyName)?properties.getProperty(versionPropertyName):ERROR;
			}
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
			return ERROR;
		}
	}
	
	
	public ProteinParser() {
		this(null);
	}
	
	public ProteinParser(ProteinParsingGUI gui){
		this.gui=gui;
		settingsManager = SettingsManager.getSettingsManager();
		settingsManager.registerListener(this);
		refreshSettings();
	}


	public void refreshSettings() {
		recalculateValues();
		String path=settingsManager.getPdbFilePath();
		if(path != null)
			atomCache.setPath(path);
//		pdbreader.setAutoFetch(true);
//		atomCache.setFetchBehavior(settingsManager.getFetchBehavior());
		
		parameters.setAlignSeqRes(true);
		parameters.setHeaderOnly(false);
//		parameters.setLoadChemCompInfo(true);
		parameters.setParseCAOnly(false);
		parameters.setParseSecStruc(false);//we don't need it

		atomCache.setFileParsingParams(parameters);
//		atomCache.setUseMmCif(false);
//		atomCache.setUseMmtf(false);
	}

	
	
	public void startParsing(Scanner scanner) {
		initialize();
		System.out.println("File Format: "+ settingsManager.getFileFormat());
		fixStartTime();
		parseStructureNamesList(scanner);
		if (gui != null) {
			gui.showResults(null);
		}
		String printableStatistics = getPrintableStatistics();
		System.out.println(printableStatistics);
		this.out.println(printableStatistics);
	}


	
	
	void parseStructureNamesList(Scanner scanner) {
		File positiveResultsFile=null;
		File tsvPositiveResultsFile=null;
		try {
			String workingFolder = settingsManager.getWorkingFolder();
			positiveResultsFile = new File(workingFolder, "PositiveResults.txt");
			if (! positiveResultsFile.exists()) {
				positiveResultsFile.createNewFile();
			}
			this.out= new PrintStream(positiveResultsFile);

			tsvPositiveResultsFile = new File(workingFolder, "PositiveResults.tsv");
			if (! tsvPositiveResultsFile.exists()) {
				tsvPositiveResultsFile.createNewFile();
			}
			writeTsvHeader(tsvPositiveResultsFile);


			File log=null;
			log = new File(workingFolder, "log.txt");
			if (! log.exists()) {
				log.createNewFile();
			}
			this.log= new PrintStream(log);
		} catch (IOException e) {
			System.err.println("couldn't create file ["+positiveResultsFile.getAbsolutePath()+"] for output");
			e.printStackTrace();
		}
		List<String> allPdbIds = new ArrayList<String>(20000);
		while (scanner.hasNextLine() && moreWork) {
			String line = scanner.nextLine();
			if (line.startsWith("##")) {
				System.out.println("Entering "+line.substring(2));
			}else{
				StringTokenizer tokenizer = new StringTokenizer(line, ";", false);
				while (tokenizer.hasMoreTokens() && moreWork) {
					String token = tokenizer.nextToken().trim();
					allPdbIds.add(token);
//					parseStructure(token);
				}
			}
		}
		System.out.println("Total number of structures = "+ allPdbIds.size());
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		System.out.println("Parallelism = "+ ForkJoinPool.getCommonPoolParallelism());
		forkJoinPool.invoke(new ExploreTask(allPdbIds));
		System.out.println("after calling parallel running");
	}

	public void importResultsFile(Scanner scanner) {
		File log=null;
		try {
			log = new File(settingsManager.getWorkingFolder(), "log.txt");
			if (! log.exists()) {
				log.createNewFile();
			}
			this.log= new PrintStream(log);
		} catch (IOException e) {
			System.err.println("couldn't create file ["+log.getAbsolutePath()+"] for output");
			e.printStackTrace();
		}
		
		parseFromScanner(scanner, true);
//		totalFoundStructuresWithInteractions += allPersistedInteractions.keySet().size();
//		System.out.println(getPrintableStatistics());
	}
	
	public void parseFromScanner(Scanner scanner, boolean imported) {
		int count = 1;
		final String startOfStructurePrefix = ResultManager.START_OF_STRUCTURE_PREFIX;
		final int tokenNamePosition = startOfStructurePrefix.length();
		while (scanner.hasNextLine() && moreWork) {
			String line = scanner.nextLine().trim();
			if (line.length()!= 0) {
				// retrieving code here
				if (line.startsWith(startOfStructurePrefix)) {
					String token = line.substring(tokenNamePosition);
					int endIdx = token.length(), dotIdx = token.lastIndexOf('.');
					if(dotIdx > 0)
						endIdx = dotIdx;
					final PdbId pdbId = new PdbId(token.substring(0, endIdx));
					List<String> bonds = new ArrayList<>();
					while (scanner.hasNextLine() && moreWork) {
						String bondString = (String) scanner.nextLine();
						if(bondString.length() != 0) {
							bonds.add(bondString);
						} else {
							//End of block. All bonds parsed.
							//persist everything
//								//1) Persist 
//								//		a] file loading 
//								//		b] file formatting 
//								//		c] TODO (+/-) ED Map loading scripts
//								//to a file (called when token is selected)
//								ResultManager.exportFileLoadingScript(pdbId, ResultManager.createCacheFolderForToken(pdbId));
							// 2) Persist list of bonds in a file.
							ResultManager.persistBondsList(pdbId, bonds);
							if (gui != null) {
								//populate found interactions in structures
								if(imported) {
									Boolean prev = settingsManager.isShowWhileProcessing();
									settingsManager.setShowWhileProcessing(false);
									gui.interactionsFoundInStructure(pdbId);
									settingsManager.setShowWhileProcessing(prev);
								}else {
									gui.interactionsFoundInStructure(pdbId);
								}
							}
							bonds.clear();

							if (count % 1000 == 0) {
								System.out.println(count+" structures parsed");
							}
							break;
						}
					}
					if (bonds.size() > 0) {
						ResultManager.persistBondsList(pdbId, bonds);
						if (gui != null) {
							//populate found interactions in structures
							if(imported) {
								Boolean prev = settingsManager.isShowWhileProcessing();
								settingsManager.setShowWhileProcessing(false);
								gui.interactionsFoundInStructure(pdbId);
								settingsManager.setShowWhileProcessing(prev);
							}else {
								gui.interactionsFoundInStructure(pdbId);
							}
						}
					}
					//note that selecting a structure should populate the interactions list
					//and selecting an interaction from the list should focus on it +/- show electron density

					count++;
				} else if (line.startsWith(ProteinParser.START_OF_STATISTICS)) {
					System.out.println("Parsed total "+count+" structures.");
					while (scanner.hasNextLine() && moreWork) {
						line=scanner.nextLine();
						if (line.contains("%")) {
							continue;//this is a derived value
						}
						final int indexOfSeparator = line.indexOf('\t');
						if (indexOfSeparator > 0) {
							long value = Long.parseLong(line.substring(0, indexOfSeparator));
							String key = line.substring(indexOfSeparator+1);
							switch (key) {
							case ATTEMPTED_FILES:
								attemptedPDBFiles += value;
								break;
							case SUCCESSFULLY_PARSED_STRUCTURE_FILES:
								successfullyParsedStructures += value;
								break;
							case FAILS:
								failedToParseStructure += value;
								break;
							case EMPTY_FILES:
								 emptyFiles+= value;
								break;
							case CHAINS_PARSED:
								 chainsParsed+= value;
								break;
							case CHAINS_SKIPPED:
								 chainsSkipped+= value;
								break;
							case CHAINS_NOT_FOUND:
								 chainsNotFound+= value;
								break;
							case AMINO_ACIDS_FOUND:
								 foundAminoAcids+= value;
								break;
							case HET_GROUPS_FOUND:
								 foundHetGroups+= value;
								break;
							case AMINO_ACIDS_FAILED:
								 failedToParseAminoAcids+= value;
								break;
							case TOTAL_STRUCTURES_WITH_INTERACTIONS:
								 totalFoundStructuresWithInteractions+= value;
								break;
//							case ATOMS_NOT_FOUND:
//								missingAtoms+= value;
//								break;
							case ISOPEPTIDE_BONDS:
								 isopeptideBonds+= value;
								break;
							case NOS_Bonds:
								 NOSBonds+= value;
								break;
							default:
								//unknown , ignore
								break;
							}
						}
					}
				}
			}
		}
	}

	void fixStartTime() {
		startTime = System.currentTimeMillis();
	}
	
	String getPrintableStatistics() {
		final StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		long endTime = System.currentTimeMillis();
		long milliSeconds = endTime-startTime;
		int days = (int) (milliSeconds /	(1000*60*60*24));
		milliSeconds %= 					(1000*60*60*24); // after complete days
		int hours = (int) (milliSeconds /	(1000*60*60));
		milliSeconds %= 					(1000*60*60);  // after complete hours
		int minutes = (int) (milliSeconds /	(1000*60));
		milliSeconds %= 					(1000*60);  // after complete minutes
		int seconds = (int) (milliSeconds /	(1000));
		milliSeconds %= 					(1000);  // after complete soconds
		
		writer.println(ProteinParser.START_OF_STATISTICS);
		writer.println(this.attemptedPDBFiles+"\t"+ProteinParser.ATTEMPTED_FILES);
		writer.println(this.successfullyParsedStructures+"\t" +ProteinParser.SUCCESSFULLY_PARSED_STRUCTURE_FILES);
		writer.println(this.failedToParseStructure+"\t" +ProteinParser.FAILS);
		writer.println(this.emptyFiles+"\t" +ProteinParser.EMPTY_FILES);
		writer.println(this.chainsParsed+"\t" +ProteinParser.CHAINS_PARSED);
		writer.println(this.chainsSkipped+"\t" +ProteinParser.CHAINS_SKIPPED);
		writer.println(this.chainsNotFound+"\t" +ProteinParser.CHAINS_NOT_FOUND);
		writer.println(this.foundAminoAcids+"\t" +ProteinParser.AMINO_ACIDS_FOUND);
		writer.println(this.foundHetGroups+"\t" +ProteinParser.HET_GROUPS_FOUND);
//		writer.println(this.processedAtoms+"\t Atoms Processed");
//		writer.println(this.missingAtoms+"\t" +ProteinParser.ATOMS_NOT_FOUND);
		writer.println(this.failedToParseAminoAcids+"\t" +ProteinParser.AMINO_ACIDS_FAILED);
		writer.println();
		writer.println(totalFoundStructuresWithInteractions + "\t" +ProteinParser.TOTAL_STRUCTURES_WITH_INTERACTIONS);

		writer.println(isopeptideBonds + "\t" +ProteinParser.ISOPEPTIDE_BONDS);
		writer.println(NOSBonds        + "\t" +ProteinParser.NOS_Bonds);
		writer.println(NxSBonds        + "\t" +ProteinParser.NXS_Bonds);
		writer.println();
		writer.println(getPercent(totalFoundStructuresWithInteractions, successfullyParsedStructures,2) + "%\t" +"structures with interactions");
		writer.println();
		
		writer.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		writer.println();
		

		writer.println("processing done in "+ days + " day(s), "+ hours + 
				" hour(s), " + minutes+" minute(s), "+seconds+" second(s), " + 
				milliSeconds+" millisecond(s)");
		return stringWriter.toString();
	}

	private float getPercent(long x, long y, int decimalPrecesion) {
		final double fractions = Math.pow(10, decimalPrecesion);
		return (float) (Math.round((fractions*100.0*x)/y)/fractions);
	}

	/** 
	 * @see #foundInteractions
	 * @param token
	 */
	public boolean parseStructure(String token, Hashtable<String, ArrayList<GroupOfInterest>> cubes, StringBuilder logStringBuilder) {
		boolean chainsInStructureParsedSuccessfully = parseChainsInStructure(token, cubes, logStringBuilder);
		if (chainsInStructureParsedSuccessfully) {
			Map<GroupOfInterest, Set<Bond>> foundInteractions = findInteractionsInCubes(cubes, logStringBuilder);
			
			if (foundInteractions.size() > 0){
				totalFoundStructuresWithInteractions++;
				// convert foundInteractions into listofDetailedConnectionsAsString (with coords)
				Set<Bond> allBonds = new LinkedHashSet<Bond>();
				Iterator<Set<Bond>> bondsIterator = foundInteractions.values().iterator();
				while(bondsIterator.hasNext()) {
					Set<Bond> bonds = bondsIterator.next();
					allBonds.addAll(bonds);
				}
				String listofDetailedConnectionsAsString = ResultManager.createListofConnectionsAsString(allBonds);				
				
				//log it to positiveresults.txt
				StringBuilder stringForParsablePositiveResultsFileSB = new StringBuilder(ResultManager.START_OF_STRUCTURE_PREFIX).append(token).append(System.getProperty("line.separator"));
				stringForParsablePositiveResultsFileSB.append(listofDetailedConnectionsAsString);
				String stringForParsablePositiveResultsFile = stringForParsablePositiveResultsFileSB.toString();
				this.out.println(stringForParsablePositiveResultsFile);

				//reduce the string by removing atom coordinates ==> xxxxxxxxNoCoords
				String listofConnectionsAsStringNoCoords = ResultManager.removeAtomCoords(listofDetailedConnectionsAsString);
				// show it to the user and in log
				logStringBuilder.append(listofConnectionsAsStringNoCoords).append('\n');
				//note that ResultManager.removeAtomCoords() is called in parseFromScanner() as well.
				
				//parse listofDetailedConnectionsAsString
				parseFromScanner(new Scanner(stringForParsablePositiveResultsFile), false);
			}
			return true;
		}
		return false;
	}

	boolean parseChainsInStructure(String token, Hashtable<String, ArrayList<GroupOfInterest>> cubes, StringBuilder logStringBuilder) {
		this.attemptedPDBFiles++;
		boolean structureParsedSuccessfully = true;
		try {
			
			String structureName = token.substring(0, 4);
			String extension = token.substring(4);
			logStringBuilder.append('\n');
			logStringBuilder.append("Starting to parse ").append(token).append(": ").append(structureName).append("\t").append(extension).append('\n');
			Structure currentStructure = atomCache.getStructure(structureName);
			
			if (gui != null) {
				gui.structureLoaded(currentStructure);
			}
			this.successfullyParsedStructures++;
//			Hashtable<String, ArrayList<GroupOfInterest>> cubes = ProteinParser.cubes;
			cubes.clear();
			
			List<Chain> chains = currentStructure.getChains(); //TODO I get all chains of the FIRST model. Consider getting all models and updating output identifiers
			if (chains==null || chains.isEmpty()) {
				this.emptyFiles++;
				logStringBuilder.append("Unexpected error: NO chains are found AT ALL in the file ").append(structureName).append('\n');
				return false;
			}
			boolean chainFound=false;
			if (extension== null || extension.equals("")) {
				for (Chain chain : chains) {
					parseChain(chain, cubes, logStringBuilder);
				}
				chainFound=true;
			} else{
				for (Chain chain : chains) {
					if (extension.equalsIgnoreCase(chain.getName())) {
						parseChain(chain, cubes, logStringBuilder);
						chainFound=true;
					}else {
						logStringBuilder.append("skipped chain ").append(chain.getName()).append('\n');
						this.chainsSkipped++;
					}
				}
			}
			if(! chainFound){
				logStringBuilder.append("Unexpected error: chain ID ").append(extension).append(" NOT FOUND in file ").append(token).append('\n');
				this.chainsNotFound++;
			}
		} catch (IOException e) {
			logStringBuilder.append("NOT FOUND or Failed to parse").append('\n');
			this.failedToParseStructure++;
			structureParsedSuccessfully=false;
		} catch (StructureException e) {
			logStringBuilder.append("Failed to parse").append('\n');
			this.failedToParseStructure++;
			structureParsedSuccessfully=false;
		}
		return structureParsedSuccessfully;
	}

	/**
	 * This method finds interactions and report its output in an Map <{@link GroupOfInterest}, {@link Set}<{@link Bond}>.
	 * Additionally, all log text is collected via logStringBuilder, in order not to mix with other threads.
	 * @param cubes
	 * @param logStringBuilder
	 * @return
	 */
	Map<GroupOfInterest, Set<Bond>> findInteractionsInCubes(Hashtable<String, ArrayList<GroupOfInterest>> cubes, StringBuilder logStringBuilder) {

		Hashtable<GroupOfInterest, Set<Bond>> foundInteractions = new Hashtable<>();
		
		for (int op = 0; moreWork && op < operations.length; op += 4) {
			String sourceSuffix = operations[op];
			String targetSuffix = operations[op+1];
			String operation = operations[op+2];
			String subOperation = operations[op+3];
			
			Enumeration<String> cubeNames = cubes.keys();
			while (cubeNames.hasMoreElements() && moreWork) {
				String sourceCubeName = cubeNames.nextElement();
				if (! sourceCubeName.endsWith(sourceSuffix)) {
					continue;
				}
				//residue1 was previously Lysine. It usually contains N
				ArrayList<GroupOfInterest> residue1List = cubes.get(sourceCubeName);
				StringTokenizer stringTokenizer = new StringTokenizer(sourceCubeName,"|");
				int x, y, z;
				try {
					x = Integer.parseInt(stringTokenizer.nextToken());
					y = Integer.parseInt(stringTokenizer.nextToken());
					z = Integer.parseInt(stringTokenizer.nextToken());
				} catch (NumberFormatException e) {
					e.printStackTrace();
					return null;
				}
				String destCubeBaseName=null;
				
				HashSet<Bond> tempInteractions = new LinkedHashSet<>(); //to decrease frequency of unnecessary objects creation
				for (int ai = 0; ai < residue1List.size(); ai++) {
					GroupOfInterest residue1 = (GroupOfInterest) residue1List.get(ai);
					for (int i = x-1; i <=x+1 ; i++) {
						for (int j = y-1; j <= y+1; j++) {
							for (int k = z-1; k <= z+1 ; k++) {
								destCubeBaseName= i+"|"+j+"|"+k;
								Set<Bond> interactions = foundInteractions.get(residue1);
								if (interactions == null) {
									interactions = tempInteractions;
								}
								ArrayList<GroupOfInterest> destCubeOfGroupsOfInterest = cubes.get(destCubeBaseName+"|"+targetSuffix);
								if (destCubeOfGroupsOfInterest == null)
									continue;
								for (GroupOfInterest residue2 : destCubeOfGroupsOfInterest) {
//									//I removed this line on purpose, to see if 2 residues are linked using more than one bond of the same type.
//									if (interactions.contains(residue2)) {//TODO change
//										continue;
//									}
									boolean confirmedLink = confirmLink(residue1, residue2, operation, subOperation, interactions);

									if (confirmedLink) {
										break;
									}					
								}
								if (interactions.size() > 0) {
									foundInteractions.put(residue1, interactions);
									tempInteractions = new LinkedHashSet<>();
								}
							}
						}
					}
				}
			}
		}
		return foundInteractions; 
	}

	private void writeTsvHeader(File tsvPositiveResultsFile) throws FileNotFoundException {
		this.tsvOut = new PrintStream(tsvPositiveResultsFile);
		this.tsvOut.print("PDB ID\tRes1\tC1\tRes1#\tA1\tAltLoc1\t<-\tDistance\t->\tRes2\tC2\tRes2#\tA2\tAltLoc2\t");
		this.tsvOut.print("bond type\tsubtype / comments\tResolution\tRFree\tDep Date\tRel Date\tMod Date\tSource Organism Scientific\t");
		this.tsvOut.print(String.format("%s\t%s\t%s\t%s\t", PILI_PILUS, ADHESIN_ADHESION, UBIQ, CYCLO_CYCLIC_LASSO));
		this.tsvOut.println("Title");
	}

	private void outputTsv(Atom atom1, Atom atom2, double distance, String operation, String subOperation) {
		Group residue1 = ((AtomImpl) atom1).getGroup();
		Group residue2 = ((AtomImpl) atom2).getGroup();
		Structure structure = residue1.getChain().getStructure();
		StringBuilder str = new StringBuilder(structure.getPdbId().getId()).append('\t');
		
		str.append(residue1.getPDBName()).append('\t')
		.append(residue1.getChain().getName()).append('\t') // I am reporting the chain authId (the one written in PDB) now.
		.append(residue1.getResidueNumber().toString()).append('\t').append(atom1.getName()).append('\t');
		final Character altLoc1 = atom1.getAltLoc();
		str.append(altLoc1 == null ? "" : altLoc1).append('\t');
		
		str.append("<-\t").append(String.format("%.3f", distance)).append("\t->\t");
		
		str.append(residue2.getPDBName()).append('\t')
		.append(residue2.getChain().getName()).append('\t') // I am reporting the chain authId (the one written in PDB) now.
		.append(residue2.getResidueNumber().toString()).append('\t').append(atom2.getName()).append('\t');
		final Character altLoc2 = atom2.getAltLoc();
		str.append(altLoc2 == null ? "" : altLoc2).append('\t');
		
		str.append(operation).append('\t').append(subOperation).append('\t');

		PDBHeader pdbHeader = structure.getPDBHeader();
		str.append(pdbHeader.getResolution()).append('\t').append(pdbHeader.getRfree()).append('\t');
		
		Date depDate = pdbHeader.getDepDate();
		Date relDate = pdbHeader.getRelDate();
		Date modDate = pdbHeader.getModDate();
		if (modDate == null || modDate.equals(new Date(0)) ) {  // TODO remove this when you upgrade to new BioJava
			modDate = relDate;
		}
		str.append(simpleDateFormat.format(depDate)).append('\t')
		.append(simpleDateFormat.format(relDate)).append('\t')
		.append(simpleDateFormat.format(modDate)).append('\t');
		
//		System.out.format("%s\t%s\t%s\n", simpleDateFormat.format(depDate), simpleDateFormat.format(relDate), simpleDateFormat.format(modDate));
		
		//Source Organism Scientific
		StringBuilder temp = new StringBuilder();
		HashSet<String> strings = new HashSet<>();
		List<EntityInfo> entityInfos = structure.getEntityInfos();
		for (EntityInfo entityInfo : entityInfos) {
			String organismScientific = entityInfo.getOrganismScientific();
			if (organismScientific != null && organismScientific.length() > 0) {
				if(strings.contains(organismScientific))
					continue;
				if(temp.length() > 0) {
					temp.append(" | ");
				}
				temp.append(organismScientific);
				strings.add(organismScientific);
			}
		}
		str.append(temp);
		temp.setLength(0);

		//Binary checks
		ArrayList<String> allStringsToScan = new ArrayList<>();
		allStringsToScan.add(pdbHeader.getTitle());
		for (EntityInfo entityInfo : entityInfos) {
			String organismScientific = entityInfo.getOrganismScientific();
			allStringsToScan.add(organismScientific);
			String details = entityInfo.getDetails();
			allStringsToScan.add(details);
			String expressionSystemOtherDetails = entityInfo.getExpressionSystemOtherDetails();
			allStringsToScan.add(expressionSystemOtherDetails);
			String description = entityInfo.getDescription();
			allStringsToScan.add(description);
			List<String> synonyms = entityInfo.getSynonyms();
			if (synonyms != null) {
				for (String synonym : synonyms) {
					allStringsToScan.add(synonym);
				}
			}
		}
		List<Site> sites = structure.getSites();
		for (Site site : sites) {
			String description = site.getDescription();
			allStringsToScan.add(description);
		}
		List<String> keywords = pdbHeader.getKeywords();
		for (String keyword : keywords) {
			allStringsToScan.add(keyword);
		}
		if(structure.getJournalArticle() != null)
			allStringsToScan.add(structure.getJournalArticle().getTitle());
		
		outputBinaryTestFind(str, allStringsToScan, PILI_PILUS);
		outputBinaryTestFind(str, allStringsToScan, ADHESIN_ADHESION);
		outputBinaryTestFind(str, allStringsToScan, UBIQ);
		outputBinaryTestFind(str, allStringsToScan, CYCLO_CYCLIC_LASSO);
		
		str.append('\t').append("\"").append(pdbHeader.getTitle()).append("\"");
		
		this.tsvOut.println(str);
	}

	private void outputBinaryTestFind(StringBuilder str, ArrayList<String> allStringsToScan, String query) {
		String found;
		found = findKeyword(allStringsToScan, query);
		if(found != null) {
			str.append('\t').append(found);
		}else {
			str.append("\t0");
		}
	}

	private static String findKeyword(ArrayList<String> stringsToScan, String keywordRegEx) {
		Pattern pattern = Pattern.compile(String.format("((\\w+ +)?((\\w*)(%s)(\\w*))( +\\w+)?)", keywordRegEx), Pattern.CASE_INSENSITIVE);
		for (String string : stringsToScan) {
			if (string == null) {
				continue;
			}
			Matcher matcher = pattern.matcher(string);
			if(matcher.find()) {
//				System.out.println(matcher.group(1));
				return matcher.group(1);
			}
		}
		return null;
	}

	private boolean confirmLink(GroupOfInterest group1, GroupOfInterest group2, String operation, String subOperation, Set<Bond> interactions) {
		//check the group type and set parameters and output list
		@SuppressWarnings("unused")
		float cutoff, cutoff2;
		Atom[] atoms1=null, atoms2=null;
		
		//Note that I use the == operator here
		if (operation == ISOPEPTIDE) {
			atoms1 = group1.getKeyNAtoms();
			atoms2 = group2.getKeyCAtoms();
			cutoff = 2.1f;
			cutoff2= 4.41f;

			if(subOperation == AA_C) {
				atoms2 = new Atom[] {group2.getAtom("C")};
			}else if(subOperation == N_AA) {
				atoms1 = new Atom[] {group1.getAtom("N")};
			}
		}else if (operation == INCLUDING_TYR) { //LYS_TYR, ARG_TYR, HIS_TYR
			cutoff = 2.2f;
			cutoff2= 4.84f;
		}else if (operation == NXS_BOND) {
			atoms1 = group1.getKeyNAtoms();
			atoms2 = group2.getKeyAtoms(); // S atom
			cutoff = 3.1f;
			cutoff2= 9.61f;
			//TODO Do we need to implement SelinoCystein?
		}else if (operation == NOS_BOND) {
			atoms1 = group1.getKeyNAtoms();
			atoms2 = group2.getKeyOAtoms();  //unusual OD atom
			cutoff = 2.1f;
			cutoff2= 4.41f;
			if (atoms2[0] == null) {
				return confirmLink(group1, group2, NXS_BOND, CSO_WITH_MISSING_O, interactions);
			}
		}else if (operation == ESTER_BOND) {
			atoms1 = group1.getKeyOAtoms();
			if(subOperation == TO_C_TERMINUS)
				atoms2 = new Atom[] {group2.getAtom("C")};
			else
				atoms2  = group2.getKeyAtoms();
			cutoff = 2.1f;
			cutoff2= 4.41f;
		}else if(operation == ETHER_BOND) {
			if(group1.getResidueNumber().equals(group2.getResidueNumber()) && group1 instanceof AminoAcidOfInterest && ((AminoAcidOfInterest)group1).aAOfInterestType==GroupOfInterest.CODE_TYR)
				return false;
			atoms1 = group1.getKeyOAtoms();
			atoms2 = group2.getKeyCAtoms();
			cutoff = 2.5f;
			cutoff2= 6.25f;
		}else if(operation == THIOESTER_BOND) {
			atoms1  = group1.getKeyAtoms();
			if(subOperation == TO_C_TERMINUS)
				atoms2 = new Atom[] {group2.getAtom("C")};
			else
				atoms2  = group2.getKeyAtoms();
			cutoff = 2.3f;
			cutoff2 = 5.29f;
		}else if(operation == THIOETHER_BOND) {
			atoms1  = group1.getKeyAtoms();
			atoms2  = group2.getKeyCAtoms();
			cutoff  = 2.3f;
			cutoff2 = 5.29f;
		}else {
			throw new IllegalArgumentException("Unknown Operation: "+ operation);
		}
		
		if(atoms1 == null || atoms2 == null) {
			return false;
		}
		boolean confirmed = false;
outer:		for (int i = 0; !confirmed && i < atoms1.length; i++) {
			Atom atom1 = atoms1[i];
			if(atom1 == null)
				continue;
			for (int j = 0; !confirmed && j < atoms2.length; j++) {
				Atom atom2 = atoms2[j];
				if(atom2 == null)
					continue;
				
				if(alreadyReported(atom1, atom2, interactions))
					continue;
				
				
				double distanceSquared = Calc.getDistanceFast(atom1, atom2);
				if (distanceSquared <= cutoff2) {
					confirmed = true;
					interactions.add(new BondImpl(atom1, atom2, 1, false));
					outputTsv(atom1, atom2, Math.sqrt(distanceSquared), operation, subOperation);
					break outer;
				}
			}
		}
		return confirmed;
	}

	private boolean alreadyReported(Atom atom1, Atom atom2, Set<Bond> interactions) {
		for (Iterator<Bond> iterator = interactions.iterator(); iterator.hasNext();) {
			Bond bond = iterator.next();
			if(atom1 == bond.getAtomA() && atom2 == bond.getAtomB())
				return true;
		}
		return false;
	}

	/**
	 * @deprecated left to compare its performance with the new one that handles AltLocs
	 * @param chain
	 * @param cubes
	 * @param logStringBuilder
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void parseChain_old(Chain chain, Hashtable<String, ArrayList<GroupOfInterest>> cubes, StringBuilder logStringBuilder) {
		logStringBuilder.append("parsing chain ").append(chain.getName()).append('\n');
		int prevResidueSeqNum = -1;
		AminoAcidOfInterest currentAA = null;
		Group prevGroup = null;
		boolean lastAACached = false;
		List<Group> allGroups = chain.getAtomGroups();
		for (Group group : allGroups) {
			lastAACached = false;
			int currentResidueNumber = group.getResidueNumber().getSeqNum();
			if (group instanceof AminoAcid) {
				this.foundAminoAcids++;
				currentAA = null;
				//TODO refactor this if statement and the AminoAcidOfInterest 
				// constructor to make it perform only one check inside the constructor
				String aaPdbName = group.getPDBName();
				if (
						GroupOfInterest.NAME___LYS.equals(aaPdbName)||
						GroupOfInterest.NAME___ARG.equals(aaPdbName)||
						GroupOfInterest.NAME___HIS.equals(aaPdbName)||
						GroupOfInterest.NAME___GLU.equals(aaPdbName)||
						GroupOfInterest.NAME___GLN.equals(aaPdbName)||
						GroupOfInterest.NAME___ASP.equals(aaPdbName)||
						GroupOfInterest.NAME___ASN.equals(aaPdbName)||
						GroupOfInterest.NAME___CYS.equals(aaPdbName)||
						GroupOfInterest.NAME___CSO.equals(aaPdbName)||
						GroupOfInterest.NAME___SEC.equals(aaPdbName)||
						GroupOfInterest.NAME___SE7.equals(aaPdbName)||
						GroupOfInterest.NAME___THR.equals(aaPdbName)||
						GroupOfInterest.NAME___SER.equals(aaPdbName)||
						GroupOfInterest.NAME___TYR.equals(aaPdbName)
						) {
					try {
						currentAA = new AminoAcidOfInterest((AminoAcid) group, cubes);
						lastAACached = true;
					} catch (IllegalArgumentException e) {
						System.err.println(e.getMessage());
//						continue; //FIXME should I continue?
					}
				}
				
				if (currentResidueNumber != prevResidueSeqNum + 1) {  //if N-Terminus
					AminoAcidOfInterest first = (currentAA != null) ? currentAA : new AminoAcidOfInterest((AminoAcid) group, cubes);
					first.putInCorrespondingCube("|"+GroupOfInterest.NAME___1ST, cubes);
				}

//				if (aa != null) {
////					this.foundAminoAcidsOfInterest++;
////					this.missingAtoms+=aa.getMissingAtoms();
//				}
			}else if (group instanceof HetatomImpl) {
				String aaPdbName = group.getPDBName();
				if(  // D Amino acids
						GroupOfInterest.NAME_D_LYS.equals(aaPdbName)||
						GroupOfInterest.NAME_D_ARG.equals(aaPdbName)||
						GroupOfInterest.NAME_D_HIS.equals(aaPdbName)||
						GroupOfInterest.NAME_D_GLU.equals(aaPdbName)||
						GroupOfInterest.NAME_D_GLN.equals(aaPdbName)||
						GroupOfInterest.NAME_D_ASP.equals(aaPdbName)||
						GroupOfInterest.NAME_D_ASN.equals(aaPdbName)||
						GroupOfInterest.NAME_D_CYS.equals(aaPdbName)||
//						GroupOfInterest.NAME_D_CSO.equals(aaPdbName)||
//						GroupOfInterest.NAME_D_SEC.equals(aaPdbName)||
//						GroupOfInterest.NAME_D_SE7.equals(aaPdbName)||
						GroupOfInterest.NAME_D_THR.equals(aaPdbName)||
						GroupOfInterest.NAME_D_SER.equals(aaPdbName)||
						GroupOfInterest.NAME_D_TYR.equals(aaPdbName)
						) {
					currentAA = new AminoAcidOfInterest(group, cubes);
					lastAACached = true;
				}else {
					this.foundHetGroups++;
					HetatomImpl hetatomImpl = (HetatomImpl) group;
					HetGroupOfInterest.newHetGroupOfInterest(hetatomImpl, cubes);
				}
			}else {
				//Don know yet
			}
			prevGroup = group;
			prevResidueSeqNum = currentResidueNumber;
		}
		if(prevGroup != null /*if there were any groups parsed in the chain*/ 
				&& prevGroup instanceof AminoAcid
				&& ! lastAACached 
				) {
			AminoAcidOfInterest last = (prevGroup == currentAA) ? currentAA : new AminoAcidOfInterest((AminoAcid) prevGroup, cubes);
			last.putInCorrespondingCube("|"+GroupOfInterest.NAME___LST, cubes);
		}

		this.chainsParsed++;
	}



	private void parseChain(Chain chain, Hashtable<String, ArrayList<GroupOfInterest>> cubes, StringBuilder logStringBuilder) {
		logStringBuilder.append("parsing chain ").append(chain.getName()).append('\n');
		
		//map from group to boolean (aminoAcid)
		//all types set to Group to allow D-AminoAcids
		Map<Group, Boolean> aminoacids = new LinkedHashMap<>();
		Map<Group, Boolean> nTerminus  = new LinkedHashMap<>();
		Map<Group, Boolean> cTerminus  = new LinkedHashMap<>();
		Map<Group, Boolean> ligands    = new LinkedHashMap<>();
		//type set to Group to allow D-AminoAcids
		Group   cTerminusCandidate = null;
		
		int prevResidueSeqNum = -1;
		List<Group> allGroups = chain.getAtomGroups();
		for (Group group : allGroups) {
//			lastAACached = false;
			final int currentResidueNumber = group.getResidueNumber().getSeqNum();
			final String groupPdbName = group.getPDBName();
			
			if (TempStructureTools.isAminoAcid(groupPdbName)) { //handles both L and D AminoAcids, as well as CSO
				if(AminoAcidOfInterest.aminoAcidsOfSpecialInterest.contains(groupPdbName) || 
				   AminoAcidOfInterest.aminoAcidsOfSpecialInterest.contains(TempStructureTools.getLChiralImage(groupPdbName))) {
					aminoacids.put(group, true);
				}
				
				if (currentResidueNumber != prevResidueSeqNum + 1) {  //if N-Terminus
					nTerminus.put(group, true);
					
					if (cTerminusCandidate != null) {  //if N-Terminus following C-Terminus
						cTerminus.put(cTerminusCandidate, true);
						cTerminusCandidate = null;
					}
				}
				cTerminusCandidate = group;
			} else {
				if (cTerminusCandidate != null) {
					cTerminus.put(cTerminusCandidate, true);
					cTerminusCandidate = null;
				}

				if(group instanceof HetatomImpl){ //Ligand
					ligands.put(group, false);
				} else { //nucleotide and others
					//Don know yet
				}
				
			}
			prevResidueSeqNum = currentResidueNumber;
		}//end of for loop
		if (cTerminusCandidate != null) {
			cTerminus.put(cTerminusCandidate, true);
			cTerminusCandidate = null;
		}

		HashMap<ResidueNumber, List<GroupOfInterest>> allParsed = new HashMap<>();
		manageGroupList(aminoacids,	null,							allParsed, cubes, logStringBuilder);
		manageGroupList(ligands,	null,							allParsed, cubes, logStringBuilder);
		manageGroupList(nTerminus,	"|"+GroupOfInterest.NAME___1ST,	allParsed, cubes, logStringBuilder);
		manageGroupList(cTerminus,	"|"+GroupOfInterest.NAME___LST,	allParsed, cubes, logStringBuilder);

		this.chainsParsed++;
	}

	private void manageGroupList(Map<Group, Boolean> groups, String suffix,
			HashMap<ResidueNumber, List<GroupOfInterest>> allParsed, Hashtable<String, ArrayList<GroupOfInterest>> cubes,
			StringBuilder logStringBuilder) {
		for (Entry<Group, Boolean> entry : groups.entrySet()) {
			Group group = entry.getKey();
			Boolean aminoAcid = entry.getValue();
			final ResidueNumber residueNumber = group.getResidueNumber();
			
			
			List<GroupOfInterest> parsedGroupsOfInterest = allParsed.get(residueNumber);
			if (parsedGroupsOfInterest == null) {
				parsedGroupsOfInterest = new ArrayList<GroupOfInterest>();
				if (!group.hasAltLoc()) {
					GroupOfInterest groupOfInterest = createGroupOfInterest(group, aminoAcid, cubes);
					parsedGroupsOfInterest.add(groupOfInterest);
				} else {
					for(Group groupTemp : group.getAltLocs()) {
						GroupOfInterest groupOfInterest = createGroupOfInterest(groupTemp, aminoAcid, cubes);
						parsedGroupsOfInterest.add(groupOfInterest);
					}
				}
				allParsed.put(residueNumber, parsedGroupsOfInterest);
			}

			//put in additional cubes
			if (suffix != null) {
				//LOOP of parsedGroupsOfInterest
				for(GroupOfInterest groupOfInterest: parsedGroupsOfInterest) {
					groupOfInterest.putInCorrespondingCube(suffix, cubes);
				}
			}
		}
	}

	private GroupOfInterest createGroupOfInterest(Group group, Boolean aminoAcid, Hashtable<String, ArrayList<GroupOfInterest>> cubes) {
		GroupOfInterest groupOfInterest;
		if (aminoAcid) {
			if(group instanceof AminoAcid) {
				groupOfInterest = AminoAcidOfInterest.newAcidOfInterest((AminoAcid)group, cubes);
			} else {
				groupOfInterest = new AminoAcidOfInterest(group, cubes);						
			}
		} else {
			groupOfInterest = HetGroupOfInterest.newHetGroupOfInterest(group, cubes);
		}
		return groupOfInterest;
	}

	
	public ProteinParsingGUI getGui() {
		return gui;
	}
	public void setGui(ProteinParsingGUI gui) {
		this.gui = gui;
	}


	public static int angstromToCube(double dimention) {
		return (int) Math.floor((dimention/CUBE_SIDE));
	}


	public static void putInCube(GroupOfInterest aa, int x, int y, int z, String suffix, Hashtable<String, ArrayList<GroupOfInterest>> cubes) {
		String cubeName=x+"|"+y+"|"+z+suffix;
		ArrayList<GroupOfInterest> cube;
		if (cubes.containsKey(cubeName)) {
			cube = cubes.get(cubeName);
		}else {
			cube = new ArrayList<GroupOfInterest>();
			cubes.put(cubeName, cube);
			//			if (suffix.equals(NAME_PHE)) {
			//				aromaticCubesNames.add(cubeName);
			//			}
		}
		if (!cube.contains(aa)) {
			cube.add(aa);
			AminoAcidOfInterest.ContainingCube containingCube= new AminoAcidOfInterest.ContainingCube();
			containingCube.x=x;
			containingCube.y=y;
			containingCube.z=z;
			aa.getContainingCubes().add(containingCube);
		}
	}	
}