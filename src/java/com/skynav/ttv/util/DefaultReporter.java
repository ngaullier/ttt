/*
 * Copyright 2013 Skynav, Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY SKYNAV, INC. AND ITS CONTRIBUTORS “AS IS” AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SKYNAV, INC. OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
package com.skynav.ttv.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.UnmarshalException;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

public class DefaultReporter implements Reporter {

    /* general state */
    private Map<String,Boolean> defaultWarnings;
    private PrintWriter output;
    /* options state */
    private int debug;
    private boolean disableWarnings;
    private Set<String> disabledWarnings;
    private Set<String> enabledWarnings;
    private boolean hideLocation;
    private boolean hidePath;
    private boolean hideWarnings;
    private boolean hideResourceLocation;
    private boolean treatWarningAsError;
    private int verbose;
    /* per-resource state */
    private Set<String> resourceDisabledWarnings;
    private Set<String> resourceEnabledWarnings;
    private int resourceErrors;
    private URI resourceUri;
    private String resourceUriString;
    private int resourceWarnings;

    public DefaultReporter() {
        this(null, null);
    }

    public DefaultReporter(Object[][] defaultWarningSpecifications) {
        this(defaultWarningSpecifications, null);
    }

    public DefaultReporter(Object[][] defaultWarningSpecifications, PrintWriter output) {
        Map<String,Boolean> defaultWarnings = new java.util.HashMap<String,Boolean>();
        if (defaultWarningSpecifications != null) {
            for (Object[] spec : defaultWarningSpecifications) {
                defaultWarnings.put((String) spec[0], (Boolean) spec[1]);
            }
        }
        this.defaultWarnings = defaultWarnings;
        this.disabledWarnings = new java.util.HashSet<String>();
        this.enabledWarnings = new java.util.HashSet<String>();
        this.hideLocation = false;
        this.hidePath = true;
        this.output = output;
    }

    public void resetResourceState() {
        resourceDisabledWarnings = new java.util.HashSet<String>(disabledWarnings);
        resourceEnabledWarnings = new java.util.HashSet<String>(enabledWarnings);
        resourceErrors = 0;
        resourceWarnings = 0;
    }

    public void setResourceURI(String uri) {
        resourceUriString = uri;
    }

    public void setResourceURI(URI uri) {
        resourceUri = uri;
    }

    public void hidePath() {
        this.hidePath = true;
    }

    public void showPath() {
        this.hidePath = false;
    }

    public boolean isHidingPath() {
        return this.hidePath;
    }

    public void hideLocation() {
        this.hideLocation = true;
    }

    public void showLocation() {
        this.hideLocation = false;
    }

    public boolean isHidingLocation() {
        return this.hideLocation;
    }

    public int getResourceErrors() {
        return resourceErrors;
    }

    public int getResourceWarnings() {
        return resourceWarnings;
    }

    public void setOutput(PrintWriter output) {
        this.output = output;
    }

    public PrintWriter getOutput() {
        if (output == null)
            output = new PrintWriter(System.out);
        return output;
    }

    protected void out(ReportType type, String message) {
        char typeChar;
        if (type == ReportType.Error)
            typeChar = 'E';
        else if (type == ReportType.Warning)
            typeChar = 'W';
        else if (type == ReportType.Info)
            typeChar = 'I';
        else if (type == ReportType.Debug)
            typeChar = 'D';
        else
            typeChar = '?';
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append(typeChar);
        sb.append(']');
        sb.append(':');
        sb.append(message);
        getOutput().println(sb.toString());
    }

    protected void out(ReportType type, Message message) {
        out(type, message.toText(isHidingLocation(), isHidingPath()));
    }

    public void flush() {
        if (output != null)
            output.flush();
    }

    public void setVerbosityLevel(int verbose) {
        this.verbose = verbose;
    }

    public void incrementVerbosityLevel() {
        ++this.verbose;
    }

    public int getVerbosityLevel() {
        return this.verbose;
    }

    public void setDebugLevel(int debug) {
        this.debug = debug;
    }

    public void incrementDebugLevel() {
        ++this.debug;
    }

    public int getDebugLevel() {
        return this.debug;
    }

    private Message message(String message) {
        return message((String) null, message);
    }

    public Message message(String key, String format, Object... arguments) {
        String uriString;
        if (resourceUri != null)
            uriString = resourceUri.toString();
        else if (resourceUriString != null)
            uriString = resourceUriString;
        else
            uriString = null;
        return new LocatedMessage(uriString, -1, -1, key, format, arguments);
    }

    private Message message(Locator locator, String message) {
        return message(locator, (String) null, message);
    }

    public Message message(Locator locator, String key, String format, Object... arguments) {
        String sysid = locator.getSystemId();
        String uriString;
        if ((sysid != null) && (sysid.length() > 0))
            uriString = sysid;
        else if (resourceUri != null)
            uriString = resourceUri.toString();
        else if (resourceUriString != null)
            uriString = resourceUriString;
        else
            uriString = null;
        return new LocatedMessage(uriString, locator.getLineNumber(), locator.getColumnNumber(), key, format, arguments);
    }

    private void logError(String message) {
        out(ReportType.Error, message);
        ++resourceErrors;
    }

    public void logError(Message message) {
        out(ReportType.Error, message);
        ++resourceErrors;
    }

    private void logError(Locator locator, String message) {
        logError(message(locator, message));
    }

    public void logError(Locator locator, Message message) {
        logError(message(locator, message.toText(isHidingLocation(), isHidingPath())));
    }

    private Locator extractLocator(SAXParseException e) {
        LocatorImpl locator = new LocatorImpl();
        locator.setSystemId(e.getSystemId());
        locator.setLineNumber(e.getLineNumber());
        locator.setColumnNumber(e.getColumnNumber());
        return locator;
    }

    private Message extractMessage(SAXParseException e) {
        String message = e.getMessage();
        if (message.indexOf("cvc") == 0) {
            int cvcEndIndex = message.indexOf(":");
            String cvcLabel = message.substring(0, cvcEndIndex);
            String cvcRemainder = message.substring(cvcEndIndex + 1);
            message = "XSD(" + cvcLabel + "):" + cvcRemainder;
        }
        return message(extractLocator(e), massageMessage(message));
    }

    private Locator extractLocator(UnmarshalException e) {
        LocatorImpl locator = new LocatorImpl();
        locator.setSystemId(resourceUriString);
        return locator;
    }

    private Message extractMessage(UnmarshalException e) {
        return message(extractLocator(e), massageMessage(e.getMessage()));
    }

    private Message extractMessage(Throwable e) {
        if ((e.getCause() != null) && (e.getCause() != e))
            return extractMessage(e.getCause());
        else if (e instanceof SAXParseException)
            return extractMessage((SAXParseException) e);
        else if (e instanceof UnmarshalException)
            return extractMessage((UnmarshalException) e);
        else {
            String message = massageMessage(e.getMessage());
            if ((message == null) || (message.length() == 0))
                message = e.toString();
            return message(message + ((debug < 2) ? "; retry with --debug-exceptions option for additional information." : "."));
        }
    }

    private String massageMessage(String message) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, n = message.length(); i < n; ++i) {
            char c = message.charAt(i);
            if (c == '\'')
                sb.append("''");
            else if (c == '{')
                sb.append("'{'");
            else if (c == '}')
                sb.append("'}'");
            else if ((i == 0) && Character.isLowerCase(c))
                sb.append(Character.toUpperCase(c));
            else
                sb.append(c);
        }
        if (sb.charAt(sb.length() -1) != '.')
            sb.append('.');
        return sb.toString();
    }

    public void logError(Exception e) {
        logError(extractMessage(e));
        logDebug(e);
    }

    public boolean hasDefaultWarning(String token) {
        return defaultWarnings.containsKey(token);
    }

    private Set<String> getEnabledWarnings() {
        if (this.resourceEnabledWarnings != null)
            return this.resourceEnabledWarnings;
        else
            return this.enabledWarnings;
    }

    private Set<String> getDisabledWarnings() {
        if (this.resourceDisabledWarnings != null)
            return this.resourceDisabledWarnings;
        else
            return this.disabledWarnings;
    }

    public void setTreatWarningAsError(boolean treatWarningAsError) {
        this.treatWarningAsError = treatWarningAsError;
    }

    public boolean isWarningEnabled(String token) {
        boolean enabled = defaultWarnings.get(token);
        if (getEnabledWarnings().contains(token) || getEnabledWarnings().contains("all"))
            enabled = true;
        if (getDisabledWarnings().contains(token) || getDisabledWarnings().contains("all"))
            enabled = false;
        return enabled;
    }

    public void enableWarning(String token) {
        getEnabledWarnings().add(token);
    }

    public void disableWarning(String token) {
        getDisabledWarnings().add(token);
    }

    public void disableWarnings() {
        this.disableWarnings = true;
    }

    public void hideWarnings() {
        this.hideWarnings = true;
    }

    private boolean logWarning(String message) {
        if (!disableWarnings) {
            if (!hideWarnings)
                out(ReportType.Warning, message);
            ++resourceWarnings;
        }
        return treatWarningAsError;
    }

    public boolean logWarning(Message message) {
        return logWarning(message.toText(isHidingLocation(), isHidingPath()));
    }

    private boolean logWarning(Locator locator, String message) {
        return logWarning(message(locator, message));
    }

    public boolean logWarning(Locator locator, Message message) {
        return logWarning(locator, message.toText(isHidingLocation(), isHidingPath()));
    }

    public boolean logWarning(Exception e) {
        boolean treatedAsError = logWarning(extractMessage(e));
        logDebug(e);
        return treatedAsError;
    }

    private void logInfo(String message) {
        if (verbose > 0) {
            out(ReportType.Info, message);
        }
    }

    public void logInfo(Message message) {
        logInfo(message.toText(isHidingLocation(), isHidingPath()));
    }

    private void logInfo(Locator locator, String message) {
        logInfo(message(locator, message));
    }

    public void logInfo(Locator locator, Message message) {
        logInfo(locator, message.toText(isHidingLocation(), isHidingPath()));
    }

    private void logDebug(String message) {
        if (isDebuggingEnabled()) {
            out(ReportType.Debug, message);
        }
    }

    public void logDebug(Message message) {
        logDebug(message.toText(isHidingLocation(), isHidingPath()));
    }

    private void logDebug(Locator locator, String message) {
        logDebug(message(locator, message));
    }

    public void logDebug(Locator locator, Message message) {
        logDebug(locator, message.toText(isHidingLocation(), isHidingPath()));
    }

    private boolean isDebuggingEnabled(int level) {
        return debug >= level;
    }

    private boolean isDebuggingEnabled() {
        return isDebuggingEnabled(1);
    }

    private void logDebug(Exception e) {
        if (isDebuggingEnabled(2)) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logDebug(sw.toString());
        }
    }

    public void showProcessingInfo() {
        if (verbose >  0) {
            if (treatWarningAsError)
                logInfo("Warnings are treated as errors.");
            else if (disableWarnings)
                logInfo("Warnings are disabled.");
            else if (hideWarnings)
                logInfo("Warnings are hidden.");
        }
    }
}
