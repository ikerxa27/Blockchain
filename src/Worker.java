import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Worker {


    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Uso: java Worker <hashAnterior> <datos> <dificultad> <idWorker> <totalWorkers>");
            System.exit(1);
        }

        String hashAnterior = args[0];
        String datos = args[1];
        int dificultad = Integer.parseInt(args[2]);
        int id = Integer.parseInt(args[3]);
        int total = Integer.parseInt(args[4]);

        String prefijo = "0".repeat(dificultad);

        for (long nonce = id; ; nonce += total) {
            String hash = calcularHash(hashAnterior + "|" + datos + "|" + nonce);
            if (hash.startsWith(prefijo)) {
                System.out.println(nonce + " " + hash);
                System.out.flush();
                return;
            }
        }
    }

    public static String calcularHash(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }
}