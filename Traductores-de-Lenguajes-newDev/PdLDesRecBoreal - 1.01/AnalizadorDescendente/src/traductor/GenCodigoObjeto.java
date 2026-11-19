package traductor;

import java.io.*;
import java.util.*;

import procesador.Procesador;

public class GenCodigoObjeto {

    // buffers
    private static StringBuilder zonaDatos = new StringBuilder();
    private static StringBuilder zonaCodigo = new StringBuilder();
    private static StringBuilder zonaInicio = new StringBuilder();
    private static StringBuilder zonaProcedimientos = new StringBuilder();

    private static List<Cuarteto> cuartetos;
    private static int contadorEtiquetas = 0;
    private static final int T = 2; // tamaño del EM(1) + PA(1), sin PC
    private static boolean enProcedimiento = false; // Por defecto estamos en el programa principal
    private static int contadorParametros = 0;
    private static int tamVariablesLocales = 0;
    private static int celdasParametros = 0;
    private static int tamRALlamador = 0;
    private static int offsetParamActual = 0;
    private static int pos;
    private static int despTrasParametros;
    private static int despGlobal;

    public static String nuevaCadenaData(String texto) {
        String etiq = "str_" + (contadorEtiquetas++);
        emite("DATA", etiq, "-", texto);
        return etiq;
    }

    public static void setDespTrasParametros(int d) {
        despTrasParametros = d;
    }

    public static void setDespGlobal(int d) {
        despGlobal = d;
    }

    public static void setTamRA(int tam) {
        tamRALlamador = tam;
    }

    public GenCodigoObjeto() {
        cuartetos = new ArrayList<>();
        zonaDatos.setLength(0);
        zonaCodigo.setLength(0);
        zonaInicio.setLength(0);
        zonaProcedimientos.setLength(0);
    }

    public static void emitirDato(String instruccion) {
        zonaDatos.append(instruccion).append("\n");
    }

    public static void emitirInicio(String instruccion) {
        zonaInicio.append(instruccion).append("\n");
    }

    public static void emitirProcedimiento(String instruccion) {
        zonaProcedimientos.append(instruccion).append("\n");
    }

    public static void writeln(String instruccion) {
        if (enProcedimiento) {
            zonaProcedimientos.append(instruccion).append("\n");
        } else {
            zonaCodigo.append(instruccion).append("\n");
        }
    }

    public static void setEnProcedimiento(boolean estado) {
        enProcedimiento = estado;
    }

    public static void setTamVariablesLocales(int tamano) {
        tamVariablesLocales = tamano;
    }

    private static int calcularCeldasOcupadas(String bloqueTexto) {
        int celdasTotales = 0;
        String[] lineas = bloqueTexto.split("\n");

        for (String linea : lineas) {
            linea = linea.trim();

            if (linea.isEmpty() || linea.startsWith(";")) {
                continue;
            }

            if (linea.endsWith(":") && !linea.contains(" ")) {
                continue;
            }

            if (linea.startsWith("ORG ")) {
                // try {
                // direccionActual = Integer.parseInt(linea.substring(4).trim());
                // maxDireccion = Math.max(maxDireccion, direccionActual);
                // } catch (Exception e) {
                // }
                continue;
            }

            if (linea.contains("RES ")) {
                try {
                    int n = Integer.parseInt(linea.split("RES ")[1].trim());
                    celdasTotales += n * 2; // cada celda reservada ocupa 2
                } catch (Exception e) {
                    celdasTotales += 2;
                }
                // maxDireccion = Math.max(maxDireccion, direccionActual);
                continue;
            }

            if (linea.contains("DATA ")) {
                String contenido = linea.substring(linea.indexOf("DATA ") + 5).trim();
                if (contenido.startsWith("\"")) {
                    int cierre = contenido.lastIndexOf("\"");
                    if (cierre > 0) {
                        int longCadena = (cierre - 1) + 1; // texto + terminador null
                        celdasTotales += longCadena * 2;
                    } else {
                        celdasTotales += 2;
                    }
                } else {
                    celdasTotales += 2;
                }
                // maxDireccion = Math.max(maxDireccion, direccionActual);
                continue;
            }

            if (linea.contains("#") || linea.contains("[")) {
                celdasTotales += 4;
            } else {
                celdasTotales += 2;
            }

            // Instrucción normal: 2 celdas
            // direccionActual += 2;
            // maxDireccion = Math.max(maxDireccion, direccionActual);
        }

        return celdasTotales;
    }

