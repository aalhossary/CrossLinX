package amralhossary.bonds;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

import org.biojava.nbio.structure.io.density.DensityMapCache;
import org.biojava.nbio.structure.io.density.DensityMapKind;
import org.biojava.nbio.structure.io.density.DensityMapSource;

/**
 * The detailed density settings: which map, how it is contoured, where it is cached, and in
 * what order the sources are tried.
 * <p>
 * Separate from {@link PreferencesDialogue}, which keeps only the two switches that decide
 * whether any of this happens at all. Separate too from {@code FineTuningDialogue}, which is
 * for the parameters of the search rather than the drawing of a result.
 *
 * @see DensityService
 */
public class DensityOptionsDialogue extends JDialog {

	private static final long serialVersionUID = 1L;

	/** Offered kinds, paired with the file token stored in the settings. */
	private static final String[] KIND_LABELS = {
			"2Fo-Fc (electron density)", "Fo-Fc (difference)", "cryo-EM map", "Whatever is available"};
	private static final String[] KIND_TOKENS = {"2fofc", "fofc", "em", "auto"};

	private final SettingsManager settingsManager = SettingsManager.getSettingsManager();

	private JComboBox<String> kindComboBox;
	private JSpinner contourSpinner;
	private JSpinner radiusSpinner;
	private JTextField cacheFolderTextField;
	private JButton cacheFolderButton;
	private SourceListPanel xraySources;
	private SourceListPanel emSources;

	public DensityOptionsDialogue(JDialog owner) {
		super(owner);
		setTitle("Electron density");
		setBounds(100, 100, 520, 520);

		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[]{140, 240, 90, 0};
		layout.rowHeights = new int[]{23, 23, 23, 23, 23, 200, 0};
		layout.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		layout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		contentPanel.setLayout(layout);

		addLabelled(contentPanel, "Map to show", getKindComboBox(), 0);
		addLabelled(contentPanel, "Contour level (sigma)", getContourSpinner(), 1);
		addLabelled(contentPanel, "Clip radius (Å)", getRadiusSpinner(), 2);

		contentPanel.add(new JLabel("Cache folder"), at(0, 3, 1));
		contentPanel.add(getCacheFolderTextField(), at(1, 3, 1));
		contentPanel.add(getCacheFolderButton(), at(2, 3, 1));

		JLabel sourcesLabel = new JLabel("Sources, in the order they are tried — drag to reorder");
		contentPanel.add(sourcesLabel, at(0, 4, 3));

		JTabbedPane tabs = new JTabbedPane();
		xraySources = new SourceListPanel(SettingsManager.parseSourceChain(
				settingsManager.getDensityXraySources(), DensityMapCache.DEFAULT_XRAY_SOURCE_CHAIN),
				DensityMapCache.DEFAULT_XRAY_SOURCE_CHAIN);
		emSources = new SourceListPanel(SettingsManager.parseSourceChain(
				settingsManager.getDensityEmSources(), DensityMapCache.DEFAULT_EM_SOURCE_CHAIN),
				DensityMapCache.DEFAULT_EM_SOURCE_CHAIN);
		tabs.addTab("X-ray", xraySources);
		tabs.addTab("cryo-EM", emSources);
		contentPanel.add(tabs, at(0, 5, 3));

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		buttonPane.add(restoreDefaultsButton());
		buttonPane.add(okButton());
		buttonPane.add(cancelButton());
	}

	private void addLabelled(JPanel panel, String label, Component field, int row) {
		panel.add(new JLabel(label), at(0, row, 1));
		panel.add(field, at(1, row, 2));
	}

