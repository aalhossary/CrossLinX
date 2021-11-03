package amralhossary.bonds;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.align.gui.jmol.JmolPanel;
import org.biojava.nbio.structure.align.util.UserConfiguration;
import org.biojava.nbio.structure.io.CifFileReader;
import org.biojava.nbio.structure.io.LocalPDBDirectory;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;

import amralhossary.bonds.SettingsManager.SettingListener;

import org.biojava.nbio.structure.io.PDBFileReader;
import javax.swing.JSplitPane;

public class ParsingUI implements ProteinParsingGUI, SettingListener{

	private static final int MAX_TEXT_CONTENTS = 80000;
	private JFrame jFrame = null;  //  @jve:decl-index=0:visual-constraint="10,10"
	private JPanel jContentPane = null;
	private JMenuBar jJMenuBar = null;
	private JMenu fileMenu = null;
	private JMenu editMenu = null;
	private JMenu helpMenu = null;
	private JMenuItem exitMenuItem = null;
	private JMenuItem aboutMenuItem = null;
	private JDialog aboutDialog = null;
	private JPanel aboutContentPane = null;
	private JLabel aboutVersionLabel = null;
//	protected File selectedFolder;
	private JScrollPane visualsScrollPane = null;
	private JPanel leftPanel = null;
	private JLabel label = null;
	private JRadioButton allFilesRadioButton = null;
	private JRadioButton theseFilesRadioButton = null;
	private JRadioButton filesListRadioButton = null;
	private JPanel jPanel1 = null;
	private JTextArea fileListTextArea = null;
	private JTextField listTextField = null;
	private JPanel jPanel2 = null;
	private JButton browseButton = null;
	private JScrollPane jScrollPane1 = null;
	private JPanel rightPanel = null;
	private JPanel jPanel3 = null;
	private JButton startButton = null;
	private JButton stopButton = null;
	private JmolPanel jmolPanel = null;
	private JScrollPane jScrollPane = null;
	private JList<String> foundInteractionsList = null;
	private JTextArea outputTextArea = null;
	private ButtonGroup buttonGroup=null;
	private SettingsManager settingsManager;
	ProteinParser parser;
	private RedirectingStream out;
	private RedirectingStream err;

	private JMenuItem settingsMenuItem;
	private JButton fineTune;
	private JMenu importMenu;
	private JMenuItem addResultsMenuItem;
	private JMenuItem importNewCleanResultsMenuItem;
	private JSplitPane splitPane;
	
	public ParsingUI() {
		settingsManager= SettingsManager.getSettingsManager();  //  @jve:decl-index=0:
		settingsManager.registerListener(this);
		parser= new ProteinParser(this);
	}

	/**
	 * This method initializes jFrame
	 * 
	 * @return javax.swing.JFrame
	 */
	JFrame getJFrame() {
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame.setJMenuBar(getJJMenuBar());
			jFrame.setSize(847, 514);
			jFrame.setContentPane(getJContentPane());
			jFrame.setTitle("Cross Links Explorer");
			redirectSystemStreams();
			jFrame.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent e) {
					settingsManager.saveSettings(true);
				}
			});
		}
		return jFrame;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			BorderLayout bl_jContentPane = new BorderLayout();
			bl_jContentPane.setHgap(5);
			jContentPane.setLayout(bl_jContentPane);
			jContentPane.add(getLeftPanel(), BorderLayout.WEST);
