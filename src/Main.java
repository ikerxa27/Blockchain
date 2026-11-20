import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Main {

    static int DIFICULTAD = 4;

    static final int NUM_WORKERS = 4;

    // Mensaje que un lector de salida deja en la cola. linea == null indica que el worker cerró su salida.
    static class Resultado {
        final int worker;
        final long pid;
        final String linea;

        Resultado(int worker, long pid, String linea) {
            this.worker = worker;
            this.pid = pid;
            this.linea = linea;
        }
    }

    public static void main(String[] args) {

        if (args.length > 0) {
            try {
                DIFICULTAD = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                System.err.println("Error: la dificultad debe ser un número entero.");
                return;
            }
        }
        if (DIFICULTAD < 1 || DIFICULTAD > 64) {
            System.err.println("Error: la dificultad debe estar entre 1 y 64.");
            return;
        }


        Scanner sc = new Scanner(System.in);

        System.out.print("Semilla inicial: ");
        String seed = sc.hasNextLine() ? sc.nextLine().trim() : "";
        if (seed.isEmpty()) {
            System.err.println("Error: la semilla no puede estar vacía.");
            return;
        }

        System.out.print("Ruta del fichero de datos: ");
        String ruta = sc.hasNextLine() ? sc.nextLine().trim() : "";

        List<String> datos = new ArrayList<>();
        try {
            for (String linea : Files.readAllLines(Paths.get(ruta), StandardCharsets.UTF_8)) {
                if (!linea.trim().isEmpty()) {
                    datos.add(linea.trim());
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error: no se puede leer el fichero '" + ruta );
            return;
        }

        if (datos.isEmpty()) {
            System.out.println("El fichero no contiene líneas con datos: no hay bloques que procesar.");
            return;
        }

        String hashAnterior = Worker.calcularHash(seed);
        System.out.println();
        System.out.println("Hash inicial (seed): " + hashAnterior);
        System.out.println("Dificultad: " + DIFICULTAD);
        System.out.println("Bloques a procesar: " + datos.size());
        System.out.println();

        // Construcción de la cadena
        List<Bloque> cadena = new ArrayList<>();

        try {
            for (int i = 0; i < datos.size(); i++) {
                Bloque bloque = minarBloque(i + 1, hashAnterior, datos.get(i));
                cadena.add(bloque);
                hashAnterior = bloque.getHash();
                if (i + 1 < datos.size()) {
                    System.out.println("Preparando el bloque " + (i + 2) + "...");
                    System.out.println();
                }
            }
        } catch (IOException e) {
            System.err.println("Error durante el minado: " + e.getMessage());
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Ejecución interrumpida.");
            return;
        }
        mostrarCadena(cadena);

    }

    // Lanza los workers de un bloque, valida la primera solución correcta
    static Bloque minarBloque(int numero, String hashAnterior, String datos)
            throws IOException, InterruptedException {

        List<Process> workers = new ArrayList<>();
        BlockingQueue<Resultado> cola = new LinkedBlockingQueue<>();

        try {
            String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
            String classpath = System.getProperty("java.class.path");

            for (int i = 0; i < NUM_WORKERS; i++) {
                ProcessBuilder pb = new ProcessBuilder(java, "-cp", classpath, "Worker",
                        hashAnterior, datos, String.valueOf(DIFICULTAD),
                        String.valueOf(i), String.valueOf(NUM_WORKERS));
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process p = pb.start();
                workers.add(p);
                lanzarLector(i, p, cola);
            }

            int cerrados = 0;
            while (cerrados < NUM_WORKERS) {
                // Atiende al primero que responda
                Resultado r = cola.take();

                if (r.linea == null) {
                    cerrados++;
                    continue;
                }

                String[] partes = r.linea.trim().split(" ");
                if (partes.length != 2) {
                    System.out.println("Worker " + r.worker + ": respuesta con formato incorrecto, rechazada.");
                    continue;
                }

                long nonce;
                try {
                    nonce = Long.parseLong(partes[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Worker " + r.worker + ": nonce no válido, propuesta rechazada.");
                    continue;
                }
                String hashRecibido = partes[1];

                //Recalcula el hash por sí mismo
                String hashRecalculado = Worker.calcularHash(hashAnterior + "|" + datos + "|" + nonce);
                boolean coincide = hashRecalculado.equals(hashRecibido);
                boolean prefijoOk = hashRecalculado.startsWith("0".repeat(DIFICULTAD));

                if (!coincide || !prefijoOk) {
                    System.out.println("Worker " + r.worker + ": propuesta rechazada (hash no coincide o prefijo insuficiente).");
                    continue;
                }

                //Se acepta y se terminan los demás workers
                boolean todosFinalizados = terminarWorkers(workers);

                System.out.println("BLOQUE " + numero + " VALIDADO");
                System.out.println();
                System.out.println("Datos: " + datos);
                System.out.println("Hash anterior: " + hashAnterior);
                System.out.println("Dificultad: " + DIFICULTAD);
                System.out.println("Worker ganador: " + r.worker + " — PID: " + r.pid);
                System.out.println("Nonce encontrado: " + nonce);
                System.out.println();
                System.out.println("Hash recibido:");
                System.out.println(hashRecibido);
                System.out.println();
                System.out.println("Hash recalculado por el principal:");
                System.out.println(hashRecalculado);
                System.out.println();
                System.out.println("Validación: hashes coincidentes y prefijo de " + DIFICULTAD + " ceros correcto.");
                System.out.println("Bloque incorporado a la cadena.");
                System.out.println("Workers de esta búsqueda: "
                        + (todosFinalizados ? "todos finalizados." : "ATENCIÓN, alguno sigue activo."));
                System.out.println();

                return new Bloque(numero, datos, hashAnterior, nonce, hashRecalculado);
            }

            throw new IOException("ningún worker aportó una solución válida para el bloque " + numero);

        } finally {
            //No deja ningun hijo vivo
            terminarWorkers(workers);
        }
    }

    //salida estándar del worker y la deja en la cola
    static void lanzarLector(int id, Process p, BlockingQueue<Resultado> cola) {
        Thread t = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linea = br.readLine();
                if (linea != null) {
                    cola.add(new Resultado(id, p.pid(), linea));
                }
            } catch (IOException e) {

            } finally {
                cola.add(new Resultado(id, p.pid(), null));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // Termina los workers que sigan activos y comprueba que han acabado.
    static boolean terminarWorkers(List<Process> workers) {
        boolean todos = true;
        for (Process p : workers) {
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        }
        for (Process p : workers) {
            try {
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    todos = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                todos = false;
            }
        }
        return todos;
    }

    static void mostrarCadena(List<Bloque> cadena) {
        System.out.println("=== CADENA FINAL ===");
        for (Bloque b : cadena) {
            System.out.println("Bloque " + b.getNumero() + " | datos: " + b.getDatos());
            System.out.println("  hash anterior: " + b.getHashAnterior());
            System.out.println("  nonce:         " + b.getNonce());
            System.out.println("  hash:          " + b.getHash());
        }
        System.out.println("Cadena completa: " + cadena.size() + " bloques.");
    }
}
