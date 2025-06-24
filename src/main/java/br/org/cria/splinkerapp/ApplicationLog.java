package br.org.cria.splinkerapp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApplicationLog {
    protected static final Logger logger = LogManager.getLogger(ApplicationLog.class);

    public static void error(String erroMessage) {
        System.out.println(erroMessage);
    }

    public static void info(String warningMessage) {
        System.out.println(warningMessage);
    }

    public static void debug(String message) {
        logger.debug(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }
}
