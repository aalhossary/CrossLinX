package amralhossary.bonds;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.biojava.nbio.structure.AminoAcidImpl;
import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.Element;
import org.biojava.nbio.structure.Group;
import org.biojava.nbio.structure.chem.ChemComp;

/**
 * 
 * 
 * @author Amr
 *
 */
public class HetGroupOfInterest extends AminoAcidImpl implements GroupOfInterest{
	private static final long serialVersionUID = -6254684424281007523L;
	
//	private Atom[] positiveKeyAtoms=null;
//	private Atom[] negativeKeyAtoms=null;
	private Atom[] keyAtoms=null;
	private Atom[] keyOAtoms = null;
	private Atom[] keyCAtoms = null;
	private Atom[] keyNAtoms = null;
	private ArrayList<ContainingCube> containingCubes=new ArrayList<AminoAcidOfInterest.ContainingCube>();
	
//	private int aAOfInterestType;

	private int missingAtoms;
	private String suffix="";

	private int groupOfInterestType;
	
	
	public static GroupOfInterest newHetGroupOfInterest(
			Group groupOfInterest,
			Hashtable<String, ArrayList<GroupOfInterest>> cubes){
		String pdbName = groupOfInterest.getPDBName();
		if (pdbName == null || "HOH".equalsIgnoreCase(pdbName)) {
			return null;
		}else {
			try {
				return new HetGroupOfInterest(groupOfInterest, cubes);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return null;
			}
		}
	}
	
	public HetGroupOfInterest(Group hetGroupOfInterest, Hashtable<String, ArrayList<GroupOfInterest>> cubes) {
		super();
		//first stage: cloning
		this.setPDBFlag(hetGroupOfInterest.has3D());		
		this.setResidueNumber(hetGroupOfInterest.getResidueNumber());
		this.setPDBName(hetGroupOfInterest.getPDBName());
//		this.setRecordType(hetGroupOfInterest.getRecordType());
		// copy the atoms
		for (Atom atom : hetGroupOfInterest.getAtoms()) {
			this.addAtom(atom);
		}
		this.setChain(hetGroupOfInterest.getChain());
		
		// copying the alt loc groups if present, otherwise they stay null
		if (hetGroupOfInterest.getAltLocs()!=null && !hetGroupOfInterest.getAltLocs().isEmpty()) {
			for (Group altLocGroup:hetGroupOfInterest.getAltLocs()) {
				Group nAltLocGroup = HetGroupOfInterest.newHetGroupOfInterest(altLocGroup, cubes);
				this.addAltLoc(nAltLocGroup);
			}
		}

		final ChemComp chemComp = hetGroupOfInterest.getChemComp();
		if (chemComp!=null)
			this.setChemComp(chemComp);
		
		//second stage: setting code_type
		this.groupOfInterestType=GroupOfInterest.CODE_HET;
		
		//third stage: filling contents
		fillContents();
		//forth stage
		putInCorrespondingCube(suffix, cubes);
	}

	private void fillContents() {
		this.suffix="|"+NAME___HET;
		List<Atom> atoms = getAtoms();
		ArrayList<Atom> heavyAtomsOfInterest= new ArrayList<Atom>();
		ArrayList<Atom> cAtomsOfInterest= new ArrayList<Atom>();
		ArrayList<Atom> oAtomsOfInterest= new ArrayList<Atom>();
		ArrayList<Atom> nAtomsOfInterest= new ArrayList<Atom>();
		for (Atom atom : atoms) {
			Element element = atom.getElement();
			if (element.isHeavyAtom()) {
				heavyAtomsOfInterest.add(atom);
				if (Element.C == element) {
					cAtomsOfInterest.add(atom);
				}else if (Element.O == element) {
					oAtomsOfInterest.add(atom);
				}if (Element.N == element) {
					nAtomsOfInterest.add(atom);
				}
			}
		}
		keyAtoms = heavyAtomsOfInterest.toArray(new Atom[heavyAtomsOfInterest.size()]);
		keyCAtoms = cAtomsOfInterest.toArray(new Atom[cAtomsOfInterest.size()]);
		keyOAtoms = oAtomsOfInterest.toArray(new Atom[oAtomsOfInterest.size()]);
		keyNAtoms = nAtomsOfInterest.toArray(new Atom[nAtomsOfInterest.size()]);
	}
	
	@Override
	public void putInCorrespondingCube(String suffix, Hashtable<String, ArrayList<GroupOfInterest>> cubes) {
		int x,y,z;
		if(keyAtoms == null)
			return;
		for (Atom keyAtom : keyAtoms) {
			if (keyAtom != null) {
				AminoAcidOfInterest.processedAtoms++;
				x = ProteinParser.angstromToCube(keyAtom.getX());
				y = ProteinParser.angstromToCube(keyAtom.getY());
				z = ProteinParser.angstromToCube(keyAtom.getZ());
				ProteinParser.putInCube(this, x, y, z, suffix, cubes);
			}
		}
	}

	//----------------Accessor methods---------------------

	/**
	 * @param containingCubes the containingCubes to set
	 */
	@Override
	public void setContainingCubes(ArrayList<ContainingCube> containingCubes) {
		this.containingCubes = containingCubes;
	}
	
	/**
	 * @return the containingCubes
	 */
	@Override
	public ArrayList<ContainingCube> getContainingCubes() {
		return containingCubes;
	}
	
	/**
	 * @return the missingAtoms
	 */
	@Override
	public int getMissingAtoms() {
		return missingAtoms;
	}

	/**
	 * @return the suffix
	 */
	@Override
	public String getSuffix() {
		return suffix;
	}

	@Override
	public int getGroupOfInterestType() {
		return groupOfInterestType;
	}

	@Override
	public Atom[] getKeyAtoms() {
		return keyAtoms;
	}

	@Override
	public Atom[] getKeyOAtoms() {
		return keyOAtoms;
	}
	
	@Override
	public Atom[] getKeyNAtoms() {
		return keyNAtoms;
	}
	
	@Override
	public Atom[] getKeyCAtoms() {
		return keyCAtoms;
	}
}
