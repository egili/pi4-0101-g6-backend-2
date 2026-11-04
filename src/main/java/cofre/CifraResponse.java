// Retorno da operação de cifragem AES-GCM (dados cifrados e IV em Base64)
package cofre;

public class CifraResponse extends CryptoResponse {

    private String dadosCifrados;
    private String iv;

    public CifraResponse() {
        super();
    }

    public CifraResponse(String dadosCifrados, String iv) {
        super(dadosCifrados, iv);
        this.dadosCifrados = dadosCifrados;
        this.iv = iv;
    }

    @Override
    public String getDadosCifrados() {
        return dadosCifrados;
    }

    public void setDadosCifrados(String dadosCifrados) {
        this.dadosCifrados = dadosCifrados;
    }

    @Override
    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String dadosCifrados() {
        return dadosCifrados;
    }

    public String iv() {
        return iv;
    }
}

