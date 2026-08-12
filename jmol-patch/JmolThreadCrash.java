import org.jmol.api.JmolViewer;
import org.jmol.adapter.smarter.SmarterJmolAdapter;

/**
 * Minimal reproduction: loading models into ONE JmolViewer from more than one
 * thread corrupts the viewer's model set.
 *
 * No data files, no GUI, no third-party libraries - only Jmol.
 *
 *   javac -cp Jmol.jar JmolThreadCrash.java
 *   java  -cp Jmol.jar:. JmolThreadCrash
 *
 * Expected: some threads fail inside Jmol, typically
 *   java.lang.NullPointerException: Cannot read field "vwr" because "modelSet" is null
 *       at org.jmol.modelsetbio.BioModel.<init>(BioModel.java:82)
 * or
 *   java.lang.NullPointerException: Cannot invoke
 *       "org.jmol.modelsetbio.BioModelSet.calculateAllStructuresExcept(...)"
 *       because "this.bioModelset" is null
 *
 * Running the same loads one after another on a single thread never fails.
 */
public class JmolThreadCrash {

    static final int THREADS = 4;
    static final int ROUNDS = 15;
    static final int RESIDUES = 400;   // a bigger model widens the window

    public static void main(String[] args) throws Exception {
        final String pdb = polyalanine(RESIDUES);
        final JmolViewer viewer = JmolViewer.allocateViewer(null, new SmarterJmolAdapter());

        System.out.println("Jmol      : " + JmolViewer.getJmolVersion());
        System.out.println("java      : " + System.getProperty("java.version"));
        System.out.println("model     : poly-ALA, " + RESIDUES + " residues");

        System.out.println("\n== single thread, " + (THREADS * ROUNDS) + " sequential loads ==");
        System.out.println("failures: " + load(viewer, pdb, 1, THREADS * ROUNDS));

        System.out.println("\n== " + THREADS + " threads, " + ROUNDS + " loads each ==");
        System.out.println("failures: " + load(viewer, pdb, THREADS, ROUNDS));
    }

    /** Loads the model {@code rounds} times on each of {@code threads} threads. */
    static int load(final JmolViewer viewer, final String pdb, int threads, final int rounds)
            throws Exception {
        final java.util.List<Throwable> failures =
                java.util.Collections.synchronizedList(new java.util.ArrayList<Throwable>());
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread() {
                public void run() {
                    for (int r = 0; r < rounds; r++) {
                        try {
                            viewer.openStringInline(pdb);
                        } catch (Throwable e) {
                            failures.add(e);
                        }
                    }
                }
            };
        }
        for (Thread w : workers) w.start();
        for (Thread w : workers) w.join();

        if (!failures.isEmpty()) {
            Throwable first = failures.get(0);
            System.out.println("first failure: " + first);
            StackTraceElement[] st = first.getStackTrace();
            for (int i = 0; i < Math.min(4, st.length); i++) {
                System.out.println("    at " + st[i]);
            }
        }
        return failures.size();
    }

    /** A poly-alanine chain, extended, as PDB text. */
    static String polyalanine(int residues) {
        StringBuilder pdb = new StringBuilder();
        int serial = 1;
        // Backbone template, offset by 3.8 A per residue along x.
        String[] names = {" N  ", " CA ", " C  ", " O  "};
        double[][] xyz = {{0.000, 0.000, 0.000},
                          {1.458, 0.000, 0.000},
                          {2.009, 1.420, 0.000},
                          {1.251, 2.390, 0.000}};
        for (int i = 0; i < residues; i++) {
            double dx = 3.8 * i;
            for (int a = 0; a < names.length; a++) {
                pdb.append(String.format(
                        "ATOM  %5d %s ALA A%4d    %8.3f%8.3f%8.3f  1.00  0.00           %s%n",
                        serial++, names[a], i + 1,
                        xyz[a][0] + dx, xyz[a][1], xyz[a][2],
                        names[a].trim().substring(0, 1)));
            }
        }
        pdb.append("END").append(System.lineSeparator());
        return pdb.toString();
    }
}
