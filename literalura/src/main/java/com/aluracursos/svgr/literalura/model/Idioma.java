package com.aluracursos.svgr.literalura.model;

public enum Idioma {
    ES("es"),
    EN("en");

    String idiomas;

    private Idioma(String idiomas) {
        this.idiomas = idiomas;
    }

    public String getIdiomas() {
        return idiomas;
    }

    public static Idioma fromString(String text) {
        for (Idioma idiomas : Idioma.values()) {
            if (idiomas.idiomas.equalsIgnoreCase(text)) {
                return idiomas;
            }
        }
        throw new IllegalArgumentException("No hay similitudes: " + text);
    }
}
