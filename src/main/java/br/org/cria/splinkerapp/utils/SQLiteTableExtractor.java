package br.org.cria.splinkerapp.utils;

import java.util.*;
import java.util.regex.*;

public class SQLiteTableExtractor {

    public static Set<String> extrairTabelas(String querySql) {
        Set<String> tabelas = new LinkedHashSet<>();
        String query = querySql.toLowerCase().replaceAll("[\\n\\r]+", " ");

        String[] padroes = {
                "\\bfrom\\s+(?:\\[)?([a-zA-Z0-9_-]+)(?:\\])?",
                "\\bjoin\\s+(?:\\[)?([a-zA-Z0-9_-]+)(?:\\])?",
                "\\bupdate\\s+(?:\\[)?([a-zA-Z0-9_-]+)(?:\\])?",
                "\\binto\\s+(?:\\[)?([a-zA-Z0-9_-]+)(?:\\])?"
        };

        for (String padrao : padroes) {
            Matcher matcher = Pattern.compile(padrao).matcher(query);
            while (matcher.find()) {
                String tabela = matcher.group(1);
                tabela = StringStandards.normalizeString(tabela);
                tabelas.add(tabela.toLowerCase());
            }
        }

        return tabelas;
    }
}
