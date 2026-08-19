package amralhossary.bonds;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.DefaultListCellRenderer;
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
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.density.DensityMapCache;
import org.biojava.nbio.structure.io.density.DensityMapKind;
import org.biojava.nbio.structure.io.density.DensityMapRequest;
import org.biojava.nbio.structure.io.density.DensityMapResult;
import org.biojava.nbio.structure.io.density.DensityMapSource;
import org.biojava.nbio.structure.io.density.DensityMapTooLargeException;

import org.biojava.nbio.structure.io.density.NoDensityMapException;
import org.jmol.api.JmolViewer;

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
	private JButton fetchAllDensityButton = null;
	private JmolPanel jmolPanel = null;
	private JPanel densityLegendPanel = null;
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
	/**
	 * Density maps are fetched on this one thread, never on the EDT and never on the Jmol
	 * thread.
	 * <p>
	 * Not the EDT because a fetch reaches the network and can take seconds. Not the Jmol
	 * thread because that thread must stay free to draw: a download queued behind it would
	 * stall every zoom and frame change until it finished. A single thread keeps downloads
	 * from competing with one another, and keeps them in the order the user asked for them.
	 */
	private final ExecutorService densityThread = Executors.newSingleThreadExecutor(new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "density");
			thread.setDaemon(true);
			return thread;
		}
	});
	/**
	 * What is known about each structure's map, drawn against its row in the list.
	 * Written from the density thread and read by the renderer on the EDT, hence concurrent.
	 */
	private final Map<PdbId, DensityService.DensityState> densityStates =
			new ConcurrentHashMap<PdbId, DensityService.DensityState>();
	/** Tooltip detail per structure - the source and size, or why there is no map. */
	private final Map<PdbId, String> densityDetails = new ConcurrentHashMap<PdbId, String>();
	/**
	 * Bumped every time a different structure is shown. A fetch captures it before starting
	 * and re-checks it before drawing, so a map that arrives after the user has moved on is
	 * cached but never drawn over the structure now on screen.
	 */
	private final AtomicInteger densityGeneration = new AtomicInteger();
	/**
	 * Bulk prefetching runs on its own thread rather than sharing {@link #densityThread}.
	 * With one queue, a structure the user just clicked would wait behind every entry still
	 * to be prefetched - which for a large sweep is thousands of them. Two threads keep the
	 * thing the user is looking at responsive while the batch grinds on behind it.
	 */
	private final ExecutorService prefetchThread = Executors.newSingleThreadExecutor(new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "density-prefetch");
			thread.setDaemon(true);
			return thread;
		}
	});
	/** Cleared to stop the batch; polled between entries. */
	private final AtomicBoolean prefetchRunning = new AtomicBoolean();
	/**
	 * What the batch still has to do, drained from the head.
	 * <p>
	 * A deque rather than a plain list so that selecting a structure can move it to the
	 * front: the user is looking at that one now, and whatever order the sweep produced
	 * matters less than the thing in front of them.
	 */
	private final java.util.concurrent.LinkedBlockingDeque<PdbId> prefetchQueue =
			new java.util.concurrent.LinkedBlockingDeque<PdbId>();
	/**
	 * Entries a fetch is already under way for, so the batch and a user's click never
	 * download the same map into the same file at the same time.
	 */
	private final java.util.Set<PdbId> densityInFlight =
			java.util.concurrent.ConcurrentHashMap.newKeySet();
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
			foundStructurePanel.add(getDensityLegendPanel(), null);
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
					+ "4izk;2ATK;1AY1;5b0w;5O81;6jky;3OPU\n"
					+ "2N2K; 2MJ8; 7N99; 2LRC; 2JU4; 2GLG; 2GLH; 2AFF; 1GAC");
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
			jPanel3.add(getFetchAllDensityButton());
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
					//a new run replaces every result, so nothing carries over - including
					//which interaction the selection was following, and what was known about
					//each structure's density map
					showBondsOfStructure(NO_BOND_LIST_ITEMS, 1);
					forgetDensityStates();
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
							loadOnEdt(panel, latest);
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
					//Stop means stop: the density batch is background work of the same kind.
					cancelDensityPrefetch();
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
			//Scrolling re-prioritises the batch: what the user is looking at is fetched next.
			jScrollPane.getViewport().addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					promoteVisibleInPrefetchQueue();
				}
			});
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
			foundStructuresWithInteractionsList.setCellRenderer(new DensityStateRenderer());
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
						requestDensityFor(pdbId, structure);
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
			final boolean showSelector = nrModels > 1;
			final boolean wasShowing = getModelSelectorPanel().isVisible();
			getModelSelectorPanel().setVisible(showSelector);
			getFoundLinksPanel().revalidate();
			if (showSelector != wasShowing) {
				giveLinksPanelRoomForSelector(showSelector);
			}
		} finally {
			adjustingModelSelector = false;
		}
	}

	/**
	 * Moves the divider so the interactions list keeps the room it had before the model
	 * selector appeared above it.
	 * <p>
	 * The share the links panel gets is enough for the list on its own, but the selector
	 * costs it two rows - the spinner row and the slider - which come straight out of the
	 * list. So the divider goes up by exactly the height the selector occupies when it
	 * appears, and back down by the same amount when it goes away, leaving a single-model
	 * structure looking precisely as it did before.
	 *
	 * @param appearing true when the selector has just been shown, false when hidden
	 */
	private void giveLinksPanelRoomForSelector(final boolean appearing) {
		//queued: the selector has only just been made visible, so its height is not laid
		//out yet, and the divider would move by zero.
		runOnEdt(new Runnable() {
			public void run() {
				int selectorHeight = getModelSelectorPanel().getPreferredSize().height;
				if (selectorHeight <= 0) {
					return;
				}
				JSplitPane split = getFoundSplitPane();
				int divider = split.getDividerLocation();
				int moved = appearing ? divider - selectorHeight : divider + selectorHeight;
				//never collapse the structures list above it
				split.setDividerLocation(Math.max(moved, split.getMinimumDividerLocation()));
			}
		});
	}

	/**
	 * @param stillDragging true while the slider knob is still under the pointer. Refilling
	 *        the list and changing the frame happen either way, because scrubbing is the
	 *        point of a slider; anything slower waits for the release, which JSlider always
	 *        delivers as a final event.
	 */
	private void modelChanged(int modelNumber, boolean stillDragging) {
		currentModelNumber = modelNumber;
		//The zoom script runs over a second - it contains an explicit delay - so running it
		//per drag step would make the slider unusable. It waits for the release, where the
		//sticky selection is restored for real.
		fillBondsListForCurrentModel(! stillDragging);
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
		//"frame 0" is Jmol's way of saying every model at once
		pendingFrame.set(settingsManager.isShowOnlySelectedModel() ? modelNumber : 0);
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
	 * The interaction the user last picked, held by its coordinate-free identity so it can
	 * be looked for again in whatever model is shown next. Null when nothing is picked.
	 * <p>
	 * Deliberately kept across model changes even when the new model does not contain it:
	 * models 1 and 3 may hold an interaction that model 2 does not, and stepping through
	 * model 2 should not lose it.
	 */
	private String rememberedBondIdentity = null;
	/**
	 * Set while the links list is being refilled and its selection restored.
	 * <p>
	 * Not defensive: setListData alone fires a zero-selection event, so without this every
	 * model change would look like the user deselecting and would erase the very thing this
	 * is for.
	 */
	private boolean restoringBondSelection = false;

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
		//a different structure means a different set of interactions; nothing to be sticky
		//about. Only a model change preserves the memory.
		this.rememberedBondIdentity = null;
		configureModelSelector(this.nrModelsOfStructure);
		fillBondsListForCurrentModel(true);
	}

	/**
	 * Refills the list with the interactions of the model on show, and only those, then
	 * puts the selection back on the remembered interaction if this model has it.
	 */
	private void fillBondsListForCurrentModel(boolean driveViewer) {
		List<BondListItem> ofThisModel = new ArrayList<BondListItem>();
		int indexToSelect = -1;
		for (BondListItem bond : allBondsOfStructure) {
			if (bond.getModelNumber() == currentModelNumber) {
				if (bond.getIdentity().equals(rememberedBondIdentity)) {
					indexToSelect = ofThisModel.size();
				}
				ofThisModel.add(bond);
			}
		}
		JList<BondListItem> linksList = getFoundLinksList();
		restoringBondSelection = true;
		try {
			linksList.setListData(ofThisModel.toArray(NO_BOND_LIST_ITEMS));
			if (indexToSelect >= 0) {
				linksList.setSelectedIndex(indexToSelect);
				linksList.ensureIndexIsVisible(indexToSelect);
			} else {
				//this model does not have it; show nothing picked but keep remembering
				linksList.clearSelection();
			}
		} finally {
			restoringBondSelection = false;
		}
		if (driveViewer && indexToSelect >= 0) {
			//the listener was suppressed, so drive the viewer here instead
			showSelectedBondInViewer(ofThisModel.get(indexToSelect));
		}
	}

	/** Zooms to and emphasises one interaction. */
	private void showSelectedBondInViewer(BondListItem bond) {
		PdbId pdbId = getFoundStructuresWithInteractionsList().getSelectedValue();
		executeJmolScript(ResultManager.generateLinkSelectedJMolScriptString(bond.getFullString(), pdbId));
	}

	private JList<BondListItem> getFoundLinksList() {
		if (foundLinksList == null) {
			foundLinksList = new JList<BondListItem>();
			foundLinksList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					//Selecting an interaction from the list should focus on it +/- show electron density
					if (e.getValueIsAdjusting())
						return;
					if (restoringBondSelection) {
						//our own refill, not a choice the user made
						return;
					}
					if (foundLinksList.getSelectedIndices().length != 1) {
						if (foundLinksList.getSelectedIndices().length == 0) {
							//the user deselected, so stop following an interaction around
							rememberedBondIdentity = null;
						}
						return;
					}
					BondListItem selected = foundLinksList.getModel().getElementAt(foundLinksList.getSelectedIndex());
					rememberedBondIdentity = selected.getIdentity();
					showSelectedBondInViewer(selected);

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
	 * Shows the density map of the structure just put on screen, fetching it first if that
	 * is allowed and it is not already cached.
	 * <p>
	 * Called from the structures-list selection, on the EDT. It only queues work.
	 *
	 * @param pdbId the structure now selected
	 * @param structure the same structure, needed for its experimental method
	 */
	private void requestDensityFor(final PdbId pdbId, final Structure structure) {
		//A new structure invalidates any fetch still in flight for the previous one.
		final int generation = densityGeneration.incrementAndGet();

		//The batch runs on its own thread, so this fetch never waits behind it. Moving the
		//entry to the head of the batch queue as well means that if this fetch does not
		//produce a map - it is skipped when downloading is off, for instance - the batch
		//reaches it next rather than in several thousand entries' time.
		promoteInPrefetchQueue(pdbId);

		//Whatever was contoured belongs to the structure that has just been replaced.
		runOnJmolThread(new Runnable() {
			public void run() {
				getJmolPanel().clearDensityMaps();
			}
		});

		if (! settingsManager.isShowElectronDensity()) {
			return;
		}

		final DensityMapKind kind = DensityService.kindFor(structure, settingsManager.getDensityMapKind());
		if (kind == null) {
			//An NMR ensemble. Not a failure and not worth a network round trip.
			setDensityState(pdbId, DensityService.DensityState.NO_DENSITY,
					"No density map exists for this experimental method");
			return;
		}

		final String selection = ResultManager.interactingAtomsSelection(pdbId);
		if (selection == null) {
			//Nothing to contour around, and contouring the whole map instead would be slow
			//enough to look like a hang.
			return;
		}

		setDensityState(pdbId, DensityService.DensityState.QUEUED, "Waiting to fetch");
		densityThread.execute(new Runnable() {
			public void run() {
				fetchAndDrawDensity(pdbId, kind, selection, generation);
			}
		});
	}

	/**
	 * Runs on the density thread: find the map, then hand the drawing to the Jmol thread.
	 * <p>
	 * The two failure modes are kept apart deliberately. {@link NoDensityMapException} means
	 * every source was asked and none has a map for this entry, which is a fact about the
	 * entry. Any other {@link IOException} means the network let us down, which is a fact
	 * about today - reporting the second as the first would quietly tell the user that a
	 * structure has no density when it may have plenty.
	 */
	private void fetchAndDrawDensity(PdbId pdbId, DensityMapKind kind, String selection, int generation) {
		//Order of preference, and the reasoning behind it:
		//
		// 1. a cached box. Built from these very atoms, so it covers them by construction, and
		//    it is already on disk. Nothing better exists.
		// 2. a box download, when allowed. This is the one step that goes online while something
		//    is cached, and it is deliberate: a cached whole-cell map that does not reach the
		//    cross-links cannot draw the picture being asked for, so treating it as "already have
		//    it" would leave the user looking at nothing. It happens once per entry and radius,
		//    for about 58 kB, after which step 1 answers for ever.
		// 3. a cached whole-cell map. Free, and often perfectly good - it only fails for entries
		//    whose coordinates fall outside the cell box.
		// 4. a whole-cell download, in the user's order.
		double radius = settingsManager.getDensityClipRadius();
		File cachedBox = firstCachedBox(pdbId, kind, radius);
		if (cachedBox != null) {
			setDensityState(pdbId, DensityService.DensityState.AVAILABLE,
					"Density shown - a cached box around these atoms");
			drawDensityFile(cachedBox, kind, selection, generation, null);
			return;
		}

		if (settingsManager.isAutoFetchElectronDensity() && kind != DensityMapKind.EM) {
			double[][] boxRegion = ResultManager.interactingAtomsBounds(pdbId, radius);
			if (boxRegion != null) {
				setDensityState(pdbId, DensityService.DensityState.FETCHING, "Downloading...");
				for (DensityMapSource source : orderedSources(kind)) {
					if (! DensityService.servesBox(source)) {
						continue;
					}
					if (generation != densityGeneration.get()) {
						return;
					}
					try {
						File box = fetchBoxMap(pdbId, kind, source, boxRegion, radius);
						if (box != null) {
							setDensityState(pdbId, DensityService.DensityState.AVAILABLE,
									String.format("Density shown - %s box from %s, %,d kB",
											kind, source, box.length() / 1024));
							drawDensityFile(box, kind, selection, generation, null);
							return;
						}
					} catch (DensityMapTooLargeException e) {
						reportTooLarge(pdbId, source, e);
						return;
					} catch (IOException e) {
						System.err.println("Box fetch from " + source + " failed for "
								+ pdbId.getId() + ": " + e);
					}
				}
			}
		}

		try {
			DensityMapResult cached = findCachedDensity(pdbId, kind);
			if (cached != null) {
				setDensityState(pdbId, DensityService.DensityState.AVAILABLE, describe(cached));
				drawDensity(cached, selection, generation);
				return;
			}
		} catch (RuntimeException e) {
			System.err.println("Density cache probe failed for " + pdbId.getId() + ": " + e);
		}

		if (! settingsManager.isAutoFetchElectronDensity()) {
			setDensityState(pdbId, DensityService.DensityState.NOT_FETCHED,
					"Not downloaded - switch on \"Download density maps when needed\"");
			return;
		}

		//Nothing cached, and downloading is allowed. Walk the user's chain in their order and
		//take the first source that answers - which is what makes the order in the dialog mean
		//something rather than being decoration.
		setDensityState(pdbId, DensityService.DensityState.FETCHING, "Downloading...");
		IOException lastFailure = null;
		NoDensityMapException lastAbsence = null;

		for (DensityMapSource source : orderedSources(kind)) {
			if (generation != densityGeneration.get()) {
				return;
			}
			try {
				DensityMapResult map = DensityService.newCache(settingsManager)
						.getDensityMap(DensityMapRequest.builder(pdbId)
								.kind(kind)
								//wwPDB serves structure factors, which need a Fourier transform
								//before anything can be drawn; asking for renderable formats only
								//skips that source rather than drawing nothing.
								.allowNonRenderableFormats(false)
								.sourceChain(java.util.Collections.singletonList(source))
								.maxDownloadBytes(settingsManager.getDensityMaxDownloadBytes())
								.build());
				setDensityState(pdbId, DensityService.DensityState.AVAILABLE, describe(map));
				drawDensity(map, selection, generation);
				return;
			} catch (DensityMapTooLargeException e) {
				reportTooLarge(pdbId, source, e);
				return;
			} catch (NoDensityMapException e) {
				lastAbsence = e;
			} catch (IOException e) {
				lastFailure = e;
				System.err.println("Density fetch from " + source + " failed for " + pdbId.getId() + ": " + e);
			} catch (RuntimeException e) {
				lastFailure = new IOException(String.valueOf(e.getMessage()), e);
				System.err.println("Density fetch from " + source + " failed for " + pdbId.getId() + ": " + e);
			}
		}

		//Every source was asked. Absence and failure are reported differently on purpose: the
		//first is a fact about the entry, the second only about today.
		if (lastFailure != null) {
			setDensityState(pdbId, DensityService.DensityState.FAILED,
					"Could not fetch the map: " + lastFailure.getMessage());
		} else {
			setDensityState(pdbId, DensityService.DensityState.NO_DENSITY,
					lastAbsence == null ? "No source has a density map for this entry"
							: lastAbsence.getMessage());
		}
	}

	/** The user's chain for this kind, in their order. AUTO follows the X-ray chain. */
	private java.util.List<DensityMapSource> orderedSources(DensityMapKind kind) {
		return DensityService.newCache(settingsManager).getSourceChain(
				kind == DensityMapKind.AUTO ? DensityMapKind.TWO_FO_FC : kind);
	}

	/**
	 * Reports a refused download in its own words, naming both numbers.
	 * <p>
	 * The remedy is one specific setting, so the message says which - "could not fetch" would
	 * send the user looking for a network fault that is not there.
	 */
	private void reportTooLarge(PdbId pdbId, DensityMapSource source, DensityMapTooLargeException e) {
		setDensityState(pdbId, DensityService.DensityState.FAILED, String.format(
				"Too large: %s offers %,d MB and the limit is %d MB. Raise \"Largest download\" to fetch it.",
				source, e.getSizeBytes() / (1024 * 1024), settingsManager.getDensityMaxDownloadMB()));
	}

	/** @return an already-downloaded box for this entry and radius, or null */
	private File firstCachedBox(PdbId pdbId, DensityMapKind kind, double radius) {
		for (DensityMapSource source : DensityService.newCache(settingsManager).getSourceChain(
				kind == DensityMapKind.AUTO ? DensityMapKind.TWO_FO_FC : kind)) {
			if (! DensityService.servesBox(source)) {
				continue;
			}
			File file = DensityService.boxCacheFile(settingsManager, pdbId, kind, source, radius);
			if (file.isFile() && file.length() > 0) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Downloads a box around the interacting atoms and caches it.
	 * <p>
	 * This is what replaced Jmol's own EDS fetching. Jmol did the same thing - it substitutes
	 * the bounding box of the selection into a box request - but always through PDBe, without
	 * caching anything, and from inside its own command, which put a network wait on the thread
	 * that has to stay free to draw. Doing it here keeps the source order meaningful, leaves the
	 * result on disk for next time, and keeps the wait on the density thread.
	 *
	 * @return the cached file, or null if this source has no box for the entry
	 * @throws DensityMapTooLargeException if the response exceeds the configured limit
	 * @throws IOException on any other transport failure
	 */
	private File fetchBoxMap(PdbId pdbId, DensityMapKind kind, DensityMapSource source,
			double[][] region, double radius) throws IOException {
		String urlString = DensityService.boxUrl(source, pdbId, region[0], region[1]);
		if (urlString == null) {
			return null;
		}
		File target = DensityService.boxCacheFile(settingsManager, pdbId, kind, source, radius);
		target.getParentFile().mkdirs();

		HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
		connection.setConnectTimeout(20000);
		connection.setReadTimeout(120000);
		try {
			int status = connection.getResponseCode();
			if (status == HttpURLConnection.HTTP_NOT_FOUND || status == HttpURLConnection.HTTP_BAD_REQUEST) {
				//This source simply has nothing for the entry; the chain moves on.
				return null;
			}
			if (status != HttpURLConnection.HTTP_OK) {
				throw new IOException("HTTP " + status + " for " + urlString);
			}
			//Checked before reading the body, so an oversized map is refused rather than
			//downloaded and then rejected.
			long length = connection.getContentLengthLong();
			long limit = settingsManager.getDensityMaxDownloadBytes();
			if (length > limit) {
				throw new DensityMapTooLargeException(urlString, length, limit);
			}
			InputStream in = connection.getInputStream();
			try {
				Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} finally {
				in.close();
			}
		} finally {
			connection.disconnect();
		}
		return target.length() > 0 ? target : null;
	}


	/**
	 * Looks for an already-downloaded map, without touching the network.
	 * <p>
	 * This asks BioJava to resolve it, rather than looking for a file by name. Naming a
	 * cached map is not as simple as it looks: a density server answers with both the 2Fo-Fc
	 * and the Fo-Fc block in one file, so it is cached once under a shared token rather than
	 * under either kind, and the file also carries the detail level it was sampled at
	 * ({@code 1m3q_both_rcsbvs_d3.bcif}). A probe written here would have to know all of
	 * that and would fall behind the moment upstream changed it - it already missed every
	 * volume-server map on the first attempt.
	 *
	 * @return the cached map, or null if nothing local answers
	 */
	private DensityMapResult findCachedDensity(PdbId pdbId, DensityMapKind kind) {
		DensityMapCache local = DensityService.newCache(settingsManager);
		local.setFetchBehavior(FetchBehavior.LOCAL_ONLY);
		try {
			return local.getDensityMap(DensityMapRequest.builder(pdbId)
					.kind(kind)
					.fetchBehavior(FetchBehavior.LOCAL_ONLY)
					.allowNonRenderableFormats(false)
					.build());
		} catch (IOException e) {
			//Nothing cached - including NoDensityMapException, which here only means "not
			//on this disk", since no source was allowed to be asked.
			return null;
		}
	}

	/**
	 * Contours {@code map} around the interacting atoms, on the Jmol thread.
	 * <p>
	 * {@code loadDensityMap} ends in an {@code evalString}, so it touches the viewer and has
	 * to be queued like every other viewer call rather than run from the fetch thread.
	 */
	private void drawDensity(final DensityMapResult map, final String selection, final int generation) {
		runOnJmolThread(new Runnable() {
			public void run() {
				if (generation != densityGeneration.get()) {
					//The user moved on while this was downloading. The map is cached for
					//next time; drawing it now would put it over a different structure.
					return;
				}
				double radius = settingsManager.getDensityClipRadius();
				Double recommended = map.getRecommendedContourLevel();
				if (map.getKind() == DensityMapKind.EM && recommended != null) {
					//An EM map is contoured at the level its depositors chose. A multiple of
					//sigma means nothing for one.
					getJmolPanel().loadDensityMap(map.getFile(), map.getKind(),
							recommended.doubleValue(), false, selection, radius);
				} else {
					getJmolPanel().loadDensityMap(map.getFile(), map.getKind(),
							settingsManager.getDensityContourSigma(), true, selection, radius);
				}
			}
		});
		//Contouring is asynchronous: loadDensityMap ends in evalString, which hands the
		//script to Jmol's own queue and returns at once, so the surface does not exist yet.
		//The check therefore waits, on this thread rather than the Jmol one, which must stay
		//free to build the very surface being waited for.
		densityThread.execute(new Runnable() {
			public void run() {
				try {
					Thread.sleep(CONTOURING_GRACE_MILLIS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				if (generation != densityGeneration.get()) {
					return;
				}
				runOnJmolThread(new Runnable() {
					public void run() {
						warnIfNothingWasDrawn(map);
					}
				});
			}
		});
	}

	/**
	 * How long to give Jmol to contour before concluding that nothing was drawn. Generous:
	 * the cost of being wrong is a misleading tooltip, and a large map takes a moment.
	 */
	private static final long CONTOURING_GRACE_MILLIS = 4000;

	/**
	 * Says so when a map loads but contours to nothing around the interacting atoms.
	 * <p>
	 * A map covers one cell box, and an entry's coordinates need not lie inside it: all four
	 * of 3ALB's cross-links have a negative z, outside a map that runs from the origin.
	 * <p>
	 * The density is not missing, only displaced. These maps are periodic - 3ALB's covers
	 * exactly one cell, 64x90x150 samples over 59.1 x 77.4 x 135.1 A - so the density at
	 * z = -24.2 is the density at z = 110.9, and contouring there yields a perfectly good
	 * surface. What has not been found is a way to make Jmol draw it at the atoms: neither
	 * {@code isosurface ... periodic} nor {@code offset}, with or without a unit cell set on
	 * the model, clips successfully around atoms outside the box. See KNOWN-ISSUES.md.
	 * <p>
	 * Until then, say so. Being told the map is available and then shown an empty screen is
	 * indistinguishable from a bug.
	 */
	private void warnIfNothingWasDrawn(DensityMapResult map) {
		String shapes = String.valueOf(getJmolPanel().getViewer()
				.getProperty("String", "shapeInfo", null));
		if (shapes.contains(JmolPanel.ISOSURFACE_ID_2FOFC)
				|| shapes.contains(JmolPanel.ISOSURFACE_ID_FOFC)
				|| shapes.contains(JmolPanel.ISOSURFACE_ID_EM)) {
			return;
		}
		String detail = "The cached " + map.getSource() + " map covers one unit cell, and these atoms "
				+ "lie outside that box. Switch on \"Download density maps when needed\" to fetch a "
				+ "map around these atoms instead.";
		densityDetails.put(map.getPdbId(), detail);
		System.out.println(map.getPdbId().getId() + ": " + detail);
		runOnEdt(new Runnable() {
			public void run() {
				getFoundStructuresWithInteractionsList().repaint();
			}
		});
	}

	/**
	 * Contours a map file that is already on disk, on the Jmol thread.
	 * <p>
	 * Used for the box maps this application fetches itself. No coverage warning follows, unlike
	 * {@link #drawDensity}: a box is built from the atoms being contoured, so it covers them by
	 * construction. Only a whole-cell map can fail to.
	 *
	 * @param contourOverride an absolute level, or null to contour at the configured sigma
	 */
	private void drawDensityFile(final File file, final DensityMapKind kind, final String selection,
			final int generation, final Double contourOverride) {
		runOnJmolThread(new Runnable() {
			public void run() {
				if (generation != densityGeneration.get()) {
					return;
				}
				DensityMapKind drawAs = (kind == DensityMapKind.AUTO) ? DensityMapKind.TWO_FO_FC : kind;
				double radius = settingsManager.getDensityClipRadius();
				if (contourOverride != null) {
					getJmolPanel().loadDensityMap(file, drawAs, contourOverride.doubleValue(), false,
							selection, radius);
				} else {
					getJmolPanel().loadDensityMap(file, drawAs,
							settingsManager.getDensityContourSigma(), true, selection, radius);
				}
			}
		});
	}

	/** @return a short description of where a map came from, for the list tooltip */
	private static String describe(DensityMapResult map) {
		return String.format("%s from %s, %,d kB%s", map.getKind(), map.getSource(),
				map.getFileSizeBytes() / 1024, map.isFromCache() ? " (cached)" : "");
	}

	/**
	 * The words behind each marker. The marker is shorthand; this is the statement.
	 */
	static String densityStatusWords(DensityService.DensityState state) {
		switch (state) {
			case AVAILABLE:   return "Density shown";
			case QUEUED:      return "Waiting to fetch";
			case FETCHING:    return "Downloading";
			case NOT_FETCHED: return "Not downloaded";
			case NO_DENSITY:  return "No density map exists";
			case FAILED:      return "Fetch failed";
			default:          return "";
		}
	}

	/** The character shown against a row. Chosen so the states differ in shape, not only colour. */
	static String densityMarker(DensityService.DensityState state) {
		switch (state) {
			case AVAILABLE:   return "\u25cf";   // filled circle
			case FETCHING:    return "\u25d0";   // half filled
			case QUEUED:      return "\u25cb";   // hollow
			case NOT_FETCHED: return "\u25cb";
			case NO_DENSITY:  return "\u00b7";   // a dot
			case FAILED:      return "\u2715";   // a cross
			default:          return " ";
		}
	}

	static Color densityColour(DensityService.DensityState state) {
		switch (state) {
			case AVAILABLE:   return new Color(0, 128, 0);
			case FETCHING:    return new Color(0, 90, 190);
			case QUEUED:      return Color.GRAY;
			case NOT_FETCHED: return Color.GRAY;
			case NO_DENSITY:  return Color.LIGHT_GRAY;
			case FAILED:      return new Color(180, 0, 0);
			default:          return Color.BLACK;
		}
	}

	/** The hints the list carried before density was added; kept, not replaced. */
	private static final String LIST_HINT =
			"<i>Click</i> an item to <B>preview</B>.<BR><i>DoubleClick</i> or <i>Enter</i> to <B>open</B>.";

	/**
	 * Draws each structure with a marker for what is known about its density map.
	 * <p>
	 * The marker sits on the right, so every identifier starts at the same place and the column
	 * stays readable; as a prefix it pushed the ids out of line. It is a character rather than
	 * an icon so it costs nothing down a list of thousands, and the states differ in shape as
	 * well as colour, which a colour-blind reader still gets.
	 * <p>
	 * The tooltip states the status in words and keeps the list's original click hints rather
	 * than replacing them.
	 */
	private class DensityStateRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		private final JPanel row = new JPanel(new BorderLayout());
		private final JLabel name = new JLabel();
		private final JLabel marker = new JLabel();

		DensityStateRenderer() {
			row.setOpaque(true);
			name.setOpaque(false);
			marker.setOpaque(false);
			marker.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
			row.add(name, BorderLayout.CENTER);
			row.add(marker, BorderLayout.EAST);
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			name.setText(String.valueOf(value));
			name.setFont(list.getFont());
			name.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			row.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

			DensityService.DensityState state = densityStates.get(value);
			if (state == null || ! settingsManager.isShowElectronDensity()) {
				marker.setText("");
				row.setToolTipText("<HTML>" + LIST_HINT + "</HTML>");
				return row;
			}
			marker.setText(densityMarker(state));
			marker.setForeground(isSelected ? list.getSelectionForeground() : densityColour(state));

			String detail = densityDetails.get(value);
			StringBuilder tip = new StringBuilder("<HTML>").append(LIST_HINT).append("<HR>")
					.append("<B>").append(densityStatusWords(state)).append("</B>");
			if (detail != null && ! detail.isEmpty()) {
				tip.append("<BR>").append(detail.replace("<", "&lt;"));
			}
			row.setToolTipText(tip.append("</HTML>").toString());
			return row;
		}
	}

	/**
	 * A one-line key for the markers, under the structures list.
	 * <p>
	 * Closable, because it is only needed until the shapes are learnt, and the panel it sits in
	 * shares its height with the interactions list. The choice is remembered.
	 */
	private JPanel getDensityLegendPanel() {
		if (densityLegendPanel == null) {
			densityLegendPanel = new JPanel();
			densityLegendPanel.setName("densityLegendPanel");
			//A one-row grid rather than a box: every cell takes an equal share of the width and
			//grows with the panel, so the key stays spread across the pane at any divider position
			//instead of bunching at one end.
			densityLegendPanel.setLayout(new GridLayout(1, 0));
			densityLegendPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

			for (DensityService.DensityState state : new DensityService.DensityState[] {
					DensityService.DensityState.AVAILABLE, DensityService.DensityState.NOT_FETCHED,
					DensityService.DensityState.NO_DENSITY, DensityService.DensityState.FAILED}) {
				JLabel key = new JLabel(densityMarker(state), SwingConstants.CENTER);
				key.setForeground(densityColour(state));
				//Each marker explains itself, so the strip can stay terse without being cryptic.
				key.setToolTipText(densityStatusWords(state) + " - " + densityLegendHelp(state));
				densityLegendPanel.add(key);
			}

			JButton close = new JButton("\u00d7");
			close.setName("densityLegendCloseButton");
			close.setToolTipText("Hide this key. \"Electron density...\" in Preferences brings it back.");
			close.setMargin(new Insets(0, 2, 0, 2));
			close.setFocusable(false);
			close.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					settingsManager.setShowDensityLegend(false);
					densityLegendPanel.setVisible(false);
					settingsManager.saveSettings(true);   // true: no listener fan-out for a view toggle
				}
			});
			densityLegendPanel.add(close);

			densityLegendPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
					densityLegendPanel.getPreferredSize().height));
			densityLegendPanel.setVisible(settingsManager.isShowDensityLegend());
		}
		return densityLegendPanel;
	}

	/** One clause saying what a marker means, for its tooltip in the key. */
	private static String densityLegendHelp(DensityService.DensityState state) {
		switch (state) {
			case AVAILABLE:   return "a map is drawn around the interacting atoms";
			case NOT_FETCHED: return "nothing cached, and downloading is switched off";
			case NO_DENSITY:  return "this experimental method produces none, or no source has one";
			case FAILED:      return "a download was tried and did not succeed; worth retrying";
			default:          return "";
		}
	}

	/**
	 * Starts or cancels prefetching a density map for every structure in the results list.
	 * <p>
	 * Deliberate rather than automatic: a large sweep lists thousands of entries, and at a few
	 * hundred kilobytes each that is a lot to set going by itself. Selecting one structure still
	 * fetches just that one, as before.
	 */
	private JButton getFetchAllDensityButton() {
		if (fetchAllDensityButton == null) {
			fetchAllDensityButton = new JButton("Fetch all density");
			fetchAllDensityButton.setName("fetchAllDensityButton");
			fetchAllDensityButton.setToolTipText("Download a density map for every structure listed, "
					+ "in the background. Already-cached entries are skipped.");
			fetchAllDensityButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (prefetchRunning.get()) {
						cancelDensityPrefetch();
					} else {
						startDensityPrefetch();
					}
				}
			});
		}
		return fetchAllDensityButton;
	}

	/** Queues every listed structure. Called on the EDT, so the list is read here, not later. */
	private void startDensityPrefetch() {
		if (! settingsManager.isAutoFetchElectronDensity()) {
			JOptionPane.showMessageDialog(getJFrame(),
					"Downloading is switched off.\n\nTurn on \"Download density maps when needed\" in "
					+ "Preferences first.",
					"Fetch all density", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		final java.util.List<PdbId> entries = orderedForPrefetch();
		if (entries.isEmpty()) {
			return;
		}
		prefetchQueue.clear();
		prefetchQueue.addAll(entries);
		prefetchRunning.set(true);
		getFetchAllDensityButton().setText("Stop fetching");
		final int total = entries.size();
		System.out.println("Fetching density for " + total + " structures...");

		prefetchThread.execute(new Runnable() {
			public void run() {
				int done = 0;
				try {
					while (prefetchRunning.get()) {
						PdbId pdbId = prefetchQueue.pollFirst();
						if (pdbId == null) {
							break;
						}
						prefetchOne(pdbId);
						done++;
						if (done % 25 == 0) {
							System.out.println("   density: " + done + " of " + total);
						}
					}
					if (! prefetchRunning.get()) {
						System.out.println("Density fetch cancelled after " + done + " of " + total);
						return;
					}
					System.out.println("Density fetch finished: " + done + " structures visited.");
				} finally {
					prefetchRunning.set(false);
					prefetchQueue.clear();
					runOnEdt(new Runnable() {
						public void run() {
							getFetchAllDensityButton().setText("Fetch all density");
						}
					});
				}
			}
		});
	}

	/**
	 * Every listed structure, the ones on screen first.
	 * <p>
	 * The order a sweep produced is alphabetical and means nothing to someone watching the
	 * list fill in. Starting with the rows actually in view means the markers the user can see
	 * are the ones that change first, and a batch cancelled after a minute has still done the
	 * part they were looking at.
	 *
	 * @return the entries, visible ones first, each appearing once
	 */
	private java.util.List<PdbId> orderedForPrefetch() {
		JList<PdbId> list = getFoundStructuresWithInteractionsList();
		PdbIdListModel model = (PdbIdListModel) list.getModel();
		int size = model.getSize();
		java.util.List<PdbId> ordered = new ArrayList<PdbId>(size);

		int first = list.getFirstVisibleIndex();
		int last = list.getLastVisibleIndex();
		if (first >= 0 && last >= first) {
			for (int i = first; i <= last && i < size; i++) {
				ordered.add(model.getElementAt(i));
			}
		}
		//The rest keep their order behind them. A set would lose that, so membership is tested
		//against the short visible run instead.
		for (int i = 0; i < size; i++) {
			if (first < 0 || i < first || i > last) {
				ordered.add(model.getElementAt(i));
			}
		}
		return ordered;
	}

	/**
	 * Moves whatever is now on screen to the head of the batch queue.
	 * <p>
	 * Called when the list is scrolled, so "visible" stays a live notion rather than a snapshot
	 * taken when the batch started. The order of priority is therefore the structure just
	 * selected, then the rows in view, then everything else.
	 * <p>
	 * Cheap enough to do on every scroll: it touches only the screenful of entries, and an entry
	 * the batch has already done is simply not in the queue to move.
	 */
	private void promoteVisibleInPrefetchQueue() {
		if (! prefetchRunning.get()) {
			return;
		}
		JList<PdbId> list = getFoundStructuresWithInteractionsList();
		int first = list.getFirstVisibleIndex();
		int last = list.getLastVisibleIndex();
		if (first < 0 || last < first) {
			return;
		}
		PdbIdListModel model = (PdbIdListModel) list.getModel();
		//Backwards, so that after each addFirst the screenful ends up in its own order.
		for (int i = Math.min(last, model.getSize() - 1); i >= first; i--) {
			PdbId pdbId = model.getElementAt(i);
			if (prefetchQueue.remove(pdbId)) {
				prefetchQueue.addFirst(pdbId);
			}
		}
	}

	/** Moves an entry to the head of the batch queue, if the batch still has it to do. */
	private void promoteInPrefetchQueue(PdbId pdbId) {
		if (prefetchRunning.get() && prefetchQueue.remove(pdbId)) {
			prefetchQueue.addFirst(pdbId);
		}
	}

	/** Stops the batch after the entry in progress. */
	private void cancelDensityPrefetch() {
		if (prefetchRunning.compareAndSet(true, false)) {
			System.out.println("Stopping density fetch...");
		}
		prefetchQueue.clear();
		getFetchAllDensityButton().setText("Fetch all density");
	}

	/**
	 * Caches one structure's map, drawing nothing.
	 * <p>
	 * Deliberately does not touch {@link #densityGeneration}: that counter exists to stop a slow
	 * fetch painting itself over a structure the user has since moved away from, and a batch
	 * that bumped it would cancel the drawing of whatever is actually on screen.
	 */
	private void prefetchOne(PdbId pdbId) {
		if (! densityInFlight.add(pdbId)) {
			return;   // the user clicked it and that fetch is already running
		}
		try {
			double radius = settingsManager.getDensityClipRadius();
			if (firstCachedBox(pdbId, DensityMapKind.TWO_FO_FC, radius) != null) {
				setDensityState(pdbId, DensityService.DensityState.AVAILABLE,
						"Density available - a cached box around these atoms");
				return;
			}
			double[][] region = ResultManager.interactingAtomsBounds(pdbId, radius);
			if (region == null) {
				return;   // no cached interactions, so nothing to build a box around
			}
			//The experimental method decides whether a map can exist at all, and it is only in
			//the structure's header - so the file is read even though nothing is drawn.
			Structure structure = ResultManager.getStructureById(pdbId);
			DensityMapKind kind = DensityService.kindFor(structure, settingsManager.getDensityMapKind());
			if (kind == null) {
				setDensityState(pdbId, DensityService.DensityState.NO_DENSITY,
						"No density map exists for this experimental method");
				return;
			}
			if (kind == DensityMapKind.EM) {
				return;   // EM goes through the cache path, which the batch does not drive
			}
			setDensityState(pdbId, DensityService.DensityState.FETCHING, "Downloading...");
			for (DensityMapSource source : orderedSources(kind)) {
				if (! prefetchRunning.get()) {
					return;
				}
				if (! DensityService.servesBox(source)) {
					continue;
				}
				try {
					File box = fetchBoxMap(pdbId, kind, source, region, radius);
					if (box != null) {
						setDensityState(pdbId, DensityService.DensityState.AVAILABLE,
								String.format("Density available - %s box from %s, %,d kB",
										kind, source, box.length() / 1024));
						return;
					}
				} catch (DensityMapTooLargeException e) {
					reportTooLarge(pdbId, source, e);
					return;
				} catch (IOException e) {
					setDensityState(pdbId, DensityService.DensityState.FAILED,
							"Could not fetch the map: " + e.getMessage());
				}
			}
			if (densityStates.get(pdbId) != DensityService.DensityState.FAILED) {
				setDensityState(pdbId, DensityService.DensityState.NO_DENSITY,
						"No source has a density map for this entry");
			}
		} catch (RuntimeException e) {
			setDensityState(pdbId, DensityService.DensityState.FAILED, String.valueOf(e.getMessage()));
		} finally {
			densityInFlight.remove(pdbId);
		}
	}

	/**
	 * Drops everything known about density maps, for when the results list is replaced.
	 * <p>
	 * The cached files themselves are left alone: they are still valid, and re-selecting a
	 * structure finds them again without a download. Only this session's knowledge goes.
	 */
	private void forgetDensityStates() {
		cancelDensityPrefetch();
		densityStates.clear();
		densityDetails.clear();
		//abandon any fetch still in flight for a structure that is no longer listed
		densityGeneration.incrementAndGet();
	}

	/** Records a state and repaints the row showing it. Callable from any thread. */
	private void setDensityState(PdbId pdbId, DensityService.DensityState state, String detail) {
		densityStates.put(pdbId, state);
		densityDetails.put(pdbId, detail == null ? "" : detail);
		runOnEdt(new Runnable() {
			public void run() {
				getFoundStructuresWithInteractionsList().repaint();
			}
		});
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
				loadOnEdt(panel, structure);
				if (afterLoadScript != null) {
					panel.getViewer().scriptWait(afterLoadScript);
				}
			}
		});
	}

	/**
	 * Replaces the model set the viewer is showing, on the EDT.
	 * <p>
	 * Loading is a zap followed by a rebuild, and in between the shapes the renderer is
	 * about to draw do not exist yet. The EDT is what paints this panel, so a load
	 * running on any other thread can be caught half way through by a repaint:
	 * <pre>NullPointerException: Cannot assign field "haveStrutPoints" because "sticks" is null
	 *     at org.jmol.render.RepaintManager.render
	 *     at org.biojava...JmolPanel.paint</pre>
	 * Doing the load on the EDT rules that out, because one thread cannot be painting and
	 * loading at the same time. Jmol's own thread-safety patch does not help here: it
	 * serialises one loader against another, not a loader against the painter.
	 * <p>
	 * Only the load needs this. Scripts stay on the Jmol thread, which is what lets their
	 * animations render, and they never replace the model set.
	 * <p>
	 * Called from the Jmol thread, which waits here for the load to finish, so tasks stay
	 * in submission order and nothing can slip between a load and the script that styles it.
	 */
	private void loadOnEdt(final JmolPanel panel, final Structure structure) {
		if (SwingUtilities.isEventDispatchThread()) {
			panel.setStructure(structure);
			return;
		}
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					panel.setStructure(structure);
				}
			});
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
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
		getDensityLegendPanel().setVisible(settingsManager.isShowDensityLegend());
		//so switching between one model and all of them takes effect now rather than at
		//the next time the user happens to change model
		applyViewerModel(currentModelNumber);

		//Density settings take effect on the structure already on screen, rather than at the
		//next selection. The cache is rebuilt per fetch from the live settings, so nothing
		//here has to be pushed anywhere - unlike AtomCache, which is built once and never
		//told (see KNOWN-ISSUES.md).
		PdbId shown = getFoundStructuresWithInteractionsList().getSelectedValue();
		if (shown == null) {
			return;
		}
		if (! settingsManager.isShowElectronDensity()) {
			densityGeneration.incrementAndGet();   // abandon anything in flight
			runOnJmolThread(new Runnable() {
				public void run() {
					getJmolPanel().clearDensityMaps();
				}
			});
			return;
		}
		Structure structure = ResultManager.getStructureById(shown);
		if (structure != null) {
			requestDensityFor(shown, structure);
		}
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
				//every result is being replaced, so the interactions list, the interaction
				//the selection was following, and what was known about each density map all
				//go with them
				showBondsOfStructure(NO_BOND_LIST_ITEMS, 1);
				forgetDensityStates();
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
