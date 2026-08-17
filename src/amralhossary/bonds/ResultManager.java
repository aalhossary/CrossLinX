package amralhossary.bonds;


import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biojava.nbio.structure.AminoAcid;
import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.Bond;
import org.biojava.nbio.structure.Group;
import org.biojava.nbio.structure.PdbId;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.align.util.UserConfiguration;
import org.biojava.nbio.structure.io.CifFileReader;
import org.biojava.nbio.structure.io.LocalPDBDirectory;
import org.biojava.nbio.structure.io.PDBFileReader;
import org.jmol.api.JmolViewer;

/** //TODO remember to declare files failed to parse in results log file and retrieving them when parsing it 
 * @author Amr
 *
 */
public class ResultManager {
	private static final String SPHERE_KEYWORD = "sphere";

	//selectAllModels is a viewer-wide flag that outlives the structure that set it, and
	//generateAfterLoadingJMolScriptString deliberately turns it OFF at the end of every
	//load. Without turning it back ON here, the "select *" below would reach only the
	//displayed frame of the NEXT structure loaded, leaving models 2..N of every structure
	//after the first with no cartoon at all. Measured on a 3-model entry: 7584 atoms
	//selected with the flag on, 2528 - model 1 alone - with it off.
	public static final String GENERAL_SELECTION_SCRIPT = "set logLevel 0; set selectAllModels TRUE; display not solvent; select *;cartoons only; color cartoon group;rockets off;ribbons off;\n"+
			"set showHydrogens false; set selectHydrogen off;"+
//			"SELECT (PHE OR TYR OR TRP OR LYS OR ARG OR GLU OR ASP) AND SIDECHAIN;"+
//			"spacefill 23%AUTO;wireframe 0.15;"
			 "color cpk;\n";

	private static final String INTERACTION_SEPARATOR = " \t-> ";

	/**
	 * Marks which model of the structure an interaction was found in, written in front of
	 * the interaction itself:
	 * <pre>model#12 | [LYS]48:B.NZ {…} 	-&gt; [GLY]76:C.C {…}</pre>
	 * An interaction line always starts with '[', so this can never be mistaken for one.
	 * @see #tagWithModel(String, int)
	 */
	private static final String MODEL_TAG_PREFIX = "model#";
	private static final String MODEL_TAG_SUFFIX = " | ";
	/**
	 * Model number meaning "do not write a tag at all", used for the single-model
	 * structures that are the overwhelming majority. Results files written for them stay
	 * byte for byte what earlier versions wrote.
	 */
	public static final int UNTAGGED = 0;
	/**
	 * What {@link #modelOf(String)} answers for a line carrying no tag. Files written
	 * before models were understood hold one model's interactions, and Jmol numbers the
	 * first model 1, so that is the model those lines belong to.
	 */
	public static final int DEFAULT_MODEL = 1;

	/**
	 * Spacefill radius given to every atom that takes part in an interaction, applied once
	 * when the structure is loaded.
	 * <p>
	 * The picked interaction is drawn at this same size: it is marked by its selection
	 * halo, and enlarging it as well made it read as a pair of bigger atoms rather than as
	 * the picked pair.
	 */
	private static final String INTERACTING_ATOM_SPACEFILL = "0.5";
	public static final String CACHE_RESULT_FOLDER = "temp/cashe";
	public static final String START_OF_STRUCTURE_PREFIX = "in structure#";
	public static final String FAILED_TO_PARSE_AMINOACID = "##Failed to Parse ";

	private static SettingsManager settingsManager = SettingsManager.getSettingsManager();

