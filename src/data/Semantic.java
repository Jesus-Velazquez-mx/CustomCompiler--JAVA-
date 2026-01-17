/**
 * La clase valida y reporta los siguientes errores semánticos:
 * - Redeclaración de variables.
 * - Uso de variables no declaradas.
 * - Incompatibilidad de tipos en asignaciones.
 * - Términos no válidos en expresiones aritméticas.
 * - Mezcla de tipos distintos dentro de una misma expresión aritmética.
 * - Comparaciones no numéricas o entre tipos distintos.
 */

package data;

import java.util.ArrayList;
import java.util.List;

public class Semantic {
    private List<Token> listaTokens;
    private int posicionActual = 0;
    private StringBuilder mensajesError = new StringBuilder();

    // Clase para los Símbolos de la tabla
    public static class Simbolo {
        public final String nombre;
        public final String tipo;
        public String valor;
        public final int direccion;
        public Simbolo(String n, String t, String v, int d) {
            this.nombre=n; this.tipo=t; this.valor=v; this.direccion=d;
        }
		public String getNombre() {
			return nombre;
		}
		public String getTipo() {
			return tipo;
		}
    }

    // Para la tabla de Símbolos
    private final List<Simbolo> tablaSimbolos = new ArrayList<>();
    private int nextDir = 0;
    private String ultimoTipoExpr = null;

    // Para asignar en "Valor"
    private StringBuilder exprActual = null;

    public Semantic(List<Token> tokens) { this.listaTokens = tokens; }
    public List<Simbolo> getTablaSimbolos(){ return tablaSimbolos; }
    public String getErrores(){ return mensajesError.toString(); }

    // Iniciar el análisis
    public boolean analizar() {
        boolean resultado = analizarProgram();

        return resultado;
    }

