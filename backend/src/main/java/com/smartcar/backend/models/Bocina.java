package com.smartcar.backend.models;

public class Bocina {
    public static final String APAGADA = "APAGADA";
    public static final String LARGO = "LARGO";
    public static final String INTERMITENTE = "INTERMITENTE";

    private boolean estaSonando;
    private String patron;
    private int intervaloMs;
    private long ultimoCambioMs;
    public Bocina() {
        this.estaSonando = false;
        this.patron = APAGADA;
        this.intervaloMs = 1000;
        this.ultimoCambioMs = 0;
    }

    public boolean isEstaSonando() {
        return estaSonando;
    }

    public String getPatron() {
        return patron;
    }

    public int getIntervaloMs() {
        return intervaloMs;
    }

    public void sonarLargo() {
        this.patron = LARGO;
        this.estaSonando = true;
    }

    public void sonarIntermitente(int intervaloMs) {
        this.patron = INTERMITENTE;
        this.intervaloMs = intervaloMs;
    }

    public void silenciar() {
        this.patron = APAGADA;
        this.estaSonando = false;
    }

    public void actualizar(long ahoraMs) {
        if (INTERMITENTE.equals(patron)) {
            if (ahoraMs - ultimoCambioMs >= intervaloMs) {
                estaSonando = !estaSonando;
                ultimoCambioMs = ahoraMs;
            }
        }
    }
}