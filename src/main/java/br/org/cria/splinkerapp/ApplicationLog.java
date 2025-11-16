package br.org.cria.splinkerapp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApplicationLog {
    protected static final Logger logger = LogManager.getLogger(ApplicationLog.class);

    public static void error(String errorMessage) {
        logger.error(errorMessage);
    }

    public static void info(String infoMessage) {
        logger.info("spLinker - " + infoMessage);
    }

    public static void debug(String message) {
        logger.debug(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }
}