	/** Program → class Identifier { DeclarationList StatementList } EOF */
    public boolean analizarProgram() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_CLASS)) {
            posicionActual++;
            if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
                posicionActual++;
                if (tokenActualEs(Parser.C_LLAVEABRE)) {
                    posicionActual++;
                    if (analizarDeclarationList()) {
                        if (analizarStatementList()) {
                            if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                posicionActual++;
                                if (esEOF()) return true;
                            }
                        }
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** DeclarationList → [[VarDeclaration]]* (ε permitido) */
    public boolean analizarDeclarationList() {
        boolean encontrado = false;
        while (analizarVarDeclaration()) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** VarDeclaration → Type Identifier ; */
    public boolean analizarVarDeclaration() {
        int inicio = posicionActual;
        String tipo = null;
        if (analizarType()) {
            int codeTipo = listaTokens.get(posicionActual).codigo;
            switch (codeTipo) {
                case Parser.C_INT: tipo = "int"; break;
                case Parser.C_FLOAT: tipo = "float"; break;
                case Parser.C_BOOLEAN: tipo = "boolean"; break;
                default: tipo = "<?>"; break; // Tipo para poner en la tabla
            }
            posicionActual++;
            if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
                String nombre = listaTokens.get(posicionActual).valor;
                if (existeSimbolo(nombre)) {
                    posicionActual = inicio;
                    registrarError("Redeclaración de variable: " + nombre); // Variables repetidas
                    return false;
                }
                posicionActual++;
                if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                    posicionActual++;
                    int dir = nextDir;
                    tablaSimbolos.add(new Simbolo(nombre, tipo, "", dir)); // Agregamos la nueva entrada
                    nextDir += sizeOf(tipo); // Modificamos el valor de la dirección
                    return true;
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** StatementList → [[Statement]]* (ε permitido) */
    public boolean analizarStatementList() {
        boolean encontrado = false;
        while (analizarStatement()) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** Statement → while (...) | if (...) | Identifier=...; */
    public boolean analizarStatement() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_WHILE)) { if (analizarWhile()) return true; }
        posicionActual = inicio;
        if (tokenActualEs(Parser.C_IF)) { if (analizarIf()) return true; }
        posicionActual = inicio;
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) { if (analizarAsignacion() || analizarAsignacionBooleana()) return true; }
        posicionActual = inicio;
        return false;
    }

    /** while ( ExpressionBooleana ) { StatementList } */
    public boolean analizarWhile() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_WHILE)) {
            posicionActual++;
            if (tokenActualEs(Parser.C_PARENTABRE)) {
                posicionActual++;
                if (analizarExpresionBooleana()) {
                    if (tokenActualEs(Parser.C_PARENTCIERRA)) {
                        posicionActual++;
                        if (tokenActualEs(Parser.C_LLAVEABRE)) {
                            posicionActual++;
                            if (analizarStatementList()) {
                                if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                    posicionActual++;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** if ( ExpressionBooleana ) { StatementList } [else { StatementList}] */
    public boolean analizarIf() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_IF)) {
            posicionActual++;
            if (tokenActualEs(Parser.C_PARENTABRE)) {
                posicionActual++;
                if (analizarExpresionBooleana()) {
                    if (tokenActualEs(Parser.C_PARENTCIERRA)) {
                        posicionActual++;
                        if (tokenActualEs(Parser.C_LLAVEABRE)) {
                            posicionActual++;
                            if (analizarStatementList()) {
                                if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                    posicionActual++;
                                    if (tokenActualEs(Parser.C_ELSE)) {
                                        posicionActual++;
                                        if (tokenActualEs(Parser.C_LLAVEABRE)) {
                                            posicionActual++;
                                            if (analizarStatementList()) {
                                                if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                                    posicionActual++;
                                                    return true;
                                                }
                                            }
                                        }
                                        posicionActual = inicio;
                                        return false;
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Identifier = Expression ; */
    public boolean analizarAsignacion() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
            String nombre = listaTokens.get(posicionActual).valor;
            Simbolo var = buscarSimbolo(nombre);
            if (var == null) {
                posicionActual = inicio;
                registrarError("Variable no declarada: " + nombre); // Variable no declarada
                return false;
            } // Si no encuentra la variable, sale
            posicionActual++;
            if (tokenActualEs(Parser.C_ASIGNACION)) {
                posicionActual++;

                // Comenzamos a construir la expresión textual
                exprActual = new StringBuilder();

                if (analizarExpresion()) {
                    String tipoExpr = ultimoTipoExpr;
                    if (tipoExpr == null || !var.tipo.equals(tipoExpr)) { // Evaluar tipos
                        posicionActual = inicio;
                        registrarError("Tipos incompatibles en asignación a '" + nombre + "': " + var.tipo + " := " + tipoExpr);
                        exprActual = null;
                        return false;
                    }
                    if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                        posicionActual++;
                        // Guardar la expresión completa en la tabla de símbolos
                        setValor(nombre, exprActual.toString().trim());
                        exprActual = null;
                        return true;
                    }
                }
                // Si falla, limpiar el builder
                exprActual = null;
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Identifier = ExpressionBooleana ; */
    public boolean analizarAsignacionBooleana() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
            String nombre = listaTokens.get(posicionActual).valor;
            Simbolo var = buscarSimbolo(nombre);
            if (var == null) {
                posicionActual = inicio;
                registrarError("Variable no declarada: " + nombre); // Variable no delcarada
                return false;
            }// Si no encuentra la variable, sale
            posicionActual++;
            if (tokenActualEs(Parser.C_ASIGNACION)) {
                posicionActual++;

                // Comenzamos a construir la expresión textual booleana
                exprActual = new StringBuilder();

                if (analizarExpresionBooleana()) {
                    String tipoExpr = ultimoTipoExpr;
                    if (tipoExpr == null || !var.tipo.equals(tipoExpr)) { // Evaluar tipos
                        posicionActual = inicio;
                        registrarError("Tipos incompatibles en asignación a '" + nombre + "': " + var.tipo + " := " + tipoExpr);
                        exprActual = null;
                        return false;
                    }
                    if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                        posicionActual++;
                        // Guardar la expresión booleana completa
                        setValor(nombre, exprActual.toString().trim());
                        exprActual = null;
                        return true;
                    }
                }
                // Si falla, limpiar el builder
                exprActual = null;
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Expression → Termino (OP Termino)* */
    public boolean analizarExpresion() {
        int inicio = posicionActual;
        ultimoTipoExpr = null;
        if (analizarTermino()) {
            String tipoAcum = tipoDelTokenActualParaTermino(); // Lee el tipo del token actual
            if (tipoAcum == null || "boolean".equals(tipoAcum)) { // Evaluar tipos
                posicionActual = inicio;
                registrarError("Término inválido en expresión aritmética");
                return false;
            }
            // Añadir el primer término al builder
            appendLexemaActualAlBuilder();

            posicionActual++;
            while (analizarOperador()) {
                // Añadir el operador al builder
                appendLexemaActualAlBuilder();
                posicionActual++;
                if (analizarTermino()) {
                    String tipoTerm = tipoDelTokenActualParaTermino(); // Lee el tipo del token actual
                    if (tipoTerm == null || "boolean".equals(tipoTerm)) { // Compara los tipos
                        posicionActual = inicio;
                        registrarError("Término no válido en expresión aritmética");
                        return false;
                    }
                    if (!tipoAcum.equals(tipoTerm)) { // Si los tipos no son iguales
                        posicionActual = inicio;
                        registrarError("Tipos incompatibles en expresión aritmética: " + tipoAcum + " con " + tipoTerm);
                        return false;
                    }
                    // Añadir el término al builder
                    appendLexemaActualAlBuilder();
                    posicionActual++;
                } else {
                    posicionActual = inicio;
                    return false;
                }
            }
            ultimoTipoExpr = tipoAcum;
            return true;
        }
        posicionActual = inicio;
        return false;
    }

    /** Termino → Identifier | NumEntero | NumFracc */
    public boolean analizarTermino() {
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) { // Si es identificador tiene que checar que esté declarada la variable
            String nombre = listaTokens.get(posicionActual).valor;
            if (!existeSimbolo(nombre)) {
                registrarError("Uso de variable no declarada: " + nombre); // Variable no declarada
                return false;
            }
            return true;
        }
        return tokenActualEs(Parser.C_NUMENTERO) || tokenActualEs(Parser.C_NUMFRACC);
    }

    /** BoolExpression → Termino CMP Termino | true | false */
    public boolean analizarExpresionBooleana() {
        int inicio = posicionActual;
        ultimoTipoExpr = null;

        if (tokenActualEs(Parser.C_TRUE) || tokenActualEs(Parser.C_FALSE)) {
            posicionActual++;
            ultimoTipoExpr = "boolean";
            return true;
        }

        if (analizarTermino()) {
            String tipoIzq = tipoDelTokenActualParaTermino();
            if (tipoIzq == null || "boolean".equals(tipoIzq)) {
                posicionActual = inicio;
                registrarError("Comparación con tipo no numérico");
                return false;
            }

            appendLexemaActualAlBuilder();
            posicionActual++; // consumir primer término

            if (!analizarComparador()) {
                posicionActual = inicio;
                return false;
            }
            appendLexemaActualAlBuilder();
            posicionActual++; // consumir comparador

            if (!analizarTermino()) {
                posicionActual = inicio;
                return false;
            }
            String tipoDer = tipoDelTokenActualParaTermino();
            if (tipoDer == null || "boolean".equals(tipoDer)) {
                posicionActual = inicio;
                registrarError("Comparación con tipo no numérico");
                return false;
            }
            if (!tipoIzq.equals(tipoDer)) {
                posicionActual = inicio;
                registrarError("Comparación entre tipos distintos: " + tipoIzq + " y " + tipoDer);
                return false;
            }

            appendLexemaActualAlBuilder();
            posicionActual++; // consumir segundo término

            ultimoTipoExpr = "boolean";
            return true;
        }

        posicionActual = inicio;
        return false;
    }


    /** OP → + | - | * */
    public boolean analizarOperador() {
        return tokenActualEs(Parser.C_OPMAS) || tokenActualEs(Parser.C_OPMENOS) || tokenActualEs(Parser.C_OPMULTI);
    }

    /** CMP → > | < */
    public boolean analizarComparador() {
        return tokenActualEs(Parser.C_CMPMAY) || tokenActualEs(Parser.C_CMPMEN);
    }

    /** Type → int | boolean | float */
    public boolean analizarType() {
        return tokenActualEs(Parser.C_INT) || tokenActualEs(Parser.C_BOOLEAN) || tokenActualEs(Parser.C_FLOAT);
    }

    private boolean tokenActualEs(int codigoEsperado) {
        if (posicionActual < listaTokens.size()) return listaTokens.get(posicionActual).codigo == codigoEsperado;
        return false;
    }

    private boolean esEOF() {
        return posicionActual >= listaTokens.size() || (posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo == 0);
    }

    private void registrarError(String msg) {
        int len = mensajesError.length();
        if (len > 0) {
            int lastNl = mensajesError.lastIndexOf("\n");
            String last = (lastNl >= 0) ? mensajesError.substring(lastNl + 1) : mensajesError.toString();
            if (msg.equals(last)) return;
            mensajesError.append('\n');
        }
        mensajesError.append(msg);
    }

    private boolean existeSimbolo(String nombre) {
        for (Simbolo s : tablaSimbolos) 
        	if (s.nombre.equals(nombre)) return true;
        return false;
    }

    private Simbolo buscarSimbolo(String nombre) {
        for (Simbolo s : tablaSimbolos) if (s.nombre.equals(nombre)) return s;
        return null;
    }

    private void setValor(String nombre, String valor) {
        for (Simbolo s : tablaSimbolos) {
            if (s.nombre.equals(nombre)) { s.valor = valor; return; }
        }
    }

    private String tipoDelTokenActualParaTermino() {
        if (posicionActual >= listaTokens.size()) return null;
        int c = listaTokens.get(posicionActual).codigo;
        if (c == Parser.C_NUMENTERO) return "int";
        if (c == Parser.C_NUMFRACC) return "float";
        if (c == Parser.C_IDENTIFICADOR) {
            Simbolo s = buscarSimbolo(listaTokens.get(posicionActual).valor);
            return (s != null) ? s.tipo : null;
        }
        return null;
    }

    private int sizeOf(String tipo){
        switch (tipo){
            case "int": return 2;
            case "float": return 4;
            case "boolean": return 1;
            default: return 0;
        }
    }

    /** Si estamos construyendo una expresión (en asignación), añade el lexema actual con un espacio */
    private void appendLexemaActualAlBuilder() {
        if (exprActual == null) return;
        if (posicionActual >= listaTokens.size()) return;
        String lexema = listaTokens.get(posicionActual).valor;
        if (lexema != null) {
            if (exprActual.length() > 0) exprActual.append(' ');
            exprActual.append(lexema);
        }
    }
}
