package model;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class coordinadores {

    static String nombre;
    static int nemp;
    static int division;
    static String puesto;
    static String nombrePuesto;
    static int departamento;
    static String campus;
    static List<String> programas = new ArrayList<>();
    static String nombreDepartamento;
    static String nombreDivision;
    static String correo;
    static String status;
    static List<String> nombreProgramas = new ArrayList<>();

    //solo se usa en la impresión de datos
    static List<String> nombreProgramas2 = new ArrayList<>();

    //Conexion
    static Connection conexion;

    //Guardar los diferentes planes de un programa
    static List<String> tablasPrograma = new ArrayList<>();

    //Programa elegido por el coordinador, poner varias pestañas en caso de que sea coordinador de más de una carrera
    static String eleccionPrograma;

    //Periodo elegido por el coordinador para ver la tabla:
    static List<String> periodoTabla = new ArrayList<>();
    static String eleccionPeriodo;

    //Para la impresión de la tabla general
    static StringBuilder sb = new StringBuilder();
    static ResultSet resultadoFinal;

    //Para imprimir la tabla resumida
    static StringBuilder sb2 = new StringBuilder();
    static ResultSet resultadoFinal2;

    //Para la tabla por plan:
    static ResultSet resultadoFinal3;

    public coordinadores(String nombre, int nemp, int division, String puesto, String nombrePuesto, int departamento, String campus, List<String> programas, String nombreDepartamento, String nombreDivision, String correo, String status, List<String> nombreProgramas, Connection conexion) {

        this.nombre = nombre;
        this.nemp = nemp;
        this.division = division;
        this.puesto = puesto;
        this.nombrePuesto = nombrePuesto;
        this.departamento = departamento;
        this.campus = campus;
        this.programas = programas;
        this.nombreDepartamento = nombreDepartamento;
        this.nombreDivision = nombreDivision;
        this.correo = correo;
        this.status = status;
        this.nombreProgramas = nombreProgramas;
        this.conexion = conexion;
        nombreProgramas2.addAll(nombreProgramas);
        imprimirDatosSesion();
        elegirProgramaCoordi();
        tablasProgramas(conexion);
        diferentesPeriodos(conexion);
        elegirPeriodoCoordi();
        if(eleccionPeriodo.contains("No hay periodos disponibles")){
            System.out.println("No hay tablas con periodos disponibles");
        }else{
            tablaGeneral(conexion);
            imprimirTablaGeneral();
            tablaResumen(conexion);
            imprimirTablaResumen();
            tablaPorPlanes(conexion);//El imprimir tabla de este, se corre dentro del método
        }
    }

    public static void imprimirDatosSesion() {
        System.out.println("NOMBRE: " + nombre);
        System.out.println("NEMP: " + nemp);
        String programasImprimir = "";
        int i = 0;
        int j = 0;
        while (i < programas.size() && j < nombreProgramas.size()) {
            programasImprimir += nombreProgramas.get(j) + "(" + programas.get(i) + "), ";
            i++;
            j++;
        }

        while (i < programas.size()) {
            programasImprimir += programas.get(i) + ", ";
            i++;
        }

        while (j < nombreProgramas.size()) {
            programasImprimir += nombreProgramas.get(j) + ", ";
            j++;
        }

        programasImprimir = programasImprimir.substring(0, programasImprimir.length() - 2);
        System.out.println("PROGRAMA(S): " + programasImprimir);
        System.out.println("CAMPUS: " + campus);
        System.out.println("CORREO: " + correo);
        System.out.println("PUESTO: " + nombrePuesto + "(" + puesto + ")");
        System.out.println("ESTATUS: " + status);
    }

    public static void elegirProgramaCoordi() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Elige programa a ver:");
        int i = 0;
        for (String p : programas) {
            System.out.println((i + 1) + ".-" + p);
            i++;
        }
        System.out.print("Opcion: ");
        int elegir = sc.nextInt();
        elegir -= 1;
        eleccionPrograma = programas.get(elegir);
    }

    public static void tablasProgramas(Connection conexion) {
        try {
            Statement stmt = conexion.createStatement();
            String query = "SELECT table_name FROM information_schema.tables WHERE table_name LIKE '" + eleccionPrograma + "_%'";
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                tablasPrograma.add(rs.getString("table_name"));
            }

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void diferentesPeriodos(Connection conexion) {
        try {
            Statement stmt = conexion.createStatement();
            for (String p : tablasPrograma) {
                String query = "SELECT DISTINCT(periodo) FROM " + p + ";";
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    String n = rs.getString("periodo");
                    if (!periodoTabla.contains(n)) {
                        periodoTabla.add(n);
                    }
                }
            }
            Collections.sort(periodoTabla);
        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void elegirPeriodoCoordi() {
        Scanner sc = new Scanner(System.in);
        if(periodoTabla.isEmpty()){
            periodoTabla.add("No hay periodos disponibles");
        }
        System.out.println("Elige periodo a ver:");
        int i = 0;
        for (String p : periodoTabla) {
            System.out.println((i + 1) + ".-" + p);
            i++;
        }
        System.out.print("Opcion: ");
        int elegir = sc.nextInt();
        elegir -= 1;
        eleccionPeriodo = periodoTabla.get(elegir);
    }

    public static void tablaGeneral(Connection conexion) {

        try {
            Statement stmt = conexion.createStatement();
            if (tablasPrograma.isEmpty()) {
                System.out.println("No se encontraron tablas.");
            } else {

                for (int i = 0; i < tablasPrograma.size(); i++) {
                    String tabla = tablasPrograma.get(i);
                    sb.append("SELECT al.nombre,pr.expediente, pr.clave, pr.descripcion, pr.campus, pr.periodo, al.riesgo, "
                            + "al.riesgoant FROM ");
                    sb.append(tabla + " pr, alumno al WHERE pr.expediente = al.expediente AND pr.campus = '" + campus + "' AND pr.periodo = " + eleccionPeriodo + "");
                    if (i != tablasPrograma.size() - 1) {
                        sb.append(" UNION ALL ");
                    }
                }
            }

            sb.append(" ORDER BY riesgo DESC;");
            resultadoFinal = stmt.executeQuery(sb.toString());

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }

    }

    public static void imprimirTablaGeneral() {
        try {
            PrintStream out = new PrintStream(System.out, true, "UTF-8");
            // Imprimir encabezados de la tabla
            out.printf("%-40s %-12s %-10s %-65s %-20s %-8s %-7s %-9s%n", "nombre", "expediente", "clave", "descripcion", "campus", "periodo", "riesgo", "riesgoant");

            // Imprimir separadores
            out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");

            // Imprimir filas de la tabla
            while (resultadoFinal.next()) {
                out.printf("%-40s %-12s %-10s %-65s %-20s %-8s %-7s %-9s%n", resultadoFinal.getString("nombre"), resultadoFinal.getString("expediente"), resultadoFinal.getString("clave"), resultadoFinal.getString("descripcion"), resultadoFinal.getString("campus"), resultadoFinal.getString("periodo"), resultadoFinal.getString("riesgo"), resultadoFinal.getString("riesgoant"));
            }
            System.out.println("");
            System.out.println("");
        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(coordinadores.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void tablaResumen(Connection conexion) {
        try {
            Statement stmt = conexion.createStatement();
            if (tablasPrograma.isEmpty()) {
                System.out.println("No se encontraron tablas.");
            } else {
                sb2.append("SELECT periodo, descripcion, COUNT(*) AS registros, SUM(CASE WHEN riesgo > 0 THEN 1 ELSE 0 END) AS en_riesgo, "
                        + "SUM(CASE WHEN riesgoant > 0 THEN 1 ELSE 0 END) AS en_riesgoant, "
                        + "CASE WHEN COUNT(*) BETWEEN 0 AND 29 THEN 'Poca demanda' WHEN COUNT(*) BETWEEN 30 AND 40 THEN 'Demanda suficiente' ELSE 'Mucha demanda' END AS demanda_abrir\n"
                        + "FROM\n"
                        + "(");
                for (int i = 0; i < tablasPrograma.size(); i++) {
                    String tabla = tablasPrograma.get(i);
                    sb2.append("SELECT pr.periodo, pr.descripcion, al.riesgo, al.riesgoant FROM ");
                    sb2.append(tabla + " pr, alumno al WHERE pr.expediente = al.expediente AND pr.campus = '" + campus + "' AND pr.periodo = " + eleccionPeriodo + "");
                    if (i != tablasPrograma.size() - 1) {
                        sb2.append(" UNION ALL ");
                    }
                }
            }

            sb2.append(") AS tabla \n"
                    + "GROUP BY descripcion;");
            resultadoFinal2 = stmt.executeQuery(sb2.toString());

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void imprimirTablaResumen() {
        try {
            PrintStream out = new PrintStream(System.out, true, "UTF-8");
            // Imprimir encabezados de la tabla
            out.printf("%-15s %-65s %-15s %-15s %-15s %-35s%n", "periodo", "descripcion", "registros", "en_riesgo", "en_riegoant", "demanda_abrir");

            // Imprimir separadores
            out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");

            // Imprimir filas de la tabla
            while (resultadoFinal2.next()) {
                out.printf("%-15s %-65s %-15s %-15s %-15s %-35s%n", resultadoFinal2.getString("periodo"), resultadoFinal2.getString("descripcion"), resultadoFinal2.getString("registros"), resultadoFinal2.getString("en_riesgo"), resultadoFinal2.getString("en_riesgoant"), resultadoFinal2.getString("demanda_abrir"));
            }
            System.out.println("");
            System.out.println("");
        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(coordinadores.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void tablaPorPlanes(Connection conexion) {
        try {
            Statement stmt = conexion.createStatement();
            for (String p : tablasPrograma) {
                System.out.println("Tabla: " + p);
                String query = "SELECT al.nombre,pr.expediente, pr.clave, pr.descripcion, pr.campus, pr.periodo, al.riesgo, "
                        + "al.riesgoant FROM " + p + " pr, alumno al WHERE pr.expediente = al.expediente AND pr.campus = '" + campus + "' AND pr.periodo = " + eleccionPeriodo + ";";
                resultadoFinal3 = stmt.executeQuery(query);
                imprimirTablaPorPlan();
            }
        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void imprimirTablaPorPlan() {
        try {
            PrintStream out = new PrintStream(System.out, true, "UTF-8");
            // Imprimir encabezados de la tabla
            out.printf("%-40s %-12s %-10s %-65s %-20s %-8s %-7s %-9s%n", "nombre", "expediente", "clave", "descripcion", "campus", "periodo", "riesgo", "riesgoant");

            // Imprimir separadores
            out.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------");

            // Imprimir filas de la tabla
            while (resultadoFinal3.next()) {
                out.printf("%-40s %-12s %-10s %-65s %-20s %-8s %-7s %-9s%n", resultadoFinal3.getString("nombre"), resultadoFinal3.getString("expediente"), resultadoFinal3.getString("clave"), resultadoFinal3.getString("descripcion"), resultadoFinal3.getString("campus"), resultadoFinal3.getString("periodo"), resultadoFinal3.getString("riesgo"), resultadoFinal3.getString("riesgoant"));
            }
            System.out.println("");
            System.out.println("");
        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(coordinadores.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
