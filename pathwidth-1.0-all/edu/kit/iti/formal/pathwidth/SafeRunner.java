package edu.kit.iti.formal.pathwidth;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Runner for flavium
 */
public class SafeRunner implements Runnable {
    private static final boolean NO_CHECK = System.getenv().getOrDefault("NO_CHECK", "").equals("true");

    private static final int SECONDS = 1000;
    private static final int MAX_POINTS = 10;
    private final List<Long> timings = new ArrayList<>();
    private volatile long startTime;
    private volatile long stopTime=-1;
    private int success;
    private int all;
    private double score;
    @NotNull
    private final StringWriter statContent = new StringWriter();
    @NotNull
    private final PrintWriter statOut = openStatisticFile();


    private static final PrintStream out = System.out;
    private static final PrintStream err = System.err;
    private double satScore, unsatScore;

    private static List<Path> findRiddles() {
        final String riddlesFolder = System.getenv().getOrDefault("RIDDLES_FOLDER", "./graphs/");
        final File file = new File(riddlesFolder);
        out.format("I am reading the folder '%s'%n", file);
        if (!file.exists() || !file.isDirectory()) {
            out.format("File '%s' does not exist or is not a directory. ABORT!%n", riddlesFolder);
            System.exit(1);
        }

        try (var s = Files.list(file.toPath())) {
            return s.collect(Collectors.toList());
        } catch (IOException e) {
            safePrintStackTrace(e,err);
            System.exit(2);
        }
        return Collections.emptyList();
    }

    public static void main(String[] args) throws InterruptedException {
        SafeRunner runner = new SafeRunner();
        int time = Integer.parseInt(System.getenv().getOrDefault("TIMEOUT", "100"));

        out.format("Solution checking is %s.%n", NO_CHECK ? "disabled" : "enabled");
        out.format("Timeout is %d seconds.%n", time);

        try {
            System.setOut(new PrintStream(new NullOutputStream()));
            System.setErr(new PrintStream(new NullOutputStream()));

            if (time > 0) {
                Thread t = new Thread(runner);
                t.start();
                t.join((long) time * SECONDS);
            } else {
                out.format("Timeout is disabled%n");
                runner.run();
            }
        } finally {
            // Flush before summary
            out.flush();
            err.flush();
            // Print summary
            runner.statistics();
            out.println(runner.statContent);
            runner.close();
        }
    }

    private void close() {
        statOut.flush();
        statOut.close();
        out.flush();
        err.flush();
    }

    private static boolean printAndCheckSolution(String id, Graph graph, Solution sol) {
        out.format("> SOLUTION %s%n", id);
        sol.print(out);
        out.format("<END SOLUTION %s%n", id);
        try {
            return sol.isValid();
        } catch (IllegalStateException e) {
            safePrintStackTrace(e,err);
            return false;
        }
    }

    private void statistics() {
        if (stopTime < 0)
            stopTime = System.currentTimeMillis();
        out.format(Locale.US,">> Your score is %4.2f%n", score);
        out.format(">> You solved %s of %s (success rate %s%%) riddles successfully!%n",
                success, all, 100.0 * success / all);
        out.format(">> Runtime: %s ms%n", stopTime - startTime);

        if (timings.isEmpty()) return;

        var stat = timings.stream().mapToLong(it -> it).summaryStatistics();

        long max = stat.getMax();
        long min = stat.getMin();
        long sum = stat.getSum();

        out.format("%% Sum time: %d ms%n", sum);
        out.format("%% Max time: %d ms%n", max);
        out.format("%% Min time: %d ms%n", min);
        out.format("%% Avg time: %s ms%n", stat.getAverage());
        out.format("%% end%n");
    }


    private boolean checkSolver(Path path) throws Exception {
        return checkSolver(path.toFile().getName(), path.toFile());
    }


    private boolean checkSolver(String id, File puzzle) throws Exception {
        out.format("== %s ======================================================%n", id);
        var graph = Graph.load(puzzle);

        var solver = PathwidthTest.solverInstance(graph);
        var error = false;

        boolean satisfiable = !id.startsWith("u");

        try {
            long start = System.currentTimeMillis();
            var ans = solver.solve();
            final var duration = System.currentTimeMillis() - start;
            timings.add(duration);
            out.format("Runtime: %d ms%n", duration);
            var answerSat = ans.getState() == SolutionState.SAT;
            var answerUnknown = ans.getState() == SolutionState.UNKNOWN;
            out.format("Riddle is %s satisfiable%n", !satisfiable ? "not " : "");
            out.format("Your answer is %s%n", answerSat ? "sat" : ( answerUnknown ? "unknown" : "unsat"));

            if (!answerUnknown) {
                if (NO_CHECK) {
                    out.format("Solution checker is disabled.%n");
                } else if (answerSat) {
                    var check = printAndCheckSolution(id, graph, ans);
                    if (!check || answerSat != satisfiable) {
                        out.format("There is an error in your solution!%n");
                        error = true;
                    } else {
                        out.format("No error found in solution%n");
                    }
                } else if (satisfiable) { // Solver sais unsat, but its sat
                    out.format("There is an error in your solution!%n");
                    error = true;
                }
            } else {
                error = true;
            }
            updateStatistics(id, answerSat, satisfiable, error, duration, "");

            if (error) return false;

            if (!answerUnknown && answerSat == satisfiable)
                out.format("SOLVED!%n");
            else
                out.format("NOT SOLVED :(%n");

            return answerSat == satisfiable;
        } catch (Throwable e) {
            updateStatistics(id, false, satisfiable, true, -1, e.getClass().getSimpleName());
            safePrintStackTrace(e,err);
        }
        return false;
    }

    private void updateStatistics(String id, boolean answerSat, boolean satisfiable, boolean error, long timing, String msg) {
        if (!error) {
            score += satisfiable ? satScore : unsatScore;
        }
        statOut.printf("%s,%s,%s,%s,%d,%s%n", id, answerSat, satisfiable, error, timing, msg);
    }

    private static void safePrintStackTrace(Throwable exc, PrintStream out) {
        out.println(exc.getClass().getName());
        for (StackTraceElement el : exc.getStackTrace()) {
            out.println("\tat "+el.toString());
        }
    }

    public void run() {
        try {
            startTime = System.currentTimeMillis();
            success = 0;
            final var riddles = findRiddles();
            int unsatRiddles = riddles.stream().mapToInt(it -> it.toFile().getName().startsWith("u") ? 1 : 0).sum();
            int satRiddles = riddles.size() - unsatRiddles;
            satScore = (MAX_POINTS / 2.0 / satRiddles);
            unsatScore = (MAX_POINTS / 2.0 / unsatRiddles);

            all = riddles.size();

            for (var p : riddles) {
                success += checkSolver(p) ? 1 : 0;
            }
        } catch (ClassNotFoundException e) {
            err.format("Could not from the solution by class name.%n");
            err.format("There should be class 'MyPathwidthSolver' in the default package on the classpath.%n");
            safePrintStackTrace(e,err);
            System.exit(3);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            err.format("Default constructor not found%n");
            safePrintStackTrace(e,err);
            System.exit(4);
        } catch (Throwable e) {
            safePrintStackTrace(e,err);
        } finally {
            stopTime = System.currentTimeMillis();
        }
    }


    private PrintWriter openStatisticFile() {
        var out = new PrintWriter(statContent);
        out.printf("%s,%s,%s,%s,%s%n", "id", "answer", "hasSolution", "solError", "duration");
        return out;
    }


    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }
}