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

	public static final String GENERAL_SELECTION_SCRIPT = "set logLevel 0; display not solvent; select *;cartoons only; color cartoon group;rockets off;ribbons off;\n"+
			"set showHydrogens false; set selectHydrogen off;"+
//			"SELECT (PHE OR TYR OR TRP OR LYS OR ARG OR GLU OR ASP) AND SIDECHAIN;"+
//			"spacefill 23%AUTO;wireframe 0.15;"
			 "color cpk;\n";

	private static final String INTERACTION_SEPARATOR = " \t-> ";
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
	 * Should be responsible for
	 * a] file formatting 
	 * b] TODO (+/-) ED Map loading scripts
	 * 
	 * please notice that if {@link #createInteractionString(AminoAcid)} changed, this method <b>MUST be updated</b>
	 * @param token
	 * @return
	 */
	public static String generateAfterLoadingJMolScriptString(PdbId pdbId) {
		//TODO update decodeDrawSphereCommand to ellipse
		StringBuffer buffer = new StringBuffer();
		buffer.append(GENERAL_SELECTION_SCRIPT);

		Set<String> interactingAtoms= new HashSet<String>();
		int separatorIndex;
		List<String> bondsList = retreiveBondsList(pdbId);
		if (bondsList == null) {
			return null;
		}

		//find interacting residues / atoms without duplicity
		for (String bondString : bondsList) {
			separatorIndex = bondString.indexOf(INTERACTION_SEPARATOR);
			String leftSide=bondString.substring(0, separatorIndex);
			String rightSide=bondString.substring(separatorIndex+INTERACTION_SEPARATOR.length()); //,line.indexOf('('));
			interactingAtoms.add(leftSide);
			interactingAtoms.add(rightSide);
		}
		
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
			buffer.append("SELECT (");
			for (int i = 0; i < interactingAtomsArray.length; i++) {
				String atomExpressionAndCoords = interactingAtomsArray[i];
				int indexOfOpeningPracket = atomExpressionAndCoords.indexOf('{'); // atom coordinates
				String atomExpression = atomExpressionAndCoords.substring(0, indexOfOpeningPracket);
				
				buffer.append(atomExpression);
				if (i < interactingAtomsArray.length -1) {
					buffer.append(" OR ");
				}
			}
			buffer.append(");");//wanted Atoms
			//make the whole residue sticks.
			buffer.append(
//					"spacefill ionic;"+
					"spacefill 0.5;"
					);//space fill
			buffer.append("color bonds [255,255,0];\n");

			//TODO complete by writing the Electron density map fetching and showing code
//				int indexOfOpeningPracket = interactingAtom.indexOf('{'); // atom coordinates
//				int indexOfClosingPracket = interactingAtom.indexOf('}'); // atom coordinates
		}
		return buffer.toString();
	}
	
	
	public static String generateLinkSelectedJMolScriptString(String linkFullString) {
		
		int separatorIndex = linkFullString.indexOf(INTERACTION_SEPARATOR);
		String leftSide = linkFullString.substring(0, separatorIndex);
		String rightSide = linkFullString.substring(separatorIndex+INTERACTION_SEPARATOR.length()); //,line.indexOf('('));
//		String atom1 = leftSide.substring(0, leftSide.indexOf('{'));
//		String atom2 = rightSide.substring(0, rightSide.indexOf('{'));
		String residue1 = leftSide.substring(0, leftSide.indexOf('.'));
		String residue2 = rightSide.substring(0, rightSide.indexOf('.'));
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("zoom 0;\n");
		buffer.append("zoomto { ").append(residue1).append(" or ").append(residue2).append(" };\n");
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
		StringBuilder listOfConnections = new StringBuilder();
		for (Bond bond : bonds) {
			String connectionString = createInteractionString(bond);
			listOfConnections.append(connectionString);
			listOfConnections.append(System.lineSeparator());
		}
		return listOfConnections.toString();
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

	public static HashSet<String> decodeInteractioString(String nextLine, Hashtable<String,HashSet<String>> interactions) {

		//extract source
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
