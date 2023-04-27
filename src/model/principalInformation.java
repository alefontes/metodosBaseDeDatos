package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class principalInformation {

    //alumnos tienen expediente
    static int expediente;

    //Otros datos del alumno:
    static String nombre;
    static String programa;
    static String campus;
    static int semestre;
    static String carrera;
    static int creditosNecesarios;
    static int creditosActuales;
    static int plan;
    static int departamento;
    static String nombreDepartamento;
    static String correo;
    static String status;
    static String tipoAlumno;
    static int ultimoperiodo;

    //maestros tienen:
    static int nemp;
    static int contra;

    //otros datos del maestro:
    static String nombre2;
    static int nemp2;
    static int division;
    static String puesto;
    static String nombrePuesto;
    static int departamento2;
    static String campus2;
    static List<String> programas = new ArrayList<>();
    static String nombreDepartamento2;
    static String nombreDivision;
    static String correo2;
    static String status2;
    static List<String> nombreProgramas = new ArrayList<>();

    //ocupamos establecer una conexion a la base de datos
    static Connection conexion;

    //probablemente usemos 
    static String query;

    //el URL siempre será el mismo, y ocupamos conectarnos a la base de datos:
    static final String URL = "jdbc:mysql://localhost:3306/unison?useSSL=false&useTimezone=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    static final String dbUser = "root";
    static final String dbContra = "";

    //usaremos varias entradas de teclado (solo en este boceto)
    static Scanner sc = new Scanner(System.in);

    //usaremos varias veces el preparedStatement para hacer las consultas, esto 
    //para evitar inyecciones de SQL
    static PreparedStatement instruccion;
    static ResultSet extraerDatos;

    //establecer intentos y dilay si se equivoca
    static final int MAX_LOGIN_ATTEMPTS = 5;
    static final long LOCKOUT_TIME = 30000;

    //todo método tendrá que tener una conexión, la cuál solo se establecerá si
    //es correcto el ingreso
    public principalInformation(Connection conexion) {
        this.conexion = conexion;
    }

    //Primero tenemos que saber si el usuario es un Alumno o coordinador, para saber que información pedir
    public static void auth() {

        int tipoPersona;
        System.out.println("Seleccione si es alumno o coordinador:");
        System.out.println("1.- Alumno.");
        System.out.println("2.- Coordinador.");
        System.out.println("0.- Salir.");

        tipoPersona = sc.nextInt();

        switch (tipoPersona) {

            case 0:
                System.out.println("A elegidor salir.");
                System.exit(0);
                break;

            case 1:
                dbConnectionAlumno();
                break;

            case 2:
                dbConnectionCoord();
                break;

            default:
                System.out.println("Numero desconocido, se cerrará");
                System.exit(0);
                break;
        }
    }

    //Dependiendo de lo que sea el usuario, usaremos su respectivo login
    //Si es alumno se usa este metodo, el alumno tendrá 5 intentos para verificar su identidad y 30 segundo de dilay en caso de equivocarse
    public static void dbConnectionAlumno() {
        Scanner sc = new Scanner(System.in);
        String queryAuth = "SELECT a.nombre, aa.status FROM alumno a, alum_acad aa WHERE a.expediente=aa.expediente AND a.expediente = ? AND (aa.status = 'A' OR aa.status = 'B38');";
        int intentosFallidos = 0;
        long ultimoIntentoFallido = 0;
        boolean autenticado = false;

        while (!autenticado && intentosFallidos < MAX_LOGIN_ATTEMPTS) {
            System.out.println("Alumno:");
            System.out.println("Ingresa tu expediente:");
            expediente = sc.nextInt();

            try {
                conexion = DriverManager.getConnection(URL, dbUser, dbContra);
                instruccion = conexion.prepareStatement(queryAuth);
                instruccion.setInt(1, expediente);
                extraerDatos = instruccion.executeQuery();

                if (extraerDatos.next()) {
                    String nombre = extraerDatos.getString("nombre");
                    System.out.println("Bienvenido " + nombre);
                    datosAlumno();
                    autenticado = true;
                } else {
                    System.out.println("Expediente de alumno no encontrado o este ya egresó.");
                    intentosFallidos++;
                    if (intentosFallidos >= MAX_LOGIN_ATTEMPTS) {
                        ultimoIntentoFallido = System.currentTimeMillis();
                        System.out.println("Has alcanzado el límite de intentos fallidos. Tu cuenta ha sido bloqueada por " + LOCKOUT_TIME / 1000 + " segundos.");
                        Thread.sleep(LOCKOUT_TIME);
                    } else {
                        System.out.println("Te quedan " + (MAX_LOGIN_ATTEMPTS - intentosFallidos) + " intentos.");
                    }
                }
            } catch (SQLException ex) {
                System.out.println("SQL Exception:" + ex.getMessage());
                System.out.println("SQL State:" + ex.getSQLState());
                System.out.println("Vendor Error:" + ex.getErrorCode());
            } catch (InterruptedException ex) {
                System.out.println("InterruptedException:" + ex.getMessage());
            } finally {
                try {
                    if (conexion != null) {
                        conexion.close();
                    }
                } catch (SQLException ex) {
                    System.out.println("SQL Exception:" + ex.getMessage());
                    System.out.println("SQL State:" + ex.getSQLState());
                    System.out.println("Vendor Error:" + ex.getErrorCode());
                }
            }
        }

        if (!autenticado && intentosFallidos >= MAX_LOGIN_ATTEMPTS && (System.currentTimeMillis() - ultimoIntentoFallido) < LOCKOUT_TIME) {
            System.out.println("Tu cuenta sigue bloqueada. Por favor, inténtalo de nuevo más tarde.");
            System.exit(0);
        }
    }

    //Si es coordindor, tendrá 5 intentos y 30 segundos de dilay al equivocarse tambien
    public static void dbConnectionCoord() {
        Scanner sc = new Scanner(System.in);
        String queryAuth = "SELECT profesor.nombre FROM profesor, acceso WHERE profesor.nemp = acceso.nemp AND acceso.nemp = ? AND acceso.nemp = ? AND profesor.estatus = 'ACTIVO' AND acceso.puesto = 'CP';";
        int intentosFallidos = 0;
        long ultimoIntentoFallido = 0;
        boolean autenticado = false;

        while (!autenticado && intentosFallidos < MAX_LOGIN_ATTEMPTS) {
            System.out.println("Coordinador:");
            System.out.println("Ingresa tu número nemp:");
            nemp = sc.nextInt();
            System.out.println("Ingresa tu contraseña:");
            contra = sc.nextInt();

            try {
                conexion = DriverManager.getConnection(URL, dbUser, dbContra);
                instruccion = conexion.prepareStatement(queryAuth);
                instruccion.setInt(1, nemp);
                instruccion.setInt(2, contra);
                extraerDatos = instruccion.executeQuery();

                if (extraerDatos.next()) {
                    String nombre = extraerDatos.getString("nombre");
                    System.out.println("Bienvenido " + nombre);
                    datosCoordinador();
                    autenticado = true;
                } else {
                    System.out.println("Nemp de coordinador no encontrado.");
                    intentosFallidos++;
                    if (intentosFallidos >= MAX_LOGIN_ATTEMPTS) {
                        ultimoIntentoFallido = System.currentTimeMillis();
                        System.out.println("Has alcanzado el límite de intentos fallidos. Tu cuenta ha sido bloqueada por " + LOCKOUT_TIME / 1000 + " segundos.");
                        Thread.sleep(LOCKOUT_TIME);
                    } else {
                        conexion.close();
                    }
                }
            } catch (SQLException ex) {
                System.out.println("SQL Exception:" + ex.getMessage());
                System.out.println("SQL State:" + ex.getSQLState());
                System.out.println("Vendor Error:" + ex.getErrorCode());
            } catch (InterruptedException ex) {
                System.out.println("InterruptedException:" + ex.getMessage());
            } finally {
                try {
                    if (conexion != null) {
                        conexion.close();
                    }
                } catch (SQLException ex) {
                    System.out.println("SQL Exception:" + ex.getMessage());
                    System.out.println("SQL State:" + ex.getSQLState());
                    System.out.println("Vendor Error:" + ex.getErrorCode());
                }
            }
        }

        if (!autenticado && intentosFallidos >= MAX_LOGIN_ATTEMPTS && (System.currentTimeMillis() - ultimoIntentoFallido) < LOCKOUT_TIME) {
            System.out.println("Tu cuenta sigue bloqueada. Por favor, inténtalo de nuevo más tarde.");
            System.exit(0);
        }
    }

    public static void datosAlumno() {
        query = "SELECT nombre, prog, campus, plan FROM alumno WHERE expediente=" + expediente + ";";

        try {
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                nombre = extraerDatos.getString("nombre");
                programa = extraerDatos.getString("prog");
                campus = extraerDatos.getString("campus");
                plan = extraerDatos.getInt("plan");
            }

            query = "SELECT descripcion FROM programa WHERE clave='" + programa + "';";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                carrera = extraerDatos.getString("descripcion");
            }

            //semestre a cursar
            query = "SELECT COUNT(DISTINCT periodo) AS semestre FROM inscripcion WHERE expediente=" + expediente + ";";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                semestre = extraerDatos.getInt("semestre");
            }

            query = "SELECT cred, cred_apro FROM `alum_acad` WHERE expediente = " + expediente + ";";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                creditosNecesarios = extraerDatos.getInt("cred");
                creditosActuales = extraerDatos.getInt("cred_apro");
            }

            correo = "a" + expediente + "@unison.mx";

            query = "SELECT alumno.depto, departamento.Descripcion FROM `alumno`, departamento "
                    + "WHERE alumno.depto = departamento.clave AND alumno.expediente = " + expediente + " LIMIT 1;";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                departamento = extraerDatos.getInt("depto");
                nombreDepartamento = extraerDatos.getString("Descripcion");
            }

            query = "SELECT status,tipo FROM alum_acad WHERE expediente = " + expediente + ";";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                status = extraerDatos.getString("status");
                if (status.equals("A")) {
                    status = "ACTIVO";
                } else {
                    status = status;
                }
                tipoAlumno = extraerDatos.getString("tipo");
                if (tipoAlumno.equals("R")) {
                    tipoAlumno = "REGULAR";
                } else if (tipoAlumno.equals("I")) {
                    tipoAlumno = "IRREGULAR";
                } else {
                    tipoAlumno = tipoAlumno;
                }
            }

            query = "SELECT ultimo FROM alum_acad WHERE expediente =" + expediente;
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                ultimoperiodo = extraerDatos.getInt("ultimo");
            }
            //Creamos un objeto menuAlumno
            alumnos alumno = new alumnos(nombre, expediente, semestre, programa, carrera, campus, plan, departamento, nombreDepartamento, correo, status, tipoAlumno, ultimoperiodo, creditosNecesarios, creditosActuales, conexion);

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }

    }

    public static void datosCoordinador() {

        try {
            query = "SELECT nombre, campus, correo, estatus FROM profesor WHERE nemp=" + nemp + ";";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                nombre2 = extraerDatos.getString("nombre");
                campus2 = extraerDatos.getString("campus");
                correo2 = extraerDatos.getString("correo");
                status2 = extraerDatos.getString("estatus");
            }

            query = "SELECT puesto, division, departamento, programas FROM acceso WHERE nemp=" + nemp + ";";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                puesto = extraerDatos.getString("puesto");
                division = extraerDatos.getInt("division");
                departamento2 = extraerDatos.getInt("departamento");
                String programs = extraerDatos.getString("programas");
                String[] separarPrograms = programs.split(",");
                for (String p : separarPrograms) {
                    programas.add(p);
                    query = "SELECT descripcion FROM programa WHERE clave = '" + p + "';";
                    instruccion = conexion.prepareStatement(query);
                    extraerDatos = instruccion.executeQuery();
                    if (extraerDatos.next()) {
                        String nomProg = extraerDatos.getString("descripcion");
                        nombreProgramas.add(nomProg);
                    }
                }
            }

            query = "SELECT nombre FROM division WHERE clave=" + division + ";";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                nombreDivision = extraerDatos.getString("nombre");
            }

            query = "SELECT Descripcion FROM departamento WHERE clave=" + departamento + ";";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                nombreDepartamento2 = extraerDatos.getString("Descripcion");
            }

            query = "SELECT nombre FROM puesto WHERE clave = '" + puesto + "';";
            instruccion = conexion.prepareStatement(query);
            extraerDatos = instruccion.executeQuery();
            if (extraerDatos.next()) {
                nombrePuesto = extraerDatos.getString("nombre");
            }
            coordinadores coordinador = new coordinadores(nombre2, nemp, division, puesto, nombrePuesto, departamento2, campus2, programas, nombreDepartamento2, nombreDivision, correo2, status2, nombreProgramas, conexion);

        } catch (SQLException ex) {
            System.out.println("SQL Exception:" + ex.getMessage());
            System.out.println("SQL State:" + ex.getSQLState());
            System.out.println("Vendor Error:" + ex.getErrorCode());
        }
    }

    public static void main(String[] args) {
        auth();
    }
}
