package traductor;

import java.io.*;
import java.util.*;
import procesador.Procesador;

public class GenCodigoIntermedio {

    private static List<String> cuartetos = new ArrayList<>();
    private static Map<Integer, String> claseVar = new HashMap<>();

    public GenCodigoIntermedio() {
        cuartetos = new ArrayList<>();
        claseVar = new HashMap<>();
    }

    // registra la clase semántica de una variable según su posición en la TS
    public static void registrar(int idPos, String clase) {
        claseVar.put(idPos, clase);
    }

    // construye el argumento de cuarteto para una variable de la TS
    public static String deIdPos(int idPos) {
        if (idPos == 0)
            return "-";
        try {
            int desp = Procesador.gestorTS.getValorAtributoEnt(idPos, "desplazamiento");
            if (idPos > 0) {
                return "{VAR_GLOBAL, " + desp + "}";
            }
            String clase = claseVar.getOrDefault(idPos, "LOCAL");
            switch (clase) {
                case "PAR_REF":
                    return "{PAR_REF, " + desp + "}";
                case "PAR_VAL":
                    return "{PAR, " + desp + "}";
                case "TEMP":
                    return "{VAR_TEMP, " + desp + "}";
                default:
                    return "{VAR_LOCAL, " + desp + "}";
            }
        } catch (Exception e) {
            return "-";
        }
    }

    public static String deCteEnt(int val) {
        return "{CTE_ENT, " + val + "}";
    }

    public static String deCteCad(String s) {
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{CTE_CAD, \"" + escaped + "\"}";
    }

    public static String deEtiqueta(String e) {
        return "{ET, \"" + e + "\"}";
    }

    public static void emite(String op, String a1, String a2, String res) {
        cuartetos.add("(" + op + ", " + a1 + ", " + a2 + ", " + res + ")");
    }

    public static void volcarAlArchivo(String ruta) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ruta))) {
            for (String c : cuartetos) {
                pw.println(c);
            }
            System.out.println("Fichero de cuartetos generado: " + ruta);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void cerrar() {
        volcarAlArchivo("cuartetos.txt");
    }
}
