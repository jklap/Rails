package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.ConfigItem;
import net.sf.rails.common.ConfigManager;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.ui.swing.elements.RailsIcon;
import net.sf.rails.util.Util;

public abstract class BaseConfigWindow extends JFrame  {
    private static final Logger log = LoggerFactory.getLogger(BaseConfigWindow.class);

    //restrict field width as there may be extremely long texts
    //(e.g. specifying file names >2000px)
    protected static final int MAX_FIELD_WIDTH = 200;

    protected final Window parentWindow;

    protected JTabbedPane configPane;

    protected JPanel buttonPanel;

    protected JButton resetButton;

    protected final ConfigManager cm;

    protected boolean isDirty = false;

    protected final Map<String, Object> configFields = new HashMap<>();

    protected abstract Map<String, List<ConfigItem>> getConfigSections();

    protected abstract String getConfigValue(String name);

    protected abstract void setupButtonPanel();

    public BaseConfigWindow(Window parentWindow) {
        cm = ConfigManager.getInstance();

        // store for various handling issues
        this.parentWindow = parentWindow;

        // hide on close and inform
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                boolean doClose = true;
                if ( isDirty ) {
                    doClose = JOptionPane.showConfirmDialog(BaseConfigWindow.this, LocalText.getText("CONFIG_CLOSE_MESSAGE"),
                            LocalText.getText("CONFIG_CLOSE_TITLE"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
                }
                if ( doClose ) {
                    closeConfig();
                }
            }
        });
    }

    public void init(final boolean startUp) {
        setupButtonPanel();
        setupConfigPane();

        resetButton = new JButton(LocalText.getText("RESET"));
        resetButton.addActionListener(actionEvent -> resetFields());
        resetButton.setEnabled(false);
        buttonPanel.add(resetButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(actionEvent -> closeConfig());
        closeButton.setEnabled(true);
        buttonPanel.add(closeButton);

        SwingUtilities.invokeLater(new Thread(() -> {
            BaseConfigWindow.this.repaint();
            if ( startUp ) {
                BaseConfigWindow.this.setMaximumSize(new Dimension(800, 600));
                BaseConfigWindow.this.setSize(BaseConfigWindow.this.getPreferredSize());
            }
        }));
    }

    protected void setupConfigPane() {
        configPane.removeAll();

        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_SETTINGS"));
        configPane.setBorder(titled);

        Map<String, List<ConfigItem>> configSections = getConfigSections();
        int maxElements = getMaxElementsInPanels(configSections);

        for (Map.Entry<String, List<ConfigItem>> entry :configSections.entrySet()) {
            JPanel newPanel = new JPanel();
            newPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(5,5,5,5);
            //gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.anchor = GridBagConstraints.NORTHWEST;

            for (ConfigItem item: entry.getValue()) {
                gbc.gridx = 0;
                gbc.gridy++;
                defineElement(newPanel, item, gbc);
            }
            // fill up to maxElements
            gbc.gridx = 0;
            while (++gbc.gridy < maxElements) {
                JLabel emptyLabel = new JLabel("");
                newPanel.add(emptyLabel, gbc);
            }
            JScrollPane slider = new JScrollPane(newPanel);
            configPane.addTab(LocalText.getText("Config.section." + entry.getKey()), slider);
        }
    }

    public int getMaxElementsInPanels(Map<String, List<ConfigItem>> configSections) {
        int maxElements = 0;
        for (List<ConfigItem> panel : configSections.values()) {
            maxElements = Math.max(maxElements, panel.size());
        }
        log.debug("Configuration sections with maximum elements of {}", maxElements);
        return maxElements;
    }

    private void addToGridBag(JComponent container, JComponent element, GridBagConstraints gbc) {
        container.add(element, gbc);
        gbc.gridx ++;
    }

    private void addEmptyLabel(JComponent container, GridBagConstraints gbc) {
//        JLabel label = new JLabel("x");
        //addToGridBag(container, label, gbc );
        GridBagConstraints horizontalFill = new GridBagConstraints();
        horizontalFill.anchor = GridBagConstraints.WEST;
        horizontalFill.fill = GridBagConstraints.HORIZONTAL;
        container.add(Box.createHorizontalGlue(), horizontalFill);
    }

    private void defineElement(JPanel panel, final ConfigItem item, GridBagConstraints gbc) {
        // current value (based on current changes and properties)
        String configValue = getConfigValue(item.name);

        // item label, toolTip and infoText
        final String itemLabel = LocalText.getText("Config.label." + item.name);
        final String toolTip = LocalText.getTextWithDefault("Config.toolTip." + item.name, null);
        final String infoText = LocalText.getTextWithDefault("Config.infoText." + item.name, null);

        // define label
        JLabel label = new JLabel(itemLabel);
        label.setToolTipText(toolTip);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.8;
        addToGridBag(panel, label, gbc);

        gbc.weightx = 1;

        switch (item.type) {
            case BOOLEAN:
                final JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(Util.parseBoolean(configValue));
                checkBox.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent arg0) {
                        checkAndSet(item, checkBox.isSelected() ? "yes" : "no");
                    }
                });
                gbc.fill = GridBagConstraints.HORIZONTAL;
                addToGridBag(panel, checkBox, gbc);
                break;
            case PERCENT: // percent uses a spinner with 5 changes
            case INTEGER:
                int spinnerStepSize;
                final int spinnerMultiple;
                if (item.type == ConfigItem.ConfigType.PERCENT) {
                    spinnerStepSize = 5;
                    spinnerMultiple = 100;
                } else {
                    spinnerStepSize = 1;
                    spinnerMultiple = 1;
                }
                int spinnerValue;
                try {
                    spinnerValue = (int)Math.round(Double.parseDouble(configValue) * spinnerMultiple);
                } catch (NumberFormatException e) {
                    spinnerValue = 0;
                }
                final JSpinner spinner = new JSpinner(new SpinnerNumberModel(spinnerValue, Integer.MIN_VALUE, Integer.MAX_VALUE, spinnerStepSize));
                ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().addPropertyChangeListener(propertyChangeEvent -> {
                    Integer value = (Integer)spinner.getValue();
                    String newValue = value.toString();
                    if (item.type == ConfigItem.ConfigType.PERCENT) {
                        double adjValue = (double)value / spinnerMultiple;
                        newValue = Double.toString(adjValue);
                    }
                    checkAndSet(item, newValue);
                });
                spinner.setMinimumSize(new Dimension(10, spinner.getPreferredSize().height));
