package amralhossary.bonds;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
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
import javax.swing.JSplitPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import org.biojava.nbio.structure.PdbId;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.align.gui.jmol.JmolPanel;

import amralhossary.bonds.SettingsManager.SettingListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ParsingUI implements ProteinParsingGUI, SettingListener{
	
	public static class BondListItem{
		private String fullString = null;
		private final int modelNumber;
		private final String identity;

		public BondListItem(String fullString) {
			this.fullString = fullString;
			this.modelNumber = ResultManager.modelOf(fullString);
			//Coordinate-free and model-free, so the same interaction has the same identity
			//in every model of an ensemble - which is exactly what NMR models do NOT share,
			//their coordinates. This is what lets a selection stay on the same interaction
			//while the user steps through models.
			this.identity = ResultManager.removeAtomCoords(ResultManager.stripModelTag(fullString));
		}

		@Override
		public String toString() {
			//The list only ever shows one model at a time, so repeating the model on every
			//row would say nothing. For an untagged line this is character for character
			//what it has always rendered.
			return identity;
		}

		/** the 1-based model this interaction was found in */
		public int getModelNumber() {
			return modelNumber;
		}

		/** identifies the same interaction across models; see the constructor */
		public String getIdentity() {
			return identity;
		}

		public String getFullString() {
			return fullString;
		}
	}

	private static final int[] NO_SELECTION = new int[] {};
	private static final BondListItem[] NO_BOND_LIST_ITEMS = new BondListItem[0];
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
	private JPanel foundStructurePanel = null;
	private JPanel foundLinksPanel = null;
	private JLabel label = null;
	private JLabel label2 = null;
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
	private JPanel modelSelectorPanel = null;
	private JPanel modelSpinnerRow = null;
	private JSlider modelSlider = null;
	private JSpinner modelSpinner = null;
	private JLabel modelCountLabel = null;
	/**
	 * Set while the selector is being pointed at a new structure. Without it, giving the
	 * slider a new range would look exactly like the user picking a model.
	 */
	private boolean adjustingModelSelector = false;
	/** latest model asked for; see {@link #applyViewerModel(int)} */
	private final AtomicInteger pendingFrame = new AtomicInteger(1);
	private final AtomicBoolean framePending = new AtomicBoolean(false);
	/**
	 * Every call into the Jmol viewer is made from this one thread.
	 * <p>
	 * It has to be a single thread, because the viewer is not thread-safe: loading a
	 * structure replaces state belonging to the whole viewer, and a script running at
	 * the same time sees it half-built. Being a single thread also keeps the calls in
	 * submission order, so a script always runs against the structure it was generated
	 * for.
	 * <p>
	 * And it has to be a thread other than the EDT. Jmol renders its animations by
	 * asking Swing to repaint, which only happens once the EDT is free; a script run
	 * on the EDT with {@code scriptWait} blocks it for the script's whole duration -
	 * over three seconds for the bond zoom, which contains a {@code delay 1.0} - so
	 * nothing is drawn until the animation is already over.
	 */
	private final ExecutorService jmolThread = Executors.newSingleThreadExecutor(new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Jmol");
			thread.setDaemon(true);
			return thread;
		}
	});
	/** Latest structure waiting to be shown; see {@link #structureLoaded(Structure)}. */
	private final AtomicReference<Structure> pendingStructure = new AtomicReference<Structure>();
	/** Whether a list selection is already queued; see {@link #interactionsFoundInStructure(PdbId)}. */
	private final AtomicBoolean selectionPending = new AtomicBoolean();
	private JScrollPane jScrollPane = null;
	private JScrollPane jScrollPane2 = null;
	private JList<PdbId> foundStructuresWithInteractionsList = null;
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
	private JSplitPane middleAndRightSplitPane;
	private JSplitPane foundSplitPane;
	private JSplitPane leftSplitPane;
	private JList<BondListItem> foundLinksList;
	
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
//			jContentPane.add(getLeftPanel(), BorderLayout.WEST);
//			jContentPane.add(getJPanel4(), BorderLayout.EAST);
//			jContentPane.add(getJmolPanel(), BorderLayout.NORTH);
			jContentPane.add(getLeftSplitPane(), BorderLayout.CENTER);
			
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
			aboutVersionLabel.setText("<html><center><br>Protein Crosslinkcs Explorer<br>Version "
					+ ProteinParser.getVersion()+ " <b>(BETA)</B><br><br></center></html>");
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
	private JPanel getFoundStructurePanel() {
		if (foundStructurePanel == null) {
			label = new JLabel();
			label.setText("Found in");
			foundStructurePanel = new JPanel();
			foundStructurePanel.setLayout(new BoxLayout(foundStructurePanel, BoxLayout.Y_AXIS));
			foundStructurePanel.setPreferredSize(new Dimension(100, -1));
			foundStructurePanel.add(label, null);
			foundStructurePanel.add(getJScrollPane(), null);
		}
		return foundStructurePanel;
	}
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getFoundLinksPanel() {
		if (foundLinksPanel == null) {
			label2 = new JLabel();
			label2.setText("Found links");
			foundLinksPanel = new JPanel();
			foundLinksPanel.setLayout(new BoxLayout(getFoundLinksPanel(), BoxLayout.Y_AXIS));
			//wide enough that the model row is legible without dragging the divider first
			foundLinksPanel.setPreferredSize(new Dimension(150, -1));
			foundLinksPanel.add(label2, null);
			foundLinksPanel.add(getModelSelectorPanel(), null);
			foundLinksPanel.add(getJScrollPane2(), null);
		}
		return foundLinksPanel;
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
			fileListTextArea.setText("1JPU;5NGQ;5nf0;3HTL;6VZX;5VBL;5EIN\n"
					+ "6ZX4;1FMA;3ALB;7CAP;6o83;6ELW;3MLI;5JQF;6ZWJ;6ZWH\n"
					+ "7B0L;1M3Q;3CLM;6ZWF;7BBX;7BBW;1v54\n"
					+ "1v55;7coh;2qpe;2yev;6e87;2b39;3p06;2PNL\n"
					+ "4izk;2ATK;1AY1;5b0w;5O81;6jky;3OPU");
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
			workingFolder=".";
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
					//Read the UI and reset it here, on the EDT, before the worker starts.
					ProteinParser.moreWork = true;
					startButton.setEnabled(false);
					getStopButton().setEnabled(true);
					((PdbIdListModel)getFoundStructuresWithInteractionsList().getModel()).clear();
					getFoundLinksList().setListData(NO_BOND_LIST_ITEMS);
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
//							e1.printStackTrace();
					}
					final Scanner finalScanner = scanner;
					new Thread() {
						public void run() {
							try {
								if (finalScanner != null) {
									parser.startParsing(finalScanner);
//									parser.parseStructureNamesList(finalScanner);
								}else {
									parser.getPrintableStatistics();
									//Then?
								}
							} finally {
								runOnEdt(new Runnable() {
									public void run() {
										startButton.setEnabled(true);
										getStopButton().setEnabled(false);
									}
								});
							}
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
	
	
	//N.B. The three callbacks below are invoked by the parser's worker threads.
	//Swing work goes to the EDT; anything touching the viewer goes to the Jmol thread.

	@Override
	public void structureLoaded(final Structure structure) {
		if (settingsManager.isShowWhileProcessing()) {
			//Parsing runs in parallel on a fork/join pool and can produce
			//structures far faster than Jmol can draw them, so queueing every
			//one would grow the queue without bound. Only the most recent
			//structure is worth showing: park it and let the pending load
			//pick up whatever is latest when it runs.
			if (pendingStructure.getAndSet(structure) == null) {
				final JmolPanel panel = getJmolPanel();
				runOnJmolThread(new Runnable() {
					public void run() {
						Structure latest = pendingStructure.getAndSet(null);
						if (latest != null) {
//							out.setEnabled(false);
							panel.setStructure(latest);
//							out.setEnabled(true);
						}
					}
				});
			}
		}
	}


	@Override
	public void interactionsFoundInStructure(final PdbId pdbId) {
		//Every hit belongs in the list, so every one is appended.
		runOnEdt(new Runnable() {
			public void run() {
				JList<PdbId> foundStructuresWithInteractionsList = getFoundStructuresWithInteractionsList();
				PdbIdListModel model = (PdbIdListModel)foundStructuresWithInteractionsList.getModel();
				model.addElement(pdbId);
			}
		});
		//Selecting an entry reloads it from disk and hands it to Jmol, all on
		//the EDT. With the parsing running in parallel, doing that once per hit
		//would swamp the EDT, so at most one selection is outstanding at a time
		//and it lands on whichever entry is newest when it runs.
		if (settingsManager.isShowWhileProcessing() && selectionPending.compareAndSet(false, true)) {
			runOnEdt(new Runnable() {
				public void run() {
					selectionPending.set(false);
					JList<PdbId> foundStructuresWithInteractionsList = getFoundStructuresWithInteractionsList();
					PdbIdListModel model = (PdbIdListModel)foundStructuresWithInteractionsList.getModel();
					if (model.getSize() > 0) {
						foundStructuresWithInteractionsList.setSelectedIndex(model.getSize() - 1); // this will fire event
					}
				}
			});
		}
	}

	@Override
	public void executeScript(final String script) {
		if (settingsManager.isShowWhileProcessing()) {
			runOnEdt(new Runnable() {
				public void run() {
					executeJmolScript(script);
				}
			});
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
			jScrollPane.setViewportView(getFoundStructuresWithInteractionsList());
		}
		return jScrollPane;
	}
	/**
	 * This method initializes jScrollPane2	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane2() {
		if (jScrollPane2 == null) {
			jScrollPane2 = new JScrollPane();
			jScrollPane2.setViewportView(getFoundLinksList());
		}
		return jScrollPane2;
	}
	
	/**
	 * This method initializes foundStructuresWithInteractionsList	
	 * 	
	 * @return javax.swing.JList
	 */
	private JList<PdbId> getFoundStructuresWithInteractionsList() {
		if (foundStructuresWithInteractionsList == null) {
			foundStructuresWithInteractionsList = new JList<PdbId>(new PdbIdListModel());
			foundStructuresWithInteractionsList.setToolTipText("<HTML><i>Click</i> an item to <B>preview</B>.<BR><i>DoubleClick</i> or <i>Enter</i> to <B>open</B>.</HTML>");
			foundStructuresWithInteractionsList.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
						final int itemIndex = foundStructuresWithInteractionsList.locationToIndex(e.getPoint());
						final PdbId pdbId = foundStructuresWithInteractionsList.getModel().getElementAt(itemIndex);
						openPdbIdInFGJM(pdbId);
					}
				}
			});

			foundStructuresWithInteractionsList.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						final int numOfSelectedItems = foundStructuresWithInteractionsList.getSelectedIndices().length;
						if(numOfSelectedItems < 1)
							return;
						if (numOfSelectedItems == 1) {
							final PdbId pdbId = foundStructuresWithInteractionsList.getSelectedValue();
							openPdbIdInFGJM(pdbId);
						} else {
							int userChoice = JOptionPane.showConfirmDialog(getJFrame(), "Are you sure you want to open these "+numOfSelectedItems + " Items?", "Open structures", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
							if (userChoice == JOptionPane.YES_OPTION) {
								int[] selectedIndices = foundStructuresWithInteractionsList.getSelectedIndices();
								for (int i = 0; i < selectedIndices.length; i++) {
									final PdbId pdbId = foundStructuresWithInteractionsList.getModel().getElementAt(selectedIndices[i]);
									openPdbIdInFGJM(pdbId);
								}
							}
						}
					}
				}
			});
			
			foundStructuresWithInteractionsList.setFixedCellHeight(foundStructuresWithInteractionsList.getFont().getSize()+1);
			foundStructuresWithInteractionsList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					final int numOfSelectedItems = foundStructuresWithInteractionsList.getSelectedIndices().length;
					if (numOfSelectedItems > 1)
						return;
					if (numOfSelectedItems == 0) {
						showBondsOfStructure(NO_BOND_LIST_ITEMS, 1);
						return;
					}
					PdbId pdbId = foundStructuresWithInteractionsList.getSelectedValue();

					//N.B. You can replace the block below with ResultManager.generateFileLoadJMolScript(pdbId)
					Structure structure = ResultManager.getStructureById(pdbId);
					if(structure == null)
						return;
					try {
						//The load and the script that styles it are handed over as one
						//task, so no other Jmol work can slip between them and leave the
						//script running against a different structure. They deliberately
						//do NOT run here on the EDT: Jmol draws by asking Swing to
						//repaint, so occupying the EDT for the length of a script means
						//none of its animation is ever shown.
//						out.setEnabled(false);
						showStructure(structure, ResultManager.generateAfterLoadingJMolScriptString(pdbId));  //TODO review
//						out.setEnabled(true);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					//selecting a structure should populate the interactions list
					//populate foundLinksList
					List<String> bondsList = ResultManager.retreiveBondsList(pdbId);
					if (bondsList == null) {
						//no cache file for this structure - the results were loaded from a
						//file written elsewhere, or the cache was cleaned out under us
						showBondsOfStructure(NO_BOND_LIST_ITEMS, structure.nrModels());
						return;
					}
					BondListItem[] bondListItems = new BondListItem[bondsList.size()];
					for (int i = 0; i < bondListItems.length; i++) {
						bondListItems[i] = new BondListItem(bondsList.get(i));
					}
					showBondsOfStructure(bondListItems, structure.nrModels());
				}


			});
		}
		return foundStructuresWithInteractionsList;
	}
	
	public void openPdbIdInFGJM(final PdbId pdbId) {
		String url = "http://firstglance.jmol.org/fg.htm?mol="+pdbId;
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Two stacked rows - "Model [spinner] of N" over a full-width slider - shown only for a
	 * structure that actually has models to choose between.
	 * <p>
	 * Two rows rather than one because the links panel is narrow: a spinner, its "of N" and
	 * a usable slider do not fit side by side.
	 */
	private JPanel getModelSelectorPanel() {
		if (modelSelectorPanel == null) {
			modelSelectorPanel = new JPanel();
			modelSelectorPanel.setLayout(new BoxLayout(modelSelectorPanel, BoxLayout.Y_AXIS));
			//JLabel aligns left and JPanel centre by default, and mixing the two in one
			//Y_AXIS box visibly staggers the rows
			modelSelectorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			modelSelectorPanel.add(getModelSpinnerRow());
			modelSelectorPanel.add(getModelSlider());
			//without a cap the box layout hands it the height the list wants
			modelSelectorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
					modelSelectorPanel.getPreferredSize().height));
			modelSelectorPanel.setVisible(false);
		}
		return modelSelectorPanel;
	}

	private JPanel getModelSpinnerRow() {
		if (modelSpinnerRow == null) {
			modelSpinnerRow = new JPanel();
			modelSpinnerRow.setLayout(new BoxLayout(modelSpinnerRow, BoxLayout.X_AXIS));
			modelSpinnerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
			modelSpinnerRow.add(new JLabel("Model "));
			modelSpinnerRow.add(getModelSpinner());
			modelSpinnerRow.add(getModelCountLabel());
			modelSpinnerRow.add(Box.createHorizontalGlue());
		}
		return modelSpinnerRow;
	}

	/**
	 * Types an exact model number. It mirrors into the slider and does nothing else - the
	 * slider owns every side effect, so a change made through either control drives the
	 * viewer exactly once, and the write back is stopped by the equality test.
	 */
	private JSpinner getModelSpinner() {
		if (modelSpinner == null) {
			modelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
			//plain digits: a 1000-model ensemble should read 1000, not 1,000
			JSpinner.NumberEditor editor = new JSpinner.NumberEditor(modelSpinner, "0");
			editor.getTextField().setColumns(3);
			modelSpinner.setEditor(editor);
			modelSpinner.setMaximumSize(modelSpinner.getPreferredSize());
			modelSpinner.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int value = ((Number) modelSpinner.getValue()).intValue();
					if (getModelSlider().getValue() != value) {
						getModelSlider().setValue(value);
					}
				}
			});
		}
		return modelSpinner;
	}

	/** Scrubs through the models. Owns the side effects; see {@link #getModelSpinner()}. */
	private JSlider getModelSlider() {
		if (modelSlider == null) {
			modelSlider = new JSlider(1, 1, 1);
			modelSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
			modelSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int value = modelSlider.getValue();
					if (((Number) getModelSpinner().getValue()).intValue() != value) {
						getModelSpinner().setValue(Integer.valueOf(value));
					}
					if (adjustingModelSelector) {
						//being reconfigured for a newly selected structure, which must not
						//drive a viewer that is still loading it
						return;
					}
					modelChanged(value, modelSlider.getValueIsAdjusting());
				}
			});
		}
		return modelSlider;
	}

	private JLabel getModelCountLabel() {
		if (modelCountLabel == null) {
			modelCountLabel = new JLabel(" of 1");
		}
		return modelCountLabel;
	}

	/**
	 * Points the selector at a newly shown structure. Guarded throughout, so growing or
	 * shrinking the range cannot be mistaken for the user choosing a model.
	 */
	private void configureModelSelector(int nrModels) {
		adjustingModelSelector = true;
		try {
			SpinnerNumberModel spinnerModel = (SpinnerNumberModel) getModelSpinner().getModel();
			spinnerModel.setMaximum(Integer.valueOf(nrModels));
			getModelSpinner().setValue(Integer.valueOf(1));
			getModelSlider().setMaximum(nrModels);
			getModelSlider().setValue(1);
			getModelCountLabel().setText(" of " + nrModels);
			//nothing to choose between in a structure with a single model
			getModelSelectorPanel().setVisible(nrModels > 1);
			getFoundLinksPanel().revalidate();
		} finally {
			adjustingModelSelector = false;
		}
	}

	/**
	 * @param stillDragging true while the slider knob is still under the pointer. Refilling
	 *        the list and changing the frame happen either way, because scrubbing is the
	 *        point of a slider; anything slower waits for the release, which JSlider always
	 *        delivers as a final event.
	 */
	private void modelChanged(int modelNumber, boolean stillDragging) {
		currentModelNumber = modelNumber;
		fillBondsListForCurrentModel();
		applyViewerModel(modelNumber);
	}

	/**
	 * Shows one model in the viewer.
	 * <p>
	 * Frame changes are coalesced latest-wins: dragging the slider across 38 models must
	 * not leave 38 scripts queued behind the pointer, and only the model the user stopped
	 * on matters.
	 */
	private void applyViewerModel(final int modelNumber) {
		if (nrModelsOfStructure <= 1) {
			//a single-model structure is displayed exactly as it always was
			return;
		}
		pendingFrame.set(modelNumber);
		if (framePending.compareAndSet(false, true)) {
			runOnJmolThread(new Runnable() {
				public void run() {
					//clear before reading, so a change arriving in between queues a fresh
					//task rather than being dropped
					framePending.set(false);
					getJmolPanel().getViewer().scriptWait("frame " + pendingFrame.get() + ";");
				}
			});
		}
	}

	/** every interaction of the structure on show, across all of its models */
	private BondListItem[] allBondsOfStructure = NO_BOND_LIST_ITEMS;
	/** how many models that structure has; 1 for everything that is not an ensemble */
	private int nrModelsOfStructure = 1;
	/** the 1-based model whose interactions the list is currently showing */
	private int currentModelNumber = 1;

	/**
	 * Hands the list a new structure's interactions and starts it at the first model.
	 * @param nrModels how many models the structure has, which is not the same as how many
	 *        of them hold interactions - a model with none is still a model the user may
	 *        want to look at
	 */
	private void showBondsOfStructure(BondListItem[] bonds, int nrModels) {
		this.allBondsOfStructure = bonds;
		this.nrModelsOfStructure = Math.max(1, nrModels);
		this.currentModelNumber = 1;
		configureModelSelector(this.nrModelsOfStructure);
		fillBondsListForCurrentModel();
	}

	/** Refills the list with the interactions of the model on show, and only those. */
	private void fillBondsListForCurrentModel() {
		List<BondListItem> ofThisModel = new ArrayList<BondListItem>();
		for (BondListItem bond : allBondsOfStructure) {
			if (bond.getModelNumber() == currentModelNumber) {
				ofThisModel.add(bond);
			}
		}
		JList<BondListItem> linksList = getFoundLinksList();
		linksList.setSelectedIndices(NO_SELECTION);
		linksList.setListData(ofThisModel.toArray(NO_BOND_LIST_ITEMS));
	}

	private JList<BondListItem> getFoundLinksList() {
		if (foundLinksList == null) {
			foundLinksList = new JList<BondListItem>();
			foundLinksList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					//Selecting an interaction from the list should focus on it +/- show electron density
					if (e.getValueIsAdjusting()|| foundLinksList.getSelectedIndices().length != 1)
						return;
					final String linkFullString = foundLinksList.getModel().getElementAt(foundLinksList.getSelectedIndex()).getFullString();
//					System.out.println(linkFullString);
					//The structure the interaction belongs to is what the script needs to
					//undo the emphasis put on the previously picked interaction.
					PdbId pdbId = getFoundStructuresWithInteractionsList().getSelectedValue();
					String linkSelectedJMolScriptString = ResultManager.generateLinkSelectedJMolScriptString(linkFullString, pdbId);
					executeJmolScript(linkSelectedJMolScriptString);

					//TODO complete
					// +/- ED Map showing
//					ResultManager.decodeDrawSphereCommand(string);
				}
			});
			foundLinksList.setFixedCellHeight(foundLinksList.getFont().getSize()+1);
		}
		return foundLinksList;
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

	/**
	 * Runs {@code task} on the Event Dispatch Thread. Swing components may only
	 * be touched from that thread, and the parser calls back into this class
	 * from its own worker threads. Runs inline when already on the EDT so that
	 * callers on the EDT keep their current synchronous behaviour.
	 */
	private static void runOnEdt(Runnable task) {
		if (SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}

	/**
	 * Queues a Jmol script on the {@link #jmolThread}, where it runs to completion.
	 * <p>
	 * {@link JmolPanel#executeCmd(String)} calls {@code evalString}, which only hands
	 * the script to Jmol's own ScriptQueueThread. A script generated for one structure
	 * could therefore execute after the next structure had been loaded, when its atom
	 * indices no longer existed in the model, failing inside Jmol with errors such as
	 * "ArrayIndexOutOfBoundsException: Index 3282 out of bounds for length 3282".
	 * Running it with {@code scriptWait} on our own single thread keeps it bound to
	 * the structure it was generated for, while leaving the EDT free to draw the
	 * animation the script asks for.
	 */
	private void executeJmolScript(String script) {
		if (script == null) {
			return;
		}
		final JmolPanel panel = getJmolPanel();
		final String toRun = script;
		runOnJmolThread(new Runnable() {
			public void run() {
				panel.getViewer().scriptWait(toRun);
			}
		});
	}

	/**
	 * Hands {@code task} to the {@link #jmolThread}. Never call the viewer directly:
	 * everything that touches it goes through here, in submission order.
	 */
	private void runOnJmolThread(Runnable task) {
		jmolThread.execute(task);
	}

	/**
	 * Loads {@code structure} into the viewer and, if given, runs {@code afterLoadScript}
	 * against it. Both happen in one task so that no other Jmol work can slip between
	 * the load and the script that styles it.
	 */
	private void showStructure(final Structure structure, final String afterLoadScript) {
		final JmolPanel panel = getJmolPanel();
		runOnJmolThread(new Runnable() {
			public void run() {
				panel.setStructure(structure);
				if (afterLoadScript != null) {
					panel.getViewer().scriptWait(afterLoadScript);
				}
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

		//Toggled from the EDT, read by whichever thread is writing to System.out.
		volatile boolean enabled = true;
		
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
			startButton.setEnabled(false);
			final String path = fileChooser.getSelectedFile().getAbsolutePath();
			System.out.println("File :");
			System.out.println(path);

			Scanner scanner = null;
			try {
				scanner = new Scanner(new FileReader(path));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			final JList<PdbId> foundStructuresWithInteractionsList = getFoundStructuresWithInteractionsList();
			final PdbIdListModel pdbIdListModel = (PdbIdListModel) foundStructuresWithInteractionsList.getModel();
			if (scanner != null && clean) {
				pdbIdListModel.clear();
				//TODO clear the Jmolpanel too.
				parser.initialize();
			}
			final Scanner finalScanner = scanner;
			new Thread() {
				public void run() {
					try {
						if (finalScanner != null) {
							parser.importResultsFile(finalScanner);
						}
						sortResults();
					} finally {
						runOnEdt(new Runnable() {
							public void run() {
								startButton.setEnabled(true);
							}
						});
					}
				}
			}.start();
		}else {
			JOptionPane.showMessageDialog(jFrame,"Couldn't open File","ERROR",JOptionPane.ERROR_MESSAGE);
		}
	}
	private JSplitPane getMiddleAndRightSplitPane() {
		if (middleAndRightSplitPane == null) {
			middleAndRightSplitPane = new JSplitPane();
			middleAndRightSplitPane.setOneTouchExpandable(true);
			middleAndRightSplitPane.setResizeWeight(1.0);
			middleAndRightSplitPane.setLeftComponent(getJmolPanel());
			middleAndRightSplitPane.setRightComponent(getJPanel4());
		}
		return middleAndRightSplitPane;
	}
	
	public void interactionSelected(String interaction) {
		if (settingsManager.isShowWhileProcessing()) {
			//TODO find script file
//			String generalViewingScript = ResultManager.generateJMolScriptString(token, specificCollectionScriptString, foundInteractions);
			String generalViewingScript = null;
			out.setEnabled(false);
			executeJmolScript(generalViewingScript);
			out.setEnabled(true);
		}
	}
	private JSplitPane getFoundSplitPane() {
		if (foundSplitPane == null) {
			foundSplitPane = new JSplitPane();
			foundSplitPane.setResizeWeight(0.8);
			foundSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			foundSplitPane.setTopComponent(getFoundStructurePanel());
			foundSplitPane.setBottomComponent(getFoundLinksPanel());
		}
		return foundSplitPane;
	}
	private JSplitPane getLeftSplitPane() {
		if (leftSplitPane == null) {
			leftSplitPane = new JSplitPane();
			leftSplitPane.setOneTouchExpandable(true);
			leftSplitPane.setLeftComponent(getFoundSplitPane());
			leftSplitPane.setRightComponent(getMiddleAndRightSplitPane());
		}
		return leftSplitPane;
	}
	
	@Override
	public void sortResults() {
		//Also called from the parser's worker thread, so sort on the EDT.
		runOnEdt(new Runnable() {
			public void run() {
				System.out.print("Sorting entries...");
				final JList<PdbId> foundStructuresWithInteractionsList = getFoundStructuresWithInteractionsList();
				PdbId selectedPdbId = foundStructuresWithInteractionsList.getSelectedValue();
				((PdbIdListModel) foundStructuresWithInteractionsList.getModel()).sort();
				foundStructuresWithInteractionsList.setSelectedValue(selectedPdbId, true);
				System.out.println("Done");
			}
		});
	}
	
}
