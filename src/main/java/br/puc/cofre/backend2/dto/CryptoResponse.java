//O que é devolvido pro Front-end
package br.puc.cofre.backend2.dto;

public class CryptoResponse {
    private String salt;
    private String hashLogin;
    private String chaveCifra;

    public CryptoResponse(String salt, String hashLogin, String chaveCifra) {
        this.salt = salt;
        this.hashLogin = hashLogin;
        this.chaveCifra = chaveCifra;
    }

    public String getSalt() { return salt; }
    public String getHashLogin() { return hashLogin; }
    public String getChaveCifra() { return chaveCifra; }
}