//                gbc.fill = GridBagConstraints.HORIZONTAL;
                addToGridBag(panel, spinner, gbc);
                addEmptyLabel(panel, gbc);
                break;
            case FONT: // fonts are a special list
                if (!Util.hasValue(configValue)) {
                    configValue = ((Font)UIManager.getDefaults().get("Label.font")).getFamily();
                }
                // fall through
            case LIST:
                String[] allowedValues = new String[1];
                if (item.type == ConfigItem.ConfigType.FONT) {
                    allowedValues = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                } else {
                    allowedValues = item.allowedValues.toArray(allowedValues);
                }
                final JComboBox<String> comboBox = new JComboBox<>(allowedValues);
                comboBox.setSelectedItem(configValue);
                comboBox.setToolTipText(toolTip);
                comboBox.addFocusListener(new FocusAdapter() {
                      @Override
                      public void focusLost(FocusEvent arg0) {
                          checkAndSet(item, (String)comboBox.getSelectedItem());
                      }
                });
                gbc.fill = GridBagConstraints.HORIZONTAL;
                addToGridBag(panel, comboBox, gbc);
                addEmptyLabel(panel, gbc);
                break;
            case DIRECTORY:
            case FILE:
                final JLabel dirLabel = new JLabel(configValue + " "); //add whitespace for non-zero preferred size
                dirLabel.setHorizontalAlignment(SwingConstants.CENTER);
                dirLabel.setToolTipText(toolTip);
                dirLabel.setPreferredSize(new Dimension(MAX_FIELD_WIDTH, dirLabel.getPreferredSize().height));
                gbc.fill = GridBagConstraints.HORIZONTAL;
                addToGridBag(panel, dirLabel, gbc);
                JButton dirButton = new JButton("Choose...");
                dirButton.addActionListener(actionEvent -> {
                    JFileChooser fc = new JFileChooser();
                    if (item.type == ConfigItem.ConfigType.DIRECTORY) {
                        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    } else {
                        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    }
                    fc.setSelectedFile(new File(dirLabel.getText()));
                    int state = fc.showOpenDialog(BaseConfigWindow.this);
                    if ( state == JFileChooser.APPROVE_OPTION ){
                        String newValue = fc.getSelectedFile().getPath();
                        if ( checkAndSet(item, newValue) ) {
                            dirLabel.setText(newValue);
                        }
                    }
                });
                gbc.fill = GridBagConstraints.NONE;
                addToGridBag(panel, dirButton, gbc);
                break;
            case COLOR:
                final JLabel colorLabel = new JLabel(configValue);
                Color selectedColor;
                try {
                    selectedColor = Util.parseColour(configValue);
                } catch (ConfigurationException e) {
                    selectedColor = Color.WHITE;
                }
                colorLabel.setOpaque(true);
                colorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                colorLabel.setBackground(selectedColor);
                if (Util.isDark(selectedColor)) {
                    colorLabel.setForeground(Color.WHITE);
                } else {
                    colorLabel.setForeground(Color.BLACK);
                }
                colorLabel.setToolTipText(toolTip);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                addToGridBag(panel, colorLabel, gbc);
                JButton colorButton = new JButton("Choose...");
                colorButton.addActionListener(
                        actionEvent -> {
                            Color newColor = JColorChooser.showDialog(BaseConfigWindow.this, "", colorLabel.getBackground());
                            if ( newColor == null) return;
                            String newValue = Integer.toHexString(newColor.getRGB()).substring(2);
                            if ( checkAndSet(item, newValue) ) {
                                colorLabel.setText(newValue);
                                colorLabel.setBackground(newColor);
                                if ( Util.isDark(newColor) ) {
                                    colorLabel.setForeground(Color.WHITE);
                                }
                                else {
                                    colorLabel.setForeground(Color.BLACK);
                                }
                            }
                        }
                );
                gbc.fill = GridBagConstraints.NONE;
                addToGridBag(panel, colorButton, gbc);
                break;
            case REGEX:
            case STRING:
            default: // default like String
                final JFormattedTextField textField = new JFormattedTextField();
                textField.setValue(configValue);
                textField.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent arg0) {
                        checkAndSet(item, textField.getText());
                    }
                });
                int width = textField.getPreferredSize().width > 1 ? textField.getPreferredSize().width : 100;
                textField.setPreferredSize(new Dimension(Math.min(MAX_FIELD_WIDTH, width), textField.getPreferredSize().height));
                gbc.fill = GridBagConstraints.HORIZONTAL;
                addToGridBag(panel, textField, gbc);
                addEmptyLabel(panel, gbc);
                break;
        } // switch

        // add info icon for infoText
        if (infoText != null) {
            JLabel infoIcon = new JLabel(RailsIcon.INFO.smallIcon);
            infoIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    final JDialog dialog = new JDialog(BaseConfigWindow.this, false);
                    final JOptionPane optionPane = new JOptionPane();
                    optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                    optionPane.setMessage(infoText);
                    optionPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, propertyChangeEvent -> dialog.dispose());
                    dialog.setTitle(LocalText.getText("CONFIG_INFO_TITLE", itemLabel));
                    dialog.getContentPane().add(optionPane);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
            gbc.fill = GridBagConstraints.NONE;
            addToGridBag(panel, infoIcon, gbc);
            addEmptyLabel(panel, gbc);
        }
    }

    private boolean checkAndSet(ConfigItem configItem, String newValue) {
        if ( StringUtils.compare(getConfigValue(configItem.name), newValue) != 0 ) {
            log.warn("found change for {}: {} to {}", configItem.name, getConfigValue(configItem.name), newValue);
            configItem.setNewValue(newValue);
            isDirty(configItem);
            return true;
        }
        return false;
    }

    protected void resetFields() {
        for ( Map.Entry<String, Object> entry : configFields.entrySet() ) {
            String configValue = getConfigValue(entry.getKey());

            if ( entry.getValue() instanceof JCheckBox ) {
                ((JCheckBox) entry.getValue()).setSelected(Util.parseBoolean(configValue));
            } else if ( entry.getValue() instanceof JFormattedTextField ) {
                ((JFormattedTextField)entry.getValue()).setValue(configValue);
            } else {
                log.warn("unhandled field for {}: {}", entry.getKey(), entry.getValue().getClass().getName());
            }
        }
    }

    protected void isDirty(ConfigItem configItem) {
        isDirty = true;
        resetButton.setEnabled(true);
    }

    protected void repaintLater() {
        EventQueue.invokeLater(() -> {
            init(false);
            BaseConfigWindow.this.repaint();
        });
    }

    protected void closeConfig() {
        // TODO: check if isDirty and alert

        this.setVisible(false);

        // TODO: should reset state so if the user re-opens anything they changed but didn't save is it left hanging around
    }
}
