package amralhossary.bonds;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.biojava.nbio.structure.align.util.UserConfiguration;


public class PreferencesDialogue extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1182914638147787256L;
	private final JPanel contentPanel = new JPanel();
	private JButton btnOkButton;
	private JButton btnCancelButton;
	private JTextField pdbFilesDirectoryTextField;
	private JButton pdbFilesButton;
	private JTextField workingFolderTextField;
	private JButton selectWorkingFolderButton;
	private JLabel lblNewLabel;
	private JLabel lblWorkingFolder;
	private JCheckBox autofetchCheckbox;
	private JCheckBox showWhileProcessingCheckbox;

	SettingsManager settingsManager = SettingsManager.getSettingsManager();
	private JCheckBox domainEnabledCheckBox;
	private final ButtonGroup fileFormatsButtonGroup = new ButtonGroup();
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			PreferencesDialogue dialog = new PreferencesDialogue();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public PreferencesDialogue() {
		setBounds(100, 100, 450, 320);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{304, 97, 0};
		gbl_contentPanel.rowHeights = new int[]{14, 23, 14, 23, 23, 23, 72, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		contentPanel.add(getLblNewLabel(), gbc_lblNewLabel);
		GridBagConstraints gbc_pdbFilesDirectoryTextField = new GridBagConstraints();
		gbc_pdbFilesDirectoryTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_pdbFilesDirectoryTextField.insets = new Insets(0, 0, 5, 5);
		gbc_pdbFilesDirectoryTextField.gridx = 0;
		gbc_pdbFilesDirectoryTextField.gridy = 1;
		contentPanel.add(getPdbFilesDirectoryTextField(), gbc_pdbFilesDirectoryTextField);
		GridBagConstraints gbc_pdbFilesButton = new GridBagConstraints();
		gbc_pdbFilesButton.anchor = GridBagConstraints.NORTH;
		gbc_pdbFilesButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_pdbFilesButton.insets = new Insets(0, 0, 5, 0);
		gbc_pdbFilesButton.gridx = 1;
		gbc_pdbFilesButton.gridy = 1;
		contentPanel.add(getPdbFilesButton(), gbc_pdbFilesButton);
		GridBagConstraints gbc_lblWorkingFolder = new GridBagConstraints();
		gbc_lblWorkingFolder.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblWorkingFolder.insets = new Insets(0, 0, 5, 5);
		gbc_lblWorkingFolder.gridx = 0;
		gbc_lblWorkingFolder.gridy = 2;
		contentPanel.add(getLblWorkingFolder(), gbc_lblWorkingFolder);
		GridBagConstraints gbc_workingFolderTextField = new GridBagConstraints();
		gbc_workingFolderTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_workingFolderTextField.insets = new Insets(0, 0, 5, 5);
		gbc_workingFolderTextField.gridx = 0;
		gbc_workingFolderTextField.gridy = 3;
		contentPanel.add(getWorkingFolderTextField(), gbc_workingFolderTextField);
		GridBagConstraints gbc_selectWorkingFolderButton = new GridBagConstraints();
		gbc_selectWorkingFolderButton.anchor = GridBagConstraints.NORTH;
		gbc_selectWorkingFolderButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_selectWorkingFolderButton.insets = new Insets(0, 0, 5, 0);
		gbc_selectWorkingFolderButton.gridx = 1;
		gbc_selectWorkingFolderButton.gridy = 3;
		contentPanel.add(getSelectWorkingFolderButton(), gbc_selectWorkingFolderButton);
		GridBagConstraints gbc_autofetchCheckbox = new GridBagConstraints();
		gbc_autofetchCheckbox.anchor = GridBagConstraints.NORTH;
		gbc_autofetchCheckbox.fill = GridBagConstraints.HORIZONTAL;
		gbc_autofetchCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_autofetchCheckbox.gridx = 0;
		gbc_autofetchCheckbox.gridy = 4;
		contentPanel.add(getAutofetchCheckbox(), gbc_autofetchCheckbox);
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Files Format", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridheight = 3;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 4;
		contentPanel.add(panel, gbc_panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JRadioButton rdbtnNewRadioButton = new JRadioButton(UserConfiguration.PDB_FORMAT);
		fileFormatsButtonGroup.add(rdbtnNewRadioButton);
		panel.add(rdbtnNewRadioButton);
		
		JRadioButton rdbtnNewRadioButton_1 = new JRadioButton(UserConfiguration.MMCIF_FORMAT);
		fileFormatsButtonGroup.add(rdbtnNewRadioButton_1);
		panel.add(rdbtnNewRadioButton_1);
		
		JRadioButton rdbtnNewRadioButton_2 = new JRadioButton(UserConfiguration.MMTF_FORMAT);
		fileFormatsButtonGroup.add(rdbtnNewRadioButton_2);
		rdbtnNewRadioButton_2.setEnabled(false);
		panel.add(rdbtnNewRadioButton_2);
		
		JRadioButton rdbtnNewRadioButton_3 = new JRadioButton(UserConfiguration.BCIF_FORMAT);
		fileFormatsButtonGroup.add(rdbtnNewRadioButton_3);
		rdbtnNewRadioButton_3.setEnabled(false);
		panel.add(rdbtnNewRadioButton_3);
		GridBagConstraints gbc_showWhileProcessingCheckbox = new GridBagConstraints();
		gbc_showWhileProcessingCheckbox.anchor = GridBagConstraints.NORTHWEST;
		gbc_showWhileProcessingCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_showWhileProcessingCheckbox.gridx = 0;
		gbc_showWhileProcessingCheckbox.gridy = 5;
		contentPanel.add(getShowWhileProcessingCheckbox(), gbc_showWhileProcessingCheckbox);
		GridBagConstraints gbc_domainEnabledCheckBox = new GridBagConstraints();
		gbc_domainEnabledCheckBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_domainEnabledCheckBox.insets = new Insets(0, 0, 0, 5);
		gbc_domainEnabledCheckBox.gridx = 0;
		gbc_domainEnabledCheckBox.gridy = 6;
		contentPanel.add(getDomainEnabledCheckBox(), gbc_domainEnabledCheckBox);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			buttonPane.add(getBtnOkButton());
			buttonPane.add(getBtnCancelButton());
			
		}
		Enumeration<AbstractButton> fileFormatAbstractButtons = fileFormatsButtonGroup.getElements();
		while (fileFormatAbstractButtons.hasMoreElements()) {
			AbstractButton abstractButton = (AbstractButton) fileFormatAbstractButtons.nextElement();
			String fileFormat = settingsManager.getFileFormat();
			if (fileFormat.equals(abstractButton.getText())) {
				abstractButton.setSelected(true);
				break;
			}
		}
	}

	private JButton getBtnOkButton() {
		if (btnOkButton == null) {
			btnOkButton = new JButton("OK");
			btnOkButton.setName("btnOkButton");
			btnOkButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						//MUST be the FIRST setting to be saved, because it creates a NEW UserConfiguration object
						settingsManager.setPdbFilePath(pdbFilesDirectoryTextField.getText());
						settingsManager.setWorkingFolder(workingFolderTextField.getText());
						String fileFormat = null;
						Enumeration<AbstractButton> fileFormatAbstractButtons = fileFormatsButtonGroup.getElements();
						while(fileFormatAbstractButtons.hasMoreElements()) {
							AbstractButton button = fileFormatAbstractButtons.nextElement();
							if (button.isSelected()) {
								fileFormat = button.getText();
								break;
							}
						}
						settingsManager.setFileFormat(fileFormat);
						settingsManager.setAutoFetch(autofetchCheckbox.isSelected());
						settingsManager.setShowWhileProcessing(showWhileProcessingCheckbox.isSelected());
						settingsManager.setDomainEnabled(domainEnabledCheckBox.isSelected());
						settingsManager.saveSettings(false);
						dispose();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(PreferencesDialogue.this, e1.getMessage(), "Can't satisfy request", JOptionPane.ERROR_MESSAGE);
						e1.printStackTrace();
					}
				}
			});
			getRootPane().setDefaultButton(btnOkButton);
		}
		return btnOkButton;
	}
	private JButton getBtnCancelButton() {
		if (btnCancelButton == null) {
			btnCancelButton = new JButton("Cancel");
			btnCancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			btnCancelButton.setName("btnCancelButton");
		}
		return btnCancelButton;
	}
	private JTextField getPdbFilesDirectoryTextField() {
		if (pdbFilesDirectoryTextField == null) {
			pdbFilesDirectoryTextField = new JTextField();
			pdbFilesDirectoryTextField.setName("pdbFilesDirectoryTextField");
			String pdbFilePath = settingsManager.getPdbFilePath();
			if(pdbFilePath.endsWith("/") || pdbFilePath.endsWith(File.separator))
				pdbFilePath = pdbFilePath.substring(0, pdbFilePath.length() - 1);
			pdbFilesDirectoryTextField.setText(pdbFilePath);
			pdbFilesDirectoryTextField.setColumns(30);
		}
		return pdbFilesDirectoryTextField;
	}
	private JButton getPdbFilesButton() {
		if (pdbFilesButton == null) {
			pdbFilesButton = new JButton("Browse");
			pdbFilesButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser(pdbFilesDirectoryTextField.getText());
					fileChooser.setDialogTitle("Select Local PDB files folder");
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int option = fileChooser.showOpenDialog(PreferencesDialogue.this);
					if (option== JFileChooser.APPROVE_OPTION) {
						String path = fileChooser.getSelectedFile().getAbsolutePath();
						System.out.println("Local Folder:");
						System.out.println(path);
						pdbFilesDirectoryTextField.setText(path);
					}
				}
			});
			pdbFilesButton.setName("pdbFilesButton");
		}
		return pdbFilesButton;
	}
	private JTextField getWorkingFolderTextField() {
		if (workingFolderTextField == null) {
			workingFolderTextField = new JTextField();
			workingFolderTextField.setColumns(30);
			String workfolderPath = settingsManager.getWorkingFolder();
			if(workfolderPath.endsWith("/") || workfolderPath.endsWith(File.separator))
				workfolderPath = workfolderPath.substring(0, workfolderPath.length() - 1);
			workingFolderTextField.setText(workfolderPath);
			workingFolderTextField.setName("workingFolderTextField");
		}
		return workingFolderTextField;
	}
	private JButton getSelectWorkingFolderButton() {
		if (selectWorkingFolderButton == null) {
			selectWorkingFolderButton = new JButton("Browse");
			selectWorkingFolderButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser(getWorkingFolderTextField().getText());
					fileChooser.setDialogTitle("Select working folder");
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int option = fileChooser.showOpenDialog(PreferencesDialogue.this);
					if (option== JFileChooser.APPROVE_OPTION) {
						String path = fileChooser.getSelectedFile().getAbsolutePath();
						System.out.println("working Folder:");
						System.out.println(path);
						workingFolderTextField.setText(path);
					}
				}
			});
			selectWorkingFolderButton.setName("pdbFilesButton");
		}
		return selectWorkingFolderButton;
	}
	private JLabel getLblNewLabel() {
		if (lblNewLabel == null) {
			lblNewLabel = new JLabel("PDB Files Folder");
			lblNewLabel.setName("lblNewLabel");
		}
		return lblNewLabel;
	}
	private JLabel getLblWorkingFolder() {
		if (lblWorkingFolder == null) {
			lblWorkingFolder = new JLabel("Working Folder");
			lblWorkingFolder.setName("lblWorkingFolder");
		}
		return lblWorkingFolder;
	}
	private JCheckBox getAutofetchCheckbox() {
		if (autofetchCheckbox == null) {
			autofetchCheckbox = new JCheckBox("Autofetch Files");
			autofetchCheckbox.setSelected(settingsManager.isAutoFetch());
			autofetchCheckbox.setName("autofetchCheckbox");
		}
		return autofetchCheckbox;
	}
	private JCheckBox getShowWhileProcessingCheckbox() {
		if (showWhileProcessingCheckbox == null) {
			showWhileProcessingCheckbox = new JCheckBox("Show Files While Processing (slower)");
			showWhileProcessingCheckbox.setSelected(settingsManager.isShowWhileProcessing());
			showWhileProcessingCheckbox.setName("showWhileProcessingCheckbox");
		}
		return showWhileProcessingCheckbox;
	}
	private JCheckBox getDomainEnabledCheckBox() {
		if (domainEnabledCheckBox == null) {
			domainEnabledCheckBox = new JCheckBox("enable viewing domains");
			domainEnabledCheckBox.setSelected(settingsManager.isDomainEnabled());
			domainEnabledCheckBox.setName("domainEnabledCheckBox");
		}
		return domainEnabledCheckBox;
	}
}
