package jKendrick.ui;

import jKendrick.Kendrick;
import jKendrick.model.GroundModel;
import jKendrick.simulation.ChartRenderer;
import jKendrick.simulation.Trajectory;
import jKendrick.simulation.VisualizationSpec;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small Swing front end on the DSL: a script editor on the left, the chart of the script's
 * Visualization entity on the right, and a log below. Open, edit, Run; syntax and semantic errors
 * are reported with their line and column, and the caret is moved to the first one.
 * <p>
 * {@code java -cp ... jKendrick.ui.KendrickStudio [script.kendrick]}
 */
public final class KendrickStudio extends JFrame {
    private static final Pattern POSITION = Pattern.compile("(?:line (\\d+), column (\\d+))|(?::(\\d+):(\\d+):)");

    private final JTextArea editor = new JTextArea();
    private final JTextArea log = new JTextArea(6, 80);
    private final JPanel chartArea = new JPanel(new BorderLayout());
    private final JLabel status = new JLabel(" ");
    private final JButton run = new JButton("Run");
    private Path file;

    public KendrickStudio() {
        super("Kendrick");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        editor.setTabSize(4);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        JButton open = new JButton("Open...");
        JButton save = new JButton("Save");
        JButton saveAs = new JButton("Save as...");
        JButton png = new JButton("Save chart as PNG...");
        open.addActionListener(e -> open());
        save.addActionListener(e -> save(false));
        saveAs.addActionListener(e -> save(true));
        run.addActionListener(e -> run());
        png.addActionListener(e -> savePng());
        for (JButton b : new JButton[]{open, save, saveAs, run, png}) bar.add(b);
        bar.add(Box.createHorizontalGlue());
        bar.add(status);

        chartArea.setPreferredSize(new Dimension(640, 480));
        chartArea.add(new JLabel("Run the script to see its Visualization here", SwingConstants.CENTER));
        JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(editor), chartArea);
        top.setResizeWeight(0.45);
        JSplitPane all = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, new JScrollPane(log));
        all.setResizeWeight(0.8);

        add(bar, BorderLayout.NORTH);
        add(all, BorderLayout.CENTER);
        editor.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "run");
        editor.getActionMap().put("run", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { run(); }
        });
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ files

    public void load(Path path) {
        try {
            editor.setText(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            editor.setCaretPosition(0);
            file = path;
            setTitle("Kendrick - " + path.getFileName());
            say("loaded " + path);
        } catch (IOException e) {
            say("cannot read " + path + ": " + e.getMessage());
        }
    }

    private void open() {
        JFileChooser chooser = chooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) load(chooser.getSelectedFile().toPath());
    }

    private void save(boolean as) {
        if (file == null || as) {
            JFileChooser chooser = chooser();
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            file = chooser.getSelectedFile().toPath();
            setTitle("Kendrick - " + file.getFileName());
        }
        try {
            Files.write(file, editor.getText().getBytes(StandardCharsets.UTF_8));
            say("saved " + file);
        } catch (IOException e) {
            say("cannot write " + file + ": " + e.getMessage());
        }
    }

    private JFileChooser chooser() {
        JFileChooser chooser = new JFileChooser(file != null ? file.getParent().toFile() : Paths.get(".").toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("Kendrick scripts (*.kendrick)", "kendrick"));
        return chooser;
    }

    private XYChart current;

    private void savePng() {
        if (current == null) { say("no chart yet: run the script first"); return; }
        JFileChooser chooser = new JFileChooser(file != null ? file.getParent().toFile() : Paths.get(".").toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path out = chooser.getSelectedFile().toPath();
        if (!out.toString().endsWith(".png")) out = Paths.get(out + ".png");
        try {
            ChartRenderer.savePng(current, out);
            say("chart written to " + out);
        } catch (IOException e) {
            say("cannot write " + out + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ run

    private void run() {
        String source = editor.getText();
        run.setEnabled(false);
        status.setText("running... ");
        long t0 = System.nanoTime();
        new SwingWorker<Result, Void>() {
            @Override protected Result doInBackground() {
                GroundModel model = Kendrick.load(source);
                List<Trajectory> runs = Kendrick.run(model);
                List<VisualizationSpec> specs = Kendrick.visualizations(model);
                return new Result(model, runs, ChartRenderer.chart(specs.get(0), runs));
            }

            @Override protected void done() {
                run.setEnabled(true);
                status.setText(" ");
                try {
                    Result r = get();
                    show(r, (System.nanoTime() - t0) / 1e9);
                } catch (java.util.concurrent.ExecutionException e) {
                    report(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private static final class Result {
        final GroundModel model;
        final List<Trajectory> runs;
        final XYChart chart;
        Result(GroundModel model, List<Trajectory> runs, XYChart chart) { this.model = model; this.runs = runs; this.chart = chart; }
    }

    private void show(Result r, double seconds) {
        current = r.chart;
        chartArea.removeAll();
        chartArea.add(new XChartPanel<>(r.chart), BorderLayout.CENTER);
        chartArea.revalidate();
        chartArea.repaint();
        Trajectory first = r.runs.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: %d concerns, %d compartments, %d transitions; %d run(s), t = %s .. %s, %.2f s%n",
                r.model.model.name, r.model.model.concerns.size(), r.model.model.compartments.size(),
                r.model.transitions().size(), r.runs.size(), first.times[0], first.times[first.size() - 1], seconds));
        String[] keys = r.model.model.attribute("species") == null ? new String[]{"status"} : new String[]{"status", "species"};
        for (Map.Entry<String, double[]> e : first.aggregate(keys).entrySet())
            sb.append(String.format("  %-24s %12.4f%n", e.getKey(), e.getValue()[first.size() - 1]));
        say(sb.toString().trim());
    }

    private void report(Throwable error) {
        String message = error.getMessage() != null ? error.getMessage() : error.toString();
        say("error: " + message);
        Matcher m = POSITION.matcher(message);
        if (m.find()) {
            int line = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(3));
            int column = m.group(2) != null ? Integer.parseInt(m.group(2)) : Integer.parseInt(m.group(4)) + 1; // syntax errors: 0-based
            try {
                int offset = editor.getLineStartOffset(Math.max(0, Math.min(line, editor.getLineCount()) - 1)) + Math.max(0, column - 1);
                editor.setCaretPosition(Math.min(offset, editor.getDocument().getLength()));
                editor.requestFocusInWindow();
            } catch (javax.swing.text.BadLocationException ignored) {
                // the position does not exist in the current text: keep the caret where it is
            }
        }
    }

    private void say(String text) {
        log.append(text + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KendrickStudio studio = new KendrickStudio();
            if (args.length > 0) studio.load(Paths.get(args[0]));
            studio.setVisible(true);
        });
    }
}