	public static File createCacheFolderForToken(PdbId pdbId) {
		int offset = pdbId.getId().length() - 3;
		String hash = pdbId.getId().substring(offset, offset+2);
		File cacheFolder = new File(settingsManager.getWorkingFolder(), CACHE_RESULT_FOLDER + "/" + hash);
		cacheFolder.mkdirs();
		return cacheFolder;
	}

//	/**
//	 * @param token
//	 * @param specificCollectionScriptString 
//	 * @param foundInteractions
//	 * @return
//	 * @deprecated Update according to new parameters, and update decodeDrawSphereCommand to ellipse
//	 */
//	static String generateJMolScriptString(String token, String specificCollectionScriptString, Map<GroupOfInterest, Set<Bond>> foundInteractions) {
//		StringBuffer buffer = new StringBuffer();
//		buffer.append(GENERAL_SELECTION_SCRIPT);
//		//add spheres
//		String[] lines =specificCollectionScriptString.split("\r?\n");
//		for (String line : lines) {
//			if (line.startsWith(SPHERE_KEYWORD) && settingsManager.isDomainEnabled()) {
//				buffer.append(decodeDrawSphereCommand(line));
//			}
//		}
//		//			buffer.append("restrict bonds not selected;");
//
//		buffer.append("SELECT (");
//		
//		Set<GroupOfInterest> lysines = foundInteractions.keySet();
//		for (GroupOfInterest lysine : lysines) {
//			addResidueToSelectionStringBuffer(buffer,lysine);
//			Set<GroupOfInterest> set = foundInteractions.get(lysine);
//			for (GroupOfInterest interactionTarget : set) {
//				addResidueToSelectionStringBuffer(buffer,interactionTarget);
//			}
//		}
//		buffer.append("FALSE) ;");//wanted Atoms
//		buffer.append("spacefill 65%;color cpk;");//space fill
//		//			buffer.append("selectionHalos ON;");
//		return buffer.toString();
//	}


