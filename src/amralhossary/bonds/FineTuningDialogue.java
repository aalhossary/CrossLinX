package amralhossary.bonds;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
//import javax.swing.JLabel;
import javax.swing.JPanel;
//import javax.swing.JSpinner;
//import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

public class FineTuningDialogue extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 452572349167862685L;
	private final JPanel contentPanel = new JPanel();
//	private JSpinner hexRingDiameterSpinner;
//	private JLabel lblNewLabel;
//	private JLabel lblHexagonalDiametercutoffRatio;
//	private JSpinner hexDiameterToCutoffSpinner;
//	private JSpinner pentDiameterToCutoffSpinner;
//	private JSpinner pentRingDiameterSpinner;
//	private JLabel lblPentagonalRingDiameter;
//	private JLabel lblPentagonalDiametercutoffRatio;

	private SettingsManager settingsManager;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			FineTuningDialogue dialog = new FineTuningDialogue();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public FineTuningDialogue() {
		settingsManager = SettingsManager.getSettingsManager();
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
//		contentPanel.add(getHexRingDiameterSpinner());
//		contentPanel.add(getLblNewLabel());
//		contentPanel.add(getLblHexagonalDiametercutoffRatio());
//		contentPanel.add(getHexDiameterToCutoffSpinner());
//		contentPanel.add(getPentDiameterToCutoffSpinner());
//		contentPanel.add(getPentRingDiameterSpinner());
//		contentPanel.add(getLblPentagonalRingDiameter());
//		contentPanel.add(getLblPentagonalDiametercutoffRatio());
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
//						settingsManager.setHexagonalRingDiameter((Double) hexRingDiameterSpinner.getValue());
//						settingsManager.setPentagonalRingDiameter((Double) pentRingDiameterSpinner.getValue());
//						settingsManager.setHexagonalRingDiameterToCutoffRatio((Double) hexDiameterToCutoffSpinner.getValue());
//						settingsManager.setPentagonalRingDiameterToCutoffRatio((Double) pentDiameterToCutoffSpinner.getValue());
						settingsManager.saveSettings(false);
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}
//	private JSpinner getHexRingDiameterSpinner() {
//		if (hexRingDiameterSpinner == null) {
//			hexRingDiameterSpinner = new JSpinner();
//			hexRingDiameterSpinner.setBounds(269, 7, 80, 20);
//			hexRingDiameterSpinner.setModel(new SpinnerNumberModel(settingsManager.getHexagonalRingDiameter(), 0.5, 5.0, 0.15));
//			hexRingDiameterSpinner.setName("hexRingDiameterSpinner");
//		}
//		return hexRingDiameterSpinner;
//	}
//	private JLabel getLblNewLabel() {
//		if (lblNewLabel == null) {
//			lblNewLabel = new JLabel("Hexagonal Ring Diameter");
//			lblNewLabel.setBounds(12, 11, 177, 16);
//			lblNewLabel.setName("lblNewLabel");
//		}
//		return lblNewLabel;
//	}
//	private JLabel getLblHexagonalDiametercutoffRatio() {
//		if (lblHexagonalDiametercutoffRatio == null) {
//			lblHexagonalDiametercutoffRatio = new JLabel("Hexagonal Diameter/CutOff ratio");
//			lblHexagonalDiametercutoffRatio.setBounds(12, 55, 211, 16);
//			lblHexagonalDiametercutoffRatio.setName("lblHexagonalDiametercutoffRatio");
//		}
//		return lblHexagonalDiametercutoffRatio;
//	}
//	private JSpinner getHexDiameterToCutoffSpinner() {
//		if (hexDiameterToCutoffSpinner == null) {
//			hexDiameterToCutoffSpinner = new JSpinner();
//			hexDiameterToCutoffSpinner.setModel(new SpinnerNumberModel(settingsManager.getHexagonalRingDiameterToCutoffRatio(), 0.0, 4.0, 0.1));
//			hexDiameterToCutoffSpinner.setBounds(269, 51, 80, 20);
//			hexDiameterToCutoffSpinner.setName("hexDiameterToCutoffSpinner");
//		}
//		return hexDiameterToCutoffSpinner;
//	}
//	private JSpinner getPentRingDiameterSpinner() {
//		if (pentRingDiameterSpinner == null) {
//			pentRingDiameterSpinner = new JSpinner();
//			pentRingDiameterSpinner.setBounds(269, 124, 80, 20);
//			pentRingDiameterSpinner.setModel(new SpinnerNumberModel(settingsManager.getPentagonalRingDiameter(), 0.5, 5.0, 0.15));
//			pentRingDiameterSpinner.setName("pentRingDiameterSpinner");
//		}
//		return pentRingDiameterSpinner;
//	}
//	private JSpinner getPentDiameterToCutoffSpinner() {
//		if (pentDiameterToCutoffSpinner == null) {
//			pentDiameterToCutoffSpinner = new JSpinner();
//			pentDiameterToCutoffSpinner.setBounds(269, 168, 80, 20);
//			pentDiameterToCutoffSpinner.setModel(new SpinnerNumberModel(settingsManager.getPentagonalRingDiameterToCutoffRatio(), 0.0, 4.0, 0.1));
//			pentDiameterToCutoffSpinner.setName("pentDiameterToCutoffSpinner");
//		}
//		return pentDiameterToCutoffSpinner;
//	}
//	private JLabel getLblPentagonalRingDiameter() {
//		if (lblPentagonalRingDiameter == null) {
//			lblPentagonalRingDiameter = new JLabel("Pentagonal Ring Diameter");
//			lblPentagonalRingDiameter.setBounds(12, 128, 177, 16);
//			lblPentagonalRingDiameter.setName("lblPentagonalRingDiameter");
//		}
//		return lblPentagonalRingDiameter;
//	}
//	private JLabel getLblPentagonalDiametercutoffRatio() {
//		if (lblPentagonalDiametercutoffRatio == null) {
//			lblPentagonalDiametercutoffRatio = new JLabel("Pentagonal Diameter/CutOff ratio");
//			lblPentagonalDiametercutoffRatio.setBounds(12, 172, 211, 16);
//			lblPentagonalDiametercutoffRatio.setName("lblPentagonalDiametercutoffRatio");
//		}
//		return lblPentagonalDiametercutoffRatio;
//	}
}
