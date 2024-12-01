package edu.kit.iti.formal.pathwidth;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Main class and test loader.
 *
 * @author Samuel Teuber
 */
public class PathwidthTest implements Runnable {
    private static final int SECONDS = 1000;
    private static boolean NO_CHECK = false;
    private final ArrayList<Long> timings = new ArrayList<>(100);
    private int success, all;
    private long startTime;
    private long stopTime = -1;

    private PathwidthTest() {
    }

    private static File[] findRiddles() {
        final String riddlesFolder = System.getenv().
                getOrDefault("RIDDLES_FOLDER", "./graphs/");
        final File folder = new File(riddlesFolder);

        System.out.format("I am using the graph instance folder '%s'%n", folder);

        if (!folder.exists()) {
            System.out.format("Folder %s does not exist. ABORT!");
            System.exit(1);
        }
        return folder.listFiles();
    }

    protected static PathwidthSolver solverInstance(Graph k)
            throws NoSuchMethodException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException {
        Class<PathwidthSolver> clazz = (Class<PathwidthSolver>) Class.forName("MyPathwidthSolver");
        return clazz.getConstructor(Graph.class).newInstance(k);
    }

    /**
     * Main function of testing class
     * @param args No arguments necessary
     */
    public static void main(String[] args) {
        PathwidthTest test = new PathwidthTest();

        NO_CHECK = Boolean.valueOf(
                System.getenv().getOrDefault("PATHWIDTH_NO_CHECK", "false"));

        int time = Integer.valueOf(System.getenv().getOrDefault("PATHWIDTH_TIMEOUT", "100"));

        Thread t = new Thread(test);
        t.start();
        try {
            t.join(time * SECONDS);
        } catch (InterruptedException e) {
            //
        } finally {
            test.statistics();
            System.exit(0);
        }
    }

    private boolean checkSolution(File file) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Graph k = Graph.load(file);

        System.out.printf("== %s ======================================================%n", file.getName());
        PathwidthSolver solver = solverInstance(k);

        long start = System.currentTimeMillis();
        Solution s = solver.solve();
        timings.add(System.currentTimeMillis() - start);


        boolean satisfiable = file.getName().startsWith("sat");

        System.out.println("Graph instance is " + (!satisfiable ? "not " : "") + "satisfiable");
        System.out.println("Your answer is " + s.getState());

        if (s.getState() == SolutionState.UNKNOWN) {
            return false;
        } else {
            boolean b;
            if (satisfiable) {
                b = s.getState() == SolutionState.SAT && s.isValid();
            } else {
                b = s.getState() == SolutionState.UNSAT;
            }
            if (b) System.out.println("SOLVED!");
            else System.out.println("NOT SOLVED :(");

            s.print(System.out);

            return b;
        }
    }

    public void run() {
        try {
            startTime = System.currentTimeMillis();
            success = 0;
            File[] fileList = findRiddles();
            all = fileList.length;
            for (File file : fileList) {
                try {
                    success += checkSolution(file) ? 1 : 0;
                } catch (IOException e) {
                    System.err.println("Error while loading " + file);
                    System.err.println(e);
                } catch (IllegalArgumentException
                        | NoSuchElementException
                        | java.awt.HeadlessException e) {
                    e.printStackTrace();
                }
            }
            // TODO(steuber): Only measure time of solver?
            stopTime = System.currentTimeMillis();
        } catch (ClassNotFoundException e) {
            System.err.println("Could not load the solution by class name.");
            System.err.println("There should be class 'MyPathwidthSolver' in the default package on the classpath.");
            e.printStackTrace();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            System.err.println("Constructor not found");
            System.err.println("There should be constructor 'public MyPathwidthSolver(Graph)' in your solution class.");
            e.printStackTrace();
        }
    }

    private void statistics() {
        if (stopTime<0)
            stopTime = System.currentTimeMillis();
        System.out.format("%%%% %s: Score %d of %d (%2.1f%%) successful!%n",
                new File(".").getAbsolutePath(),
                success, all, 100.0 * success / all);
        System.out.printf("%%%% %s: %10d ms%n",
                new File(".").getAbsolutePath(),
                stopTime - startTime);

        Optional<Long> optMax = timings.stream().max(Long::compare);
        Optional<Long> optMin = timings.stream().min(Long::compare);

        if (optMax.isPresent() && optMin.isPresent()) {
            long max = optMax.get();
            long min = optMin.get();
            long sum = timings.stream().reduce((a, b) -> a + b).get();
            System.out.printf("%% Max time: %10d ms%n", max);
            System.out.printf("%% Min time: %10d ms%n", min);
            System.out.printf("%% Avg time: %10.0f ms%n", sum / (float) timings.size());
        }
        System.out.println("%% end");
    }
}
