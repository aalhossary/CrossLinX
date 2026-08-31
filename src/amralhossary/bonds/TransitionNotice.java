package amralhossary.bonds;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;

import javax.swing.JOptionPane;

/**
 * A reminder, from a date this build cannot see past, that the PDB service URLs need checking
 * again.
 * <p>
 * On 21 July 2027 the PDB archive is replaced by the extended-identifier archive. The part of
 * that which could be settled in advance has been: both archive layouts are read, and every URL
 * this program builds addresses an entry by identifier rather than by archive path, so none of
 * them is invalidated by the move.
 * <p>
 * One thing could not be settled, because it depends on what two services do in the future
 * rather than on anything here. When this was written, the RCSB and PDBe volume servers accepted
 * the four-character spelling only: {@code 1cbs} returned 200 and {@code pdb_00001cbs} returned
 * 404, from both. Every entry that existed then had a four-character form, so nothing was
 * affected. Entries deposited once that space is exhausted will not have one, and until the
 * servers accept the extended spelling there is no density box that can be asked for.
 * <p>
 * Rather than fail quietly in that case, this says so, from {@link #REVIEW_DATE} - chosen a
 * little over two months before the cutover, so that whoever is using it has time to act.
 *
 * @author Amr ALHOSSARY
 */
public class TransitionNotice {

	/** When this build starts asking to be checked: 5 May 2027. */
	public static final LocalDate REVIEW_DATE = LocalDate.of(2027, 5, 5);

	/** When the archive is replaced, per the wwPDB. */
	public static final LocalDate ARCHIVE_CUTOVER = LocalDate.of(2027, 7, 21);

	/** Where to report what the services now do. */
	public static final String PROJECT_URL = "https://github.com/aalhossary/CrossLinX";

	private static boolean shown;

	private TransitionNotice() {
	}

	/** @return whether the reminder is due today */
	public static boolean isDue() {
		return isDue(LocalDate.now());
	}

	/**
	 * @param on the date to judge by
	 * @return whether the reminder is due on that date
	 */
	public static boolean isDue(LocalDate on) {
		return ! on.isBefore(REVIEW_DATE);
	}

	/** @return the reminder, as plain text */
	public static String text() {
		return "The PDB archive changes on " + ARCHIVE_CUTOVER + ", and this build predates it.\n"
				+ "\n"
				+ "Handled already: both archive layouts are read, and every service URL here\n"
				+ "addresses an entry by its identifier rather than by an archive path, so the\n"
				+ "change of layout does not invalidate them.\n"
				+ "\n"
				+ "Needs checking: when this version was written, the RCSB and PDBe volume\n"
				+ "servers accepted only the four-character identifier. \"1cbs\" returned 200 and\n"
				+ "\"pdb_00001cbs\" returned 404, from both. Entries deposited after the\n"
				+ "four-character space is exhausted have no short form, so until those servers\n"
				+ "accept the extended spelling, no density map can be requested for them.\n"
				+ "\n"
				+ "Please update CrossLinX to a newer version if one exists. If none does, or if\n"
				+ "you can tell whether the servers now accept the extended spelling, contact the\n"
				+ "author: " + PROJECT_URL;
	}

	/**
	 * Prints the reminder if it is due, once per run.
	 * <p>
	 * Used by the command line, and by the graphical version when it has no display to put a
	 * dialog on.
	 */
	public static synchronized void warnOnCommandLine() {
		if (! isDue() || shown) {
			return;
		}
		shown = true;
		System.err.println();
		System.err.println("=========================================================================");
		System.err.println(" CrossLinX: the PDB service URLs need checking");
		System.err.println("=========================================================================");
		System.err.println(text());
		System.err.println("=========================================================================");
		System.err.println();
	}

	/**
	 * Shows the reminder if it is due, once per run.
	 * <p>
	 * Falls back to the command-line form when there is no display, so that a headless or
	 * scripted run still says it rather than blocking on a dialog nobody can dismiss.
	 *
	 * @param parent the window to attach to, may be <code>null</code>
	 */
	public static synchronized void warnInGui(Component parent) {
		if (! isDue()) {
			return;
		}
		if (GraphicsEnvironment.isHeadless()) {
			warnOnCommandLine();
			return;
		}
		if (shown) {
			return;
		}
		shown = true;
		JOptionPane.showMessageDialog(parent, text(),
				"CrossLinX: the PDB service URLs need checking", JOptionPane.WARNING_MESSAGE);
	}

	/** Lets a test show it again. */
	static synchronized void forget() {
		shown = false;
	}
}
