package br.org.cria.splinkerapp.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    public static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}