	private static GridBagConstraints at(int x, int y, int width) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(2, 2, 2, 2);
		return c;
	}

	private JComboBox<String> getKindComboBox() {
		if (kindComboBox == null) {
			kindComboBox = new JComboBox<String>(KIND_LABELS);
			kindComboBox.setName("kindComboBox");
			kindComboBox.setSelectedIndex(indexOfToken(settingsManager.getDensityMapKind()));
		}
		return kindComboBox;
	}

	private JSpinner getContourSpinner() {
		if (contourSpinner == null) {
			contourSpinner = new JSpinner(new SpinnerNumberModel(
					settingsManager.getDensityContourSigma(), 0.1, 10.0, 0.1));
			contourSpinner.setName("contourSpinner");
			contourSpinner.setToolTipText("How many standard deviations above the mean the surface "
					+ "is drawn at. A cryo-EM map uses its depositors' level instead.");
		}
		return contourSpinner;
	}

	private JSpinner getRadiusSpinner() {
		if (radiusSpinner == null) {
			radiusSpinner = new JSpinner(new SpinnerNumberModel(
					settingsManager.getDensityClipRadius(), 1.0, 50.0, 0.5));
			radiusSpinner.setName("radiusSpinner");
			radiusSpinner.setToolTipText("How far around the interacting atoms to contour. "
					+ "Contouring a whole map can take long enough to look like a freeze.");
		}
		return radiusSpinner;
	}

	private JTextField getCacheFolderTextField() {
		if (cacheFolderTextField == null) {
			cacheFolderTextField = new JTextField(settingsManager.getDensityCacheFolder());
			cacheFolderTextField.setName("cacheFolderTextField");
			cacheFolderTextField.setToolTipText("Leave empty to keep maps beside the PDB store, "
					+ "in BioJava's own cache folder.");
		}
		return cacheFolderTextField;
	}

	private JButton getCacheFolderButton() {
		if (cacheFolderButton == null) {
			cacheFolderButton = new JButton("Browse");
			cacheFolderButton.setName("cacheFolderButton");
			cacheFolderButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser chooser = new JFileChooser();
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					String current = getCacheFolderTextField().getText();
					if (current != null && ! current.trim().isEmpty()) {
						chooser.setCurrentDirectory(new File(current));
					}
					if (chooser.showOpenDialog(DensityOptionsDialogue.this) == JFileChooser.APPROVE_OPTION) {
						getCacheFolderTextField().setText(chooser.getSelectedFile().getAbsolutePath());
					}
				}
			});
		}
		return cacheFolderButton;
	}

	private JButton okButton() {
		JButton ok = new JButton("OK");
		ok.setName("okButton");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					apply();
					settingsManager.saveSettings(false);
					dispose();
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(DensityOptionsDialogue.this, ex.getMessage(),
							"Can't satisfy request", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		getRootPane().setDefaultButton(ok);
		return ok;
	}

	private JButton cancelButton() {
		JButton cancel = new JButton("Cancel");
		cancel.setName("cancelButton");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		return cancel;
	}

	private JButton restoreDefaultsButton() {
		JButton restore = new JButton("Restore defaults");
		restore.setName("restoreDefaultsButton");
		restore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				restoreDefaults();
			}
		});
		return restore;
	}

	/**
	 * Writes the controls into the settings. Package-private so a test can drive it without
	 * pressing OK.
	 *
	 * @throws IOException if the cache folder cannot be created
	 */
	void apply() throws IOException {
		settingsManager.setDensityMapKind(KIND_TOKENS[getKindComboBox().getSelectedIndex()]);
		settingsManager.setDensityContourSigma(((Number) getContourSpinner().getValue()).doubleValue());
		settingsManager.setDensityClipRadius(((Number) getRadiusSpinner().getValue()).doubleValue());
		settingsManager.setDensityCacheFolder(getCacheFolderTextField().getText());
		settingsManager.setDensityXraySources(SettingsManager.formatSourceChain(xraySources.enabledSources()));
		settingsManager.setDensityEmSources(SettingsManager.formatSourceChain(emSources.enabledSources()));
	}

	/** Resets the controls only; nothing is saved until OK, so Cancel still abandons it. */
	void restoreDefaults() {
		getKindComboBox().setSelectedIndex(indexOfToken(SettingsManager.DEFAULT_DENSITY_MAP_KIND));
		getContourSpinner().setValue(Double.valueOf(SettingsManager.DEFAULT_DENSITY_CONTOUR_SIGMA));
		getRadiusSpinner().setValue(Double.valueOf(SettingsManager.DEFAULT_DENSITY_CLIP_RADIUS));
		getCacheFolderTextField().setText(SettingsManager.DEFAULT_DENSITY_CACHE_FOLDER);
		xraySources.restoreDefaults();
		emSources.restoreDefaults();
	}

	private static int indexOfToken(String token) {
		for (int i = 0; i < KIND_TOKENS.length; i++) {
			if (KIND_TOKENS[i].equalsIgnoreCase(token)) {
				return i;
			}
		}
		return 0;
	}

	/** One source, and whether it is in the chain. */
	static class SourceEntry {
		final DensityMapSource source;
		boolean enabled;

		SourceEntry(DensityMapSource source, boolean enabled) {
			this.source = source;
			this.enabled = enabled;
		}

		/**
		 * @return false for a source that serves structure factors rather than a sampled
		 *         grid, which no viewer can contour as it stands
		 */
		boolean isSelectable() {
			return source != DensityMapSource.WWPDB_MAP_COEFFICIENTS;
		}

		@Override
		public String toString() {
			return source.name().replace('_', ' ').toLowerCase();
		}
	}

	/**
	 * A reorderable list of sources: checkbox to include, drag to move.
	 * <p>
	 * The reordering itself is {@link #moveEntry}, a plain method the drag handler merely
	 * calls, so it can be tested without synthesising a drag - which this project's
	 * reflection-driven, headless harnesses cannot do.
	 */
	static class SourceListPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private final DefaultListModel<SourceEntry> model = new DefaultListModel<SourceEntry>();
		private final JList<SourceEntry> list = new JList<SourceEntry>(model);
		private final List<DensityMapSource> candidates;

		SourceListPanel(List<DensityMapSource> chain, List<DensityMapSource> candidates) {
			super(new BorderLayout());
			this.candidates = candidates;
			fill(chain);

			list.setName("sourceList");
			list.setCellRenderer(new SourceEntryRenderer());
			list.setDragEnabled(true);
			list.setDropMode(DropMode.INSERT);
			list.setTransferHandler(new ReorderHandler());
			//A click on the box includes or excludes the source; anywhere else just selects.
			list.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					int index = list.locationToIndex(e.getPoint());
					if (index < 0 || e.getX() > 24) {
						return;
					}
					SourceEntry entry = model.get(index);
					if (entry.isSelectable()) {
						entry.enabled = ! entry.enabled;
						list.repaint();
					}
				}
			});
			add(new JScrollPane(list), BorderLayout.CENTER);
			setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		}

		/** Chain members first, in order; then the rest, unchecked, so they can be added. */
		private void fill(List<DensityMapSource> chain) {
			model.clear();
			for (DensityMapSource source : chain) {
				model.addElement(new SourceEntry(source, source != DensityMapSource.WWPDB_MAP_COEFFICIENTS));
			}
			for (DensityMapSource source : candidates) {
				if (! chain.contains(source)) {
					model.addElement(new SourceEntry(source, false));
				}
			}
		}

		void restoreDefaults() {
			fill(candidates);
			list.repaint();
		}

		/** @return the ticked sources, in the order shown */
		List<DensityMapSource> enabledSources() {
			List<DensityMapSource> enabled = new ArrayList<DensityMapSource>();
			for (int i = 0; i < model.size(); i++) {
				SourceEntry entry = model.get(i);
				if (entry.enabled) {
					enabled.add(entry.source);
				}
			}
			return enabled;
		}

		DefaultListModel<SourceEntry> getModel() {
			return model;
		}

		JList<SourceEntry> getList() {
			return list;
		}

		/**
		 * Moves one row, the whole of what a drag does.
		 * <p>
		 * {@code to} is a drop position between rows, so removing the dragged row first
		 * shifts every later position down by one - which is why the index is adjusted
		 * rather than used as given. Getting this wrong moves the row one place too far
		 * whenever it travels downwards.
		 *
		 * @param model the list being reordered
		 * @param from index of the row to move
		 * @param to insertion point before the move
		 */
		static void moveEntry(DefaultListModel<SourceEntry> model, int from, int to) {
			if (from < 0 || from >= model.size() || to < 0 || to > model.size() || from == to) {
				return;
			}
			SourceEntry entry = model.remove(from);
			model.add(to > from ? to - 1 : to, entry);
		}

		/** Drag to reorder, within this list only. */
		private class ReorderHandler extends TransferHandler {

			private static final long serialVersionUID = 1L;
			private int draggedFrom = -1;

			@Override
			public int getSourceActions(javax.swing.JComponent c) {
				return MOVE;
			}

			@Override
			protected java.awt.datatransfer.Transferable createTransferable(javax.swing.JComponent c) {
				draggedFrom = list.getSelectedIndex();
				return new java.awt.datatransfer.StringSelection(String.valueOf(draggedFrom));
			}

			@Override
			public boolean canImport(TransferSupport support) {
				return support.isDrop() && draggedFrom >= 0;
			}

			@Override
			public boolean importData(TransferSupport support) {
				if (! canImport(support)) {
					return false;
				}
				JList.DropLocation drop = (JList.DropLocation) support.getDropLocation();
				moveEntry(model, draggedFrom, drop.getIndex());
				draggedFrom = -1;
				list.repaint();
				return true;
			}
		}
	}

	/** Draws a source as a checkbox, greyed out when nothing could be drawn from it. */
	static class SourceEntryRenderer extends JCheckBox implements ListCellRenderer<SourceEntry> {

		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(JList<? extends SourceEntry> list, SourceEntry value,
				int index, boolean isSelected, boolean cellHasFocus) {
			setText(String.valueOf(value));
			setSelected(value.enabled);
			setEnabled(value.isSelectable());
			setToolTipText(value.isSelectable() ? null
					: "Serves structure factors, not a sampled map. These need a Fourier transform "
							+ "before anything can be drawn from them.");
			setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			setOpaque(true);
			return this;
		}
	}

	/** @return the kind the settings currently name, for callers that want it resolved */
	public static DensityMapKind configuredKind(SettingsManager settings) {
		return DensityService.kindForToken(settings.getDensityMapKind());
	}
}
