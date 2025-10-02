/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l1j.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Custom formatter for console logging in L1J server
 * Provides formatted output with timestamps, class names, and exception handling
 */
public class ConsoleLogFormatter extends Formatter {
    
    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    
    private final SimpleDateFormat dateFormatter;
    
    /**
     * Creates a new ConsoleLogFormatter instance
     */
    public ConsoleLogFormatter() {
        this.dateFormatter = new SimpleDateFormat(DATE_FORMAT);
    }
    
    @Override
    public String format(LogRecord record) {
        StringBuilder output = new StringBuilder();
        
        // Handle CONFIG level messages (simple output without formatting)
        if (record.getLevel().intValue() == Level.CONFIG.intValue()) {
            output.append(record.getMessage());
            output.append(LINE_SEPARATOR);
            return output.toString();
        }
        
        // Format standard log messages
        formatStandardMessage(output, record);
        
        // Handle exceptions if present
        if (record.getThrown() != null) {
            formatException(output, record);
        }
        
        return output.toString();
    }
    
    /**
     * Formats a standard log message with timestamp, class, method, level and message
     */
    private void formatStandardMessage(StringBuilder output, LogRecord record) {
        output.append(dateFormatter.format(new Date(record.getMillis())));
        output.append(" ");
        output.append(extractSimpleClassName(record.getSourceClassName()));
        output.append(".");
        output.append(record.getSourceMethodName());
        output.append(" ");
        output.append(record.getLevel());
        output.append(": ");
        output.append(record.getMessage());
        output.append(LINE_SEPARATOR);
    }
    
    /**
     * Formats exception information with stack trace
     */
    private void formatException(StringBuilder output, LogRecord record) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            
            record.getThrown().printStackTrace(printWriter);
            printWriter.close();
            
            output.append(dateFormatter.format(new Date(record.getMillis())));
            output.append(" ");
            output.append(stringWriter.toString());
            output.append(LINE_SEPARATOR);
            
        } catch (Exception ex) {
            // Silently ignore formatting errors to prevent logging loops
        }
    }
    
    /**
     * Extracts the simple class name from a fully qualified class name
     * @param fullClassName The fully qualified class name
     * @return The simple class name, or the full name if extraction fails
     */
    private String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null) {
            return "Unknown";
        }
        
        String[] parts = fullClassName.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : fullClassName;
    }
}