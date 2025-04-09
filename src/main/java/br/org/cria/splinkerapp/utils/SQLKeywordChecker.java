package br.org.cria.splinkerapp.utils;

import java.util.Set;

public class SQLKeywordChecker {
    private static final Set<String> SQL_KEYWORDS = Set.of(
            "select", "insert", "update", "delete", "from", "where", "join",
            "inner", "outer", "left", "right", "group", "by", "order", "having",
            "distinct", "table", "create", "alter", "drop", "index", "primary",
            "foreign", "key", "references", "view", "check", "constraint",
            "default", "case", "when", "then", "else", "end", "as", "like", "not",
            "null", "is", "and", "or", "between", "exists", "union", "all",
            "into", "values", "set", "commit", "rollback", "grant", "revoke"
    );

    public static boolean isReservedSQLKeyword(String word) {
        return SQL_KEYWORDS.contains(word.toLowerCase());
    }
}
