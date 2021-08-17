package amralhossary.bonds;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

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
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		contentPanel.add(getPdbFilesDirectoryTextField());
		contentPanel.add(getPdbFilesButton());
		contentPanel.add(getWorkingFolderTextField());
		contentPanel.add(getSelectWorkingFolderButton());
		contentPanel.add(getLblNewLabel());
		contentPanel.add(getLblWorkingFolder());
		contentPanel.add(getAutofetchCheckbox());
		contentPanel.add(getShowWhileProcessingCheckbox());
		contentPanel.add(getDomainEnabledCheckBox());
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			buttonPane.add(getBtnOkButton());
			buttonPane.add(getBtnCancelButton());
			
		}
	}

	private JButton getBtnOkButton() {
		if (btnOkButton == null) {
			btnOkButton = new JButton("OK");
			btnOkButton.setName("btnOkButton");
			btnOkButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						settingsManager.setPdbFilePath(pdbFilesDirectoryTextField.getText());
						settingsManager.setWorkingFolder(workingFolderTextField.getText());
						settingsManager.setAutoFetch(autofetchCheckbox.isSelected());
						settingsManager.setShowWhileProcessing(showWhileProcessingCheckbox.isSelected());
						settingsManager.setDomainEnabled(domainEnabledCheckBox.isSelected());
						settingsManager.saveSettings(false);
						dispose();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(PreferencesDialogue.this, e1.getMessage(), "can't satsfy request", JOptionPane.ERROR_MESSAGE);
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
			pdbFilesDirectoryTextField.setBounds(10, 36, 296, 20);
			pdbFilesDirectoryTextField.setName("pdbFilesDirectoryTextField");
			pdbFilesDirectoryTextField.setText(settingsManager.getPdbFilePath());
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
			pdbFilesButton.setBounds(324, 35, 89, 23);
			pdbFilesButton.setName("pdbFilesButton");
		}
		return pdbFilesButton;
	}
	private JTextField getWorkingFolderTextField() {
		if (workingFolderTextField == null) {
			workingFolderTextField = new JTextField();
			workingFolderTextField.setColumns(30);
			workingFolderTextField.setBounds(10, 90, 296, 20);
			workingFolderTextField.setText(settingsManager.getWorkingFolder());
			workingFolderTextField.setName("workingFolderTextField");
		}
		return workingFolderTextField;
	}
	private JButton getSelectWorkingFolderButton() {
		if (selectWorkingFolderButton == null) {
			selectWorkingFolderButton = new JButton("Browse");
			selectWorkingFolderButton.setBounds(324, 89, 89, 23);
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
			lblNewLabel.setBounds(10, 11, 115, 14);
			lblNewLabel.setName("lblNewLabel");
		}
		return lblNewLabel;
	}
	private JLabel getLblWorkingFolder() {
		if (lblWorkingFolder == null) {
			lblWorkingFolder = new JLabel("Working Folder");
			lblWorkingFolder.setBounds(10, 67, 115, 14);
			lblWorkingFolder.setName("lblWorkingFolder");
		}
		return lblWorkingFolder;
	}
	private JCheckBox getAutofetchCheckbox() {
		if (autofetchCheckbox == null) {
			autofetchCheckbox = new JCheckBox("Autofetch Files");
			autofetchCheckbox.setSelected(false);
//			autofetchCheckbox.setEnabled(false);
			autofetchCheckbox.setBounds(10, 129, 115, 23);
			autofetchCheckbox.setName("autofetchCheckbox");
		}
		return autofetchCheckbox;
	}
	private JCheckBox getShowWhileProcessingCheckbox() {
		if (showWhileProcessingCheckbox == null) {
			showWhileProcessingCheckbox = new JCheckBox("Show Files While Processing (slower)");
			showWhileProcessingCheckbox.setSelected(settingsManager.isShowWhileProcessing());
			showWhileProcessingCheckbox.setBounds(10, 162, 260, 23);
			showWhileProcessingCheckbox.setName("showWhileProcessingCheckbox");
		}
		return showWhileProcessingCheckbox;
	}
	private JCheckBox getDomainEnabledCheckBox() {
		if (domainEnabledCheckBox == null) {
			domainEnabledCheckBox = new JCheckBox("enable viewing domains");
			domainEnabledCheckBox.setSelected(settingsManager.isDomainEnabled());
			domainEnabledCheckBox.setBounds(10, 199, 260, 23);
			domainEnabledCheckBox.setName("domainEnabledCheckBox");
		}
		return domainEnabledCheckBox;
	}
}
