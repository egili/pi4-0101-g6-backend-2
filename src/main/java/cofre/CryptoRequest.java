package cofre;

public class CryptoRequest {
    private String senha;
    private String salt; // recebe o salt do login

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
}