	public static Structure getStructureById(PdbId pdbId) {
		try {
			LocalPDBDirectory fileReader = null;
			if(UserConfiguration.PDB_FORMAT.equals(settingsManager.getFileFormat())) {
				fileReader = new PDBFileReader(settingsManager.getPdbFilePath()); 
			} else if(UserConfiguration.MMCIF_FORMAT.equals(settingsManager.getFileFormat())) {
				fileReader = new CifFileReader(settingsManager.getPdbFilePath());
			}
//			fileReader.setFetchBehavior(settingsManager.isAutoFetch());
			return fileReader.getStructureById(pdbId);
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}

	
	/**
	 * Styles a structure the moment it is loaded: cartoons, then the interacting residues as
	 * sticks, then the interacting atoms themselves.
	 * <p>
	 * The density map is not part of this script - see the note beside the interacting-atom
	 * styling below, and {@link #interactingAtomsSelection(PdbId)}.
	 * <p>
	 * please notice that if {@link #createInteractionString(Bond)} changed, this method <b>MUST be updated</b>
	 * @param pdbId the structure being shown
	 * @return the script, or null when the structure has no cached interactions
	 */
	public static String generateAfterLoadingJMolScriptString(PdbId pdbId) {
		//TODO update decodeDrawSphereCommand to ellipse
		StringBuffer buffer = new StringBuffer();
		buffer.append(GENERAL_SELECTION_SCRIPT);

		List<String> bondsList = retreiveBondsList(pdbId);
		if (bondsList == null) {
			return null;
		}
		Set<String> interactingAtoms = collectInteractingAtoms(bondsList);

		//augment interacting residues / atoms
		if (interactingAtoms.size()>0) {
			buffer.append("SELECT ("
//					+ "("
					);
			String[] interactingAtomsArray = interactingAtoms.toArray(new String[] {});
			for (int i = 0; i < interactingAtomsArray.length; i++) {
				String interactingAtom = interactingAtomsArray[i];
//				int indexOfClosingSquareBracket = interactingAtom.indexOf(']');
				int indexOfColon = interactingAtom.indexOf(':');
				int indexOfDot = interactingAtom.indexOf('.', indexOfColon);

				final String residueDefinition = interactingAtom.substring(0, indexOfDot);
				buffer.append(residueDefinition);
				if (i < interactingAtomsArray.length -1) {
					buffer.append(" OR ");
				}
			}
			buffer.append(" )"
//					+ " AND (sidechain OR *.CA)"
//					+ ")"
					+ ";");
			//make the whole residue sticks.
//			buffer.append("wireframe 0.3 only;color cpk;\n");
			buffer.append("spacefill off; wireframe 0.25;"
//					+ "set bondmode AND;"
					+ "color bonds none;\n");

			//Then make the interacting atoms spacefill and/or show ED map
			buffer.append("SELECT (").append(asAtomSelectionExpression(interactingAtoms)).append(");");//wanted Atoms
			//make the whole residue sticks.
			buffer.append(
//					"spacefill ionic;"+
					"spacefill ").append(INTERACTING_ATOM_SPACEFILL).append(";"
					);//space fill
			buffer.append("color bonds [255,255,0];\n");

			//The density map is NOT drawn from here. It is fetched and contoured by
			//ParsingUI's density service, because it may have to be downloaded first and
			//nothing that reaches the network belongs in a script generator. The selection
			//it contours around comes from interactingAtomsSelection(PdbId) below, which
			//covers exactly the atoms selected here.
		}
		//A structure is loaded with nothing picked yet, so drop any halo left over from
		//the interaction picked in the previously shown structure. See
		//generateLinkSelectedJMolScriptString, which is what turns halos back on.
		buffer.append("set selectionHalos OFF;\n");
		//An atom expression such as [LYS]48:B.NZ names an atom in EVERY model at once
		//while selectAllModels is TRUE, and zoomto then centres on the average of all of
		//them - which for an NMR ensemble is a point the user never asked to look at.
		//Restricting selection to the displayed frame makes each model behave like the
		//single structure it represents.
		//
		//This has to stay the LAST line of this script and must NOT move into
		//GENERAL_SELECTION_SCRIPT: everything above styles the interacting atoms, and that
		//styling has to reach every model while the flag is still TRUE, or models 2..N
		//would load unstyled. The restriction applies from here on.
		//
		//Because the flag is viewer-wide it survives into the next structure loaded, which
		//is why GENERAL_SELECTION_SCRIPT turns it back ON before it styles anything.
		buffer.append("set selectAllModels FALSE;\n");
		return buffer.toString();
	}

	/**
	 * Collects, without duplicates, every atom taking part in an interaction, each still
	 * carrying its coordinate block exactly as {@link #createInteractionString(Bond)} wrote it.
	 * @param bondsList interaction lines, as stored in the cache file
	 */
	private static Set<String> collectInteractingAtoms(List<String> bondsList) {
		Set<String> interactingAtoms = new HashSet<String>();
		for (String taggedBondString : bondsList) {
			String bondString = stripModelTag(taggedBondString);
			int separatorIndex = bondString.indexOf(INTERACTION_SEPARATOR);
			interactingAtoms.add(bondString.substring(0, separatorIndex));
			interactingAtoms.add(bondString.substring(separatorIndex+INTERACTION_SEPARATOR.length())); //,line.indexOf('('));
		}
		return interactingAtoms;
	}

	/**
	 * Turns atom strings into one Jmol atom expression, dropping the coordinate block that
	 * follows each atom: {@code [LYS]48:B.NZ%A OR [GLY]76:C.C}.
	 * @param atomsWithCoords atoms as produced by {@link #collectInteractingAtoms(List)}
	 */
	private static String asAtomSelectionExpression(Collection<String> atomsWithCoords) {
		StringBuilder expression = new StringBuilder();
		for (String atomAndCoords : atomsWithCoords) {
			if (expression.length() > 0) {
				expression.append(" OR ");
			}
			expression.append(stripAtomCoords(atomAndCoords));
		}
		return expression.toString();
	}

	/**
	 * A Jmol atom expression covering every atom of every interaction found in a structure,
	 * braced and ready to hand to a command that takes a selection - notably
	 * {@code JmolPanel.loadDensityMap(file, kind, level, isSigma, selection, radius)}, which
	 * contours only within a distance of it.
	 * <p>
	 * Model tags are stripped, so an ensemble contributes the same atoms once rather than
	 * once per model. The expression names atoms, not coordinates, so it is equally valid in
	 * whichever model the viewer is currently showing.
	 *
	 * @param pdbId the structure whose cached interactions to cover
	 * @return {@code {[LYS]48:B.NZ OR [GLY]76:C.C}}, or null when the structure has no cache
	 *         file or no interactions in it. Callers should skip drawing rather than fall
	 *         back to {@code {*}}: contouring a whole map is slow enough to look like a hang.
	 */
	public static String interactingAtomsSelection(PdbId pdbId) {
		List<String> bondsList = retreiveBondsList(pdbId);
		if (bondsList == null || bondsList.isEmpty()) {
			return null;
		}
		String expression = asAtomSelectionExpression(collectInteractingAtoms(bondsList));
		return expression.isEmpty() ? null : "{" + expression + "}";
	}

	/** Drops the trailing <code>{x y z}</code> block from a single atom string. */
	private static String stripAtomCoords(String atomAndCoords) {
		int indexOfOpeningBracket = atomAndCoords.indexOf('{'); // atom coordinates
		return atomAndCoords.substring(0, indexOfOpeningBracket).trim();
	}

	/**
	 * The script run when the user picks one interaction out of the list: zoom out, zoom
	 * back in on the two residues, then pick out the two interacting atoms themselves.
	 * <p>
	 * The halo alone marks the picked pair. The atoms keep the size every interacting atom
	 * has, because enlarging them as well made them read as bigger atoms rather than as the
	 * picked ones - the halo is already unmistakable, and doubling up only obscured the
	 * atoms underneath.
	 * <p>
	 * The size reset below is still needed: it puts back the size
	 * {@link #generateAfterLoadingJMolScriptString(PdbId)} gave every interacting atom, so
	 * results written by an older version - which did enlarge the picked pair - do not
	 * leave atoms enlarged for the rest of the session. The halo needs no such reset; it
	 * follows the current selection.
	 *
	 * @param linkFullString the interaction, as written in the results file
	 * @param pdbId structure the interaction belongs to; used to find the atoms to reset.
	 *              May be null, in which case any leftover sizes are left as they are.
	 */
	public static String generateLinkSelectedJMolScriptString(String taggedLinkFullString, PdbId pdbId) {

		String linkFullString = stripModelTag(taggedLinkFullString);
		int separatorIndex = linkFullString.indexOf(INTERACTION_SEPARATOR);
		String leftSide = linkFullString.substring(0, separatorIndex);
		String rightSide = linkFullString.substring(separatorIndex+INTERACTION_SEPARATOR.length()); //,line.indexOf('('));
		String atom1 = stripAtomCoords(leftSide);
		String atom2 = stripAtomCoords(rightSide);
		String residue1 = leftSide.substring(0, leftSide.indexOf('.'));
		String residue2 = rightSide.substring(0, rightSide.indexOf('.'));

		StringBuffer buffer = new StringBuffer();
		buffer.append("zoomto 1.0 {visible} 0; delay 1.0;\n");
		buffer.append("zoomto 0.5 { ").append(residue1).append(" or ").append(residue2).append(" };\n");
		buffer.append("zoomto 0.5 { ").append(residue1).append(" or ").append(residue2).append(" } 0 *0.7;\n");

		List<String> bondsList = (pdbId == null) ? null : retreiveBondsList(pdbId);
		if (bondsList != null) {
			buffer.append("SELECT (").append(asAtomSelectionExpression(collectInteractingAtoms(bondsList))).append(");");
			buffer.append("spacefill ").append(INTERACTING_ATOM_SPACEFILL).append(";\n");
		}
		buffer.append("SELECT (").append(atom1).append(" OR ").append(atom2).append(");");
		buffer.append("set selectionHalos ON;\n");
		return buffer.toString();
	}



public static File prepareFilesList(boolean temp) {
		System.out.println("Preparing Files List");
		Pattern fileNamePattern = Pattern.compile("(pdb)?(([0-9a-z]{4})?([1-9]\\p{Alnum}{3}))((\\.ent\\.gz)|(\\.cif\\.gz))", Pattern.CASE_INSENSITIVE);
//		Pattern fileNamePattern = Pattern.compile("(([0-9a-z]{4})?([1-9]\\p{Alnum}{3}))", Pattern.CASE_INSENSITIVE);
		try {
			String filesPath = null;
			if(UserConfiguration.PDB_FORMAT.equals(settingsManager.getFileFormat()))
				filesPath = "data/structures/divided/pdb";
			else if (UserConfiguration.MMCIF_FORMAT.equals(settingsManager.getFileFormat()))
				filesPath = "data/structures/divided/mmCIF";
			else
				throw new IllegalArgumentException("Unknown File Format ["+ settingsManager.getFileFormat() + "]");
			File pdbFolder= new File(settingsManager.getPdbFilePath(), filesPath);			
			System.out.println("creating list for: "+pdbFolder.getAbsolutePath());
			File[] potentialFolders = pdbFolder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.length()==2;
				}
			});
			Arrays.sort(potentialFolders);
			File list;
			if (temp) {
				list = File.createTempFile("FilesList", null, new File(settingsManager.getWorkingFolder()));
				list.deleteOnExit();
			} else {
				list = new File(settingsManager.getWorkingFolder(), "FilesList.txt");
			}
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return fileNamePattern.matcher(name).matches();
				}
			};
			PrintStream out = new PrintStream(list);
			for (int i = 0; ProteinParser.moreWork && i < potentialFolders.length; i++) {
				File potentialFolder = potentialFolders[i];
				if (potentialFolder.isDirectory()) {
					String[] potentialFileNames = potentialFolder.list(filter);
					if (potentialFileNames.length>0) {
						out       .println("##Folder :"+potentialFolder.getName());
						System.out.println("##Folder :"+potentialFolder.getName());
						Arrays.sort(potentialFileNames);
						for (int j = 0; j < potentialFileNames.length; j++) {
							String potentialFileName = potentialFileNames[j];
							Matcher matcher = fileNamePattern.matcher(potentialFileName);
							if(matcher.find()) {
								out.print(matcher.group(2));
								if (j < potentialFileNames.length-1) {
									out.print(';');
								}else {
									out.println();
								}
							}
						}
					}
				}
			}
			out.close();
			System.out.println("List prepared");
			return list;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**transforms the data structure passed to a simple human readable and machine parsable string
	 * @param foundInteractions
	 * @return
	 */
	public static String createListofConnectionsAsString(Set<Bond> bonds) {
		return createListofConnectionsAsString(bonds, UNTAGGED);
	}

	/**
	 * As {@link #createListofConnectionsAsString(Set)}, but records which model each
	 * interaction was found in.
	 * @param modelNumber the 1-based model the bonds came from, or {@link #UNTAGGED} for a
	 *        structure with only one model, which is written exactly as it always was
	 */
	public static String createListofConnectionsAsString(Set<Bond> bonds, int modelNumber) {
		StringBuilder listOfConnections = new StringBuilder();
		for (Bond bond : bonds) {
			String connectionString = createInteractionString(bond);
			listOfConnections.append(tagWithModel(connectionString, modelNumber));
			listOfConnections.append(System.lineSeparator());
		}
		return listOfConnections.toString();
	}

	/**
	 * Puts the model tag in front of an interaction line.
	 * @param modelNumber {@link #UNTAGGED} returns the line untouched
	 */
	public static String tagWithModel(String interactionLine, int modelNumber) {
		if (modelNumber == UNTAGGED) {
			return interactionLine;
		}
		return MODEL_TAG_PREFIX + modelNumber + MODEL_TAG_SUFFIX + interactionLine;
	}

	/**
	 * Removes the model tag, if there is one. Every place that hands a line to Jmol has to
	 * do this first: the tag is ours, and Jmol would choke on it.
	 */
	public static String stripModelTag(String interactionLine) {
		if (! interactionLine.startsWith(MODEL_TAG_PREFIX)) {
			return interactionLine;
		}
		int endOfTag = interactionLine.indexOf(MODEL_TAG_SUFFIX);
		if (endOfTag < 0) {
			return interactionLine;
		}
		return interactionLine.substring(endOfTag + MODEL_TAG_SUFFIX.length());
	}

	/**
	 * Which model an interaction line belongs to.
	 * @return the tagged model number, or {@link #DEFAULT_MODEL} when the line carries no
	 *         tag - which is every line in a results file written for a single-model
	 *         structure, and every line in a file written before models were understood
	 */
	public static int modelOf(String interactionLine) {
		if (! interactionLine.startsWith(MODEL_TAG_PREFIX)) {
			return DEFAULT_MODEL;
		}
		int endOfTag = interactionLine.indexOf(MODEL_TAG_SUFFIX);
		if (endOfTag < 0) {
			return DEFAULT_MODEL;
		}
		try {
			return Integer.parseInt(interactionLine.substring(MODEL_TAG_PREFIX.length(), endOfTag).trim());
		} catch (NumberFormatException e) {
			//a line that only looks like a tag; treat it as untagged rather than fail
			return DEFAULT_MODEL;
		}
	}

	/** the first is usually the pi system.
	 * please notice that if you updated this method, {@link #generateAfterLoadingJMolScriptString(String)} must me updated too.
	 * @param interactionTarget
	 * @return
	 */
	public static String createInteractionString(Bond bond) {
		StringBuilder interactionOfInterestFound = new StringBuilder(getRepresentativeString(bond.getAtomA())).append(INTERACTION_SEPARATOR);
		interactionOfInterestFound.append(getRepresentativeString(bond.getAtomB()));

		return interactionOfInterestFound.toString();
	}

	public static HashSet<String> decodeInteractionString(String nextLine, Hashtable<String,HashSet<String>> interactions) {

		//extract source
		nextLine = stripModelTag(nextLine);
		final int interactionSeparatorPosition = nextLine.indexOf(INTERACTION_SEPARATOR);
		String source = nextLine.substring(0,interactionSeparatorPosition);
		//get targets or create a new set
		HashSet<String> targetsSet = interactions.get(source);
		if (targetsSet == null) {
			targetsSet = new HashSet<String>();
		}
		//decode target(s)
		nextLine = nextLine.substring(interactionSeparatorPosition+INTERACTION_SEPARATOR.length());
		String target= nextLine;
		
//		InteractionTarget persistedInteractionTarget = new InteractionTarget(target, new Vector3d(x, y, z), th, charge, side);
		
		//add to targets set
		targetsSet.add(target);
		interactions.put(source, targetsSet);
		return targetsSet;
	}



	public static boolean exportFileToJmol(String path, String fileName, JmolViewer jmoll ) {
		String pathFile = "Export "+ path + File.separatorChar + fileName + ".jmol";

		jmoll.evalString(pathFile);

		return true;
	}



	/**
	 * 
	 * a] file loading 
	 * b] file formatting 
	 * c] TODO (+/-) ED Map loading scripts
	 * 
	 * @param pdbId
	 * @param exportFolder
	 * @return
	 * @deprecated scripts should be created on the fly
	 */
	public static boolean exportFileLoadingScript(PdbId pdbId, File exportFolder) {
		String command = generateFileLoadJMolScript(pdbId);
		String pdbIdString = pdbId.toString();
		if (exportFolder.exists() || exportFolder.mkdirs()) {
			try {
				PrintWriter file = new PrintWriter(new File(exportFolder, pdbIdString+".spt"));
				file.println(command);
				file.println(GENERAL_SELECTION_SCRIPT);
				file.close();
				return true;
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Currently replaced by {@link #getStructureById(PdbId)}. We need to choose the better one of them.
	 * @param pdbId
	 * @return
	 * @see #getStructureById(PdbId)
	 */
	public static String generateFileLoadJMolScript(PdbId pdbId) {
		String pdbIdString = pdbId.toString();
		int beginIndex = pdbIdString.length() - 3;
		String middle = pdbIdString.substring(beginIndex, beginIndex + 2);
		String fileName;
		switch(settingsManager.getFileFormat()) {
			case UserConfiguration.PDB_FORMAT:
				fileName = "pdb" + pdbIdString +".ent.gz";
				break;
			case UserConfiguration.MMCIF_FORMAT:
				fileName = pdbIdString +".cif.gz";
				break;
			default:
				throw new IllegalArgumentException("Unknown file format ["+settingsManager.getFileFormat()+"]");
		}
		String command = "load \"" + settingsManager.getPdbFilePath() + File.separatorChar + middle + File.separatorChar + fileName + "\";";
		return command;
	}


	/**
	 * ellipsoid ID amr1 AXES {0 0 10} {0 20 0} {30 0 0} color TRANSLUCENT green CENTER {0 0 0};
	 * 
	 * @param name
	 * @param axisX
	 * @param axisY
	 * @param axisZ
	 * @param center
	 * @return
	 */
	public static String generateDrawEllipsoidCommand(String name, float[] axisX, float[] axisY, float[] axisZ, float[] center) {
		String axX = "{" + axisX[0] + " " + axisX[1] + " " + axisX[2] + "} ";
		String axY = "{" + axisY[0] + " " + axisY[1] + " " + axisY[2] + "} ";
		String axZ = "{" + axisZ[0] + " " + axisZ[1] + " " + axisZ[2] + "} ";
		String centerstring = "{" + center[0] + " " + center[1] + " " + center[2] + "} ";
		String commandEllip = "ellipsoid " + " ID " + name + " AXES " + axX + axY + axZ + " CENTER " + centerstring + " color translucent green;"+
				"ellipsoid " + " ID " + name + " on;"+System.getProperty("line.separator");

		return commandEllip;
	}
	//	public static String generateDrawSphereCommand(String name, Point3d aromaticAACenter, double radius) {
	//		String axX = "{" + 0 + " " + 0 + " " + radius + "} ";
	//		String axY = "{" + 0 + " " + radius + " " + 0 + "} ";
	//		String axZ = "{" + radius + " " + 0 + " " + 0 + "} ";
	//		String centerstring = "{" + aromaticAACenter.x + " " + aromaticAACenter.y + " " + aromaticAACenter.z + "} ";
	//		String commandEllip = "ellipsoid " + " ID " + name + " AXES " + axX + axY + axZ + " CENTER " + centerstring + " color translucent yellow;" +
	//				"ellipsoid " + " ID " + name + " on;"+System.getProperty("line.separator");
	//		
	//		return commandEllip;
	//	}
	public static String encodeDrawSphereCommand(String name, double[] center, double radius) {
		StringBuilder builder = new StringBuilder()
		.append(SPHERE_KEYWORD).append('\t').append(name).append('\t')
		.append(String.format("%.2f", center[0])).append('\t')
		.append(String.format("%.2f", center[1])).append('\t')
		.append(String.format("%.2f", center[2])).append('\t')
		.append(String.format("%.2f", radius)).append(System.getProperty("line.separator"));
		return builder.toString();
	}
	public static String decodeDrawSphereCommand(String command) {
		String[] strings = command.split("\t");
		String id = strings[1];
		String centerX=strings[2];
		String centerY=strings[3];
		String centerZ=strings[4];
		String radius =strings[5];

		StringBuilder builder = new StringBuilder();
		builder.append("ellipsoid ").append(" ID ").append(id).
		append(" AXES ")
		.append('{').append(radius).append(' ').append(0).append(' ').append(0).append("} ") 
		.append('{').append(0).append(' ').append(radius).append(' ').append(0).append("} ") 
		.append('{').append(0).append(' ').append(0).append(' ').append(radius).append("} ")
		.append(" CENTER ")
		.append('{').append(centerX).append(' ').append(centerY).append(' ').append(centerZ).append("} ")
		.append(" color translucent {50 150 50}; ")
		.append("ellipsoid ID ").append(id).append(" on;").append(System.getProperty("line.separator"));
		return builder.toString();
	}


	// draw cylinder diameter 28.0 {70 -25 0} {70 25 0} color TRANSLUCENT red ;

	public static String generateDrawCylinderCommand(double diameter, double[] point1, double[] point2) {
		double diameter1 =  diameter;
		String firstpoint = " {" + point1[0] + " " + point1[1] + " " + point1[2] + "} ";
		String secondpoint = "{" + point2[0] + " " + point2[1] + " " + point2[2] + "} ";
		String commandCylinder = "draw cylinder diameter " + diameter1 + firstpoint + secondpoint  +  " color translucent green;" ;

		return commandCylinder;
	}


//	/**
//	 * @param source the number of positive and negative interactions above a ring
//	 * @param dest the number of positive and negative interactions below a ring
//	 * @param frequency number of occurrence of this event (it will be divided by 10 to represent the thickness of the line) 
//	 * @param color preferred color of the representing line 
//	 * @return the command of the line representation of the state provided 
//	 */
//	public static String generateRepresentativeLine(int[] source,int[] dest, double frequency, Color color) {
//		return generateRepresentativeLine(Frequency4D.normalizePairedCoordinates(source, dest), frequency, color);
//	}
//	
//	static String generateRepresentativeLine(int[] normalizedCoordinates, double frequency, Color color) {
//		StringBuilder id = new StringBuilder("line");
//		for (int i = 0; i < normalizedCoordinates.length; i++) {
//			id.append(normalizedCoordinates[i]).append('_');
//		}
//		id.deleteCharAt(id.length()-1);
////		return generateDrawLineCommand(id.toString()+"scaf", new Point3d(FREQUENCY_DISTANCE* normalizedCoordinates[0], Math.log(frequency), -FREQUENCY_DISTANCE*normalizedCoordinates[1]), new Point3d(FREQUENCY_DISTANCE*normalizedCoordinates[0], - Math.log(frequency),-FREQUENCY_DISTANCE*normalizedCoordinates[1]), Color.YELLOW, true, 1)
////				+generateDrawLineCommand(id.toString()+"fold", new Point3d(FREQUENCY_DISTANCE* normalizedCoordinates[2], Math.log(frequency), -FREQUENCY_DISTANCE*normalizedCoordinates[3]), new Point3d(FREQUENCY_DISTANCE*normalizedCoordinates[2], - Math.log(frequency),-FREQUENCY_DISTANCE*normalizedCoordinates[3]), Color.YELLOW, true, 1)
////				+generateDrawLineCommand(id.toString(), new Point3d(FREQUENCY_DISTANCE* normalizedCoordinates[0], Math.log(frequency), -FREQUENCY_DISTANCE*normalizedCoordinates[1]), new Point3d(FREQUENCY_DISTANCE*normalizedCoordinates[2], -Math.log(frequency),-FREQUENCY_DISTANCE*normalizedCoordinates[3]), color,true, Math.log(frequency));
//		return   generateDrawLineCommand(id.toString(), new Point3d(FREQUENCY_DISTANCE* normalizedCoordinates[0], 5*Math.log(frequency), -FREQUENCY_DISTANCE*normalizedCoordinates[1]), new Point3d(FREQUENCY_DISTANCE*normalizedCoordinates[2], -5 * Math.log(frequency),-FREQUENCY_DISTANCE*normalizedCoordinates[3]), color,true, Math.log(frequency));
//		//		return generateDrawLineCommand(id.toString(), new Point3d(10* normalizedCoordinates[0], 10, -10*normalizedCoordinates[1]), new Point3d(10*normalizedCoordinates[2],-10,-10*normalizedCoordinates[3]), color, frequency/10);
//	}



	/**general purpose function to draw a line.<br>
	 * example "draw line  width 20 color red {40,50,60} {80,90,100} ;"
	 * @param point1
	 * @param point2
	 * @param color
	 * @param width
	 * @return
	 */
	public static String generateDrawLineCommand(String id, double[] point1, double[] point2, Color color,boolean translucent, double width) {
		double width1 =  width;
		String color1 = "["+color.getRed()+','+color.getGreen()+','+color.getBlue()+']';
		String firstpoint = " {" + point1[0] + " " + point1[1] + " " + point1[2] + "} ";
		String secondpoint = "{" + point2[0] + " " + point2[1] + " " + point2[2] + "} ";
		String commandLine = "draw id"+id+ " line width " + width1 + " color "+(translucent? "translucent ":"") + color1 +  firstpoint + secondpoint+";\r\n";
		return commandLine;

	}

	/**
	 * @param atom
	 * @return
	 */
	public static String getRepresentativeString(Atom atom) {
		Group group = atom.getGroup();
		StringBuilder representativeString = new StringBuilder().append("[").append(group.getPDBName()).append("]");
		representativeString.append(group.getResidueNumber()).append(":").append(group.getChain().getName());
		//add atom name / altLot
		representativeString.append('.').append(atom.getName());

		final Character altLoc = atom.getAltLoc();
		if(altLoc != null && altLoc != ' ') {
			representativeString.append('%').append(altLoc);
		}
		double[] coords = atom.getCoords();
		representativeString.append(String.format(" {%.3f %.3f %.3f}", coords[0], coords[1], coords[2]));

//		if (groupOfInterest instanceof HetGroupOfInterest) {
//			representativeString.append('{');
//			Atom[] positiveKeyAtoms = groupOfInterest.getPositiveKeyAtoms();
//			for (int i = 0; i < positiveKeyAtoms.length; i++) {
//				representativeString.append(positiveKeyAtoms[i].getElement().name()).append('|').append(positiveKeyAtoms[i].getPDBserial());
//				if (i<positiveKeyAtoms.length-1) {
//					representativeString.append(',');
//				}
//			}
//			representativeString.append('}');
//		}
		return representativeString.toString();
	}
	public static String removeAtomCoords(String listofDetailedConnectionsAsString) {
		return listofDetailedConnectionsAsString.replaceAll("\\{.+?\\}", "");
	}

	public static void persistBondsList(PdbId pdbId, List<String> bonds) {
		try {
			File folderForToken = ResultManager.createCacheFolderForToken(pdbId);
			FileWriter writer;
			writer = new FileWriter(new File(folderForToken, pdbId.getId()));
			for(String bond: bonds) {
				writer.append(bond.toString()).append(System.lineSeparator());									
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static List<String> retreiveBondsList(PdbId pdbId) {
		try {
			File folderForToken = ResultManager.createCacheFolderForToken(pdbId);
			Scanner scanner = new Scanner(new File(folderForToken, pdbId.getId()));
			List<String> ret = new ArrayList<>();
			while (scanner.hasNextLine()) {
				String string = (String) scanner.nextLine();
				ret.add(string);
			}
			return ret;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
}
