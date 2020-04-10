package net.sf.rails.util;

import java.awt.*;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.io.File;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.ConfigManager;
import net.sf.rails.ui.swing.AutoSaveLoadDialog;
import net.sf.rails.ui.swing.GameSetupController;

public class RunGame {

    private static final Logger log = LoggerFactory.getLogger(RunGame.class);

    public static void main(String[] args) {

        // Initialize configuration
        ConfigManager.initConfiguration(false);

        String fileName = null;

        if (args != null && args.length > 0) {
            for (String arg : args) {
                System.out.println ("Arg: "+arg);
            }
            fileName = args[0];
        }

        if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            Desktop.getDesktop().setOpenFileHandler(e -> {
                for ( File file : e.getFiles() ) {
                    // open the file
                    log.warn("found file: {}", file.toString());
                    //GameLoader.loadAndStartGame(file);
                    // break out as we only handle one game at a time...
                    break;
                }
            });
        }

        // currently we only start one game
        if ( fileName != null ) {
            GameLoader.loadAndStartGame(new File(fileName));
        } else {
            /* Start the rails.game selector, which will do all the rest. */
            GameSetupController.Builder setupBuilder = GameSetupController.builder();
            setupBuilder.start();
        }
    }
}