    public static synchronized void volcarAlArchivo(String rutaFichero) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(rutaFichero))) {
            // Calcular el ORG del código: justo después de la zona de datos,
            // redondeado a la centena siguiente para que sea legible
            int celdasOcupadasDatos = calcularCeldasOcupadas(zonaDatos.toString());
            // System.out.println("celdasOcupadasDatos: " + celdasOcupadasDatos);
            int orgCodigo = ((celdasOcupadasDatos + despGlobal + 99) / 100) * 100;
            // System.out.println("orgCodigo: " + orgCodigo);
            // Mínimo 200 para respetar el espacio del ORG 64 + inicio_globales
            orgCodigo = Math.max(orgCodigo, 200);

            int celdasCodigoPrincipal = calcularCeldasOcupadas(zonaCodigo.toString());
            int celdasProcedimientos = calcularCeldasOcupadas(zonaProcedimientos.toString());
            // System.out.println("despGlobal2: " + despGlobal);
            int orgPilaDinamico = (((orgCodigo + celdasCodigoPrincipal + celdasProcedimientos + despGlobal) + 99) / 100)
                    * 100;
            // System.out.println("orgPilaDinamico: " + orgPilaDinamico);

            StringBuilder orgCodigoHeader = new StringBuilder();
            // System.out.println("celdasProcedimientos: " + celdasProcedimientos);
            // System.out.println("celdasCodigoPrincipal: " + celdasCodigoPrincipal);
            orgCodigoHeader.append("ORG ").append(orgCodigo).append("\n");

            pw.print(zonaInicio.toString());
            pw.print(zonaDatos.toString());
            pw.print(orgCodigoHeader); // ORG dinámico antes del código
            pw.print(zonaCodigo.toString());
            pw.print(zonaProcedimientos.toString());

            StringBuilder pila = new StringBuilder();
            pila.append("ORG ").append(orgPilaDinamico).append("\n");
            pila.append("inicio_pila:\n");
            pw.print(pila.toString());
            contadorParametros = 0;
            celdasParametros = 0;

            System.out.println("Archivo ensamblador generado con éxito.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String nuevaEtiqueta() {
        return "L" + (contadorEtiquetas++);
    }

    public static void emite(String op, String a1, String a2, String res) {
        Cuarteto c = new Cuarteto(op, a1, a2, res);
        cuartetos.add(c);
        traducir(c);
    }

    public static void emitirSaltoInicial(String etiquetaInicio) {
        writeln("BR /" + etiquetaInicio);

    }

    private static void traducir(Cuarteto c) {
        // System.out.println("Traduciendo: " + c);
        String loop, end;

        switch (c.getOp()) {
            case "ETIQ":
                if (c.getArg2().equals("3")) {
                    emitirInicio(c.getResultado() + ":");
                    emitirInicio("RES " + c.getArg1());
                } else if (c.getArg2().equals("2")) {
                    emitirInicio(c.getResultado() + ":");

                } else {
                    writeln(c.getResultado() + ":");
                }
                break;
            case "GOTO":
                if (c.getArg2().equals("3")) {
                    emitirInicio("BR /" + c.getResultado());
                } else {
                    writeln("BR /" + c.getResultado());
                }
                break;
            case "HALT":
                writeln("HALT");
                break;
            case "ASIG":
                // revisar como poner la dirección de memoria de las variables al asignar
                if (c.getArg2().equals("1")) {
                    writeln("MOVE " + c.getArg1() + ", " + c.getResultado());
                } else if (c.getArg2().equals("2")) {
                    writeln("MOVE " + c.getArg1() + ", .R8");
                    writeln("MOVE .R8, " + c.getResultado());
                } else if (c.getArg2().equals("3")) {
                    emitirInicio("MOVE " + c.getArg1() + ", " + c.getResultado());
                }
                break;
            case "ASIG_CAD":
                loop = "asig_cad_" + System.nanoTime();
                end = "asig_cad_" + (System.nanoTime() + 1);
                // Resolver dirección fuente (arg1)
                String srcCad = c.getArg1();
                if (srcCad.contains("[.IY]")) {
                    // Variable global: calcular dirección absoluta
                    String dGlob = srcCad.substring(srcCad.indexOf("#") + 1, srcCad.indexOf("[.IY]"));
                    writeln("ADD .IY, #" + dGlob);
                    writeln("MOVE .A, .R1");
                } else if (srcCad.contains("[.SP]")) {
                    // Variable local / parámetro por valor: calcular dirección absoluta
                    String dLocal = srcCad.substring(srcCad.indexOf("#") + 1, srcCad.indexOf("[.SP]"));
                    writeln("ADD .SP, #" + dLocal);
                    writeln("MOVE .A, .R1");
                } else if (srcCad.equals("[.R9]")) {
                    // Parámetro por referencia: .R9 ya contiene la dirección
                    writeln("MOVE .R9, .R1");
                } else {
                    // Etiqueta literal (e.g. "L1"): comportamiento original
                    writeln("MOVE #" + srcCad + ", .R1");
                }
                // Resolver dirección destino (resultado)
                String destCad = c.getResultado();
                if (destCad.equals("[.R9]")) {
                    writeln("MOVE .R9, .R2");
                } else if (destCad.equals(".R9")) {
                    writeln("MOVE .R9, .R2");
                } else {
                    writeln("MOVE " + destCad + ", .R2");
                }
                writeln("MOVE #0, .R4");
                writeln(loop + ":");
                writeln("MOVE [.R1], .R3");
                writeln("MOVE .R3, [.R2]");
                writeln("CMP .R3, .R4");
                writeln("BZ /" + end);
                writeln("INC .R1");
                writeln("INC .R2");
                writeln("BR /" + loop);
                writeln(end + ":");
                break;
            case "SUMA":
                writeln("ADD " + c.getArg1() + ", " + c.getArg2());
                writeln("MOVE .A, " + c.getResultado());
                break;
            case "NOT":
                writeln("MOVE #1, .R5");
                writeln("XOR .R5," + c.getArg1());
                writeln("MOVE .A, " + c.getResultado());
                break;
            case "EXP":
                String a = c.getArg1();
                String b = c.getArg2();
                String r = c.getResultado();

                loop = "exp_" + System.nanoTime();
                end = "exp_" + (System.nanoTime() + 1);

                // r = 1
                writeln("XOR " + r + ", " + r);
                writeln("MOVE #1, " + r);

                // contador = b
                writeln("MOVE " + b + ", .R1");

                // ponemos el operando1 en R2
                writeln("MOVE " + a + ", .R2");

                // etiqueta inicio bucle
                writeln(loop + ":");

                // si R1 == 0 salir
                writeln("CMP #1, .R1");
                writeln("BZ /" + end);

                // registro acumulador = ap1 * R2 (R2 contiene acumulación de operaciones)
                writeln("MUL " + a + ", .R2");

                // movemos el resultado a R2
                writeln("MOVE .A, .R2");

                // R1--
                writeln("SUB .R1, #1");
                writeln("MOVE .A, .R1");

                // salto al inicio
                writeln("BR /" + loop);

                // fin
                writeln(end + ":");

                // finalmente dejamos el resultado en la dirección correspondiente
                writeln("MOVE .R2, " + r);

                break;
            case "GOTO_MAY":
                writeln("CMP " + c.getArg1() + ", " + c.getArg2());
                writeln("BP /" + c.getResultado());
                break;
            case "PRINT_ENT":
                writeln("WRINT " + c.getArg1());
                break;
            case "PRINT_CAD":
                if (c.getArg1().contains("[.IY]")) {
                    // Variable cadena global: obtener dirección absoluta
                    String desp = c.getArg1().substring(
                            c.getArg1().indexOf("#") + 1, c.getArg1().indexOf("[.IY]"));
                    writeln("ADD .IY, #" + desp);
                    writeln("MOVE .A, .R1");
                    writeln("WRSTR [.R1]");
                } else if (c.getArg1().contains("[.SP]")) {
                    // Variable cadena local: igual pero con SP
                    String desp = c.getArg1().substring(
                            c.getArg1().indexOf("#") + 1, c.getArg1().indexOf("[.SP]"));
                    writeln("ADD .SP, #" + desp);
                    writeln("MOVE .A, .R1");
                    writeln("WRSTR [.R1]");
                } else if (c.getArg1().contains("[")) {
                    writeln("WRSTR " + c.getArg1());
                } else {
                    // Etiqueta directa (literal de cadena como L1, str_0...)
                    writeln("WRSTR /" + c.getArg1());
                }
                break;
            case "WRITE_NEWLINE": {
                String etiqNL = nuevaCadenaData("\\n");
                writeln("WRSTR /" + etiqNL);
                break;
            }
            case "INPUT_INT":
                writeln("ININT " + c.getResultado());
                break;
            case "INPUT_STR":
                writeln("INSTR " + c.getResultado());
                break;
            case "CMP_MAY":
                String etiquetaVerdad = "cmp_may_true_" + System.nanoTime();
                String etiquetaFin = "cmp_may_fin_" + (System.nanoTime() + 1);
                writeln("MOVE #0, " + c.getResultado());
                writeln("CMP " + c.getArg1() + ", " + c.getArg2());
                writeln("BP /" + etiquetaVerdad);
                writeln("BR /" + etiquetaFin);
                writeln(etiquetaVerdad + ":");
                writeln("MOVE #1, " + c.getResultado());
                writeln(etiquetaFin + ":");
                break;
            case "CMP_IF":
                writeln("CMP " + c.getArg1() + ", #0");
                writeln("BZ /" + c.getResultado());
                break;
            case "RES":
                if (c.getArg2().equals("inicio")) {
                    emitirDato(c.getResultado() + ":");
                    emitirDato("RES " + c.getArg1());
                } else {
                    writeln(c.getResultado() + ":");
                    writeln("RES " + c.getArg1());
                }
                break;
            case "ORG":
                if (c.getArg2().equals("inicio")) {
                    emitirInicio("ORG " + c.getResultado());
                } else {
                    writeln("ORG " + c.getResultado());
                }
                break;
            case "DATA":
                emitirDato(c.getArg1() + ":");
                emitirDato("DATA \"" + c.getResultado() + "\", 0");
                break;
            case "PARAM_CAD":
                pos = tamRALlamador + 2 + offsetParamActual;
                writeln("MOVE " + c.getArg1() + ", .R1");
                writeln("ADD #" + pos + ", .SP");
                writeln("MOVE .A, .R2");
                writeln("MOVE #0, .R4");
                String loopCad = "param_cad_" + System.nanoTime();
                String endCad = "param_cad_" + (System.nanoTime() + 1);
                writeln(loopCad + ":");
                writeln("MOVE [.R1], .R3");
                writeln("MOVE .R3, [.R2]");
                writeln("CMP .R3, .R4");
                writeln("BZ /" + endCad);
                writeln("INC .R1");
                writeln("INC .R2");
                writeln("BR /" + loopCad);
                writeln(endCad + ":");
                offsetParamActual += 64;
                contadorParametros++;
                break;
            case "PARAM":
                // Posición relativa al SP actual donde se ubicará el parámetro del hijo
                pos = tamRALlamador + 2 + offsetParamActual;

                // Comprobamos si el argumento es una cadena/etiqueta de texto (como L2, L6,
                // etc.)
                if (c.getArg1().startsWith("L") || c.getArg1().contains("cadena")) {
                    writeln("MOVE " + c.getArg1() + ", .R1");
                    writeln("ADD .SP, #" + pos); // Calculamos la dirección física de destino en la pila
                    writeln("MOVE .A, .R2");
                    writeln("MOVE #0, .R4");
                    loopCad = "param_cad_" + System.nanoTime();
                    endCad = "param_cad_" + (System.nanoTime() + 1);
                    writeln(loopCad + ":");
                    writeln("MOVE [.R1], .R3");
                    writeln("MOVE .R3, [.R2]");
                    writeln("CMP .R3, .R4");
                    writeln("BZ /" + endCad);
                    writeln("INC .R1");
                    writeln("INC .R2");
                    writeln("BR /" + loopCad);
                    writeln(endCad + ":");
                    offsetParamActual += 64; // Las cadenas ocupan 64 celdas
                } else {
                    // Si es un entero o temporal simple (.R9, variables, etc.)
                    // Primero lo movemos a un registro limpio para evitar modos de direccionamiento
                    // ilegales
                    writeln("MOVE " + c.getArg1() + ", .R9");
                    writeln("MOVE .R9, #" + pos + "[.SP]");
                    offsetParamActual += 1;
                }
                contadorParametros++;
                break;

            case "PARAM_REF":
                pos = tamRALlamador + 2 + offsetParamActual;
                String arg = c.getArg1();

                if (arg.contains("[.SP]")) {
                    // Caso 1: Es una variable local (ej. #66[.SP]). Extraemos el número del
                    // desplazamiento.
                    String desp = arg.substring(arg.indexOf("#") + 1, arg.indexOf("[.SP]"));
                    writeln("ADD .SP, #" + desp); // Sumamos al SP para obtener su dirección absoluta de memoria
                    writeln("MOVE .A, .R9"); // .R9 ahora guarda de forma legal la dirección física
                    writeln("MOVE .R9, #" + pos + "[.SP]"); // Almacenamos la dirección en el hueco del parámetro
                } else if (arg.contains("[.IY]")) {
                    // Caso 2: Es una variable global (relativa a .IY)
                    String desp = arg.substring(arg.indexOf("#") + 1, arg.indexOf("[.IY]"));
                    writeln("ADD .IY, #" + desp);
                    writeln("MOVE .A, .R9");
                    writeln("MOVE .R9, #" + pos + "[.SP]");
                } else {
                    // Caso 3: Es un registro puro (como .R9) o etiqueta directa
                    if (arg.startsWith(".")) {
                        writeln("MOVE " + arg + ", .R9"); // Quitamos cualquier almohadilla accidental
                        writeln("MOVE .R9, #" + pos + "[.SP]");
                    } else {
                        writeln("MOVE #" + arg + ", #" + pos + "[.SP]");
                    }
                }
                offsetParamActual += 1; // Las referencias a memoria ocupan siempre 1 celda (puntero)
                contadorParametros++;
                break;

            case "CALL":
                String dirRetorno = "dir_ret_" + System.nanoTime();

                // 1. Guardar la Dirección de Retorno (EM) en la base del nuevo marco
                int posEM = tamRALlamador;
                writeln("MOVE #" + dirRetorno + ", #" + posEM + "[.SP]");

                // 2. CORRECCIÓN DEL PA: Comprobar si llamamos desde el Main o desde un Proc
                if (!enProcedimiento) {
                    // Si llamamos desde el programa principal, no hay PA padre en la pila.
                    // Ponemos #0 (puntero nulo) o .IY para evitar leer basura de las cadenas
                    // locales.
                    writeln("MOVE #0, .R9");
                } else {
                    // Si estamos dentro de un procedimiento, para recuperar su PA real
                    // tenemos que mirar hacia atrás restando el tamaño de sus variables locales.
                    int posicionPAReal = tamVariablesLocales - 1; // Ajustar según tu índice
                    writeln("MOVE #" + posicionPAReal + "[.SP], .R9");
                }

                // Guardar el PA calculado en la segunda celda del marco del hijo
                writeln("MOVE .R9, #" + (posEM + 1) + "[.SP]");

                // 3. Desplazar el puntero de la pila y saltar
                int saltoPila = tamRALlamador;
                writeln("ADD .SP, #" + saltoPila);
                writeln("MOVE .A, .SP");
                writeln("BR /" + c.getArg1());

                // --- PUNTO DE RETORNO ---
                writeln(dirRetorno + ":");
                writeln("SUB .SP, #" + saltoPila);
                writeln("MOVE .A, .SP");

                // Resetear contadores
                offsetParamActual = 0;
                contadorParametros = 0;
                break;

            case "RETURN":

                // writeln("SUB .SP, #" + tamVariablesLocales);
                // writeln("MOVE .A, .SP");
                writeln("BR [.SP]");
                tamVariablesLocales = 0;
                break;
            case "MOVE":
                writeln("MOVE " + c.getArg1() + ", " + c.getResultado());
                break;
            case "PROLOGO_MAIN":
                // SP ya apunta al inicio del RA del main (inicio_pila)
                // Solo inicializar EM y PA
                writeln("MOVE #0, #0[.SP]"); // EM = 0
                writeln("MOVE #0, #1[.SP]"); // PA = 0
                // No mover SP — las vars locales se acceden como #desp+2[.SP]
                break;
            default:
                break;
        }

    }

    public static void cerrar() {
        volcarAlArchivo("codigo.txt");
    }

}
