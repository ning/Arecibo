package com.ning.arecibo.util;

public class Logger
{
    private final org.apache.log4j.Logger log4j;

    public enum Level
    {
        DEBUG(org.apache.log4j.Level.DEBUG),
        INFO(org.apache.log4j.Level.INFO),
        WARN(org.apache.log4j.Level.WARN),
        ERROR(org.apache.log4j.Level.ERROR);

        private org.apache.log4j.Level log4jLevel;

        Level(org.apache.log4j.Level log4jLevel)
        {
            this.log4jLevel = log4jLevel;
        }

        org.apache.log4j.Level getLog4jLevel()
        {
            return log4jLevel;
        }
    }

    public static Logger getLoggerViaExpensiveMagic()
    {
        return getLoggerNestedInStacktrace(0);
    }

    public static Logger getCallersLoggerViaExpensiveMagic()
    {
        return getLoggerNestedInStacktrace(1);
    }

    public static Logger getLoggerNestedInStacktrace(int nestingLevel)
    {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stacktrace.length - 1; ++i) {
            if (!stacktrace[i + 1].getClassName().equals(Logger.class.getName())) {
                if (i + 1 + nestingLevel < stacktrace.length) {
                    return getLogger(stacktrace[i + 1 + nestingLevel].getClassName());
                }
                else {
                    throw new IllegalStateException(String.format("Attempt to generate a logger for an invalid nesting level.  " + "Nesting level of [%s] with available nesting level [%s]", nestingLevel, (stacktrace.length - (i + 1))));
                }
            }
        }
        throw new IllegalStateException();
    }

    public static Logger getLogger(Class<?> clazz)
    {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(String name)
    {
        return new Logger(org.apache.log4j.Logger.getLogger(name));
    }

    public Logger(org.apache.log4j.Logger log4j)
    {
        this.log4j = log4j;
    }

    public final void debug(Throwable t, String message, Object... args)
    {
        if (log4j.isDebugEnabled()) {
            log4j.debug(String.format(message, args), t);
        }
    }

    public final void debug(Throwable t, String message)
    {
        log4j.debug(message, t);
    }

    public final void debug(String message)
    {
        log4j.debug(message);
    }

    public final void debug(String message, Object... args)
    {
        if (log4j.isDebugEnabled()) {
            log4j.debug(String.format(message, args));
        }
    }

    public void debug(Throwable e)
    {
        log4j.debug(e, e);
    }

    public final void info(Throwable t, String message, Object... args)
    {
        if (log4j.isInfoEnabled()) {
            log4j.info(String.format(message, args), t);
        }
    }

    public final void info(Throwable t, String message)
    {
        log4j.info(message, t);
    }

    public void info(Throwable e)
    {
        log4j.info(e, e);
    }

    public final void info(String message, Object... args)
    {
        if (log4j.isInfoEnabled()) {
            log4j.info(String.format(message, args));
        }
    }

    public final void info(String message)
    {
        log4j.info(message);
    }

    public final void warn(Throwable t, String message, Object... args)
    {
        log4j.warn(String.format(message, args), t);
    }

    public final void warn(Throwable t, String message)
    {
        log4j.warn(message, t);
    }

    public final void warn(Throwable t)
    {
        log4j.warn(t, t);
    }

    public final void warn(String message, Object... args)
    {
        log4j.warn(String.format(message, args));
    }

    public final void warn(String message)
    {
        log4j.warn(message);
    }

    public final void error(Throwable t, String message, Object... args)
    {
        log4j.error(String.format(message, args), t);
    }

    public final void error(Throwable t, String message)
    {
        log4j.error(message, t);
    }

    public final void error(String message, Object... args)
    {
        log4j.error(String.format(message, args));
    }

    public final void error(String message)
    {
        log4j.error(message);
    }

    public final void error(Throwable e)
    {
        log4j.error(e, e);
    }

    public final void infoDebug(final Throwable t, final String infoMessage, final Object... args)
    {
        if (log4j.isDebugEnabled()) {
            log4j.info(String.format(infoMessage, args), t);
        }
        else if (log4j.isInfoEnabled()) {
            log4j.info(zusammenfassen(t, infoMessage, args));
        }
    }

    public final void infoDebug(final Throwable t, final String infoMessage)
    {
        if (log4j.isDebugEnabled()) {
            log4j.info(infoMessage, t);
        }
        else {
            log4j.info(zusammenfassen(t, infoMessage));
        }
    }

    public void infoDebug(final Throwable t)
    {
        if (log4j.isDebugEnabled()) {
            log4j.info(t, t);
        }
        else {
            log4j.info(zusammenfassen(t, null));
        }
    }

    public final void warnDebug(final Throwable t, final String warnMessage, final Object... args)
    {
        if (log4j.isDebugEnabled()) {
            log4j.warn(String.format(warnMessage, args), t);
        }
        else if (isWarnEnabled()) {
            log4j.warn(zusammenfassen(t, warnMessage, args));
        }
    }

    public final void warnDebug(final Throwable t, final String warnMessage)
    {
        if (log4j.isDebugEnabled()) {
            log4j.warn(warnMessage, t);
        }
        else {
            log4j.warn(zusammenfassen(t, warnMessage));
        }
    }

    public void warnDebug(final Throwable t)
    {
        if (log4j.isDebugEnabled()) {
            log4j.warn(t, t);
        }
        else {
            log4j.warn(zusammenfassen(t, null));
        }
    }

    public final void errorDebug(final Throwable t, final String errorMessage, final Object... args)
    {
        if (log4j.isDebugEnabled()) {
            log4j.error(String.format(errorMessage, args), t);
        }
        else if (isErrorEnabled()) {
            log4j.error(zusammenfassen(t, errorMessage, args));
        }
    }

    public final void errorDebug(final Throwable t, final String errorMessage)
    {
        if (log4j.isDebugEnabled()) {
            log4j.error(errorMessage, t);
        }
        else {
            log4j.error(zusammenfassen(t, errorMessage));
        }
    }

    public void errorDebug(final Throwable t)
    {
        if (log4j.isDebugEnabled()) {
            log4j.error(t, t);
        }
        else {
            log4j.error(zusammenfassen(t, null));
        }
    }

    public final boolean isDebugEnabled()
    {
        return log4j.isDebugEnabled();
    }

    public final boolean isErrorEnabled()
    {
        return log4j.isEnabledFor(org.apache.log4j.Level.ERROR);
    }

    public final boolean isWarnEnabled()
    {
        return log4j.isEnabledFor(org.apache.log4j.Level.WARN);
    }

    public final boolean isInfoEnabled()
    {
        return log4j.isInfoEnabled();
    }

    public final void setLevel(Level level)
    {
        log4j.setLevel(level.getLog4jLevel());
    }

    // http://dict.leo.org/?lp=ende&search=zusammenfassen&searchLoc=0&relink=on&spellToler=standard&cmpType=relaxed
    private String zusammenfassen(final Throwable t, final String format, final Object... args)
    {
        final String message = (t!=null)?t.getMessage():null;

        if (message == null) {
            return formatieren(format, args);
        }

        final int index = message.indexOf("\n");

        if (index == -1) {
            return formatieren(format, args) + ": " + message;
        }

        final String shortMsg = message.substring(0, index);
        return formatieren(format, args) + " (Switch to DEBUG for full stack trace): " + shortMsg;
    }

    // http://dict.leo.org/?lp=ende&search=formatieren&searchLoc=0&relink=on&spellToler=standard&cmpType=relaxed
    private String formatieren(final String format, final Object ... args)
    {
        if (format != null) {
            return String.format(format, args);
        }
        else {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                // Poor mans String format...
                for (int i = 0 ; i < args.length; i++) {
                    sb.append(String.valueOf(args[i]));
                    if (i < args.length - 1) {
                        sb.append(", ");
                    }
                }
            }
            return sb.toString();
        }

    }
}
