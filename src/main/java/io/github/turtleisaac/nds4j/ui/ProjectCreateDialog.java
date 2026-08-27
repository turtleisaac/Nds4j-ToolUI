/*
 * Created by JFormDesigner
 */

package io.github.turtleisaac.nds4j.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.*;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;

/**
 * @author turtleisaac
 */
public class ProjectCreateDialog extends JDialog {
    private static final int height = 250;
    private static final int width = 500;

    private static String resultPrefix;

    private String projectPath;
    private boolean projectCreated = false;

    private final Tool tool;

    protected ProjectCreateDialog(Tool tool) {
        super(tool.getProjectStartFrame());
        initComponents();
        setIcons();
        Dimension d = new Dimension(width, height);
        setPreferredSize(d);
        setMinimumSize(d);
        setModalityType(ModalityType.APPLICATION_MODAL);
        this.tool = tool;
        resultPrefix = resultLabel.getText();
        projectNameField.setText("");
        projectPath = null;

        projectNameField.getDocument().addDocumentListener(new DocumentListener()
        {
            private void update()
            {
                String parentDir = parentFolderField.getText();
                if (!parentDir.isEmpty()) {
                    setResultText();
                }
                attemptEnableOkButton();
            }

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                update();
            }
        });

        if (!tool.isGitEnabled())
        {
            contentPanel.remove(gitRadioButton);
        }
    }

    private void setIcons()
    {
        baseRomButton.setIcon(ThemeUtils.fileSearchIcon);
        parentFolderButton.setIcon(ThemeUtils.folderSearchIcon);
        pack();
    }

    private void setResultText()
    {
        if (projectNameField.getText().isEmpty()) {
            // Path.of(parent, "") collapses to the parent directory, so leaving the previous verdict up would
            // claim the parent folder itself is a valid new project.
            resultLabel.setIcon(null);
            resultLabel.setForeground(UIManager.getColor("Label.foreground"));
            resultLabel.setText(resultPrefix);
            return;
        }

        Path path = Path.of(parentFolderField.getText(), projectNameField.getText());

        if (!path.toFile().exists()) {
            resultLabel.setForeground(themeColor("Actions.Green"));
            resultLabel.setIcon(ThemeUtils.validIcon);
        } else {
            resultLabel.setForeground(themeColor("Actions.Red"));
            resultLabel.setIcon(ThemeUtils.invalidIcon);
        }

        resultLabel.setText(resultPrefix + " " + path);
    }

    /**
     * Gets a color from the active look and feel, so the result line stays legible under every theme.
     * <p>Color.GREEN on a light panel and Color.RED on a dark one are both close to unreadable.</p>
     */
    private static Color themeColor(String key)
    {
        Color color = UIManager.getColor(key);
        return color != null ? color : UIManager.getColor("Label.foreground");
    }

    private void attemptEnableOkButton()
    {
        okButton.setEnabled(tool.getRom() != null && !projectNameField.getText().isEmpty() && !parentFolderField.getText().isEmpty());
    }

    private void cancelButtonPressed(ActionEvent e) {
        dispose();
    }

    private void baseRomButtonPressed(ActionEvent e) {
        String romPath = tool.selectAndValidateRom(this);
        // a null return means the user cancelled or picked an unsupported ROM, so keep the previous selection
        if (romPath != null)
            baseRomField.setText(romPath);
        attemptEnableOkButton();
    }

    private void parentFolderButtonPressed(ActionEvent e) {
        String lastPath = Tool.preferences.get("openProjectPath", null);

        if (lastPath == null) {
            lastPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(lastPath);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Choose Project Directory");

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            Tool.preferences.put("openProjectPath", selected.getAbsolutePath());
            parentFolderField.setText(selected.getAbsolutePath());
            setResultText();
            attemptEnableOkButton();
        }
    }

    private void okButtonPressed(ActionEvent e) {
        File projectDir = Path.of(parentFolderField.getText(), projectNameField.getText()).toFile();
        JDialog progressDialog = createProgressDialog();

        okButton.setEnabled(false);
        cancelButton.setEnabled(false);

        // unpacking a ROM writes tens of thousands of files, so it must not be done on the EDT
        new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                if (!projectDir.mkdir())
                    throw new IOException("Failed to create project directory: " + projectDir.getAbsolutePath());

                try {
                    tool.getRom().unpack(FileUtils.getProjectUnpackedRomPath(projectDir.getAbsolutePath()));

                    File projectFile = new File(FileUtils.getProjectfilePath(projectDir.getAbsolutePath()));
                    if (!projectFile.createNewFile())
                        throw new IOException("Failed to write " + FileUtils.projectFileName);
                    Files.write(projectFile.toPath(), "{}".getBytes(StandardCharsets.UTF_8));
                }
                catch (Exception | Error ex) {
                    // a half-created project would make this project name unusable forever, so roll it back
                    FileUtils.clearDirectory(projectDir);
                    throw ex;
                }

                return null;
            }

            @Override
            protected void done()
            {
                progressDialog.dispose();

                try {
                    get();
                }
                catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    allowRetry();
                    return;
                }
                catch(ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(ProjectCreateDialog.this, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    allowRetry();
                    return;
                }

                if (gitRadioButton.isSelected())
                {
                    Thread gitThread = getGitThread(projectDir);
                    tool.setGitThread(gitThread);
                }
                else
                {
                    tool.setGitEnabledInternal(false);
                }

                projectPath = projectDir.getAbsolutePath();
                projectCreated = true;
                dispose();
            }
        }.execute();

        progressDialog.setVisible(true);
    }

    private void allowRetry()
    {
        cancelButton.setEnabled(true);
        setResultText();
        attemptEnableOkButton();
    }

    private JDialog createProgressDialog()
    {
        JDialog progressDialog = new JDialog(this, "Creating Project", ModalityType.APPLICATION_MODAL);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        JPanel panel = new JPanel(new MigLayout("insets 15", "[grow,fill]", "[][]"));
        panel.add(new JLabel("Unpacking the ROM into the new project. Please standby."), "cell 0 0");
        panel.add(progressBar, "cell 0 1");

        progressDialog.setContentPane(panel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);
        return progressDialog;
    }

    private Thread getGitThread(File projectDir)
    {
        Thread gitThread = new Thread(() -> {
            // Take the same lock Tool.commit() uses, so a save or a window close during the initial commit waits
            // for it instead of racing it - the shutdown wait probes gitLock and would otherwise see it free.
            tool.getGitLock().lock();
            // the handle is handed off to the Tool for later commits, so it must not be closed here
            try {
                Git git = Git.init().setDirectory(new File(projectDir.getAbsolutePath())).call();
                tool.setGit(git);
                AddCommand add = git.add();
                add.addFilepattern(".").call();
                // never signed, for the same reason Tool.commit() does not sign - see the note there
                CommitCommand commit = git.commit().setSign(Boolean.FALSE);
                commit.setMessage("Initial commit").call();
            }
            catch (Throwable ex) {
                // JGit raises unchecked JGitInternalException and IllegalArgumentException for ordinary
                // configuration problems (hooks, signing), which would otherwise die silently on this thread.
                String message = ex.getMessage();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "The project was created, but setting up its git repository failed:\n" + message,
                        "Git Init Failed", JOptionPane.ERROR_MESSAGE));
                tool.setGitEnabledInternal(false);
            }
            finally {
                tool.getGitLock().unlock();
                tool.setGitThread(null);
            }
        }, "Nds4j-ToolUI git init");
        gitThread.start();
        return gitThread;
    }

    protected boolean wasProjectCreated() {return projectCreated;}

    protected String getProjectPath() {return projectPath;}

    private void helpButtonActionPerformed(ActionEvent e) {
        ResourceBundle bundle = ResourceBundle.getBundle("project_gui");

        String helpText = bundle.getString("ProjectCreateDialog.projectNameLabel.text") + " " +
                bundle.getString("ProjectCreateDialog.HelpDialog.projectName.text") + "\n\n" +
                bundle.getString("ProjectCreateDialog.parentFolderLabel.text") + " " +
                bundle.getString("ProjectCreateDialog.HelpDialog.parentFolder.text") + "\n\n" +
                bundle.getString("ProjectCreateDialog.baseRomLabel.text") + " " +
                bundle.getString("ProjectCreateDialog.HelpDialog.baseRom.text");

        JOptionPane.showMessageDialog(this, helpText, bundle.getString("ProjectCreateDialog.HelpDialog.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        ResourceBundle bundle = ResourceBundle.getBundle("project_gui");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        projectNameLabel = new JLabel();
        projectNameField = new JTextField();
        parentFolderLabel = new JLabel();
        parentFolderField = new JTextField();
        parentFolderButton = new JButton();
        baseRomLabel = new JLabel();
        baseRomField = new JTextField();
        baseRomButton = new JButton();
        gitRadioButton = new JRadioButton();
        resultLabel = new JLabel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        helpButton = new JButton();

        //======== this ========
        setTitle(bundle.getString("ProjectCreateDialog.this.title"));
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new MigLayout(
                    "insets dialog,hidemode 3",
                    // columns
                    "[fill]" +
                    "[grow,fill]" +
                    "[fill]",
                    // rows
                    "[]" +
                    "[]" +
                    "[]" +
                    "[]" +
                    "[]" +
                    "[]" +
                    "[]"));

                //---- projectNameLabel ----
                projectNameLabel.setText(bundle.getString("ProjectCreateDialog.projectNameLabel.text"));
                projectNameLabel.setLabelFor(projectNameField);
                contentPanel.add(projectNameLabel, "cell 0 0");
                contentPanel.add(projectNameField, "cell 1 0");

                //---- parentFolderLabel ----
                parentFolderLabel.setText(bundle.getString("ProjectCreateDialog.parentFolderLabel.text"));
                parentFolderLabel.setLabelFor(parentFolderField);
                contentPanel.add(parentFolderLabel, "cell 0 2");

                //---- parentFolderField ----
                parentFolderField.setEditable(false);
                parentFolderField.setEnabled(false);
                contentPanel.add(parentFolderField, "cell 1 2,grow");

                //---- parentFolderButton ----
                parentFolderButton.setIcon(UIManager.getIcon("FileView.directoryIcon"));
                parentFolderButton.addActionListener(e -> parentFolderButtonPressed(e));
                contentPanel.add(parentFolderButton, "cell 2 2");

                //---- baseRomLabel ----
                baseRomLabel.setText(bundle.getString("ProjectCreateDialog.baseRomLabel.text"));
                baseRomLabel.setLabelFor(baseRomField);
                contentPanel.add(baseRomLabel, "cell 0 3");

                //---- baseRomField ----
                baseRomField.setEditable(false);
                baseRomField.setEnabled(false);
                contentPanel.add(baseRomField, "cell 1 3,grow");

                //---- baseRomButton ----
                baseRomButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
                baseRomButton.addActionListener(e -> baseRomButtonPressed(e));
                contentPanel.add(baseRomButton, "cell 2 3");

                //---- gitRadioButton ----
                gitRadioButton.setText(bundle.getString("ProjectCreateDialog.gitRadioButton.text"));
                gitRadioButton.setActionCommand(bundle.getString("ProjectCreateDialog.gitRadioButton.text"));
                contentPanel.add(gitRadioButton, "cell 1 4 2 1");

                //---- resultLabel ----
                resultLabel.setText(bundle.getString("ProjectCreateDialog.resultLabel.text"));
                resultLabel.setFont(resultLabel.getFont().deriveFont(resultLabel.getFont().getStyle() | Font.ITALIC));
                contentPanel.add(resultLabel, "cell 0 5 3 1");
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setLayout(new MigLayout(
                    "insets dialog,alignx right",
                    // columns
                    "[button,fill]" +
                    "[button,fill]" +
                    "[button,fill]",
                    // rows
                    null));

                //---- okButton ----
                okButton.setText(bundle.getString("ProjectCreateDialog.okButton.text"));
                okButton.setEnabled(false);
                okButton.addActionListener(e -> okButtonPressed(e));
                buttonBar.add(okButton, "cell 0 0");

                //---- cancelButton ----
                cancelButton.setText(bundle.getString("ProjectCreateDialog.cancelButton.text"));
                cancelButton.addActionListener(e -> cancelButtonPressed(e));
                buttonBar.add(cancelButton, "cell 1 0");

                //---- helpButton ----
                helpButton.setText(bundle.getString("ProjectCreateDialog.helpButton.text"));
                helpButton.addActionListener(e -> helpButtonActionPerformed(e));
                buttonBar.add(helpButton, "cell 2 0");
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel projectNameLabel;
    private JTextField projectNameField;
    private JLabel parentFolderLabel;
    private JTextField parentFolderField;
    private JButton parentFolderButton;
    private JLabel baseRomLabel;
    private JTextField baseRomField;
    private JButton baseRomButton;
    private JRadioButton gitRadioButton;
    private JLabel resultLabel;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
