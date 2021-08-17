package amralhossary.bonds;


import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;

import javax.vecmath.Point3d;

import org.biojava.nbio.structure.AminoAcid;
import org.biojava.nbio.structure.Atom;
import org.jmol.api.JmolViewer;

/** //TODO remember to declare files failed to parse in results log file and retrieving them when parsing it 
 * @author Amr
 *
 */
public class ResultManager {
	public static final int FREQUENCY_DISTANCE = 25;
	private static final String SPHERE_KEYWORD = "sphere";
//	private static final String ELLIPSOID_KEYWORD = "ellipsoid";

	public static final String GENERAL_SELECTION_SCRIPT = "set logLevel 0; select *;wireframe on;color cpk;\n"+
			"set showHydrogens false; set selectHydrogen off;"+
			"SELECT (PHE OR TYR OR TRP OR LYS OR ARG OR GLU OR ASP) AND SIDECHAIN;"+
			"spacefill 23%AUTO;wireframe 0.15;color cpk;\n";

	private static final String INTERACTION_SEPARATOR = " \t-> ";
	public static final String CACHE_RESULT_FOLDER = "temp/cashe";
	private static SettingsManager settingsManager = SettingsManager.getSettingsManager();

	/**This function creates the list of connections to be persisted (cached), already 
	 * saves it along with other specific select/ellipsoid commands. 
	 * and returns the list of connections string to be used else where.
	 * @param token
	 * @param specificCollectionScriptString
	 * @param foundInteractions
	 * @return the listofConnectionsAsString if succeeded, null otherwise.
	 */
	public static String persistInteractionsHashTable(String token, String specificCollectionScriptString, Hashtable<GroupOfInterest,HashSet<GroupOfInterest>> foundInteractions) {
		String listofConnectionsAsString = createListofConnectionsAsString(foundInteractions);
		try {
			File filecacheFile = createCacheFileNameForToken(token);
			FileWriter fileWriter = new FileWriter(filecacheFile);
			fileWriter.append(specificCollectionScriptString);
			fileWriter.append(listofConnectionsAsString);
			fileWriter.close();
			return listofConnectionsAsString;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static Hashtable<String, HashSet<String>> retrieveSetOfInteractions(Scanner scanner) {

		Hashtable<String, HashSet<String>> interactions= new Hashtable<String, HashSet<String>>();
		while (scanner.hasNextLine()) {
			final String nextLine = scanner.nextLine();
			if (nextLine.length() > 0) {
				if (nextLine.startsWith("[")) {//this condition is unnecessary
					//interactions
					decodeInteractioString(nextLine, interactions);
				} else {
					throw new RuntimeException("unknown or unexpected format: "+nextLine);
				}
			} else {
				return interactions;
			}
		}
		return null;
	}
	
	private static File createCacheFileNameForToken(String token) {
		File cacheFolder = new File(settingsManager.getWorkingFolder(), CACHE_RESULT_FOLDER);
		cacheFolder.mkdirs();
		return new File(cacheFolder, token);
	}

	/**
	 * @param token
	 * @param specificCollectionScriptString 
	 * @param foundInteractions
	 * @return
	 */
	static String generateJMolScriptString(String token, String specificCollectionScriptString, Hashtable<GroupOfInterest,HashSet<GroupOfInterest>> foundInteractions) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(GENERAL_SELECTION_SCRIPT);
		//add spheres
		String[] lines =specificCollectionScriptString.split(System.getProperty("line.separator"));
		for (String line : lines) {
			if (line.startsWith(SPHERE_KEYWORD) && settingsManager.isDomainEnabled()) {
				buffer.append(decodeDrawSphereCommand(line));
			}
		}
		//			buffer.append("restrict bonds not selected;");

		buffer.append("SELECT (");
		
		Set<GroupOfInterest> lysines = foundInteractions.keySet();
		for (GroupOfInterest lysine : lysines) {
			addResidueToSelectionStringBuffer(buffer,lysine);
			Set<GroupOfInterest> set = foundInteractions.get(lysine);
			for (GroupOfInterest interactionTarget : set) {
				addResidueToSelectionStringBuffer(buffer,interactionTarget);
			}
		}
		buffer.append("FALSE) ;");//wanted Atoms
		buffer.append("spacefill 65%;color cpk;");//space fill
		//			buffer.append("selectionHalos ON;");
		return buffer.toString();
	}


	/**please notice that if {@link #createInteractionString(AminoAcid, AminoAcid)} changed, this method <b>MUST be updated</b>
	 * @param token
	 * @return
	 */
	public static String retrieveJMolScriptString(String token) {
		StringBuffer buffer = new StringBuffer();
		//		buffer.append("select *;wireframe on;color cpk;\n");
		//		buffer.append("SELECT (PHE OR TYR OR TRP OR LYS OR ARG) AND SIDECHAIN;");
		//		buffer.append("spacefill 23%AUTO;wireframe 0.15;color cpk;\n");
		buffer.append(GENERAL_SELECTION_SCRIPT);

		try {
			Set<String> collectedAminoAcids= new HashSet<String>();
			Scanner scanner = new Scanner(createCacheFileNameForToken(token));
			int separatorIndex;
			while (scanner.hasNext()) {
				String line = scanner.nextLine().trim();
				//find residues without duplicity
				if (line.length()>0) {
					if (line.startsWith("[")) {
						separatorIndex = line.indexOf(INTERACTION_SEPARATOR);
						String leftSide=line.substring(0, separatorIndex);
						String rightSide=line.substring(separatorIndex+INTERACTION_SEPARATOR.length()); //,line.indexOf('('));
						collectedAminoAcids.add(leftSide);
						collectedAminoAcids.add(rightSide);
					} else if (line.startsWith(SPHERE_KEYWORD) && settingsManager.isDomainEnabled()) {
						buffer.append(decodeDrawSphereCommand(line));
					}
				}
			}

			buffer.append("SELECT (");
			if (collectedAminoAcids.size()>0) {
				for (String aaRepresentation: collectedAminoAcids) {
					int indexOfClosingSquareBracket = aaRepresentation.indexOf(']');
					int indexOfColon = aaRepresentation.indexOf(':');
					String residueNumber = aaRepresentation.substring(indexOfClosingSquareBracket+1,indexOfColon);
					int indexOfOpeningPracket = aaRepresentation.indexOf('{');
					if (indexOfOpeningPracket> -1) {//if there are specific atoms
						String atomList = aaRepresentation.substring(indexOfOpeningPracket+1,aaRepresentation.indexOf('}'));
						String[] splits = atomList.split(",");
						if (splits != null && splits.length>0) {
							buffer.append('(');
							for (int i = 0; i < splits.length; i++) {
								buffer.append(" atomno = ").append(splits[i].substring(splits[i].indexOf('|')+1));
								if (i < splits.length - 1) {
									buffer.append(" OR ");
								}
							}
							buffer.append(") OR ");
						}
					}else{
						String chainId = aaRepresentation.substring(indexOfColon+1);
						addResidueToSelectionStringBuffer(buffer, Integer.parseInt(residueNumber),chainId);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		buffer.append("FALSE);");//wanted Atoms
		buffer.append("spacefill 75%;color cpk;");//space fill
		return buffer.toString();
	}


	private static void addResidueToSelectionStringBuffer(StringBuffer buffer, GroupOfInterest groupOfInterest) {
		if (groupOfInterest instanceof HetGroupOfInterest) {
//			addAtomsToSelectionStringBuffer(buffer, groupOfInterest.getPositiveKeyAtoms(),groupOfInterest.getNegativeKeyAtoms());
			addAtomsToSelectionStringBuffer(buffer, groupOfInterest.getKeyAtoms(), null);
		} else {
			addResidueToSelectionStringBuffer(buffer, groupOfInterest.getResidueNumber().getSeqNum(), groupOfInterest.getChain().getName());
		}
	}
	private static void addAtomsToSelectionStringBuffer(StringBuffer buffer, Atom[] positiveKeyAtoms, Atom[] negativeKeyAtoms) {
		if (positiveKeyAtoms != null && positiveKeyAtoms.length>0) {
			buffer.append('(');
			for (int i = 0; i < positiveKeyAtoms.length; i++) {
				buffer.append(" atomno = ").append(
						positiveKeyAtoms[i].getPDBserial());
				if (i < positiveKeyAtoms.length - 1) {
					buffer.append(" OR ");
				}
			}
			buffer.append(") OR ");
		}
		if (negativeKeyAtoms != null && negativeKeyAtoms.length>0) {
			buffer.append('(');
			for (int i = 0; i < negativeKeyAtoms.length; i++) {
				buffer.append(" atomno = ").append(
						negativeKeyAtoms[i].getPDBserial());
				if (i < negativeKeyAtoms.length - 1) {
					buffer.append(" OR ");
				}
			}
			buffer.append(") OR ");
		}
	}
	private static void addResidueToSelectionStringBuffer(StringBuffer buffer,int residurNumber,String chainId) {
		buffer.append("(resno = ").append(residurNumber).append(" AND chain = ").append(chainId);
//		buffer.append(" AND SIDECHAIN ");
		buffer.append(") OR ");
	}

	public static File prepareFilesList(boolean temp) {
		System.out.println("Preparing Files List");
		try {
			File pdbFolder= new File(settingsManager.getPdbFilePath(), "data/structures/divided/pdb");
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
				public boolean accept(File dir, String name) {
					return name.matches("pdb.+"+dir.getName()+"..+");
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
							out.print(potentialFileNames[j].substring(3, 7));
							if (j < potentialFileNames.length-1) {
								out.print(';');
							}else {
								out.println();
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
	public static String createListofConnectionsAsString(Hashtable<GroupOfInterest, HashSet<GroupOfInterest>> foundInteractions) {
		StringBuilder listOfConnections = new StringBuilder();
		for (GroupOfInterest aromaticAminoAcid : foundInteractions.keySet()) {
			HashSet<GroupOfInterest> interactionTargets=foundInteractions.get(aromaticAminoAcid);
			for (GroupOfInterest interactionTarget : interactionTargets) {
				String connectionString = createInteractionString(aromaticAminoAcid, interactionTarget);
				listOfConnections.append(connectionString);
				listOfConnections.append(System.getProperty("line.separator"));
			}
		}
		return listOfConnections.toString();
	}

	/** the first is usually the pi system.
	 * please notice that if you updated this method, {@link #retrieveJMolScriptString(String)} must me updated too.
	 * @param aromaticAminoAcid
	 * @param interactionTarget
	 * @return
	 */
	public static String createInteractionString(GroupOfInterest aromaticAminoAcid, GroupOfInterest interactionTarget) {
		StringBuilder interactionOfInterestFound = new StringBuilder(getRepresentativeString(aromaticAminoAcid)).append(INTERACTION_SEPARATOR);
//		GroupOfInterest groupOfInterest= interactionTarget;
		interactionOfInterestFound.append(getRepresentativeString(interactionTarget));
		
//		interactionOfInterestFound.append('(');
////		Atom[] positiveKeyAtoms = groupOfInterest.getPositiveKeyAtoms();
////		if (positiveKeyAtoms != null) {
////			interactionOfInterestFound.append('+');
////			Float distanceSign = interactionTarget.side;
////			interactionOfInterestFound.append(distanceSign==1.0?'R':distanceSign==-1.0?'L':'S');
////		}
////		Atom[] negativeKeyAtoms = groupOfInterest.getNegativeKeyAtoms();
////		if (negativeKeyAtoms != null) {
////			interactionOfInterestFound.append('-');
////			Float distanceSign = interactionTarget.side;
////			interactionOfInterestFound.append(distanceSign==1.0?'R':distanceSign==-1.0?'L':'S');
////		}
//		
//		interactionOfInterestFound.append(interactionTarget.charge);
//		interactionOfInterestFound.append(interactionTarget.side);
//
//		//start of adding r & theta
//		interactionOfInterestFound.append(';').append('\t')
//		.append(interactionTarget.r.x).append(',')
//		.append(interactionTarget.r.y).append(',')
//		.append(interactionTarget.r.z).append(',')
//		.append(interactionTarget.theta);
//		//end    of adding r & theta
//		interactionOfInterestFound.append(')');

		String string = interactionOfInterestFound.toString();
		return string;
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



	public static boolean exportFileToScript(String pdbId, String relativeExportedFilePath, String additionalScript) {
		File exportFolder = new File(relativeExportedFilePath);
		if (exportFolder.exists() || exportFolder.mkdirs()) {
			try {
				FileWriter fw = new FileWriter (new File(exportFolder, pdbId+".spt"),false);
				PrintWriter file = new PrintWriter(fw);

				String command = "load \"" + settingsManager.getPdbFilePath()+File.separatorChar+ pdbId.substring(1, 3)+ File.separatorChar+ "pdb"+pdbId+".ent.gz\";";
				file.println(command);
				file.println(GENERAL_SELECTION_SCRIPT);
				String[] strings = additionalScript.split(System.getProperty("line.separator"));
				for (String string : strings) {
					if (string.startsWith(SPHERE_KEYWORD) /*&& settingsManager.isDomainEnabled()*/) {
						file.println(decodeDrawSphereCommand(string));
					}
				}
				file.close();
				return true;
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
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
	public static String encodeDrawSphereCommand(String name, Point3d center, double radius) {
		StringBuilder builder = new StringBuilder()
		.append(SPHERE_KEYWORD).append('\t').append(name).append('\t')
		.append(String.format("%.2f", center.x)).append('\t')
		.append(String.format("%.2f", center.y)).append('\t')
		.append(String.format("%.2f", center.z)).append('\t')
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

	public static String generateDrawCylinderCommand(double diameter, Point3d point1, Point3d point2) {
		double diameter1 =  diameter;
		String firstpoint = " {" + point1.x + " " + point1.y + " " + point1.z + "} ";
		String secondpoint = "{" + point2.x + " " + point2.y + " " + point2.z + "} ";
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
	public static String generateDrawLineCommand(String id, Point3d point1, Point3d point2, Color color,boolean translucent, double width) {
		double width1 =  width;
		String color1 = "["+color.getRed()+','+color.getGreen()+','+color.getBlue()+']';
		String firstpoint = " {" + point1.x + " " + point1.y + " " + point1.z + "} ";
		String secondpoint = " {" + point2.x + " " + point2.y + " " + point2.z + "} ";
		String commandLine = "draw id"+id+ " line width " + width1 + " color "+(translucent? "translucent ":"") + color1 +  firstpoint + secondpoint+";\r\n";
		return commandLine;

	}



	public static final String START_OF_STRUCTURE_PREFIX = "in structure#";
	public static final String FILED_TO_PARSE_AMINOACID = "##Filed to Parse ";

	public static String getRepresentativeString(GroupOfInterest groupOfInterest) {
		StringBuilder representativeString=
		new StringBuilder().append("[").append(groupOfInterest.getPDBName()).append("]").append(groupOfInterest.getResidueNumber()).append(":").append(groupOfInterest.getChain().getName());
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

}
