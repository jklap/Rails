package net.sf.rails.ui.swing;

import java.awt.*;
import java.util.List;
import java.util.Map;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.ConfigItem;
import net.sf.rails.common.GameConfig;
import net.sf.rails.common.LocalText;

public class GameConfigWindow extends BaseConfigWindow {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(GameConfigWindow.class);

    private final GameConfig config;

    private JButton saveButton;
    private JButton resetButton;

    public GameConfigWindow(GameUIManager gameUIManager, Window parent) {
        super(parent);
        config = gameUIManager.getRoot().getConfig();

        // JFrame properties
        setTitle(LocalText.getText("GAME_CONFIG_WINDOW_TITLE"));

        // configSetup pane
        configPane = new JTabbedPane();
        add(configPane, "Center");

        // buttons
        buttonPanel = new JPanel();
        add(buttonPanel, "South");
    }

    @Override
    protected Map<String, List<ConfigItem>> getConfigSections() {
        return cm.getGameConfigSections();
    }

     @Override
     protected String getConfigValue(String name) {
        return config.get(name);
     }

    @Override
    protected void setupButtonPanel() {
        buttonPanel.removeAll();

        saveButton = new JButton(LocalText.getText("SAVE"));
        saveButton.addActionListener(actionEvent -> GameConfigWindow.this.saveConfig());
        saveButton.setEnabled(false);
        buttonPanel.add(saveButton);

        // reset button: revert to activeProfile
        resetButton = new JButton(LocalText.getText("RESET"));
//        resetButton.addActionListener(actionEvent -> changeProfile(cm.getActiveProfile()));
        resetButton.setEnabled(false);
        buttonPanel.add(resetButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(actionEvent -> GameConfigWindow.this.closeConfig());
        closeButton.setEnabled(true);
        buttonPanel.add(closeButton);
    }

    private boolean saveConfig() {
        // check for parent if initMethods have to be called
        boolean initMethods = parentWindow instanceof StatusWindow;

        // save depending (either as newProfile or as existing)
        boolean result = cm.saveConfig(config, initMethods);

        if (result) {
            saveButton.setEnabled(false);
            resetButton.setEnabled(false);
            isDirty = false;
        } else {
            JOptionPane.showMessageDialog(GameConfigWindow.this, LocalText.getText("CONFIG_SAVE_ERROR_MESSAGE", cm.getActiveProfile()),
                    LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
        }

        return result;
    }

    @Override
    protected void isDirty(ConfigItem configItem) {
        super.isDirty(configItem);
        saveButton.setEnabled(true);
        resetButton.setEnabled(true);
    }

    @Override
    protected void closeConfig() {
        super.closeConfig();

        if ( parentWindow instanceof StatusWindow) {
            ((StatusWindow) parentWindow).setMenuItemCheckbox(StatusWindow.GAME_CONFIG_CMD, false);
        }
    }

}
