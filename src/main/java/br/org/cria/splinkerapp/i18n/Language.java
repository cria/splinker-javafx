package br.org.cria.splinkerapp.i18n;

public enum Language {
    PORTUGUESE("pt"),
    SPANISH("es"),
    ENGLISH("en");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String code() { return code; }

    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code.equals(code)) return language;
        }
        return PORTUGUESE;
    }

    @Override
    public String toString() { return I18n.text(this, "language.name"); }
}
