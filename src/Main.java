import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

public class Main {

    static int DIFICULTAD = 4;

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


        }
}
