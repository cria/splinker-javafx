### Detalhes técnicos da implementação

1) parsers/CsvFileParser.java: A checagem do limiter (se existe ou não) é para os casos de CSV separado por vírgulas. Nesses casos, o columnRow vem como array mesmo e não com todos os elements no elemento 0 do array.