/**
 * 
 */
package amralhossary.bonds;

import java.util.ArrayList;
import java.util.Hashtable;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.Group;

/**
 * @author Amr
 *
 */
public interface GroupOfInterest extends Group {

//	public static final String NAME___PHE = "PHE";
//	public static final String NAME___TRP = "TRP";
	public static final String NAME___ARG = "ARG";
	public static final String NAME_D_ARG = "DAR";
	public static final String NAME___HIS = "HIS";
	public static final String NAME_D_HIS = "DHI";
	public static final String NAME___LYS = "LYS";
	public static final String NAME_D_LYS = "DLY";
	public static final String NAME___GLU = "GLU";
	public static final String NAME_D_GLU = "DGL";
	public static final String NAME___GLN = "GLN";
	public static final String NAME_D_GLN = "DGN";
	public static final String NAME___ASP = "ASP";
	public static final String NAME_D_ASP = "DAS";
	public static final String NAME___ASN = "ASN";
	public static final String NAME_D_ASN = "DSG";
	public static final String NAME___CYS = "CYS";
	public static final String NAME_D_CYS = "DCY";
	public static final String NAME___CSO = "CSO";
//	public static final String NAME_D_CSO = "";  // not found (yet)
	public static final String NAME___SEC = "SEC";
//	public static final String NAME_D_SEC = "";
	public static final String NAME___SE7 = "SE7";
	public static final String NAME___THR = "THR";
	public static final String NAME_D_THR = "DTH";
	public static final String NAME___SER = "SER";
	public static final String NAME_D_SER = "DSN";
	public static final String NAME___TYR = "TYR";
	public static final String NAME_D_TYR = "DTY";
	public static final String NAME___HET = "HET";
	public static final String NAME___1ST = "1ST";
	public static final String NAME___LST = "LST";
//	public static final int CODE_PHE = 0;
//	public static final int CODE_TRP = 2;
	public static final int CODE_LYS = 0;
	public static final int CODE_ARG = 1;
	public static final int CODE_HIS = 2;
	public static final int CODE_GLU = 3;
	public static final int CODE_GLN = 4;
	public static final int CODE_ASP = 5;
	public static final int CODE_ASN = 6;
	public static final int CODE_CSO = 7;
	public static final int CODE_CYS = 8;
	public static final int CODE_SEC = 9;
	public static final int CODE_SE7 = 10;
	public static final int CODE_THR = 11;
	public static final int CODE_SER = 12;
	public static final int CODE_TYR = 13;
	public static final int CODE_OTHERS = 14;  //new, for all other AAs
	public static final int CODE_HET = 15;

	public abstract String getSuffix();
	
	public abstract int getGroupOfInterestType();

	public abstract int getMissingAtoms();

//	public abstract Atom[] getPositiveKeyAtoms();
//	
//	public abstract Atom[] getNegativeKeyAtoms();
	
	public abstract Atom[] getKeyAtoms();
	public abstract Atom[] getKeyCAtoms();
	public abstract Atom[] getKeyNAtoms();
	public abstract Atom[] getKeyOAtoms();

	public abstract ArrayList<ContainingCube> getContainingCubes();

	public abstract void setContainingCubes(ArrayList<ContainingCube> containingCubes);
	
	void putInCorrespondingCube(String suffix, Hashtable<String, ArrayList<GroupOfInterest>> cubes);

	public static class ContainingCube{
		public int x;
		public int y;
		public int z;
	}

}
