/**
 * 
 */
package amralhossary.bonds;

import java.util.ArrayList;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.Group;

/**
 * @author Amr
 *
 */
public interface GroupOfInterest extends Group {

//	public static final String NAME_PHE = "PHE";
//	public static final String NAME_TRP = "TRP";
	public static final String NAME_ARG = "ARG";
	public static final String NAME_HIS = "HIS";
	public static final String NAME_LYS = "LYS";
	public static final String NAME_GLU = "GLU";
	public static final String NAME_GLN = "GLN";
	public static final String NAME_ASP = "ASP";
	public static final String NAME_ASN = "ASN";
	public static final String NAME_CYS = "CYS";
	public static final String NAME_CSO = "CSO";
	public static final String NAME_SEC = "SEC";
	public static final String NAME_SE7 = "SE7";
	public static final String NAME_THR = "THR";
	public static final String NAME_SER = "SER";
	public static final String NAME_TYR = "TYR";
	public static final String NAME_HET = "HET";
	public static final String NAME_1ST = "1ST";
	public static final String NAME_LST = "LST";
//	public static final int CODE_PHE = 0;
//	public static final int CODE_TRP = 2;
	public static final int CODE_LYS = 0;
	public static final int CODE_ARG = 1;
	public static final int CODE_HIS = 2; //new
	public static final int CODE_GLU = 3;
	public static final int CODE_GLN = 4;
	public static final int CODE_ASP = 5;
	public static final int CODE_ASN = 6;
	public static final int CODE_CSO = 7;
	public static final int CODE_CYS = 8;
	public static final int CODE_SEC = 9;
	public static final int CODE_SE7 = 10;
	public static final int CODE_THR = 11;
	public static final int CODE_SER = 12; //new
	public static final int CODE_TYR = 13; //new
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
	
	public static class ContainingCube{
		public int x;
		public int y;
		public int z;
	}

}
