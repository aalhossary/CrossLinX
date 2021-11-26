package amralhossary.bonds;


import org.biojava.nbio.structure.PdbId;
import org.biojava.nbio.structure.Structure;

public interface ProteinParsingGUI {

	/**
	 * @param token
	 * @param specificCollectionScriptString
	 * @param foundInteractions
	 */
	public abstract void interactionsFoundInStructure(PdbId token);
	public abstract void executeScript(String script);
	public abstract void structureLoaded(Structure structure);
	public abstract void showResults(Object results);
	public abstract void sortResults();

}
