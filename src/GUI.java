import java.util.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GUI extends JFrame {

    // GUI-related fields
    private JPanel main;
    private JEditorPane codeEditor;
    private JPanel code;
    private JPanel output;
    private JProgressBar progressBar;
    private JPanel execute;
    private JButton runScript;
    private JList placeholder;

    // storage fields
    private File currentFile;
    private List<String> scriptOutput;

    public GUI() {
        initComponents();
        initFields();
    }

    public void initComponents() {
        JFrame frame = new JFrame("GUI");
        frame.setContentPane(main);

        runScript.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveContents();
                runScript();
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void initFields() {
        currentFile = new File("/Users/jacobliu/");
        scriptOutput = new ArrayList<>();
    }

    public static void main(String[] args) {
        new GUI();
    }

    // Helper methods
    public JFileChooser selectDirectory() {
        JFileChooser fileChooser = new JFileChooser("f:");
        int response = fileChooser.showSaveDialog(null);
        if (response == JFileChooser.APPROVE_OPTION) {
            return fileChooser;
        }
        return null;
    }

    public void saveContents() {
        if (!currentFile.isFile()) {
            JFileChooser fileChooser = selectDirectory();
            currentFile = new File(fileChooser.getSelectedFile().getAbsolutePath());
        }

        try {
            // Create a file writer
            FileWriter fileWriter = new FileWriter(currentFile, false);

            // Create buffered writer to write
            BufferedWriter writer = new BufferedWriter(fileWriter);

            // Write
            writer.write(codeEditor.getText());

            writer.flush();
            writer.close();
        } catch (Exception evt) {
            System.err.println(evt);
        }
    }

    public void runScript() {
//        boolean isWindows = System.getProperty("os.name")
//                .toLowerCase().startsWith("windows");

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("kotlinc", "-script", currentFile.getAbsolutePath());
        builder.directory(new File(System.getProperty("user.home")));
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert exitCode == 0;

    }

    // Helper classes
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
