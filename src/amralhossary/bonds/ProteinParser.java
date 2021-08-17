package amralhossary.bonds;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.vecmath.Point3d;

import org.biojava.nbio.structure.AminoAcid;
import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.AtomImpl;
import org.biojava.nbio.structure.Chain;
import org.biojava.nbio.structure.EntityInfo;
import org.biojava.nbio.structure.Group;
import org.biojava.nbio.structure.HetatomImpl;
import org.biojava.nbio.structure.Site;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.biojava.nbio.structure.io.FileParsingParameters;
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
	private static final String ISOPEPTIDE_BONDS = "IspPeptide Bonds";
	private static final String NOS_Bonds        = "NOS Bonds";
	private static final String NXS_Bonds        = "NxS Bonds (not known previously)";
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
	static final String TYR_TYR 		= "TYR-TYR";
	static final String ESTER_BOND		= "Ester bond";
	static final String THIOETHER_BOND	= "ThioEther bond";
	static final String THIOESTER_BOND	= "ThioEster bond";
	static final String TO_C_TERMINUS	= "To C-Terminus";
	static final String INCLUDING_TYR 	= "Including Tyr";
	
	//source, target, bond type, subtype.
	//This quadruplet structure is not the best option, but it is a fast one.
	static final String[] operations = new String[] {
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_TYR, ISOPEPTIDE, INCLUDING_TYR,
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_TYR, ISOPEPTIDE, INCLUDING_TYR,
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_TYR, ISOPEPTIDE, INCLUDING_TYR, //Tyr, new, His <-> Tyr (2.2 A)
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_GLU, ISOPEPTIDE, "",
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_ASP, ISOPEPTIDE, "",
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_GLN, ISOPEPTIDE, "",
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_ASN, ISOPEPTIDE, "",
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_GLU, ISOPEPTIDE, "",
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_ASP, ISOPEPTIDE, "",
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_GLN, ISOPEPTIDE, "",
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_ASN, ISOPEPTIDE, "",
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_GLU, ISOPEPTIDE, "",
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_ASP, ISOPEPTIDE, "",
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_GLN, ISOPEPTIDE, "",
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_ASN, ISOPEPTIDE, "",
			
			GroupOfInterest.NAME_1ST, GroupOfInterest.NAME_TYR, ISOPEPTIDE, N_AA+INCLUDING_TYR, // TODO review
			GroupOfInterest.NAME_1ST, GroupOfInterest.NAME_GLU, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME_1ST, GroupOfInterest.NAME_ASP, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME_1ST, GroupOfInterest.NAME_GLN, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME_1ST, GroupOfInterest.NAME_ASN, ISOPEPTIDE, N_AA,
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_LST, ISOPEPTIDE, AA_C,
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_LST, ISOPEPTIDE, AA_C,
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_LST, ISOPEPTIDE, AA_C,
			GroupOfInterest.NAME_1ST, GroupOfInterest.NAME_HET, ISOPEPTIDE, N_HET, //new
			GroupOfInterest.NAME_HET, GroupOfInterest.NAME_LST, ISOPEPTIDE, HET_C, //new
			
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_HET, ISOPEPTIDE, AA_HET,
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_HET, ISOPEPTIDE, AA_HET,
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_HET, ISOPEPTIDE, AA_HET,
			GroupOfInterest.NAME_HET, GroupOfInterest.NAME_GLU, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME_HET, GroupOfInterest.NAME_ASP, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME_HET, GroupOfInterest.NAME_GLN, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME_HET, GroupOfInterest.NAME_ASN, ISOPEPTIDE, HET_AA,
			GroupOfInterest.NAME_HET, GroupOfInterest.NAME_TYR, ISOPEPTIDE, HET_AA, //new
			
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_CSO, NOS_BOND, "",
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_CSO, NOS_BOND, ARG_CSO,
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_CSO, NOS_BOND, HIS_CSO,
			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_CYS, NXS_BOND, "",
			GroupOfInterest.NAME_ARG, GroupOfInterest.NAME_CYS, NXS_BOND, ARG_CYS,
			GroupOfInterest.NAME_HIS, GroupOfInterest.NAME_CYS, NXS_BOND, HIS_CYS,
//			GroupOfInterest.NAME_LYS, GroupOfInterest.NAME_SEC, NXS_BOND, "", //We know there should be no results
			//I ordered ester and Thioester before ether and thioether
			//Ester: Thr/Ser/Tyr <-> C-Terminous / Glu/Asp/Gln/Asn/Arg (/Gly ?) (2.1 A)
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_GLU, ESTER_BOND, "",
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_ASP, ESTER_BOND, "",
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_GLN, ESTER_BOND, "",
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_ASN, ESTER_BOND, "",
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_ARG, ESTER_BOND, "",
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_LST, ESTER_BOND, TO_C_TERMINUS,
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_GLU, ESTER_BOND, "",
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_ASP, ESTER_BOND, "",
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_GLN, ESTER_BOND, "",
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_ASN, ESTER_BOND, "",
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_ARG, ESTER_BOND, "",
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_LST, ESTER_BOND, TO_C_TERMINUS,
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_GLU, ESTER_BOND, "",
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_ASP, ESTER_BOND, "",
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_GLN, ESTER_BOND, "",
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_ASN, ESTER_BOND, "",
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_ARG, ESTER_BOND, "",
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_LST, ESTER_BOND, TO_C_TERMINUS,
			//Thioester: Cys <-> C-Terminous / Glu/Asp/Gln/Asn / ((Lys.CA)) (2.3 A)
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_GLU, THIOESTER_BOND, "",
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_ASP, THIOESTER_BOND, "",
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_GLN, THIOESTER_BOND, "",
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_ASN, THIOESTER_BOND, "",
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_ARG, THIOESTER_BOND, "",
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_LST, THIOESTER_BOND, TO_C_TERMINUS,
			
			
			//Ethers: O from the first to C from the second
			//Ether: Thr/Ser/Tyr <-> Any C ?
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_TYR, ETHER_BOND, "",
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_TYR, ETHER_BOND, "",
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_TYR, ETHER_BOND, TYR_TYR, //TODO check this
			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_LST, ETHER_BOND, "",
			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_LST, ETHER_BOND, "",
			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_LST, ETHER_BOND, "",
//			GroupOfInterest.NAME_THR, GroupOfInterest.NAME_HET, ETHER_BOND, "",
//			GroupOfInterest.NAME_SER, GroupOfInterest.NAME_HET, ETHER_BOND, "",
//			GroupOfInterest.NAME_TYR, GroupOfInterest.NAME_HET, ETHER_BOND, "",
			//Thioether (sulfide): Cys <-> any C (incl. Tyr CB/CD1/CD2/CE1/CE2) (2.3 A)
			//						6nef has Cys <-> HEC (heme) some within cutoff and some unrealisticly far
			//						6ef8 (same protein) has all of them within range although worse resolution
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_TYR, THIOETHER_BOND, "",
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_LST, THIOETHER_BOND, "",
			GroupOfInterest.NAME_CYS, GroupOfInterest.NAME_HET, THIOETHER_BOND, "",
			
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
	
	static Hashtable<String, ArrayList<GroupOfInterest>> cubes = new Hashtable<String, ArrayList<GroupOfInterest>>();
	
//	private long missingAtoms;//TODO complete it

	private long startTime;
	
	static SettingsManager settingsManager = null; 
	AtomCache atomCache = new AtomCache(settingsManager.getUserConfiguration());

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
		
		cubes.clear();
	}
	
	public static void main (String [] args) throws IOException {
		InputStream docStream = ProteinParser.class.getResourceAsStream("/doc.txt");
		Docopt docopt = new Docopt(docStream);
		Map<String, Object> options = docopt.withVersion("0.1.0").parse(args);
		
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
	
	
	public ProteinParser() {
		this(null);
	}
	
	public ProteinParser(ProteinParsingGUI gui){
		this.gui=gui;
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
			positiveResultsFile = new File(settingsManager.getWorkingFolder(), "PositiveResults.txt");
			if (! positiveResultsFile.exists()) {
				positiveResultsFile.createNewFile();
			}
			this.out= new PrintStream(positiveResultsFile);

			tsvPositiveResultsFile = new File(settingsManager.getWorkingFolder(), "PositiveResults.tsv");
			if (! tsvPositiveResultsFile.exists()) {
				tsvPositiveResultsFile.createNewFile();
			}
			this.tsvOut = new PrintStream(tsvPositiveResultsFile);
			this.tsvOut.print("PDB ID\tResidue 1\tchain 1\tResidue 1 No\tAtom1 name\t"
					+ "<--\tDistance\t-->\t"
					+ "Residue 2\tchain 2\tResidue 2 No\tAtom2 name\t");
			this.tsvOut.print("bond type\tsubtype / comments\tResolution\tRFree\tSource Organism Scientific\t");
			this.tsvOut.print(String.format("%s\t%s\t%s\t%s\t", PILI_PILUS, ADHESIN_ADHESION, UBIQ, CYCLO_CYCLIC_LASSO));
			this.tsvOut.println("Title");


			File log=null;
			log = new File(settingsManager.getWorkingFolder(), "log.txt");
			if (! log.exists()) {
				log.createNewFile();
			}
			this.log= new PrintStream(log);
		} catch (IOException e) {
			System.err.println("couldn't create file ["+positiveResultsFile.getAbsolutePath()+"] for output");
			e.printStackTrace();
		}
		while (scanner.hasNextLine() && moreWork) {
			String line = scanner.nextLine();
			if (line.startsWith("##")) {
				System.out.println("Entering "+line.substring(2));
			}else{
				StringTokenizer tokenizer = new StringTokenizer(line, ";", false);
				while (tokenizer.hasMoreTokens() && moreWork) {
					String token = tokenizer.nextToken().trim();
					parseStructure(token);
				}
			}
		}
	}
	public void parseResults(Scanner scanner) {
//		final String startOfStructurePrefix = ResultManager.START_OF_STRUCTURE_PREFIX;
//		final int tokenNamePosition = startOfStructurePrefix.length();
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
		Hashtable<String, Hashtable<String, HashSet<String>>> allPersistedInteractions= new Hashtable<String, Hashtable<String,HashSet<String>>>();
//		int count=0;
//		int aromaticAAOfInterestType;
//		while (scanner.hasNextLine() && moreWork) {
//			String line = scanner.nextLine()/*.trim()*/;
//			if (line.length()!= 0) {
//				// retrieving code here
//				if (line.startsWith(startOfStructurePrefix)) {
//					String token = line.substring(tokenNamePosition);
//					Hashtable<String, HashSet<String>> retreivedSetOfInteractions = ResultManager.retrieveSetOfInteractions(scanner);
//					//update frequencies
//					for (String sourceAA : retreivedSetOfInteractions.keySet()) {
//						char ch=sourceAA.charAt(3);
//						switch (ch) {
//						case 'E':
//							aromaticAAOfInterestType=GroupOfInterest.CODE_PHE;
//							break;
//						case 'R':
//							aromaticAAOfInterestType=GroupOfInterest.CODE_TYR;
//							break;
//						case 'P':
//							aromaticAAOfInterestType=GroupOfInterest.CODE_TRP;
//							break;
//
//						default:
//							throw new IllegalArgumentException("Unexpected Aminoacid");
//						}
////						updateFrequencies(aromaticAAOfInterestType, retreivedSetOfInteractions.get(sourceAA));
//					}
//
////					//show a sign of life
//					if (count % 1000 == 0) {
//						System.out.println(count+" structures parsed");
//					}
//					//put it in cubes, 
//					allPersistedInteractions.put(token, retreivedSetOfInteractions);
//					if (gui != null) {
//						settingsManager.setShowWhileProcessing(false);//this line is important in order not to throw a NullPointerException on the next line
//						gui.interactionsFoundInStructure(token, null, null);
//					}
//					count++;
//				}else if (line.startsWith(ProteinParser.START_OF_STATISTICS)) {
//					System.out.println("Parsed "+count+" structures. Please wait...");
//					while (scanner.hasNextLine() && moreWork) {
//						line=scanner.nextLine();
//						if (line.contains("%")) {
//							continue;//this is a derived value
//						}
//						final int indexOfSeparator = line.indexOf('\t');
//						if (indexOfSeparator>0) {
//							long value = Long.parseLong(line.substring(0, indexOfSeparator));
//							String key = line.substring(indexOfSeparator+1);
//							switch (key) {
//							case ATTEMPTED_FILES:
//								attemptedPDBFiles += value;
//								break;
//							case SUCCESSFULLY_PARSED_STRUCTURE_FILES:
//								successfullyParsedStructures += value;
//								break;
//							case FAILS:
//								failedToParseStructure += value;
//								break;
//							case EMPTY_FILES:
//								 emptyFiles+= value;
//								break;
//							case CHAINS_PARSED:
//								 chainsParsed+= value;
//								break;
//							case CHAINS_SKIPPED:
//								 chainsSkipped+= value;
//								break;
//							case CHAINS_NOT_FOUND:
//								 chainsNotFound+= value;
//								break;
//							case AMINO_ACIDS_FOUND:
//								 foundAminoAcids+= value;
//								break;
//							case HET_GROUPS_FOUND:
//								 foundHetGroups+= value;
//								break;
//							case ATOMS_NOT_FOUND:
//								 missingAtoms+= value;
//								break;
//							case AMINO_ACIDS_FAILED:
//								 failedToParseAminoAcids+= value;
//								break;
//							case TOTAL_STRUCTURES_WITH_INTERACTIONS:
//								 totalFoundStructuresWithInteractions+= value;
//								break;
//							case TOTAL_FOUND_PHE:
//								 totalFoundPhe+= value;
//								 long pheNotInInteractions=totalFoundPhe-pheInInteractions;
//								break;
//							case TOTAL_FOUND_TYR:
//								 totalFoundTyr+= value;
//								 long tyrNotInInteractions=totalFoundTyr-tyrInInteractions;
//								break;
//							case TOTAL_FOUND_TRP:
//								 totalFoundTrp+= value;
//								 long trpNotInInteractions=totalFoundTrp-trpInInteractions;
//								break;
//							case TOTAL_FOUND_ARG:
//								 totalFoundArg+= value;
//								break;
//							case TOTAL_FOUND_LYC:
//								 totalFoundLyc+= value;
//								break;
//							case TOTAL_FOUND_GLU:
//								 totalFoundGlu+= value;
//								break;
//							case TOTAL_FOUND_ASP:
//								 totalFoundAsp+= value;
//								break;
//							case TOTAL_FOUND_HET_GROUPS_OF_INTEREST:
//								 totalFoundHetGroupsOfInterest+= value;
//								break;
//							case ISOPEPTIDE_BONDS:
//								 pheInInteractions+= value;
//								break;
//							case NOS_Bonds:
//								 tyrInInteractions+= value;
//								break;
//							case TRP_IN_INTERACTIONS:
//								 trpInInteractions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_PHE_ARG:
//								phe_arg_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_PHE_LYC:
//								phe_lyc_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_PHE_ASP:
//								phe_asp_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_PHE_GLU:
//								phe_glu_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_PHE_VE_HET_ATOMS:
//								phe_hetGroup_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TYR_ARG:
//								tyr_arg_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TYR_LYC:
//								tyr_lyc_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TYR_ASP:
//								tyr_asp_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TYR_GLU:
//								tyr_glu_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TYR_VE_HET_ATOMS:
//								tyr_hetGroup_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TRP_ARG:
//								trp_arg_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TRP_LYC:
//								trp_lyc_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TRP_ASP:
//								trp_asp_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TRP_GLU:
//								trp_glu_interactions+= value;
//								break;
//							case FOUND_INTERACTIONS_BETWEEN_TRP_VE_HET_ATOMS:
//								trp_hetGroup_interactions+= value;
//								break;								
//							default:
//								//unknown , ignore
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
		totalFoundStructuresWithInteractions += allPersistedInteractions.keySet().size();
		if (gui != null) {
			gui.showResults(null);
		}
		
		System.out.println(getPrintableStatistics());
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
	public boolean parseStructure(String token) {
		boolean chainsInStructureParsedSuccessfully = parseChainsInStructure(token);
		if (chainsInStructureParsedSuccessfully) {
			Hashtable<String, String> scriptCollectionBuffer = new Hashtable<String, String>();
			Hashtable<GroupOfInterest, HashSet<GroupOfInterest>> foundInteractions = findInteractionsInCubes(scriptCollectionBuffer);

			Collection<String> encodedScripts = scriptCollectionBuffer.values();
			StringBuilder builder= new StringBuilder();
			for (Iterator<String> iterator = encodedScripts.iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				builder.append(string);
//				builder.append(System.lineSeparator());
			}
			String specificCollectionScriptString = builder.toString();
			
			if (foundInteractions.size() > 0){
				totalFoundStructuresWithInteractions++;
				if(gui != null) {
					gui.interactionsFoundInStructure(token, specificCollectionScriptString, foundInteractions);
				}
				
				String listofConnectionsAsString = ResultManager.persistInteractionsHashTable(token,specificCollectionScriptString, foundInteractions);
//						ResultManager.createListofConnectionsAsString(foundInteractions);
				System.out.println(listofConnectionsAsString);

				StringBuilder stringForBrendan = new StringBuilder(ResultManager.START_OF_STRUCTURE_PREFIX).append(token).append(System.getProperty("line.separator"));
				stringForBrendan.append(listofConnectionsAsString);
				this.out.println(stringForBrendan.toString());

				//export
				ResultManager.exportFileToScript(token.substring(0, 4), settingsManager.getWorkingFolder()+System.getProperty("file.separator")+"EXPORTED", specificCollectionScriptString);
			}
			return true;
		}
		return false;
	}


	boolean parseChainsInStructure(String token) {
		this.attemptedPDBFiles++;
		boolean structureParsedSuccessfully = true;
		try {
			
			String structureName = token.substring(0, 4);
			String extension = token.substring(4);
			System.out.println();
			System.out.println("Starting to parse "+token + ": " + structureName+"\t"+extension);
			Structure currentStructure = atomCache.getStructure(structureName);
			
			if (gui != null) {
				gui.structureLoaded(currentStructure);
			}
			this.successfullyParsedStructures++;
			Hashtable<String, ArrayList<GroupOfInterest>> cubes = ProteinParser.cubes;
			cubes.clear();
			
			List<Chain> chains = currentStructure.getChains(); //TODO I get all chains of the FIRST modell. Consider getting all models and updating outputted identifiers
			if (chains==null || chains.isEmpty()) {
				this.emptyFiles++;
				System.out.println("Unexpected error: NO chains are found AT ALL in the file "+structureName);
				return false;
			}
			boolean chainFound=false;
			if (extension== null || extension.equals("")) {
				for (Chain chain : chains) {
					parseChain(chain);
				}
				chainFound=true;
			} else{
				for (Chain chain : chains) {
					if (extension.equalsIgnoreCase(chain.getName())) {
						parseChain(chain);
						chainFound=true;
					}else {
						System.out.println("skipped chain "+chain.getName());
						this.chainsSkipped++;
					}
				}
			}
			if(! chainFound){
				System.out.println("Unexpected error: chain ID "+extension+" NOT FOUND in file "+token);
				this.chainsNotFound++;
			}
		} catch (IOException e) {
			System.out.println("NOT FOUND or Failed to parse");
			this.failedToParseStructure++;
			structureParsedSuccessfully=false;
		} catch (StructureException e) {
			System.out.println("Failed to parse");
			this.failedToParseStructure++;
			structureParsedSuccessfully=false;
		}
		return structureParsedSuccessfully;
	}

	Hashtable<GroupOfInterest, HashSet<GroupOfInterest>> findInteractionsInCubes(Hashtable<String, String> scriptCollectionBuffer) {
		
		Hashtable<GroupOfInterest, HashSet<GroupOfInterest>> foundInteractions = new Hashtable<>();
		Hashtable<String, ArrayList<GroupOfInterest>> cubes = ProteinParser.cubes;
		
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
				
				HashSet<GroupOfInterest> tempInteractingGroupsOfInterest = new HashSet<>(); //to decrease frequency of unnecessary objects creation
				for (int ai = 0; ai < residue1List.size(); ai++) {
					GroupOfInterest residue1 = (GroupOfInterest) residue1List.get(ai);
					for (int i = x-1; i <=x+1 ; i++) {
						for (int j = y-1; j <= y+1; j++) {
							for (int k = z-1; k <= z+1 ; k++) {
								destCubeBaseName= i+"|"+j+"|"+k;
								HashSet<GroupOfInterest> interactingGroupsOfInterest = foundInteractions.get(residue1);
								if (interactingGroupsOfInterest == null) {
									interactingGroupsOfInterest = tempInteractingGroupsOfInterest;
								}
								ArrayList<GroupOfInterest> destCubeOfGroupsOfInterest = cubes.get(destCubeBaseName+"|"+targetSuffix);
								if (destCubeOfGroupsOfInterest == null)
									continue;
								for (GroupOfInterest residue2 : destCubeOfGroupsOfInterest) {
									//I removed this line on purpose, to see if 2 residues are linked using more than one bond.
									if (interactingGroupsOfInterest.contains(residue2)) {
										continue;
									}
									boolean confirmedLink = confirmLink(residue1, residue2, operation, subOperation, scriptCollectionBuffer);

									if (confirmedLink) {
										//				outputCsv(residue1, destGroupOfInterest, operation, subOperation);
										interactingGroupsOfInterest.add(residue2);
										break;
									}					
								}
								if (interactingGroupsOfInterest.size() > 0) {
									foundInteractions.put(residue1, interactingGroupsOfInterest);
									tempInteractingGroupsOfInterest = new HashSet<GroupOfInterest>();
								}
							}
						}
					}
				}
			}
		}
		return foundInteractions; 
	}

	private void outputCsv(Atom atom1, Atom atom2, double distance, String operation, String subOperation) {
		Group residue1 = ((AtomImpl) atom1).getGroup();
		Group residue2 = ((AtomImpl) atom2).getGroup();
		Structure structure = residue1.getChain().getStructure();
		StringBuilder str = new StringBuilder(structure.getPDBCode()).append('\t');
		
		str.append(residue1.getPDBName()).append('\t')
		.append(residue1.getChain().getName()).append('\t') // I am reporting the chain authId (the one written in PDB) now.
		.append(residue1.getResidueNumber().toString()).append('\t').append(atom1.getName()).append('\t');
		
		str.append("<--\t").append(String.format("%.3f", distance)).append("\t-->\t");
		
		str.append(residue2.getPDBName()).append('\t')
		.append(residue2.getChain().getName()).append('\t') // I am reporting the chain authId (the one written in PDB) now.
		.append(residue2.getResidueNumber().toString()).append('\t').append(atom2.getName()).append('\t');
		
		str.append(operation).append('\t').append(subOperation).append('\t');

		str.append(structure.getPDBHeader().getResolution()).append('\t')
		.append(structure.getPDBHeader().getRfree());
		
		//Source Organism Scientific
		str.append('\t');
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
		allStringsToScan.add(structure.getPDBHeader().getTitle());
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
		List<String> keywords = structure.getKeywords();
		for (String keyword : keywords) {
			allStringsToScan.add(keyword);
		}
		if(structure.getJournalArticle() != null)
			allStringsToScan.add(structure.getJournalArticle().getTitle());
		
		outputBinaryTestFind(str, allStringsToScan, PILI_PILUS);
		outputBinaryTestFind(str, allStringsToScan, ADHESIN_ADHESION);
		outputBinaryTestFind(str, allStringsToScan, UBIQ);
		outputBinaryTestFind(str, allStringsToScan, CYCLO_CYCLIC_LASSO);
		
		str.append('\t').append("\"").append(structure.getPDBHeader().getTitle()).append("\"");
		
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

	private boolean confirmLink(GroupOfInterest group1, GroupOfInterest group2, String operation, String subOperation, Hashtable<String, String> scriptCollectionBuffer) {
		//check the group type and set parameters and output list
		float cutoff, cutoff2;
		Atom[] atoms1=null, atoms2=null;
		
		if (operation == ISOPEPTIDE) {
			atoms1 = group1.getKeyNAtoms();
			atoms2 = group2.getKeyCAtoms();
			cutoff = 2.1f;
			cutoff2= 4.41f;

			//Note that I use the == operator here
			if(subOperation == AA_C) {
				atoms2 = new Atom[] {group2.getAtom("C")};
			}
			if(subOperation == N_AA) {
				atoms1 = new Atom[] {group1.getAtom("N")};
			}
		}else if (operation == NXS_BOND) {
			atoms1 = group1.getKeyNAtoms();
			atoms2 = group2.getKeyAtoms(); // S atom
			cutoff = 3.1f;
			cutoff2= 9.61f;
			//TODO Do we need to implement SelinoCystein?
		}else if (operation == NOS_BOND) {
			atoms1 = group1.getKeyNAtoms();
			atoms2 = group2.getKeyOAtoms();  //unusual OD atom
			cutoff = 3.1f;
			cutoff2= 9.61f;
			if (atoms2[0] == null) {
				return confirmLink(group1, group2, NXS_BOND, "CSO with missing O", scriptCollectionBuffer);
			}
		}else if (operation == ESTER_BOND) {
			atoms1 = group1.getKeyOAtoms();
			if(subOperation == TO_C_TERMINUS)
				atoms2 = new Atom[] {group2.getAtom("C")};
			else
				atoms2  = group2.getKeyAtoms();
			cutoff = 2.5f;
			cutoff2= 6.25f;
		}else if(operation == ETHER_BOND) {
			if(group1 == group2 && group1 instanceof AminoAcidOfInterest && ((AminoAcidOfInterest)group1).aAOfInterestType==GroupOfInterest.CODE_TYR)
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
		
//		switch (group2.getGroupOfInterestType()) {
//		case GroupOfInterest.CODE_CYS:
//		case GroupOfInterest.CODE_CSO:
//			Atom[] keyAtoms = group2.getKeyAtoms();
//			if (keyAtoms.length == 2 && keyAtoms[1] != null) {
//				atom = keyAtoms[1];
//				cutoff = 2.0f;
//			} else {
//				atom = keyAtoms[0];
//				cutoff = 3.1f;
//			}
//			break;
//		case GroupOfInterest.CODE_SEC:
//		case GroupOfInterest.CODE_SE7:
//			atom = group2.getKeyAtoms()[0];
//			cutoff = 1.5f;
//			break;
//		default:
//			//TODO write warning
//			return false;
////			break;
//		}
		
		if(atoms1 == null || atoms2 == null) {
			return false;
		}
		boolean confirmed = false;
		for (int i = 0; !confirmed && i < atoms1.length; i++) {
			Atom atom1 = atoms1[i];
			if(atom1 == null)
				continue;
			for (int j = 0; !confirmed && j < atoms2.length; j++) {
				Atom atom2 = atoms2[j];
				if(atom2 == null)
					continue;
				Point3d point3d1 = new Point3d(atom1.getCoords());		
				Point3d point3d2 = new Point3d(atom2.getCoords());
				double distanceSquared = point3d1.distanceSquared(point3d2);
				if (distanceSquared <= cutoff2) {
					String name = "Res" + group1.getResidueNumber().getSeqNum() + "_"+ group1.getChain().getName();
					String value = ResultManager.encodeDrawSphereCommand(name, point3d2, cutoff);
					scriptCollectionBuffer.put(name, value);
					confirmed = true;
					outputCsv(atom1, atom2, Math.sqrt(distanceSquared), operation, subOperation);
				}
			}
		}
		return confirmed;
	}

	private void parseChain(Chain chain) {
		System.out.println("parsing chain "+chain.getName());
		int prevResidueSeqNum = -1;
		AminoAcidOfInterest currentAA = null;
		Group prevGroup = null;
		boolean lastAACached = false;
		List<Group> allGroups = chain.getAtomGroups();
		for (Group group : allGroups) {
			lastAACached = false;
			int currentResidueNumber = group.getResidueNumber().getSeqNum();
			if ( group instanceof AminoAcid) {
				this.foundAminoAcids++;
				currentAA = null;

				String aaPdbName = group.getPDBName();
				if (GroupOfInterest.NAME_LYS.equals(aaPdbName)||
						GroupOfInterest.NAME_ARG.equals(aaPdbName)||
						GroupOfInterest.NAME_HIS.equals(aaPdbName)||
						GroupOfInterest.NAME_LYS.equals(aaPdbName)||
						GroupOfInterest.NAME_GLU.equals(aaPdbName)||
						GroupOfInterest.NAME_GLN.equals(aaPdbName)||
						GroupOfInterest.NAME_ASP.equals(aaPdbName)||
						GroupOfInterest.NAME_ASN.equals(aaPdbName)||
						GroupOfInterest.NAME_CYS.equals(aaPdbName)||
						GroupOfInterest.NAME_CSO.equals(aaPdbName)||
						GroupOfInterest.NAME_SEC.equals(aaPdbName)||
						GroupOfInterest.NAME_SE7.equals(aaPdbName)||
						GroupOfInterest.NAME_THR.equals(aaPdbName)||
						GroupOfInterest.NAME_SER.equals(aaPdbName)||
						GroupOfInterest.NAME_TYR.equals(aaPdbName)
						) {
					try {
						currentAA = new AminoAcidOfInterest((AminoAcid) group);
						lastAACached = true;
					} catch (IllegalArgumentException e) {
						System.err.println(e.getMessage());
//						continue; //FIXME should I continue?
					}
				}
				
				if (currentResidueNumber != prevResidueSeqNum + 1) {  //if N-Terminus
					AminoAcidOfInterest first = (currentAA != null) ? currentAA : new AminoAcidOfInterest((AminoAcid) group);
					first.putGroupOfInterestInCorrespondingCube("|"+GroupOfInterest.NAME_1ST);
				}

//				try {
//				} catch (RuntimeException e) {
//					this.failedToParseAminoAcids++;
//					AminoAcid acid = (AminoAcid) group;
//					StringBuilder builder = new StringBuilder();
//					builder.append(ResultManager.FILED_TO_PARSE_AMINOACID).append(acid.getPDBName()).append("]").append(acid.getResidueNumber()).append(":").append(acid.getChain().getName());
//					System.out.println(builder.toString());
//					this.log.append(builder.toString());
//					e.printStackTrace();
//					e.printStackTrace(out);
//				}

//				if (aa != null) {
////					this.foundAminoAcidsOfInterest++;
////					this.missingAtoms+=aa.getMissingAtoms();
//				}
			}else if (group instanceof HetatomImpl) {
				this.foundHetGroups++;
				HetatomImpl hetatomImpl = (HetatomImpl) group;
				HetGroupOfInterest.newHetGroupOfInterest(hetatomImpl);
			}else {
				//Don know yet
			}
			prevGroup = group;
			prevResidueSeqNum = currentResidueNumber;
		}
		if(prevGroup != null && ! lastAACached && prevGroup instanceof AminoAcid) {
			AminoAcidOfInterest last = (prevGroup == currentAA) ? currentAA : new AminoAcidOfInterest((AminoAcid) prevGroup);
			last.putGroupOfInterestInCorrespondingCube("|"+GroupOfInterest.NAME_LST);
		}

		this.chainsParsed++;
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


	public static void putInCube(GroupOfInterest aa, int x, int y, int z, String suffix) {
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