package wci.ide;

import wci.ide.commons.AddInfo;

import javax.swing.*;
import java.awt.event.*;
import java.util.Objects;

public class AddFrame extends JFrame {
    private JPanel mainPanel;
    private JPanel namePanel;
    private JLabel nameLabel;
    private JTextField nameText;
    private JPanel buttonPanel;
    private JButton confirmButton;
    private JButton cancelButton;

    public AddFrame(final AddInfo info) {
        mainPanel = new JPanel();
        namePanel = new JPanel();
        nameLabel = new JLabel(info.getInfo());
        nameText = new JTextField("", 20);
        buttonPanel = new JPanel();
        confirmButton = new JButton("确定");
        cancelButton = new JButton("取消");
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                cancel(info);
            }
        });
        setLocation(200, 200);
        setResizable(false);
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.add(nameLabel);
        namePanel.add(nameText);
        nameText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (nameText.getText().equals("")) {
                    confirmButton.setEnabled(false);
                } else {
                    confirmButton.setEnabled(true);
                }
            }
        });
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        confirmButton.setEnabled(false);
        buttonPanel.add(confirmButton);
        buttonPanel.add(new JLabel("         "));
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(e -> cancel(info));
        confirmButton.addActionListener(e -> {
            if (Objects.equals(nameText.getText(), "")) {
                return;
            }
            handleConfirm(info);
        });
        mainPanel.add(namePanel);
        mainPanel.add(buttonPanel);
        add(mainPanel);
        pack();
    }

    private void handleConfirm(AddInfo info) {
        String data = nameText.getText();
        info.getHandler().afterAdd(info.getEditorFrame(), this, data);
    }

    private void cancel(AddInfo info) {
        info.getEditorFrame().setEnabled(true);
        setVisible(false);
    }
}
