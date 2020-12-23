import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GUI extends JFrame {

    // GUI-related fields
    private JFrame frame;
    private JPanel main;
    private JTextPane codeEditor;
    private JPanel code;
    private JPanel output;
    private JPanel execute;
    private JButton runScript;
    private JPanel additional;
    private JTextPane displayOutput;
    private JScrollPane displayOutputScrolling;
    private JScrollPane codeEditorScroll;
    private JButton openFile;
    private JButton saveFile;
    private JButton newFile;
    private JLabel runningIndicator;
    private JLabel hasBeenEditedDisplay;

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
                int response = (hasBeenEdited ? prompt("You must save the script before you can run it. Would you like to save?") : -1);
                if (response == JOptionPane.YES_OPTION) {
                    saveContents();
                    updateHeader();
                }
                runScript();
            }
        });

        newFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = (hasBeenEdited ? prompt("Would you like to save this script? It will be lost otherwise.") : -1);
                if (response == JOptionPane.YES_OPTION) {
                    saveContents();
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
                int response = (hasBeenEdited ? prompt("Would you like to save this script? It will be lost otherwise.") : -1);
                if (response == JOptionPane.YES_OPTION) {
                    saveContents();
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
                if (hasBeenEdited) {
                    saveContents();
                    updateHeader();
                }
            }
        });

        codeEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                hasBeenEdited = true;
                hasBeenEditedDisplay.setText("*");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                hasBeenEdited = true;
                hasBeenEditedDisplay.setText("*");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                hasBeenEdited = true;
                hasBeenEditedDisplay.setText("*");
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void initFields() {
        currentFile = new File("");
        hasBeenEdited = false;
    }

    public static void main(String[] args) {
        new GUI();
    }

    // Helper methods
    public File selectSaveDirectory() {
        JFileChooser fileChooser = new JFileChooser("f:");
        int response = fileChooser.showSaveDialog(null);
        if (response == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return new File("");
    }

    public File selectOpenDirectory() {
        JFileChooser fileChooser = new JFileChooser("f:");
        int response = fileChooser.showOpenDialog(null);
        if (response == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return new File("");
    }

    public int prompt(String message) {
        return JOptionPane.showConfirmDialog(frame, message);
    }

    public void inform(String message) {
        JOptionPane.showMessageDialog(frame, message);
    }

    public void saveContents() {
        if (!currentFile.isFile()) {
            currentFile = selectSaveDirectory();
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

        hasBeenEdited = false;
        hasBeenEditedDisplay.setText("");
    }

    public void clearContents() {
        runningIndicator.setText("No Script Running");

        codeEditor.setText("");
        currentFile = new File("");
        displayOutput.setText("");

        hasBeenEdited = false;
        hasBeenEditedDisplay.setText("");
    }

    public void openContents() {
        currentFile = selectOpenDirectory();

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

        hasBeenEdited = false;
        hasBeenEditedDisplay.setText("");
        runningIndicator.setText("No Script Running");
        updateHeader();
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
            runningIndicator.setText("Running...");

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
                runningIndicator.setText("Done!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assert exitCode == 0;
        }
    }
}
