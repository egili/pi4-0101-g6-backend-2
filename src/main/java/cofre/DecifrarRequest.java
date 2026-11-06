// O que o Front-end envia para decifrar um blob
package cofre;

public class DecifrarRequest {
    private String dadosCifrados;
    private String iv;
    private String chaveCifra;

    public boolean isInvalido() {
        return emBranco(dadosCifrados) || emBranco(iv) || emBranco(chaveCifra);
    }

    private static boolean emBranco(String valor) {
        return valor == null || valor.isBlank();
    }

    public String getDadosCifrados() {
        return dadosCifrados;
    }

    public void setDadosCifrados(String dadosCifrados) {
        this.dadosCifrados = dadosCifrados;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getChaveCifra() {
        return chaveCifra;
    }

    public void setChaveCifra(String chaveCifra) {
        this.chaveCifra = chaveCifra;
    }
}
