public class Bloque {
    private final int numero;
    private final String datos;
    private final String hashAnterior;
    private final long nonce;
    private final String hash;

    public Bloque(int numero, String datos, String hashAnterior, long nonce, String hash) {
        this.numero = numero;
        this.datos = datos;
        this.hashAnterior = hashAnterior;
        this.nonce = nonce;
        this.hash = hash;
    }

    public int getNumero() { return numero; }
    public String getDatos() { return datos; }
    public String getHashAnterior() { return hashAnterior; }
    public long getNonce() { return nonce; }
    public String getHash() { return hash; }
}
