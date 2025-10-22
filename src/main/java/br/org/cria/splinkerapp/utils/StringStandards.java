package br.org.cria.splinkerapp.utils;

import org.apache.commons.lang3.StringUtils;

public class StringStandards {

    public static String normalizeString(String str) {
        var lowered = str.toLowerCase().trim();
        var stripped = StringUtils.stripAccents(lowered);
        return stripped
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_")
                .replaceAll("(_+)", "_");

    }

}
