package mz.maputobustrackerModule.domain;

import java.io.Serializable;

/**
 * Created by Hawkingg on 23/09/2016.
 */
public class Associacao implements Serializable {

    private String idDispositivo;
    private String codAutocarro;

    public String getIdDispositivo() {
        return idDispositivo;
    }

    public void setIdDispositivo(String idDispositivo) {
        this.idDispositivo = idDispositivo;
    }

    public String getCodAutocarro() {
        return codAutocarro;
    }

    public void setCodAutocarro(String codAutocarro) {
        this.codAutocarro = codAutocarro;
    }
}
