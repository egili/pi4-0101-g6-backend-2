//O que é devolvido pro Front-end
package cofre;

public class CryptoResponse {
    private String salt;
    private String hashLogin;
    private String chaveCifra;
    private String dadosCifrados;
    private String iv;

    public CryptoResponse() {}

    public CryptoResponse(String salt, String hashLogin, String chaveCifra) {
        this.salt = salt;
        this.hashLogin = hashLogin;
        this.chaveCifra = chaveCifra;
    }

    public CryptoResponse(String dadosCifrados, String iv) {
        this.dadosCifrados = dadosCifrados;
        this.iv = iv;
    }

    public String getSalt() { return salt; }
    public String getHashLogin() { return hashLogin; }
    public String getChaveCifra() { return chaveCifra; }
    public String getDadosCifrados() { return dadosCifrados; }
    public String getIv() { return iv; }
}
