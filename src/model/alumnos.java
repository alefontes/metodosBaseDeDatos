package model;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class alumnos {

    //Traer la conexion
    Connection conexion;

    //Datos del alumno "Para el constructor", se usan para las consultas y se usarán para imprimir los datos de la sesión
    static String nombre;
    static int expediente;
    static int semestre;
    static String campus;
    static String programa;
    static String carrera;
    static int plan;
    static int departamento;
    static String nombreDepartamento;
    static String correo;
    static String status;
    static String tipoAlumno;
    static int ultimoperiodo;

    //Creditos, se usa para sacar el tiempo aproximado para terminar la carrera
    static int creditosNecesarios;
    static int creditosActuales;

    //materias minimas y maximas para concluir el programa
    static int minimasMaterias;//minimas materias que ha llevado el semestre por alumno
    static int maximasMaterias;//maximas materias que puede llevar por semestre
    static double cantidadMinima;//cantidad minima de materias que le faltan
    static double cantidadMaxima;//cantidad maxima de materias que le faltan
    static double minimoTiempo;
    static double maximoTiempo;
    static int redondeoMinimo;
    static int redondeoMaximo;

    //guardar los creditos de las materias que no se ha cursado
    static List<Integer> creditosMaterias = new ArrayList<>(); //arreglo con los creditos de las materias que está cursando y los creditos de las que no ha cursado

    //Materias llevadas y cursando, se usa para saber si cumplió con los requisitos
    static List<Integer> materiasYaLlevadas = new ArrayList<>();
    static List<Integer> materiasCursando = new ArrayList<>();

    // Arreglos para almacenar los resultados de la consulta de las materias a cursar, reprobadas, dadas de baja, no tiene mmucha explicacion
    static List<Integer> clave = new ArrayList<>();
    static List<String> descripciones = new ArrayList<>();
    static List<Double> promedioMateria = new ArrayList<>();
    static List<Double> indiceBajas = new ArrayList<>();
    static List<Double> porcentajeDeAprobacion = new ArrayList<>();
    static List<Integer> alumnosBajas = new ArrayList<>();
    static List<Integer> alumnosInscritos = new ArrayList<>();
    static List<String> estados = new ArrayList<>();
    static List<Integer> creditos = new ArrayList<>();
    static List<String> req = new ArrayList<>();

    //Arreglo para guardar las posiciones cuyos requisitos son cumplidos
    static List<Integer> listaPosiciones = new ArrayList<>();
    static int cantidad;

    //Arreglos para solo guardar las materias anteriores cuyos requisitos se cumplen
    static List<Integer> clave2 = new ArrayList<>();
    static List<String> descripciones2 = new ArrayList<>();
    static List<Double> promedioMateria2 = new ArrayList<>();
    static List<Double> indiceBajas2 = new ArrayList<>();
    static List<Double> porcentajeDeAprobacion2 = new ArrayList<>();
    static List<Integer> alumnosBajas2 = new ArrayList<>();
    static List<Integer> alumnosInscritos2 = new ArrayList<>();
    static List<String> estados2 = new ArrayList<>();
    static List<Integer> creditos2 = new ArrayList<>();
    static List<String> req2 = new ArrayList<>();

    //guardar las posciones de las materias que se quieran cursar el otro semestre, se usa para la elección de materias
    static List<Integer> materiasElegidas = new ArrayList<>();

    //Para Culturest y prácticas
    static Map<String, String> datos = new HashMap<String, String>();

    //Para el nombre del mapa curricular
    static String mapa;

    //Si ya eligió materias:
    static List<String> elegiste = new ArrayList<>();
    static boolean seguirVerificando;
    static boolean volverSolicitar;

    public alumnos(String nombre, int expediente, int semestre, String programa, String carrera, String campus, int plan, int departamento, String nombreDepartamento, String correo, String status, String tipoAlumno, int ultimoperiodo, int creditosNecesarios, int creditosActuales, Connection conexion) {
        this.nombre = nombre;
        this.expediente = expediente;
        this.semestre = semestre;
        this.programa = programa;
        this.carrera = carrera;
        this.campus = campus;
        this.plan = plan;
        this.departamento = departamento;
        this.nombreDepartamento = nombreDepartamento;
        this.correo = correo;
        this.status = status;
        this.tipoAlumno = tipoAlumno;
        this.ultimoperiodo = ultimoperiodo;
        this.creditosNecesarios = creditosNecesarios;
        this.creditosActuales = creditosActuales;
        this.conexion = conexion;

        //valor por defecto de elegiste
        elegiste.add("No has elegido materias.");

        //procesos que no ocupan ingreso de informacion y se pueden hacer en 2do plano y primero que todo, no modificar de no ser necesario
        materiasLlevadas(conexion);
        conseguirMateriasNoCursadas(conexion);
        estadoDePracticasCulturest(conexion);
        tiempoEstimado(conexion);
        nombreArchivoMapaCurricular();

        //impresiones, es necesario modificar para imprimir
        imprimirDatosSesion();
        imprimirCreditos();
        imprimirTiempoEstimado();
        imprimirTablaPracticasCulturest();
        imprimirMateriasNoCursadas();

        //Los siguientes metodos se corren en este orden
        crearTablaSolicitudes(conexion);
        existeEnSolicitudes(conexion);
        materiasElegidas(conexion);

        //Este se imprime en Elecciones
        imprimirMateriasElegidas();
    }

    public static void imprimirDatosSesion() {
        System.out.println("NOMBRE: " + nombre);
        System.out.println("EXPEDIENTE: " + expediente);
        System.out.println("CARRERA: " + carrera + "(" + programa + ")");
        System.out.println("SEMESTRE (APROX): " + semestre);
        System.out.println("CAMPUS: " + campus);
        System.out.println("CORREO: " + correo);
        System.out.println("ESTATUS: " + status);
        System.out.println("TIPO DE ALUMNO: " + tipoAlumno);
    }

    public static void imprimirCreditos() {
        System.out.println("Tienes " + creditosActuales + " de " + creditosNecesarios + " créditos.");
    }

    public static void materiasLlevadas(Connection conexion) {
        try {
            Statement stmt = conexion.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT clave FROM inscripcion WHERE expediente = " + expediente + " AND status = 'A';");

            while (rs.next()) {
                int claveMateriaCursada = rs.getInt("clave");
                materiasYaLlevadas.add(claveMateriaCursada);
            }

            rs = stmt.executeQuery("SELECT clave FROM inscripcion WHERE expediente = " + expediente + " AND status = 'C';");

            while (rs.next()) {
                int claveMateriaCursando = rs.getInt("clave");
                materiasCursando.add(claveMateriaCursando);
            }

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void conseguirMateriasNoCursadas(Connection conexion) {

        try {

            // Crear el statement para ejecutar la consulta
            Statement stmt = conexion.createStatement();

            // Ejecutar la consulta
            stmt.executeUpdate("CREATE TEMPORARY TABLE IF NOT EXISTS materiasAlumnoTemporal AS \n"
                    + "    SELECT m.descripcion, m.clave, 'PC' AS estado, i.prog, i.plan\n"
                    + "    FROM materia m, mat_prog mp, (\n"
                    + "        SELECT *\n"
                    + "        FROM inscripcion i3\n"
                    + "        WHERE i3.expediente = " + expediente + "\n"
                    + "    ) AS i\n"
                    + "    WHERE mp.sem <= " + (semestre + 1) + " AND m.clave = mp.clave\n"
                    + "    AND NOT EXISTS (\n"
                    + "        SELECT i2.clave, i2.status\n"
                    + "        FROM inscripcion i2\n"
                    + "        WHERE i2.expediente = " + expediente + "\n"
                    + "        AND mp.clave = i2.clave\n"
                    + "    )\n"
                    + "    AND mp.plan = i.plan AND mp.programa = i.prog AND mp.clave != 119 AND mp.clave != 660 AND mp.clave != 733 AND mp.clave != 19006 AND mp.clave != 81 AND m.descripcion != 'PRÁCTICAS PROFESIONALES'\n"
                    + "    GROUP BY m.descripcion\n"
                    + "\n"
                    + "    UNION\n"
                    + "\n"
                    + "    SELECT m.descripcion, m.clave, (CASE WHEN(i4.clave=m.clave) THEN i4.status ELSE 'PC' END), i.prog, i.plan\n"
                    + "    FROM materia m, mat_prog mp, (\n"
                    + "        SELECT *\n"
                    + "        FROM inscripcion i3\n"
                    + "        WHERE i3.expediente = " + expediente + "\n"
                    + "    ) AS i, (\n"
                    + "        SELECT i3.clave, i3.status\n"
                    + "        FROM inscripcion i3\n"
                    + "        WHERE i3.expediente = " + expediente + "\n"
                    + "    ) AS i4\n"
                    + "    WHERE mp.sem <= " + (semestre + 1) + " AND m.clave = mp.clave AND i4.clave = mp.clave\n"
                    + "    AND NOT EXISTS (\n"
                    + "        SELECT i2.clave, i2.status\n"
                    + "        FROM inscripcion i2\n"
                    + "        WHERE i2.expediente = " + expediente + "\n"
                    + "        AND (i2.status = 'A' OR i2.status = 'C')\n"
                    + "        AND mp.clave = i2.clave\n"
                    + "    )\n"
                    + "    AND mp.plan = i.plan AND mp.programa = i.prog AND mp.clave != 119 AND mp.clave != 660 AND mp.clave != 733 AND mp.clave != 19006 AND mp.clave != 81 AND m.descripcion != 'PRÁCTICAS PROFESIONALES'\n"
                    + "    GROUP BY m.descripcion;");

            ResultSet rs = stmt.executeQuery("SELECT materia.descripcion AS descripcion,\n"
                    + "       ROUND(SUM(CASE WHEN (inscripcion.ord > 0 OR inscripcion.ord IS NOT NULL) AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN inscripcion.ord ELSE (CASE WHEN (inscripcion.extra > 0 OR inscripcion.extra IS NOT NULL) AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN inscripcion.extra END) END) / SUM(CASE WHEN (inscripcion.bajas = 0 OR inscripcion.bajas IS NULL) AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN 1 ELSE 0 END), 2) AS promedioMateria,\n"
                    + "       ROUND((SUM(CASE WHEN (bajas > 0 AND bajas IS NOT NULL) AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN 1 ELSE 0 END) / COUNT(CASE WHEN (inscripcion.bajas >= 0 OR inscripcion.bajas IS NOT NULL) AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN 1 ELSE 0 END))*100, 2) AS indiceBajas,\n"
                    + "       ROUND((SUM(CASE WHEN (inscripcion.ord >= 60 OR inscripcion.extra >= 60) AND inscripcion.status = 'A' AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN 1 ELSE 0 END) / SUM(CASE WHEN (inscripcion.status = 'A' OR inscripcion.status = 'R') AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN 1 ELSE 0 END))*100, 2) AS porcentajeDeAprobacion,\n"
                    + "       SUM(CASE WHEN (inscripcion.bajas > 0 AND inscripcion.bajas IS NOT NULL) AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN 1 ELSE 0 END) AS alumnosBajas,\n"
                    + "       SUM(CASE WHEN (inscripcion.bajas = 0 OR inscripcion.bajas IS NULL) AND inscripcion.plan = " + plan + " AND inscripcion.prog = '" + programa + "' THEN 1 ELSE 0 END) AS alumnosInscritos, materiasAlumnoTemporal.estado AS estado, mat_prog.creditos as creditos, mat_prog.clave as clave,\n"
                    + "       REGEXP_REPLACE(REPLACE (REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(req, ':', ''), 'O  ', ''), 'e', ''), 'y', ''),',',''),'/',' '),'Inscribir o Cursar', 'Cursar' ),' +', ' ') AS req\n"
                    + "FROM inscripcion\n"
                    + "JOIN materia ON inscripcion.clave = materia.clave\n"
                    + "JOIN mat_prog ON materia.clave = mat_prog.clave\n"
                    + "JOIN materiasAlumnoTemporal ON materia.clave = materiasAlumnoTemporal.clave\n"
                    + "WHERE mat_prog.programa = '" + programa + "' AND mat_prog.plan = " + plan + "\n"
                    + "GROUP BY materia.descripcion, mat_prog.req, materia.clave;");

            // Iterar sobre los resultados y guardarlos en los arreglos
            while (rs.next()) {
                String descrip = rs.getString("descripcion");
                double promMat = rs.getDouble("promedioMateria");
                double indBajas = rs.getDouble("indiceBajas");
                double porDeApro = rs.getDouble("porcentajeDeAprobacion");
                int alumBajas = rs.getInt("alumnosBajas");
                int alumInscritos = rs.getInt("alumnosInscritos");
                String est = rs.getString("estado");
                int credit = rs.getInt("creditos");
                int clav = rs.getInt("clave");
                String r = rs.getString("req");

                descripciones.add(descrip);
                promedioMateria.add(promMat);
                indiceBajas.add(indBajas);
                porcentajeDeAprobacion.add(porDeApro);
                alumnosBajas.add(alumBajas);
                alumnosInscritos.add(alumInscritos);
                estados.add(est);
                creditos.add(credit);
                clave.add(clav);
                req.add(r);

            }
            materiasRequisitos();

            // Cerrar los recursos de base de datos
            rs.close();
            stmt.close();

            cantidad = 0;
            for (Integer p : listaPosiciones) {
                String descrip = descripciones.get(p);
                double promMat = promedioMateria.get(p);
                double indBajas = indiceBajas.get(p);
                double porDeApro = porcentajeDeAprobacion.get(p);
                int alumBajas = alumnosBajas.get(p);
                int alumInscritos = alumnosInscritos.get(p);
                String est = estados.get(p);
                int credit = creditos.get(p);
                int clav = clave.get(p);
                String r = req.get(p);

                descripciones2.add(descrip);
                promedioMateria2.add(promMat);
                indiceBajas2.add(indBajas);
                porcentajeDeAprobacion2.add(porDeApro);
                alumnosBajas2.add(alumBajas);
                alumnosInscritos2.add(alumInscritos);
                estados2.add(est);
                creditos2.add(credit);
                clave2.add(clav);
                req2.add(r);

                cantidad++;
            }

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void materiasRequisitos() {
        // Crear un mapa para almacenar las posiciones de requisitos que se cumplen
        Map<String, List<Integer>> posicionesCumplidas = new HashMap<>();

        // Iterar sobre la lista de requisitos
        for (int i = 0; i < req.size(); i++) {
            String requisito = req.get(i);
            String[] elementosRequisito = requisito.split(" "); // Separar la cadena en palabras

            // Verificar si la cadena está vacía
            if (requisito.isEmpty()) {
                // Guardar la posición del requisito en la lista correspondiente
                if (posicionesCumplidas.containsKey("Vacio")) {
                    posicionesCumplidas.get("Vacio").add(i);
                } else {
                    List<Integer> posiciones = new ArrayList<>();
                    posiciones.add(i);
                    posicionesCumplidas.put("Vacio", posiciones);
                }
                continue; // Pasar al siguiente requisito
            }

            // Verificar cada palabra en el requisito
            for (String elemento : elementosRequisito) {
                if (elemento.equals("Aprobar")) {
                    int num = Integer.parseInt(elementosRequisito[1]); // Obtener el número
                    if (materiasYaLlevadas.contains(num)) {
                        // Guardar la posición del requisito en la lista correspondiente
                        if (posicionesCumplidas.containsKey("Aprobar")) {
                            posicionesCumplidas.get("Aprobar").add(i);
                        } else {
                            List<Integer> posiciones = new ArrayList<>();
                            posiciones.add(i);
                            posicionesCumplidas.put("Aprobar", posiciones);
                        }
                    }
                } else if (elemento.equals("Cursar")) {
                    int num = Integer.parseInt(elementosRequisito[1]); // Obtener el número
                    if (materiasCursando.contains(num) || materiasYaLlevadas.contains(num)) {
                        // Guardar la posición del requisito en la lista correspondiente
                        if (posicionesCumplidas.containsKey("Cursar")) {
                            posicionesCumplidas.get("Cursar").add(i);
                        } else {
                            List<Integer> posiciones = new ArrayList<>();
                            posiciones.add(i);
                            posicionesCumplidas.put("Cursar", posiciones);
                        }
                    }
                } else if (elemento.equals("Inscribir") && elementosRequisito.length > 1 && (elementosRequisito[1].equals("Aprobar") || elementosRequisito[1].equals("Cursar"))) {
                    int num = Integer.parseInt(elementosRequisito[2]); // Obtener el número
                    if (materiasYaLlevadas.contains(num) || materiasCursando.contains(num)) {
                        // Guardar la posición del requisito en la lista correspondiente
                        if (posicionesCumplidas.containsKey("Inscribir")) {
                            posicionesCumplidas.get("Inscribir").add(i);
                        } else {
                            List<Integer> posiciones = new ArrayList<>();
                            posiciones.add(i);
                            posicionesCumplidas.put("Inscribir", posiciones);
                        }
                    }
                }
            }
        }
        // Imprimir los resultados
        for (Map.Entry<String, List<Integer>> entry : posicionesCumplidas.entrySet()) {
            List<Integer> posiciones = entry.getValue();
            listaPosiciones.addAll(posiciones);
        }
        // Eliminar duplicados utilizando un HashSet
        Set<Integer> valoresUnicos = new HashSet<>(listaPosiciones);
        listaPosiciones.clear();
        listaPosiciones.addAll(valoresUnicos);
        Collections.sort(listaPosiciones);
        // Imprimir los valores, borrar al final
        System.out.println("Todos los valores: " + listaPosiciones);
        System.out.println("");
        System.out.println("-------------------------------------------");
    }

    public static void imprimirMateriasNoCursadas() {

        try {
            // Imprimir la información
            PrintStream out = new PrintStream(System.out, true, "UTF-8");
            out.printf("%-3s %-10s %-65s %10s %25s %25s %10s %10s %-10s %10s %15s%n", "n", "Clave", "Descripción", "Promedio", "Índice", "Aprobación", "Bajas", "Inscritos", "Estado", "Créditos", "Req");

            for (int p = 0; p < cantidad; p++) {
                out.printf("%-3d %-10s %-65s %10s %25s %25s %10s %10s %-10s %10s %15s%n",
                        p + 1,
                        clave2.get(p),
                        descripciones2.get(p),
                        promedioMateria2.get(p),
                        indiceBajas2.get(p),
                        porcentajeDeAprobacion2.get(p),
                        alumnosBajas2.get(p),
                        alumnosInscritos2.get(p),
                        estados2.get(p),
                        creditos2.get(p),
                        req2.get(p));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(alumnos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void estadoDePracticasCulturest(Connection conexion) {

        String query = "SELECT DISTINCT(materia.descripcion), inscripcion.status "
                + "FROM inscripcion, materia "
                + "WHERE materia.clave = inscripcion.clave "
                + "AND expediente = " + expediente
                + " AND materia.descripcion = 'ACTIVIDADES CULTURALES Y DEPORTIVAS' "
                + "GROUP BY materia.descripcion "
                + "UNION "
                + "SELECT materia.descripcion, inscripcion.status "
                + "FROM inscripcion, materia "
                + "WHERE materia.clave = inscripcion.clave "
                + "AND expediente = " + expediente
                + " AND materia.descripcion = 'PRÁCTICAS PROFESIONALES'";
        datos.put("PRÁCTICAS PROFESIONALES", "PC");
        datos.put("ACTIVIDADES CULTURALES Y DEPORTIVAS", "PC");
        try {

            Statement stmt = conexion.createStatement();

            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String descripcion = rs.getString("descripcion");
                String status = rs.getString("status");
                if (datos.containsKey(descripcion)) {
                    datos.put(descripcion, status != null ? status : "PC");
                }
            }

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void imprimirTablaPracticasCulturest() {
        System.out.println("+-------------------------------+");
        System.out.printf("%-55s %-10s%n", "Descripcion", "Estado");
        System.out.println("+-------------------------------+");
        for (Map.Entry<String, String> entry : datos.entrySet()) {
            System.out.printf("%-55s %-10s%n", entry.getKey(), entry.getValue());
        }
        System.out.println("+-------------------------------+");
    }

    public static void nombreArchivoMapaCurricular() {
        mapa = programa + "_" + plan;
    }

    public void tiempoEstimado(Connection conexion) {
        try {
            Statement stmt = conexion.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT mat_prog.creditos \n"
                    + "FROM mat_prog\n"
                    + "WHERE mat_prog.plan = " + plan + " \n"
                    + "  AND mat_prog.programa = '" + programa + "'\n"
                    + "  AND sem < (\n"
                    + "    SELECT MAX(sem) \n"
                    + "    FROM mat_prog \n"
                    + "    WHERE plan = " + plan + " \n"
                    + "      AND programa = '" + programa + "'\n"
                    + "  )\n"
                    + "  AND mat_prog.clave NOT IN (\n"
                    + "    SELECT inscripcion.clave \n"
                    + "    FROM inscripcion \n"
                    + "    WHERE expediente = " + expediente + " \n"
                    + "      AND status = 'A'\n"
                    + "  ) AND mat_prog.clave != 119;");

            while (rs.next()) {
                int creditMateria = rs.getInt("creditos");
                creditosMaterias.add(creditMateria);
            }

            rs = stmt.executeQuery("SELECT MIN(periodoMateria) AS minimo, MAX(periodoMateria) AS maximo "
                    + "FROM (SELECT COUNT(periodo) AS periodoMateria FROM inscripcion "
                    + "WHERE expediente = " + expediente + " GROUP BY periodo) AS per;");

            while (rs.next()) {
                minimasMaterias = rs.getInt("minimo");
                maximasMaterias = rs.getInt("maximo");
            }

            int creditosRestantes = creditosNecesarios - creditosActuales;

            Collections.sort(creditosMaterias);
            //sacar materias minimas a llevar
            cantidadMaxima = 0;
            int suma = 0;
            for (Integer n : creditosMaterias) {
                suma += n;
                cantidadMaxima++;
                if (suma >= creditosRestantes) {
                    break;
                }
            }

            Collections.sort(creditosMaterias, Collections.reverseOrder());

            cantidadMinima = 0;
            suma = 0;

            for (Integer n : creditosMaterias) {
                suma += n;
                cantidadMinima++;
                if (suma >= creditosRestantes) {
                    break;
                }
            }

            minimoTiempo = cantidadMaxima / maximasMaterias;
            maximoTiempo = cantidadMinima / minimasMaterias;
            redondeoMinimo = (int) Math.ceil(minimoTiempo);
            redondeoMaximo = (int) Math.ceil(maximoTiempo);
        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void imprimirTiempoEstimado() {
        System.out.println("Tus tiempos estimados son tomando en cuenta que: ");
        System.out.println("Tu maximo de materias llevadas en un semestre son " + maximasMaterias);
        System.out.println("Tu minimo de materias llevadas en un semestre son " + minimasMaterias);
        System.out.println("Tu tiempo minimo estimado es: " + redondeoMinimo + " semestres");
        System.out.println("Tu tiempo maximo estimado es: " + redondeoMaximo + " semestres");
        System.out.println("");
        System.out.println("------------------------------------------------------------------");
    }

    public void crearTablaSolicitudes(Connection conexion) {
        try {
            Statement stmt = conexion.createStatement();

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + programa + "_" + plan + " (\n"
                    + "  id INT NOT NULL AUTO_INCREMENT,\n"
                    + "  expediente INT NOT NULL,\n"
                    + "  clave INT NOT NULL,\n"
                    + "  descripcion VARCHAR(255) NOT NULL,\n"
                    + "  campus VARCHAR(255) NOT NULL,\n"
                    + "  periodo INT NOT NULL,\n"
                    + "  PRIMARY KEY (id)\n"
                    + ") ENGINE=InnoDB;");

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void existeEnSolicitudes(Connection conexion) {
        try {
            seguirVerificando = true;
            do {
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + programa + "_" + plan + " WHERE expediente = " + expediente + ";");
                if (rs.next()) {
                    volverSolicitar = true;
                    System.out.println("Ya solicitaste tus materias.");
                    volverASolicitar(conexion);
                } else {
                    eleccionMaterias();
                    insertarMateriasElegidas(conexion);
                }
            } while (seguirVerificando);
        } catch (SQLException ex) {
            System.out.println("SQL Exception: " + ex.getMessage());
            System.out.println("SQL State: " + ex.getSQLState());
            System.out.println("Vendor Error: " + ex.getErrorCode());
        }
    }

    public static void volverASolicitar(Connection conexion) {
        Scanner sc = new Scanner(System.in);
        try {
            Statement stmt = conexion.createStatement();
            do {
                System.out.println("¿Quieres solicitarlas nuevamente? yes/no");
                String cambiar = sc.next();
                cambiar = cambiar.toLowerCase();
                if (cambiar.equals("yes")) {
                    stmt.executeUpdate("DELETE FROM " + programa + "_" + plan + " WHERE expediente = " + expediente + ";");
                    System.out.println("Actualización de datos completado.");
                    System.out.println("Redirigiendo a las opciones de materias...");
                    System.out.println("");
                    System.out.println("-----------------------------------------");
                    elegiste.clear();
                    elegiste.add("No has elegido materias.");
                    volverSolicitar = false;
                } else if (cambiar.equals("no")) {
                    System.out.println("Ta bien...");
                    volverSolicitar = false;
                    seguirVerificando = false;
                } else {
                    System.out.println("Eso no es una opcion");
                }
            } while (volverSolicitar);
        } catch (SQLException ex) {
            System.out.println("SQL Exception: " + ex.getMessage());
            System.out.println("SQL State: " + ex.getSQLState());
            System.out.println("Vendor Error: " + ex.getErrorCode());
        }

    }

    public static void materiasElegidas(Connection conexion) {

        try {
            Statement stmt = conexion.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT descripcion FROM " + programa + "_" + plan + " WHERE expediente = " + expediente + ";");
            while (rs.next()) {
                elegiste.remove("No has elegido materias.");
                String desc = rs.getString("descripcion");
                elegiste.add(desc);
            }

        } catch (SQLException ex) {
            System.out.println("SQL Exception: " + ex.getMessage());
            System.out.println("SQL State: " + ex.getSQLState());
            System.out.println("Vendor Error: " + ex.getErrorCode());
        }
    }

    public static void imprimirMateriasElegidas() {
        System.out.println(elegiste.size());
        if (elegiste.size() > 1) {
            System.out.println("Elegiste: ");
        } else {

        }
        for (int i = 0; i < elegiste.size(); i++) {
            System.out.println(elegiste.get(i));
        }
    }

    public static void eleccionMaterias() {
        Scanner scanner = new Scanner(System.in);
        ArrayList<Integer> posicionesDisponibles = new ArrayList<Integer>();
        materiasElegidas = new ArrayList<Integer>();
        int numMaximoElegido;

        // Aquí agregarías elementos a la listaOriginal
        for (int i = 0; i < clave2.size(); i++) {
            posicionesDisponibles.add(i);
        }

        // Establecer el número máximo de posiciones que se pueden elegir
        if (posicionesDisponibles.size() >= 7) {
            numMaximoElegido = 7;
        } else {
            numMaximoElegido = posicionesDisponibles.size();
        }
        int maximoElegidoPasar = 0;
        // Pedir al usuario que elija las posiciones
        String[] numerosComoString = new String[numMaximoElegido];
        boolean n = true;
        while (n) {
            System.out.println("Elige hasta " + numMaximoElegido + " posiciones separadas por coma (por ejemplo: 1,3,5):");
            String entrada = scanner.nextLine();
            String[] numerosComoString2 = entrada.split(",");
            if (numerosComoString2.length <= numerosComoString.length) {
                for (int i = 0; i < numerosComoString2.length; i++) {
                    numerosComoString[i] = numerosComoString2[i];
                }
                System.out.println("Materias colocadas correctamente");
                break;
            } else {
                System.out.println("Sobrepasaste el número máximo");
            }
        }

        for (int i = 0; i < numerosComoString.length; i++) {
            try {
                int num = Integer.parseInt(numerosComoString[i]);
                int posicion = num - 1;
                if (posicionesDisponibles.contains(posicion) && !materiasElegidas.contains(posicion)) {
                    materiasElegidas.add(posicion);
                }
            } catch (NumberFormatException e) {
                // Ignorar elementos que no sean números
            }
        }

        // Mostrar las posiciones elegidas por el usuario
        System.out.println("Posiciones elegidas por el usuario: " + materiasElegidas);
    }

    public static void insertarMateriasElegidas(Connection conexion) {
        try {
            for (Integer p : materiasElegidas) {
                Statement stmt = conexion.createStatement();

                stmt.executeUpdate("INSERT INTO " + programa + "_" + plan + " (expediente, clave, descripcion, campus, periodo) VALUES"
                        + " (" + expediente + "," + clave2.get(p) + ",'" + descripciones2.get(p) + "', '" + campus + "', " + ultimoperiodo + ");");
            }
            System.out.println("Materias solicitadas correctamente");

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }
}
