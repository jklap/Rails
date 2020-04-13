package net.sf.rails.util.logback;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import ch.qos.logback.core.PropertyDefinerBase;
import net.sf.rails.common.Config;
import net.sf.rails.common.ConfigManager;
import net.sf.rails.util.SystemOS;

public class LogFile extends PropertyDefinerBase {

    public static final String CONFIG_LOG_DIRECTORY = "log.directory";
    public static final String CONFIG_LOG_FILENAME_PATTERN = "log.filename";

    @Override
    public String getPropertyValue() {
        ConfigManager.initConfiguration(false);

        String logDir = Config.get(CONFIG_LOG_DIRECTORY);
        if ( StringUtils.isBlank(logDir) ) {
            logDir = System.getProperty("user.home");
            switch ( SystemOS.get() ) {
                case MAC:
                case UNIX:
                    logDir = logDir + File.separator + ".rails" + File.separator;

                case WINDOWS:
                default:
                    // nothing to do
            }
        }

        String fileName = Config.get(CONFIG_LOG_FILENAME_PATTERN);
        if ( StringUtils.isNotBlank(fileName) ) {
            if ( fileName.startsWith(File.separator ) ) {
                // use the whole thing as is
                logDir = "";
            }
            // support inject of date, etc
            StringSubstitutor substitutor = StringSubstitutor.createInterpolator();
            substitutor.setEnableSubstitutionInVariables(true);
            fileName = substitutor.replace(fileName);
            if ( SystemOS.get() == SystemOS.MAC || SystemOS.get() == SystemOS.UNIX ) {
                // sanitize
                fileName = fileName.replace(' ', '_');
            }
        }
        else {
            fileName = "18xx_" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".log";
        }

        String file = logDir + fileName;
        return file;
    }
}
