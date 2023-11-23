package br.org.cria.splinkerapp.utils;
import org.apache.commons.lang3.StringUtils;

public final class StringStandards {

    public static String normalizeString(String str) 
    {
        var normalizedString = StringUtils.stripAccents(str.toLowerCase()).trim()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_")
                .replace("__", "_");
                //TODO: LTRIM e RTRIM na string
                //TODO: Checar caracteres especiais no começo e fim da string
        
        return normalizedString;
    }
    
}
