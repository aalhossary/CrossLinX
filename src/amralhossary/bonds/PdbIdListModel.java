package amralhossary.bonds;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractListModel;

import org.biojava.nbio.structure.PdbId;

public class PdbIdListModel extends AbstractListModel<PdbId> {
	private static final long serialVersionUID = 1L;

	ArrayList<PdbId> elements;
//	TreeSet<PdbId> elements;

	public PdbIdListModel() {
		elements = new ArrayList<PdbId>();
//		elements = new TreeSet<>();
	}

	@Override
	public int getSize() {
		return elements.size();
	}

	@Override
	public PdbId getElementAt(int index) {
		return elements.get(index);
	}
	
	public void clear() {
		int last = getSize() -1;
		elements.clear();
		fireIntervalRemoved(elements, 0, last);
	}
	
	public void sort() {
		Collections.sort(elements);
		fireContentsChanged(elements, 0, getSize()-1);
	}

	public void addElement(PdbId pdbId) {
		elements.add(pdbId);
		fireContentsChanged(elements, getSize()-2, getSize()-1);
	}

}
