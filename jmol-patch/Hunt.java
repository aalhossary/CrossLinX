import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jmol.api.JmolViewer;
import org.jmol.adapter.smarter.SmarterJmolAdapter;

/**
 * Hunts the residual failure left after the synchronization patch.
 *
 *   java -cp patch:Jmol.jar:. Hunt [cycles] [threads] [rounds] [residues]
 *
 * Records EVERY throwable with its full stack, deduplicated by
 * (exception class + top frame), so a rare interleaving is not lost.
 */
public class Hunt {

    static final Map<String, int[]> counts = new ConcurrentHashMap<String, int[]>();
    static final Map<String, Throwable> examples = new ConcurrentHashMap<String, Throwable>();

    public static void main(String[] args) throws Exception {
        int cycles   = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        final int threads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        final int rounds  = args.length > 2 ? Integer.parseInt(args[2]) : 15;
        int residues = args.length > 3 ? Integer.parseInt(args[3]) : 400;

        final String pdb = polyalanine(residues);
        System.out.println("Jmol " + JmolViewer.getJmolVersion()
                + " | cycles=" + cycles + " threads=" + threads
                + " rounds=" + rounds + " residues=" + residues);

        int totalLoads = 0;
        for (int c = 0; c < cycles; c++) {
            // A fresh viewer per cycle, as an application would have.
            final JmolViewer viewer = JmolViewer.allocateViewer(null, new SmarterJmolAdapter());
            Thread[] ws = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                ws[t] = new Thread() {
                    public void run() {
                        for (int r = 0; r < rounds; r++) {
                            try {
                                viewer.openStringInline(pdb);
                            } catch (Throwable e) {
                                record(e);
                            }
                        }
                    }
                };
            }
            for (Thread w : ws) w.start();
            for (Thread w : ws) w.join();
            totalLoads += threads * rounds;
            if ((c + 1) % 10 == 0) {
                System.out.println("  cycle " + (c + 1) + "/" + cycles
                        + "  loads=" + totalLoads + "  distinct failures=" + counts.size());
            }
        }

        System.out.println("\ntotal loads: " + totalLoads);
        if (counts.isEmpty()) {
            System.out.println("NO FAILURES");
        } else {
            for (Map.Entry<String, int[]> e : counts.entrySet()) {
                System.out.println("\n=== " + e.getValue()[0] + " x  " + e.getKey() + " ===");
                Throwable t = examples.get(e.getKey());
                System.out.println(t.toString());
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < Math.min(12, st.length); i++) {
                    System.out.println("    at " + st[i]);
                }
            }
        }
    }

    static void record(Throwable e) {
        StackTraceElement[] st = e.getStackTrace();
        String key = e.getClass().getName() + " @ " + (st.length > 0 ? st[0].toString() : "?");
        int[] c = counts.get(key);
        if (c == null) {
            counts.put(key, new int[]{1});
            examples.put(key, e);
        } else {
            synchronized (c) { c[0]++; }
        }
    }

    static String polyalanine(int residues) {
        StringBuilder pdb = new StringBuilder();
        int serial = 1;
        String[] names = {" N  ", " CA ", " C  ", " O  "};
        double[][] xyz = {{0.000, 0.000, 0.000}, {1.458, 0.000, 0.000},
                          {2.009, 1.420, 0.000}, {1.251, 2.390, 0.000}};
        for (int i = 0; i < residues; i++) {
            double dx = 3.8 * i;
            for (int a = 0; a < names.length; a++) {
                pdb.append(String.format(
                        "ATOM  %5d %s ALA A%4d    %8.3f%8.3f%8.3f  1.00  0.00           %s%n",
                        serial++, names[a], i + 1, xyz[a][0] + dx, xyz[a][1], xyz[a][2],
                        names[a].trim().substring(0, 1)));
            }
        }
        return pdb.append("END").append(System.lineSeparator()).toString();
    }
}
