package br.org.cria.splinkerapp.utils;
import org.apache.commons.lang3.StringUtils;

public final class StringStandards {

    public static String normalizeString(String str) 
    {
        var normalizedString = StringUtils.stripAccents(str.toLowerCase()).trim()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_")
                .replaceAll("(_+)", "_");
        var lastCharPosition = normalizedString.length() - 1;
        var lastChar = normalizedString.charAt(lastCharPosition);
        var firstChar = normalizedString.charAt(0);
        
        if(firstChar == '_')
        {
            normalizedString = normalizedString.substring(1, lastCharPosition);
        }
        if(lastChar == '_')
        {
            normalizedString = normalizedString.substring(0, lastCharPosition-1);
        }
        
        return normalizedString;
    }
    
}
