package org.openjdk.jextract;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Filters out logs which does not belong to jextract package
 */
public class LoggerNameFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record.getLevel().intValue() >= Level.WARNING.intValue()) return true;
        var name = record.getSourceClassName();
        if (name == null) return false;
        return name.startsWith(LoggerNameFilter.class.getPackageName());
    }
}
