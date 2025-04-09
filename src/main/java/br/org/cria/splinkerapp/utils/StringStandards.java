package br.org.cria.splinkerapp.utils;

import org.apache.commons.lang3.StringUtils;

public final class StringStandards {

    public static String normalizeString(String str) {
        var lowered = str.toLowerCase().trim();
        var stripped = StringUtils.stripAccents(lowered);
        var normalizedString = stripped
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_")
                .replaceAll("(_+)", "_");
        var lastCharPosition = normalizedString.length() - 1;
        if (normalizedString.length() > 0) {
            var lastChar = normalizedString.charAt(lastCharPosition);
            var firstChar = normalizedString.charAt(0);

            if (firstChar == '_') {
                normalizedString = normalizedString.substring(1, lastCharPosition);
            }
            if (lastChar == '_') {
                normalizedString = normalizedString.substring(0, lastCharPosition);
            }
        }


        return normalizedString;
    }

}
