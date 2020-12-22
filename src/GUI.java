import java.util.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GUI extends JFrame {

    // GUI-related fields
    private JFrame frame;
    private JPanel main;
    private JEditorPane codeEditor;
    private JPanel code;
    private JPanel output;
    private JProgressBar progressBar;
    private JPanel execute;
    private JButton runScript;
    private JPanel additional;
    private JEditorPane displayOutput;
    private JScrollPane displayOutputScrolling;
    private JScrollPane codeEditorScroll;
    private JButton openFile;
    private JButton saveFile;
    private JButton newFile;

    // storage fields
    private File currentFile;
    private boolean hasBeenEdited;

    public GUI() {
        initComponents();
        initFields();
    }

    public void initComponents() {
        frame = new JFrame("Kotlin Scripter - Untitled");
        frame.setContentPane(main);

        runScript.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (prompt("You must save the script before you can run it. Would you like to save?") == JOptionPane.YES_OPTION) {
                    saveContents();
                    runScript();
                    updateHeader();
                }
            }
        });

        newFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = prompt("Would you like to save this script? It will be lost otherwise.");
                if (response == JOptionPane.YES_OPTION) {
                    saveContents();
                    runScript();
                    updateHeader();
                }
                else if (response == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                clearContents();
                updateHeader();
            }
        });

        openFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = prompt("Would you like to save this script? It will be lost otherwise.");
                if (response == JOptionPane.YES_OPTION) {
                    saveContents();
                    runScript();
                    updateHeader();
                }
                else if (response == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                openContents();
                updateHeader();
            }
        });

        saveFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveContents();
                updateHeader();
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void initFields() {
        currentFile = new File("");
    }

    public static void main(String[] args) {
        new GUI();
    }

    // Helper methods
    public JFileChooser selectSaveDirectory() {
        JFileChooser fileChooser = new JFileChooser("f:");
        int response = fileChooser.showSaveDialog(null);
        if (response == JFileChooser.APPROVE_OPTION) {
            return fileChooser;
        }
        return null;
    }

    public JFileChooser selectOpenDirectory() {
        JFileChooser fileChooser = new JFileChooser("f:");
        int response = fileChooser.showOpenDialog(null);
        if (response == JFileChooser.APPROVE_OPTION) {
            return fileChooser;
        }
        return null;
    }

    public int prompt(String message) {
        return JOptionPane.showConfirmDialog(frame, message);
    }

    public void saveContents() {
        if (!currentFile.isFile()) {
            JFileChooser fileChooser = selectSaveDirectory();
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

    public void clearContents() {
        codeEditor.setText("");
        currentFile = new File("");
        displayOutput.setText("");
    }

    public void openContents() {
        JFileChooser fileChooser = selectOpenDirectory();
        currentFile = new File(fileChooser.getSelectedFile().getAbsolutePath());

        try {
            String nextLine = "", totalLines = "";
            FileReader fileReader = new FileReader(currentFile);
            BufferedReader reader = new BufferedReader(fileReader);

            totalLines = reader.readLine();
            while ((nextLine = reader.readLine()) != null) {
                totalLines = totalLines + "\n" + nextLine;
            }

            codeEditor.setText(totalLines);
        } catch (Exception evt) {
            System.err.println(evt);
        }
    }

    public void runScript() {
        Thread runner = new Thread(new ScriptRunner());
        runner.start();
    }

    public void updateHeader() {
        String path = currentFile.getPath();
        if (path.equals("")) {
            frame.setTitle("Kotlin Scripter - Untitled");
        }
        else {
            frame.setTitle("Kotlin Scripter - " + path);
        }
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

    private class ScriptRunner implements Runnable {
        @Override
        public void run() {
            /*
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
*/
            displayOutput.setEditable(true);
            displayOutput.setText("");

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
                    new StreamGobbler(process.getInputStream(), line -> displayOutput.setText(displayOutput.getText() + line + "\n"));
            StreamGobbler streamGobblerError =
                    new StreamGobbler(process.getErrorStream(), line -> displayOutput.setText(displayOutput.getText() + line + "\n"));
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            Executors.newSingleThreadExecutor().submit(streamGobblerError);
            int exitCode = 0;
            try {
                exitCode = process.waitFor();
                displayOutput.setEditable(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assert exitCode == 0;
        }
    }
}
