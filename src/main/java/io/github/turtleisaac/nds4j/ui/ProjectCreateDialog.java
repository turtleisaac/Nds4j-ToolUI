/*
 * Created by JFormDesigner
 */

package io.github.turtleisaac.nds4j.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.*;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

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
        Path path = Path.of(parentFolderField.getText(), projectNameField.getText());
        String resultText = resultPrefix + " " + path.toString();

        if (!projectNameField.getText().isEmpty()) {
            if (!path.toFile().exists()) {
                resultLabel.setForeground(Color.GREEN);
                resultLabel.setIcon(ThemeUtils.validIcon);
            } else {
                resultLabel.setForeground(Color.RED);
                resultLabel.setIcon(ThemeUtils.invalidIcon);
            }
        }
        resultLabel.setText(resultText);
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

        try {
            if (!projectDir.mkdir())
                throw new RuntimeException("Failed to create project directory: " + projectDir.getAbsolutePath());

            tool.getRom().unpack(FileUtils.getProjectUnpackedRomPath(projectDir.getAbsolutePath()));
            projectCreated = true;
            projectPath = projectDir.getAbsolutePath();
            File projectFile = new File(FileUtils.getProjectfilePath(projectDir.getAbsolutePath()));
            if (!projectFile.createNewFile())
                throw new RuntimeException("Failed to write " + FileUtils.projectFileName);
            if (gitRadioButton.isSelected())
            {
                Thread gitThread = getGitThread(projectDir);
                tool.setGitThread(gitThread);
            }
            else
            {
                tool.setGitEnabledInternal(false);
            }
        }
        catch(IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(ex);
        }
        catch(RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            throw ex;
        }

        dispose();
    }

    private Thread getGitThread(File projectDir)
    {
        Thread gitThread = new Thread(() -> {
            try (Git git = Git.init().setDirectory(new File(projectDir.getAbsolutePath())).call()) {
                tool.setGit(git);
                AddCommand add = git.add();
                add.addFilepattern(".").call();
                CommitCommand commit = git.commit();
                commit.setMessage("Initial commit").call();
            }
            catch (GitAPIException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Git Commit Error", JOptionPane.ERROR_MESSAGE);
                throw new RuntimeException(ex);
            }
            tool.setGitThread(null);
        });
        gitThread.start();
        return gitThread;
    }

    protected boolean wasProjectCreated() {return projectCreated;}

    protected String getProjectPath() {return projectPath;}

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
