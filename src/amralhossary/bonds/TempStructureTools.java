package amralhossary.bonds;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public abstract class TempStructureTools {

	// amino acid 3 and 1 letter code definitions
	private static final Map<String, Character> aminoAcids;
	
	private static final Map<String, String> l2dAminioAcids;
	private static final Map<String, String> d2lAminioAcids;
	/**
	 * The character to use for unknown compounds in sequence strings
	 */
	public static final char UNKNOWN_GROUP_LABEL = 'X';
	
	
	static {
		aminoAcids = new HashMap<String, Character>();
		aminoAcids.put("GLY", 'G');
		aminoAcids.put("ALA", 'A');
		aminoAcids.put("VAL", 'V');
		aminoAcids.put("LEU", 'L');
		aminoAcids.put("ILE", 'I');
		aminoAcids.put("PHE", 'F');
		aminoAcids.put("TYR", 'Y');
		aminoAcids.put("TRP", 'W');
		aminoAcids.put("PRO", 'P');
		aminoAcids.put("HIS", 'H');
		aminoAcids.put("LYS", 'K');
		aminoAcids.put("ARG", 'R');
		aminoAcids.put("SER", 'S');
		aminoAcids.put("THR", 'T');
		aminoAcids.put("GLU", 'E');
		aminoAcids.put("GLN", 'Q');
		aminoAcids.put("ASP", 'D');
		aminoAcids.put("ASN", 'N');
		aminoAcids.put("CYS", 'C');
		aminoAcids.put("CSO", 'C'); //TODO double check CSO one letter code
		aminoAcids.put("MET", 'M');
		// MSE is only found as a molecular replacement for MET
		aminoAcids.put("MSE", 'M');
		// 'non-standard', genetically encoded
		// http://www.chem.qmul.ac.uk/iubmb/newsletter/1999/item3.html
		// IUBMB recommended name is 'SEC' but the wwPDB currently use 'CSE'
		// likewise 'PYL' (IUBMB) and 'PYH' (PDB)
		aminoAcids.put("CSE", 'U');
		aminoAcids.put("SEC", 'U');
		aminoAcids.put("PYH", 'O');
		aminoAcids.put("PYL", 'O');
		//D-AminoAcids https://proteopedia.org/wiki/index.php/Amino_Acids
		//are optical isomers or enantiomers (mirror images) of naturally occuring L-AminoAcids.
		//They have the same structure but with opposite chirality.
		aminoAcids.put("DAL", UNKNOWN_GROUP_LABEL);//D-ALA
		aminoAcids.put("DAR", UNKNOWN_GROUP_LABEL);//D-ARG
		aminoAcids.put("DSG", UNKNOWN_GROUP_LABEL);//D-ASN
		aminoAcids.put("DAS", UNKNOWN_GROUP_LABEL);//D-ASP
		aminoAcids.put("DCY", UNKNOWN_GROUP_LABEL);//D-CYS
		aminoAcids.put("DGN", UNKNOWN_GROUP_LABEL);//D-GLN
		aminoAcids.put("DGL", UNKNOWN_GROUP_LABEL);//D-GLU
		aminoAcids.put("DHI", UNKNOWN_GROUP_LABEL);//D-HIS
		aminoAcids.put("DIL", UNKNOWN_GROUP_LABEL);//D-ILE
		aminoAcids.put("DLE", UNKNOWN_GROUP_LABEL);//D-LEU
		aminoAcids.put("DLY", UNKNOWN_GROUP_LABEL);//D-LYS
		aminoAcids.put("MED", UNKNOWN_GROUP_LABEL);//D-MET
		aminoAcids.put("DPN", UNKNOWN_GROUP_LABEL);//D-PHE
		aminoAcids.put("DPR", UNKNOWN_GROUP_LABEL);//D-PRO
		aminoAcids.put("DSN", UNKNOWN_GROUP_LABEL);//D-SER
		aminoAcids.put("DTH", UNKNOWN_GROUP_LABEL);//D-THR
		aminoAcids.put("DTR", UNKNOWN_GROUP_LABEL);//D-TRP
		aminoAcids.put("DTY", UNKNOWN_GROUP_LABEL);//D-TYR
		aminoAcids.put("DVA", UNKNOWN_GROUP_LABEL);//D-VAL
				
		d2lAminioAcids = new Hashtable<String, String>();
		d2lAminioAcids.put("DAL", "ALA");
		d2lAminioAcids.put("DAR", "ARG");
		d2lAminioAcids.put("DSG", "ASN");
		d2lAminioAcids.put("DAS", "ASP");
		d2lAminioAcids.put("DCY", "CYS");
		d2lAminioAcids.put("DGN", "GLN");
		d2lAminioAcids.put("DGL", "GLU");
		d2lAminioAcids.put("DHI", "HIS");
		d2lAminioAcids.put("DIL", "ILE");
		d2lAminioAcids.put("DLE", "LEU");
		d2lAminioAcids.put("DLY", "LYS");
		d2lAminioAcids.put("MED", "MET");
		d2lAminioAcids.put("DPN", "PHE");
		d2lAminioAcids.put("DPR", "PRO");
		d2lAminioAcids.put("DSN", "SER");
		d2lAminioAcids.put("DTH", "THR");
		d2lAminioAcids.put("DTR", "TRP");
		d2lAminioAcids.put("DTY", "TYR");
		d2lAminioAcids.put("DVA", "VAL");
		
		l2dAminioAcids = new Hashtable<String, String>();
		l2dAminioAcids.put("ALA", "DAL");
		l2dAminioAcids.put("ARG", "DAR");
		l2dAminioAcids.put("ASN", "DSG");
		l2dAminioAcids.put("ASP", "DAS");
		l2dAminioAcids.put("CYS", "DCY");
		l2dAminioAcids.put("GLN", "DGN");
		l2dAminioAcids.put("GLU", "DGL");
		l2dAminioAcids.put("HIS", "DHI");
		l2dAminioAcids.put("ILE", "DIL");
		l2dAminioAcids.put("LEU", "DLE");
		l2dAminioAcids.put("LYS", "DLY");
		l2dAminioAcids.put("MET", "MED");
		l2dAminioAcids.put("PHE", "DPN");
		l2dAminioAcids.put("PRO", "DPR");
		l2dAminioAcids.put("SER", "DSN");
		l2dAminioAcids.put("THR", "DTH");
		l2dAminioAcids.put("TRP", "DTR");
		l2dAminioAcids.put("TYR", "DTY");
		l2dAminioAcids.put("VAL", "DVA");

	}
	
	

	/**
	 * Test if the three-letter code of an ATOM entry corresponds to an aminoacid.
	 *
	 * @param groupCode3
	 *            3-character code for a group.
	 *
	 */
	public static boolean isAminoAcid(String groupCode3) {
		String code = groupCode3.trim().toUpperCase();
		return aminoAcids.containsKey(code);
	}
	
	/**
	 * Returns the chiral image of an aminoacid.
	 * Except for Glycine, all aminoacids have chiral images.
	 * @param aa the aminoacid name
	 * @return the chiral image of the passed in aminoacid, <code>null</code> if not found
	 * @throws IllegalArgumentException aa is <code>null</code>
	 */
	public static String getChiralImage(String aa) {
		if (aa == null) {
			throw new IllegalArgumentException("aminoacid is null");
		}
		aa = aa.toUpperCase();
		if (aa.equals("GLY")) {
			return "GLY";
		}else if (aa.startsWith("D")) {
			return d2lAminioAcids.get(aa);
		}else {
			return l2dAminioAcids.get(aa);
		}
	}

	/**
	 * Returns the D image of an aminoacid.
	 * Except for Glycine, all aminoacids have chiral images.
	 * @param aa the aminoacid name
	 * @return the D chiral image of the passed in aminoacid, <code>null</code> if not found
	 * @throws IllegalArgumentException aa is <code>null</code>
	 */
	public static String getDChiralImage(String aa) {
		if (aa == null) {
			throw new IllegalArgumentException("aminoacid is null");
		}
		aa = aa.toUpperCase();
		if (aa.equals("GLY")) {
			return "GLY";
		}else {
			return l2dAminioAcids.get(aa);
		}
	}

	/**
	 * Returns the L image of an aminoacid.
	 * Except for Glycine, all aminoacids have chiral images.
	 * @param aa the aminoacid name
	 * @return the L chiral image of the passed in aminoacid, <code>null</code> if not found
	 * @throws IllegalArgumentException aa is <code>null</code>
	 */
	public static String getLChiralImage(String aa) {
		if (aa == null) {
			throw new IllegalArgumentException("aminoacid is null");
		}
		aa = aa.toUpperCase();
		if (aa.equals("GLY")) {
			return "GLY";
		}else {
			return d2lAminioAcids.get(aa);
		}
	}
}
