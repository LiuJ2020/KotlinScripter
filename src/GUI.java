import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private JButton indexKeywordsButton;
    private JButton stopScriptButton;
    private JPanel scriptRunningPanel;
    private JLabel exitCodeDisplay;

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

        codeEditor.getStyledDocument().addDocumentListener(new DocumentListener() {
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
        ((AbstractDocument) codeEditor.getDocument()).setDocumentFilter(new KeywordDocumentFilter());

        stopScriptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

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
            codeEditor.setText("");
            String nextLine = "";
            FileReader fileReader = new FileReader(currentFile);
            BufferedReader reader = new BufferedReader(fileReader);

            StyledDocument doc = codeEditor.getStyledDocument();

            addLine(doc, reader.readLine());
            while ((nextLine = reader.readLine()) != null) {
                addLine(doc, nextLine);
            }
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

    public void addLine(StyledDocument doc, String line) {
        try {
            doc.insertString(doc.getLength(), line + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
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
            displayOutput.setEditable(true);
            displayOutput.setText("");
            runningIndicator.setText("Running...");

            StyledDocument doc = displayOutput.getStyledDocument();

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
                    new StreamGobbler(process.getInputStream(), line -> addLine(doc, line));
            StreamGobbler streamGobblerError =
                    new StreamGobbler(process.getErrorStream(), line -> addLine(doc, line));
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            Executors.newSingleThreadExecutor().submit(streamGobblerError);
            int exitCode = 0;
            try {
                exitCode = process.waitFor();
                displayOutput.setEditable(false);
                runningIndicator.setText("Done!");
                exitCodeDisplay.setText("Exit Code: " + process.exitValue());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assert exitCode == 0;
        }
    }

    private final class KeywordDocumentFilter extends DocumentFilter
    {
        private final StyledDocument styledDocument = codeEditor.getStyledDocument();

        private final StyleContext styleContext = StyleContext.getDefaultStyleContext();
        private final AttributeSet orangeAttributeSet = styleContext.addAttribute(styleContext.getEmptySet(), StyleConstants.Foreground, Color.ORANGE);
        private final AttributeSet grayAttributeSet = styleContext.addAttribute(styleContext.getEmptySet(), StyleConstants.Foreground, Color.GRAY);
        private final AttributeSet blackAttributeSet = styleContext.addAttribute(styleContext.getEmptySet(), StyleConstants.Foreground, Color.BLACK);

        private final String[] keywords = new String[] {"as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
                "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
                "val", "var", "when", "while"};

        // Use a regular expression to find the words you are looking for
        Pattern keywordPattern = buildKeywordPattern();
        Pattern commentPattern = buildCommentPattern();

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributeSet) throws BadLocationException {
            super.insertString(fb, offset, text, attributeSet);

            handleTextChanged();
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);

            handleTextChanged();
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributeSet) throws BadLocationException {
            super.replace(fb, offset, length, text, attributeSet);

            handleTextChanged();
        }

        /**
         * Runs your updates later, not during the event notification.
         */
        private void handleTextChanged()
        {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateTextStyles();
                }
            });
        }

        /**
         * Build the regular expression that looks for the whole word of each word that you wish to find.
         * The "\\b" is the beginning or end of a word boundary.  The "|" is a regex "or" operator.
         * @return
         */
        private Pattern buildKeywordPattern()
        {
            StringBuilder sb = new StringBuilder();
            for (String token : keywords) {
                sb.append("\\b"); // Start of word boundary
                sb.append(token);
                sb.append("\\b|"); // End of word boundary and an or for the next word
            }
            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1); // Remove the trailing "|"
            }

            return Pattern.compile(sb.toString());
        }

        private Pattern buildCommentPattern() {
            return Pattern.compile("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)");
        }

        private void updateTextStyles()
        {
            // Clear existing styles
            styledDocument.setCharacterAttributes(0, codeEditor.getText().length(), blackAttributeSet, true);

            // Look for tokens and highlight them
            Matcher keywordMatcher = keywordPattern.matcher(codeEditor.getText());
            while (keywordMatcher.find()) {
                // Change the color of recognized tokens
                styledDocument.setCharacterAttributes(keywordMatcher.start(), keywordMatcher.end() - keywordMatcher.start(), orangeAttributeSet, false);
            }

            Matcher commentMatcher = commentPattern.matcher(codeEditor.getText());
            while (commentMatcher.find()) {
                styledDocument.setCharacterAttributes(commentMatcher.start(), commentMatcher.end() - commentMatcher.start(), grayAttributeSet, false);
            }
        }
    }
}
