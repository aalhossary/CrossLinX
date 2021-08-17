package amralhossary.bonds;


import java.util.HashSet;
import java.util.Hashtable;

import org.biojava.nbio.structure.Structure;

public interface ProteinParsingGUI {

	/**
	 * @param token
	 * @param specificCollectionScriptString
	 * @param foundInteractions
	 */
	public abstract void interactionsFoundInStructure(String token, String specificCollectionScriptString, 
			Hashtable<GroupOfInterest, HashSet<GroupOfInterest>> foundInteractions);
	public abstract void executeScript(String script);
	public abstract void structureLoaded(Structure structure);
	public abstract void showResults(Object results);

}
