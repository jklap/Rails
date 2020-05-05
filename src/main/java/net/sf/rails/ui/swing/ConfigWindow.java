package net.sf.rails.ui.swing;

import java.awt.*;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.Config;
import net.sf.rails.common.ConfigItem;
import net.sf.rails.common.LocalText;

class ConfigWindow extends BaseConfigWindow {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ConfigWindow.class);

    private final JPanel profilePanel;

    public ConfigWindow(Window parent) {
        super(parent);

        // JFrame properties
        setTitle(LocalText.getText("CONFIG_WINDOW_TITLE"));

        // add profile panel
        profilePanel = new JPanel();
        add(profilePanel, "North");

        // configSetup pane
        configPane = new JTabbedPane();
        add(configPane, "Center");

        // buttons
        buttonPanel = new JPanel();
        add(buttonPanel, "South");
    }

    @Override
    public void init(final boolean startUp) {
        setupProfilePanel();
        super.init(startUp);
    }

    private void setupProfilePanel() {
        profilePanel.removeAll();

        String activeProfile = cm.getActiveProfile();
        String profileText;
        if (cm.IsActiveUserProfile()) {
            profileText =  LocalText.getText("CONFIG_USER_PROFILE", activeProfile, cm.getActiveParent());
        } else {
            profileText =  LocalText.getText("CONFIG_PREDEFINED_PROFILE", activeProfile);
        }

        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, profileText);
        profilePanel.setBorder(titled);

        JLabel userLabel = new JLabel(LocalText.getText("CONFIG_SELECT_PROFILE"));
        profilePanel.add(userLabel);
        String[] profiles = cm.getProfiles().toArray(new String[0]);

        final JComboBox<String> comboBoxProfile = new JComboBox<>(profiles);
        comboBoxProfile.setSelectedItem(activeProfile);
        comboBoxProfile.addItemListener(arg0 -> changeProfile((String)comboBoxProfile.getSelectedItem()));
        profilePanel.add(comboBoxProfile);
    }

    @Override
    protected Map<String, List<ConfigItem>> getConfigSections() {
        return cm.getConfigSections();
    }

     @Override
     protected String getConfigValue(String name) {
        return Config.get(name);
     }

    @Override
    protected void setupButtonPanel() {
        buttonPanel.removeAll();

        // save button for user profiles
        if (cm.IsActiveUserProfile()) {
            JButton saveButton = new JButton(LocalText.getText("SAVE"));
            saveButton.addActionListener(
                    actionEvent -> ConfigWindow.this.saveConfig()
            );
            buttonPanel.add(saveButton);
        }

        // save (as) button
        JButton saveAsButton = new JButton(LocalText.getText("SAVEAS"));
        saveAsButton.addActionListener(
                actionEvent -> ConfigWindow.this.saveAsNewConfig()
        );
        buttonPanel.add(saveAsButton);

        JButton resetButton = new JButton(LocalText.getText("RESET"));
        resetButton.addActionListener(
                actionEvent -> {
                    // reset button: revert to activeProfile
                    changeProfile(cm.getActiveProfile());
                }
        );
        buttonPanel.add(resetButton);

        if (cm.IsActiveUserProfile()) {
            JButton deleteButton = new JButton(LocalText.getText("DELETE"));
            deleteButton.addActionListener(
                    actionEvent -> {
                        // store active Profile for deletion (see below)
                        String activeProfile = cm.getActiveProfile();
                        if (cm.deleteActiveProfile()) {
                            // delete item from selection in GameSetupWindow
                            if ( parentWindow instanceof GameSetupWindow) {
                                ((GameSetupWindow) parentWindow).removeConfigureProfile(activeProfile);
                            }
                            changeProfile(cm.getActiveProfile());
                        }
                    }
            );
            buttonPanel.add(deleteButton);
        }
    }

    private void changeProfile(String profileName) {
        // TODO: dirty check and alert if so
        cm.changeProfile(profileName);
        if ( parentWindow instanceof GameSetupWindow) {
            ((GameSetupWindow) parentWindow).changeConfigureProfile(profileName);
        }
        repaintLater();
        isDirty = false;
    }

    private boolean saveProfile(String newProfile) {
        // check for parent if initMethods have to be called
        boolean initMethods = parentWindow instanceof StatusWindow;

        // save depending (either as newProfile or as existing)
        boolean result;
        if (newProfile == null) {
            result = cm.saveProfile(initMethods);
        } else {
            result = cm.saveNewProfile(newProfile, initMethods);
        }

        if (result) {
            JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_CONFIRM_MESSAGE", cm.getActiveProfile()),
                    LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.INFORMATION_MESSAGE);
            isDirty = false;
        } else {
            JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_ERROR_MESSAGE", cm.getActiveProfile()),
                    LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
        }

        return result;
    }

    private boolean saveConfig() {
        return saveProfile(null);
    }

    private boolean saveAsNewConfig() {
        // get all profile Names
        List<String> allProfileNames = cm.getProfiles();

        // select profile name
        String newProfile;
        do {
            newProfile = JOptionPane.showInputDialog(ConfigWindow.this, LocalText.getText("CONFIG_NEW_MESSAGE"),
                LocalText.getText("CONFIG_NEW_TITLE"), JOptionPane.QUESTION_MESSAGE);
        } while (newProfile != null && allProfileNames.contains(newProfile));

        boolean result;
        if ( StringUtils.isBlank(newProfile) ) {
            result = false;
        } else {
            result = saveProfile(newProfile);
            // only change if save was possible
            if (result) {
                // add new item to selection in GameSetupWindow
                if ( parentWindow instanceof GameSetupWindow) {
                    ((GameSetupWindow) parentWindow).addConfigureProfile(newProfile);
                }
                changeProfile(newProfile);
                isDirty = false;
            }
        }
        return result;
    }

    @Override
    protected void closeConfig() {
        super.closeConfig();

        if ( parentWindow instanceof StatusWindow) {
            ((StatusWindow) parentWindow).setMenuItemCheckbox(StatusWindow.CONFIG_CMD, false);
        }
    }

}