//			jContentPane.add(getJPanel4(), BorderLayout.EAST);
//			jContentPane.add(getJmolPanel(), BorderLayout.NORTH);
			jContentPane.add(getSplitPane(), BorderLayout.CENTER);
			
		}
		return jContentPane;
	}

	/**
	 * This method initializes jJMenuBar	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */
	private JMenuBar getJJMenuBar() {
		if (jJMenuBar == null) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.add(getFileMenu());
			jJMenuBar.add(getEditMenu());
			jJMenuBar.add(getHelpMenu());
		}
		return jJMenuBar;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu();
			fileMenu.setText("File");
			fileMenu.add(getImportMenu());
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getEditMenu() {
		if (editMenu == null) {
			editMenu = new JMenu();
			editMenu.setText("Edit");
			editMenu.add(getSettingsMenuItem());
		}
		return editMenu;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = new JMenu();
			helpMenu.setText("Help");
			helpMenu.add(getAboutMenuItem());
		}
		return helpMenu;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getExitMenuItem() {
		if (exitMenuItem == null) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText("Exit");
			exitMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getAboutMenuItem() {
		if (aboutMenuItem == null) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText("About");
			aboutMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JDialog aboutDialog = getAboutDialog();
					aboutDialog.pack();
					Point loc = getJFrame().getLocation();
					loc.translate(20, 20);
					aboutDialog.setLocation(loc);
					aboutDialog.setVisible(true);
				}
			});
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes aboutDialog	
	 * 	
	 * @return javax.swing.JDialog
	 */
	private JDialog getAboutDialog() {
		if (aboutDialog == null) {
			aboutDialog = new JDialog(getJFrame(), true);
			aboutDialog.setTitle("About");
			aboutDialog.setContentPane(getAboutContentPane());
		}
		return aboutDialog;
	}

	/**
	 * This method initializes aboutContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getAboutContentPane() {
		if (aboutContentPane == null) {
			aboutContentPane = new JPanel();
			aboutContentPane.setLayout(new BorderLayout());
			aboutContentPane.add(getAboutVersionLabel(), BorderLayout.CENTER);
		}
		return aboutContentPane;
	}

	/**
	 * This method initializes aboutVersionLabel	
	 * 	
	 * @return javax.swing.JLabel	
	 */
	private JLabel getAboutVersionLabel() {
		if (aboutVersionLabel == null) {
			aboutVersionLabel = new JLabel();
			aboutVersionLabel.setText("<html><center>Protein Crosslinkcs Explorer<br>Version 0.1.3 <b>(BETA)</B></center></html>");
			aboutVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		return aboutVersionLabel;
	}

	/**
	 * This method initializes visualsScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getVisualsScrollPane() {
		if (visualsScrollPane == null) {
			visualsScrollPane = new JScrollPane();
			visualsScrollPane.setViewportView(getOutputTextArea());
		}
		return visualsScrollPane;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getLeftPanel() {
		if (leftPanel == null) {
			label = new JLabel();
			label.setText("Found");
			leftPanel = new JPanel();
			leftPanel.setLayout(new BoxLayout(getLeftPanel(), BoxLayout.Y_AXIS));
			leftPanel.setPreferredSize(new Dimension(100, -1));
			leftPanel.add(label, null);
			leftPanel.add(getJScrollPane(), null);
		}
		return leftPanel;
	}

	/**
	 * This method initializes allFilesRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getAllFilesRadioButton() {
		if (allFilesRadioButton == null) {
			allFilesRadioButton = new JRadioButton();
			allFilesRadioButton.setText("All Files in local folder");
		}
		return allFilesRadioButton;
	}

	/**
	 * This method initializes theseFilesRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getTheseFilesRadioButton() {
		if (theseFilesRadioButton == null) {
			theseFilesRadioButton = new JRadioButton();
			theseFilesRadioButton.setText("These Files");
			theseFilesRadioButton.setSelected(true);
			theseFilesRadioButton.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					getJScrollPane1().setVisible(e.getStateChange()==ItemEvent.SELECTED);
				}
			});
		}
		return theseFilesRadioButton;
	}

	/**
	 * This method initializes filesListRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getFilesListRadioButton() {
		if (filesListRadioButton == null) {
			filesListRadioButton = new JRadioButton();
			filesListRadioButton.setText("List");
//			filesListRadioButton.setEnabled(false);
			filesListRadioButton.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					getJPanel2().setVisible(e.getStateChange()==ItemEvent.SELECTED);
				}
			});
		}
		return filesListRadioButton;
	}

	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BoxLayout(getJPanel1(), BoxLayout.Y_AXIS));
			jPanel1.setBorder(BorderFactory.createTitledBorder(null, "Select Files", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
			jPanel1.setPreferredSize(new Dimension(350, 150));
			jPanel1.add(getAllFilesRadioButton(), null);
			jPanel1.add(getFilesListRadioButton(), null);
			jPanel1.add(getJPanel2(), null);
			jPanel1.add(getTheseFilesRadioButton(), null);
			jPanel1.add(getJScrollPane1(), null);
			getButtonGroup();
		}
		return jPanel1;
	}

	private ButtonGroup getButtonGroup() {
		if (buttonGroup == null) {
			buttonGroup = new ButtonGroup();
			buttonGroup.add(allFilesRadioButton);
			buttonGroup.add(filesListRadioButton);
			buttonGroup.add(theseFilesRadioButton);
		}
		return buttonGroup;
	}

	/**
	 * This method initializes fileListTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getFileListTextArea() {
		if (fileListTextArea == null) {
			fileListTextArea = new JTextArea();
			fileListTextArea.setText("5nf0;3HTL;6VZX;5VBL;5EIN;6ZX4;1FMA;3ALB;7CAP;6o83;6ELW;3MLI;5JQF;6ZWJ;6ZWH;7B0L;1M3Q;3CLM;6ZWF;7BBX;7BBW;1v54;"
					+ "1v55;7coh;2qpe;2yev;6e87;2b39;3p06;2PNL;4izk;2ATK;1AY1;5b0w;5O81;6jky;3OPU");
			fileListTextArea.setColumns(50);
		}
		return fileListTextArea;
	}

	/**
	 * This method initializes listTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getListTextField() {
		if (listTextField == null) {
			listTextField = new JTextField();
			listTextField.setColumns(30);
			updateListTextFieldContent();
		}
		return listTextField;
	}

	private void updateListTextFieldContent() {
		String workingFolder = settingsManager.getWorkingFolder();
		if (workingFolder== null) {
			workingFolder="";
		}
		listTextField.setText(workingFolder+File.separatorChar+"list.txt");
	}

	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jPanel2.setLayout(new BoxLayout(getJPanel2(), BoxLayout.X_AXIS));
			jPanel2.setVisible(false);
			jPanel2.add(getListTextField(), null);
			jPanel2.add(getBrowseButton(), null);
		}
		return jPanel2;
	}

	/**
	 * This method initializes browseButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBrowseButton() {
		if (browseButton == null) {
			browseButton = new JButton();
			browseButton.setText("Browse");
			browseButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser(getListTextField().getText());
					fileChooser.setDialogTitle("Select file containing PDB list");
					fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					int option = fileChooser.showOpenDialog(jFrame);
					if (option== JFileChooser.APPROVE_OPTION) {
						String path = fileChooser.getSelectedFile().getAbsolutePath();
						System.out.println("List:");
						System.out.println(path);
						getListTextField().setText(path);
					}

				}
			});
		}
		return browseButton;
	}

	/**
	 * This method initializes jScrollPane1	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setViewportView(getFileListTextArea());
		}
		return jScrollPane1;
	}

	/**
	 * This method initializes rightPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel4() {
		if (rightPanel == null) {
			rightPanel = new JPanel();
			BorderLayout bl_rightPanel = new BorderLayout();
			rightPanel.setLayout(bl_rightPanel);
			rightPanel.setPreferredSize(new Dimension(250, -1));
			rightPanel.add(getJPanel1(), BorderLayout.NORTH);
			rightPanel.add(getVisualsScrollPane(), BorderLayout.CENTER);
			rightPanel.add(getJPanel3(), BorderLayout.SOUTH);
		}
		return rightPanel;
	}

	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			jPanel3 = new JPanel();
			jPanel3.setLayout(new FlowLayout());
			jPanel3.add(getStartButton(), null);
			jPanel3.add(getStopButton(), null);
			jPanel3.add(getFineTune());
		}
		return jPanel3;
	}

	/**
	 * This method initializes startButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getStartButton() {
		if (startButton == null) {
			startButton = new JButton();
			startButton.setText("Start");
			startButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					new Thread() {
						public void run() {
							ProteinParser.moreWork = true;
							startButton.setEnabled(false);
							getStopButton().setEnabled(true);
							((DefaultListModel<String>)getFoundInteractionsList().getModel()).clear();
							Scanner scanner = null;
							ButtonModel selectionModel = getButtonGroup().getSelection();
							try {
								if (selectionModel == getAllFilesRadioButton().getModel()) {
									scanner = new Scanner(new FileReader(ResultManager.prepareFilesList(true)));
								} else if (selectionModel == getFilesListRadioButton().getModel()) {
									scanner = new Scanner(new FileReader(getListTextField().getText()));
								} else if (selectionModel == getTheseFilesRadioButton().getModel()) {
									scanner = new Scanner(getFileListTextArea().getText());
								}
							} catch (FileNotFoundException e1) {
								JOptionPane.showMessageDialog(getJFrame(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//								e1.printStackTrace();
							}
							if (scanner != null) {
								parser.startParsing(scanner);
//								parser.parseStructureNamesList(scanner);
							}else {
								parser.getPrintableStatistics();
							}
							startButton.setEnabled(true);
							getStopButton().setEnabled(false);
						}
					}.start();
				}
			});
		}
		return startButton;
	}
	
	public void showResults(Object results){
		//TODO implement
	}
	
	
	@Override
	public void structureLoaded(Structure structure) {
		if (settingsManager.isShowWhileProcessing()) {
			out.setEnabled(false);
			getJmolPanel().setStructure(structure);
			out.setEnabled(true);
		}
	}
	
	@Override
	public void interactionsFoundInStructure(String token, String specificCollectionScriptString, Hashtable<GroupOfInterest, HashSet<GroupOfInterest>> foundInteractions) {
//		ParsingUI.this.foundInteractionsInFiles.put(token,foundInteractions);
		((DefaultListModel<String>)getFoundInteractionsList().getModel()).addElement(token);
		if (settingsManager.isShowWhileProcessing()) {
			String generalViewingScript = ResultManager.generateJMolScriptString(token, specificCollectionScriptString, foundInteractions);
			out.setEnabled(false);
			getJmolPanel().executeCmd(generalViewingScript);
			out.setEnabled(true);
		}
	}
	
	@Override
	public void executeScript(String script) {
		if (settingsManager.isShowWhileProcessing()) {
			getJmolPanel().executeCmd(script);
		}
	}
	
	/**
	 * This method initializes stopButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getStopButton() {
		if (stopButton == null) {
			stopButton = new JButton();
			stopButton.setText("Stop");
			stopButton.setEnabled(false);
			stopButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					System.out.println("finishing current file...");
					ProteinParser.moreWork = false;
				}
			});
		}
		return stopButton;
	}

	/**
	 * This method initializes jmolPanel	
	 * 	
	 * @return org.biojava.bio.structure.align.gui.jmol.JmolPanel	
	 */
	private JmolPanel getJmolPanel() {
		if (jmolPanel == null) {
			jmolPanel = new JmolPanel();
		}
		return jmolPanel;
	}

	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getFoundInteractionsList());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes foundInteractionsList	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList<String> getFoundInteractionsList() {
		if (foundInteractionsList == null) {
			foundInteractionsList = new JList<String>(new DefaultListModel<String>());
			foundInteractionsList.setFixedCellHeight(foundInteractionsList.getFont().getSize()+1);
			foundInteractionsList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					try {
						LocalPDBDirectory fileReader = null;
						if(UserConfiguration.PDB_FORMAT.equals(settingsManager.getFileFormat())) {
							fileReader = new PDBFileReader(); 
						} else if(UserConfiguration.MMCIF_FORMAT.equals(settingsManager.getFileFormat())) {
							fileReader = new CifFileReader(settingsManager.getPdbFilePath());
						}
//						pdbFileReader.setPdbDirectorySplit(settingsManager.getUserConfiguration().isSplit());
						fileReader.setPath(settingsManager.getPdbFilePath());
						fileReader.setFetchBehavior(FetchBehavior.LOCAL_ONLY);
						JmolPanel jmolPanel = getJmolPanel();
						String token = (String) foundInteractionsList.getSelectedValue();
						if (token != null) {
							out.setEnabled(false);
							jmolPanel.setStructure((fileReader.getStructureById(token)));
							out.setEnabled(true);
							String buffer = ResultManager.retrieveJMolScriptString(token);
//							System.out.println("String To Evaluate is: "+buffer);
							jmolPanel.executeCmd(buffer);
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
		}
		return foundInteractionsList;
	}

	/**
	 * This method initializes outputTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getOutputTextArea() {
		if (outputTextArea == null) {
			outputTextArea = new JTextArea() {
				private static final long serialVersionUID = 1L;

				@Override
				public void append(String str) {
					super.append(str);
					int newLength = getDocument().getLength();
					if (newLength >MAX_TEXT_CONTENTS) {
						try {
							getDocument().remove(0, newLength-MAX_TEXT_CONTENTS);
						} catch (BadLocationException e) {
							e.printStackTrace();
						}
					}
				}
			};
		}
		return outputTextArea;
	}


	/**
	 * Launches this application
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ParsingUI application = new ParsingUI();
				application.getJFrame().setVisible(true);
			}
		});
	}
	
	
	
	private JMenuItem getSettingsMenuItem() {
		if (settingsMenuItem == null) {
			settingsMenuItem = new JMenuItem("Settings");
			settingsMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new PreferencesDialogue().setVisible(true);
				}
			});
			settingsMenuItem.setName("settingsMenuItem");
		}
		return settingsMenuItem;
	}
	
	
	
	
	
	
	
	private void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				getOutputTextArea().append(text);
			}
		});
	}
	
	
	
	private void redirectSystemStreams() {
		System.out.println("redirecting streams");
		out = new RedirectingStream();
		err = new RedirectingStream();
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(err, true));
	}

	
	class RedirectingStream extends OutputStream {
		
		boolean enabled = true;
		
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
		
		@Override
		public void write(int b) throws IOException {
			parser.log.write(b);
			if(enabled) {
				updateTextArea(String.valueOf((char) b));
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			parser.log.write(b, off, len);
			if(enabled) {
				updateTextArea(new String(b, off, len));
			}
		}

		@Override
		public void write(byte[] b) throws IOException {
			parser.log.write(b);
			if(enabled){
				write(b, 0, b.length);
			}
		}
	}


	@Override
	public void refreshSettings() {
		updateListTextFieldContent();
	}
	
	
	
	private JButton getFineTune() {
		if (fineTune == null) {
			fineTune = new JButton("Fine Tune");
			fineTune.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					new FineTuningDialogue().setVisible(true);
				}
			});
			fineTune.setName("fineTune");
		}
		return fineTune;
	}
	private JMenu getImportMenu() {
		if (importMenu == null) {
			importMenu = new JMenu("Import Results");
			importMenu.setName("importMenu");
			importMenu.add(getImportNewCleanResultsMenuItem());
			importMenu.add(getAddResultsMenuItem());
		}
		return importMenu;
	}
	private JMenuItem getAddResultsMenuItem() {
		if (addResultsMenuItem == null) {
			addResultsMenuItem = new JMenuItem("Add More Results");
			addResultsMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					importResults(false);
				}
			});
			addResultsMenuItem.setName("addResultsMenuItem");
		}
		return addResultsMenuItem;
	}
	private JMenuItem getImportNewCleanResultsMenuItem() {
		if (importNewCleanResultsMenuItem == null) {
			importNewCleanResultsMenuItem = new JMenuItem("New Clean Results");
			importNewCleanResultsMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					importResults(true);
				}
			});
			importNewCleanResultsMenuItem.setName("importNewCleanResultsMenuItem");
		}
		return importNewCleanResultsMenuItem;
	}
	private void importResults(final boolean clean) {
		final JFileChooser fileChooser = new JFileChooser(getListTextField().getText());
		fileChooser.setDialogTitle("Select file positive results");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int option = fileChooser.showOpenDialog(jFrame);
		if (option== JFileChooser.APPROVE_OPTION) {
			new Thread() {
				public void run() {
					String path = fileChooser.getSelectedFile().getAbsolutePath();
					System.out.println("File :");
					System.out.println(path);
					
					Scanner scanner = null;
					try {
						scanner = new Scanner(new FileReader(path));
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					if (scanner != null) {
						if (clean) {
							((DefaultListModel<String>) getFoundInteractionsList().getModel()).clear();
							//TODO clear the Jmolpanel too.
							parser.initialize();
						}
						parser.parseResults(scanner);
					}
					
				}
			}.start();
		}else {
			JOptionPane.showMessageDialog(jFrame,"Couldn't open File","ERROR",JOptionPane.ERROR_MESSAGE);
		}
	}
	private JSplitPane getSplitPane() {
		if (splitPane == null) {
			splitPane = new JSplitPane();
			splitPane.setOneTouchExpandable(true);
			splitPane.setResizeWeight(1.0);
			splitPane.setLeftComponent(getJmolPanel());
			splitPane.setRightComponent(getJPanel4());
		}
		return splitPane;
	}
	}
