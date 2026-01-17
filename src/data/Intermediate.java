package data;

import java.util.List;

import javax.swing.JTextArea;

import data.Semantic.Simbolo;



/** Para generar el código intermedio, vamos a hacer un recorrido del árbol de sintaxis,
 	y usaremos plantillas de traducción. Este fue el primer método que se vio en clase. */
public class Intermediate {
    private List<Token> listaTokens;
    private List<Simbolo> tablaSemantica;
    JTextArea out;
    private int posicionActual = 0;
    private enum RegMode { INT16, INT32 } 
    private int contadorTemporales = 0;
	private String nombreClase;
    
	/* Usar temporales para las multiplicaciones*/
	private String nuevaTemporal(RegMode modo) {
        String nombre = "T" + contadorTemporales++;
        String tipo;
                if (modo == RegMode.INT32) {
            tipo = "float"; 
        } else {
            tipo = "int";  
        }
        tablaSemantica.add(new Simbolo(nombre, tipo, "?", 0)); 
        return nombre;
    }

  
    public Intermediate(List<Token> listaTokens, List<Simbolo> tablaSemantica,  JTextArea out) {
        this.listaTokens = listaTokens;
        this.tablaSemantica = tablaSemantica;
        this.out = out;
    }
    

    public void imprimirTodo() {        
    	// Para declarar las variables temporales, se lee todo el programa sin imprimir nada
    	JTextArea finalOut = this.out; 
        this.out = new JTextArea(); 
        this.posicionActual = 0; 
        leerProgram(); 
        this.out = finalOut; 
        this.contadorTemporales = 0;
        capturarNombreClase();
        imprimirHeader();
        imprimirData(); 
        imprimirCODE(); 
        leerProgram();
        imprimirEND();
    }
    
    /** PLANTILLAS */
    public void imprimirHeader() {
    	out.append("\tTITLE\t" + nombreClase + "\n");
        out.append("\t.MODEL\tSMALL\n");
        out.append("\t.STACK\t100h\n");
        out.append("\t.DATA\n");
    }
    
    public void imprimirCODE() {
        out.append("\n\t.CODE\n");
        out.append("MAIN \tPROC\tFAR\n");
        out.append("\tMOV\tAX,@data\n\tMOV\tDS,AX\n\n");
    }
    
    public void imprimirEND() {
        out.append("\n\tMOV\tAX,4C00h\n\tINT\t21h\n\n");
        out.append("MAIN \tENDP");
    }

    private void imprimirCONDICION(Token izquierdaTok, Token derechaTok) {
        RegMode m = (esFloat(izquierdaTok) || esFloat(derechaTok)) ? RegMode.INT32 : RegMode.INT16;
        String rx = RX(m);
        String izquierda = izquierdaTok.valor;
        String derecha   = derechaTok.valor;

        out.append("\tMOV\t" + rx + "," + izquierda + "\n");
        out.append("\tCMP\t" + rx + "," + derecha   + "\n");
    }

    private void imprimirSALTO(String operador, String etiquetaFalsa) {
        if (operador.equals("<"))       out.append("\tJGE\t" + etiquetaFalsa + "\n");
        else if (operador.equals(">"))  out.append("\tJLE\t" + etiquetaFalsa + "\n");
    }

    private void imprimirASIGNACIONBOOLEANA(String destino, String valor) {
        out.append("\tMOV\t" + destino + "," + valor +"\n");
    }

    private void imprimirSUMA(String destino, String primerTermino, String terminoActual, boolean esSoloDosTerminos, RegMode m) {
        String rx = RX(m);
        if (esSoloDosTerminos) {
            out.append("\tMOV\t" + rx + "," + primerTermino + "\n");
            out.append("\tADD\t" + rx + "," + terminoActual + "\n");
            out.append("\tMOV\t" + destino + "," + rx + "\n");
        } else {
            out.append("\tADD\t" + rx + "," + terminoActual + "\n");
        }
    }

    private void imprimirRESTA(String destino, String primerTermino, String terminoActual, boolean esSoloDosTerminos, RegMode m) {
        String rx = RX(m);
        if (esSoloDosTerminos) {
            out.append("\tMOV\t" + rx + "," + primerTermino + "\n");
            out.append("\tSUB\t" + rx + "," + terminoActual + "\n");
            out.append("\tMOV\t" + destino + "," + rx + "\n");
        } else {
            out.append("\tSUB\t" + rx + "," + terminoActual + "\n");
        }
    }
    
    private void imprimirMULTI(String destino, String primerTermino, String terminoActual, boolean esSoloDosTerminos, RegMode m) {
        String rx = RX(m); 
        String rd = RXM(m);

        if (esSoloDosTerminos) {
            if (esNumero(terminoActual)) { 
                out.append("\tMOV\t" + rx + "," + primerTermino + "\n");
                out.append("\tMOV\t" + rd + "," + terminoActual + "\n");
                out.append("\tMUL\t" + rd + "\n");
            } else {
                out.append("\tMOV\t" + rx + "," + primerTermino + "\n");
                out.append("\tMUL\t" + terminoActual + "\n");
            }
            
            if (!destino.equals(rx)) {
                out.append("\tMOV\t" + destino + "," + rx + "\n");
            } 
            
        } else {
            if (esNumero(terminoActual)) { 
                out.append("\tMOV\t" + rd + "," + terminoActual + "\n");
                out.append("\tMUL\t" + rd + "\n"); 
                out.append("\tMOV\t" + destino + "," + rx + "\n");
            } else {
                out.append("\tMUL\t" + terminoActual + "\n");       
                out.append("\tMOV\t" + destino + "," + rx + "\n");
            }
        }
    }
    
    private void imprimirASIGNACIONRX(String destino, RegMode m) {
        out.append("\tMOV\t" + destino + "," + RX(m) + "\n");
    }
    
    private void imprimirComparacion(List<Token> condicion, String etiquetaFalsa) {
        if (condicion.size() == 1) {
            Token unico = condicion.get(0);
            String v = (unico.valor == null ? "" : unico.valor).trim().toLowerCase();
            String rx = RX(RegMode.INT16); 

            if ("true".equals(v)) {
                out.append("\tMOV\t" + rx + ",1\n");
                out.append("\tCMP\t" + rx + ",1\n");
                out.append("\tJNE\t" + etiquetaFalsa + "\n");

                return;
            } else if ("false".equals(v)) {
                out.append("\tMOV\t" + rx + ",0\n");
                out.append("\tCMP\t" + rx + ",1\n"); 
                out.append("\tJNE\t" + etiquetaFalsa + "\n");
                return;
            }
        }

        int i = 0;
        Token izquierda = condicion.get(i); i++;
        String operador = condicion.get(i).valor; i++;
        Token derecha   = condicion.get(i);

        imprimirCONDICION(izquierda, derecha);
        imprimirSALTO(operador, etiquetaFalsa);
    }
    
    /** Imprime la expresión [ini, fin) en RX (AX/EAX) usando plantillas con precedencia */
    private void imprimirExpressionEnRX(int ini, int fin) {
    	RegMode modo = modoDeExpresion(listaTokens, ini, fin);
    	String rx = RX(modo);
    	int longitud = fin - ini;
        if (longitud == 3 && "*".equals(listaTokens.get(ini + 1).valor)) {
            String primerTermino = listaTokens.get(ini).valor;
            String segundoTermino = listaTokens.get(ini + 2).valor;
            imprimirMULTI(rx, primerTermino, segundoTermino, true, modo);
            return; 
        }
    	List<String> terminosParaSumaResta = new java.util.ArrayList<>();
    	Token primerToken = listaTokens.get(ini);
    	String operandoActual = primerToken.valor;
    	int i = ini + 1;
    	while (i < fin) {
    		String op = listaTokens.get(i).valor;
    		Token siguienteTerm = listaTokens.get(i + 1);
    		if ("*".equals(op)) {
    			String temporal = nuevaTemporal(modo);
    			out.append("\tMOV\t" + rx + "," + operandoActual + "\n");
    			imprimirMULTI(temporal, operandoActual, siguienteTerm.valor, false, modo);
    			operandoActual = temporal;
    			i += 2;
    		} else {
    			terminosParaSumaResta.add(operandoActual);
    			terminosParaSumaResta.add(op);
    			operandoActual = siguienteTerm.valor;
    			i += 2;
    		}
    	}
    	terminosParaSumaResta.add(operandoActual);
    	if (terminosParaSumaResta.isEmpty()) return;
    	String primerOp = terminosParaSumaResta.get(0);
    	out.append("\tMOV\t" + rx + "," + primerOp + "\n");
    	int j = 1;
    	while (j + 1 < terminosParaSumaResta.size()) {
    		String op = terminosParaSumaResta.get(j);
    		String term = terminosParaSumaResta.get(j + 1);
    		if ("+".equals(op)) {
    			imprimirSUMA("ignorado", "ignorado", term, false, modo);
    		} else if ("-".equals(op)) {
    			imprimirRESTA("ignorado", "ignorado", term, false, modo);
    		}
    		j += 2;
    	}
    }
    
    public void imprimirData() {
        for (Simbolo s : tablaSemantica) {
            String nombre = s.getNombre();
            String tipo = s.getTipo();
            String directiva = obtenerDirectiva(tipo);
            out.append(nombre + "\t" + directiva + "\t" + " ?\n");
        }
    }

    private String obtenerDirectiva(String tipo) {
        tipo = tipo.toLowerCase();
        switch (tipo) {
            case "int": return "DW";
            case "boolean": return "DB";
            case "float": return "DD";
            default: return "DW";
        }
    }
    
    private void capturarNombreClase() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_CLASS)) {
            posicionActual++;
            if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
                nombreClase = listaTokens.get(posicionActual).valor;
            }
        }
        posicionActual = inicio;
    }
    
    /** Program → class Identifier { DeclarationList StatementList } EOF */
    public boolean leerProgram() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_CLASS)) {
            posicionActual++;
            if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
                posicionActual++;
                if (tokenActualEs(Parser.C_LLAVEABRE)) {
                    posicionActual++;
                    if (leerDeclarationList()) {
                        if (leerStatementList()) {
                            if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                posicionActual++;
                            }
                        }
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }
    
    /** DeclarationList → [[VarDeclaration]]* */
    public boolean leerDeclarationList() {
        boolean encontrado = false;
        while (leerVarDeclaration()) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** VarDeclaration → Type Identifier ; */
    public boolean leerVarDeclaration() {
        int inicio = posicionActual;
        if (leerType()) {
            posicionActual++;
            if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
                posicionActual++;
                if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                    posicionActual++;
                    return true;
                }
            }
        }
        posicionActual = inicio;
        return false;
    }
    
    /** StatementList → [[Statement]]* */
    public boolean leerStatementList() {
        boolean encontrado = false;
        while (leerStatement()) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** Statement → while (BoolExpression){StatementList} | if (ExpressionBooleana ) {StatementList} [else { StatementList}]; 
     * | Identifier=Expression; | Identifier=BoolExpression; */
    public boolean leerStatement() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_WHILE)) { if (leerWhile()) return true; }
        posicionActual = inicio;
        if (tokenActualEs(Parser.C_IF)) { if (leerIf()) return true; }
        posicionActual = inicio;
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) { if (leerAsignacion() || leerAsignacionBooleana()) return true; }
        posicionActual = inicio;
        return false;
    }

    /** while ( ExpressionBooleana ) { StatementList } */
    public boolean leerWhile() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_WHILE)) {
            posicionActual++;
            if (tokenActualEs(Parser.C_PARENTABRE)) {
                posicionActual++;
                int condIni = posicionActual;
                if (leerExpresionBooleana()) {
                    int condFin = posicionActual;
                    List<Token> condicion = sublista(condIni, condFin);
                    if (tokenActualEs(Parser.C_PARENTCIERRA)) {
                        posicionActual++;

                        String etqInicio = "IniWh";
                        String etqFin    = "FinWh";

                        out.append(etqInicio + ":\n");
                        imprimirComparacion(condicion, etqFin);

                        if (tokenActualEs(Parser.C_LLAVEABRE)) {
                            posicionActual++;
                            if (leerStatementList()) {
                                if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                    posicionActual++;
                                    out.append("\tJMP\t" + etqInicio + "\n");
                                    out.append(etqFin + ":\n");
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
    public boolean leerIf() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_IF)) {
            posicionActual++;
            if (tokenActualEs(Parser.C_PARENTABRE)) {
                posicionActual++;
                int condIni = posicionActual;
                if (leerExpresionBooleana()) {
                    int condFin = posicionActual;
                    List<Token> condicion = sublista(condIni, condFin);
                    if (tokenActualEs(Parser.C_PARENTCIERRA)) {
                        posicionActual++;
                        imprimirComparacion(condicion, "Sino");
                        if (tokenActualEs(Parser.C_LLAVEABRE)) {
                            posicionActual++;
                            if (leerStatementList()) {
                                if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                    posicionActual++;
                                    if (tokenActualEs(Parser.C_ELSE)) {
                                        out.append("\tJMP\tFinIF\n");
                                        out.append("Sino:\n");
                                        posicionActual++;
                                        if (tokenActualEs(Parser.C_LLAVEABRE)) {
                                            posicionActual++;
                                            if (leerStatementList()) {
                                                if (tokenActualEs(Parser.C_LLAVECIERRA)) {
                                                    posicionActual++;
                                                    out.append("FinIF:\n");
                                                    return true;
                                                }
                                            }
                                        }
                                        posicionActual = inicio;
                                        return false;
                                    }
                                    out.append("Sino:\n");
                                    out.append("FinIF:\n");
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
    public boolean leerAsignacion() {
        int posIni = posicionActual;
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
            String var = listaTokens.get(posicionActual).valor;
            posicionActual++;
            if (tokenActualEs(Parser.C_ASIGNACION)) {
                posicionActual++;
                int posInicioDer = posicionActual;     
                if (leerExpression()) {
                    int posFinDer = posicionActual; 
                    if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                        posicionActual++;
                        if (posFinDer - posInicioDer == 1) {
                            Token valor = listaTokens.get(posInicioDer);
                            out.append("\tMOV\t" + var + "," + valor.valor + "\n");
                            return true;
                        }
                        imprimirExpressionEnRX(posInicioDer, posFinDer);
                        RegMode modo = modoDeExpresion(listaTokens, posInicioDer, posFinDer);
                        imprimirASIGNACIONRX(var, modo);
                        return true;
                    }
                }
            }
        }
        posicionActual = posIni;
        return false;
    }

    /** Identifier = ExpressionBooleana ; */
    public boolean leerAsignacionBooleana() {
        int inicio = posicionActual; 
        if (tokenActualEs(Parser.C_IDENTIFICADOR)) {
            String nombreVar = listaTokens.get(posicionActual).valor;
            posicionActual++;
            if (tokenActualEs(Parser.C_ASIGNACION)) {
                posicionActual++;
                int inicioExpr = posicionActual; 
                if (leerExpresionBooleana()) {
                    int finExpr = posicionActual; 
                    if (tokenActualEs(Parser.C_PUNTOCOMA)) {
                        posicionActual++;
                        if (finExpr - inicioExpr == 1) {
                            Token valorDerecha = listaTokens.get(inicioExpr);
                            if (isBooleano(valorDerecha)) {
                                imprimirASIGNACIONBOOLEANA(nombreVar, normalizarBooleano(valorDerecha.valor));
                            }
                        }
                        return true;
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Expression → Termino (OP Termino)*/
    public boolean leerExpression() {
        int inicio = posicionActual;          
        if (leerTermino()) {
            posicionActual++;
            while (leerOperador()) {
                posicionActual++;
                if (leerTermino()) {
                    posicionActual++;
                } else {
                    posicionActual = inicio;
                    return false;
                }
            }
            return true;
        }
        posicionActual = inicio;
        return false;
    }


    /** Termino → Identifier | NumEntero | NumFracc */
    public boolean leerTermino() {
        return tokenActualEs(Parser.C_IDENTIFICADOR) ||
               tokenActualEs(Parser.C_NUMENTERO) ||
               tokenActualEs(Parser.C_NUMFRACC);
    }

    /** BoolExpression → Expression CMP Expression | true | false */
    public boolean leerExpresionBooleana() {
        int inicio = posicionActual;
        if (tokenActualEs(Parser.C_TRUE) || tokenActualEs(Parser.C_FALSE)) { posicionActual++; return true; }
        if (leerExpression()) {
            if (leerComparador()) {
                posicionActual++;
                if (leerExpression()) return true;
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** OP → + | - | * */
    public boolean leerOperador() { 
        return tokenActualEs(Parser.C_OPMAS) || tokenActualEs(Parser.C_OPMENOS) || tokenActualEs(Parser.C_OPMULTI);
    }

    /** CMP → > | < */
    public boolean leerComparador() { 
        return tokenActualEs(Parser.C_CMPMAY) || tokenActualEs(Parser.C_CMPMEN);
    }

    /** Type → int | boolean | float */
    public boolean leerType() { 
        return tokenActualEs(Parser.C_INT) || tokenActualEs(Parser.C_BOOLEAN) || tokenActualEs(Parser.C_FLOAT);
    }

    private boolean tokenActualEs(int codigoEsperado) {
        if (posicionActual < listaTokens.size()) return listaTokens.get(posicionActual).codigo == codigoEsperado;
        return false;
    }

    /** Método auxiliar: booleano */
    private boolean isBooleano(Token t) {
        if (t == null) return false;
        String v = (t.valor == null ? "" : t.valor).trim().toLowerCase();
        return "true".equals(v) || "false".equals(v);
    }

    /** Método auxiliar: cambiar booleano a bit */
    private String normalizarBooleano(String v) {
        if (v == null) return "0";
        String s = v.trim().toLowerCase();
        if ("true".equals(s))  return "1";
        if ("false".equals(s)) return "0";
        return v; 
    }
    
    /** Método auxiliar: copia segura del rango (de, hasta) en listaTokens */
    private List<Token> sublista(int de, int hasta) {
        return new java.util.ArrayList<>(listaTokens.subList(de, hasta));
    }
    
    /** Método auxiliar: Ver si es float */
    private boolean esFloat(Token t) {
        return t != null && t.tipo == Token.TokenTipo.NumFracc;
    }

    /** Método auxiliar: Determinar el modo de la expresión (tipo de registro)*/
    private RegMode modoDeExpresion(List<Token> toks, int desdeIncl, int hastaExcl) {
        for (int k = desdeIncl; k < hastaExcl; k++) {
            if (esFloat(toks.get(k))) return RegMode.INT32;
        }
        return RegMode.INT16;
    }
    
    /** Método auxiliar: Devuelve el registro dependiendo del modo*/
    private String RX(RegMode m) { return (m == RegMode.INT32) ? "EAX" : "AX"; }
    
    private String RXM(RegMode m) { return (m == RegMode.INT32) ? "EDX" : "DX"; }
    
    /** Método auxiliar: verifica si un término es un número literal (entero o flotante) */
    private boolean esNumero(String termino) {
        for (Token t : listaTokens) {
            if (termino.equals(t.valor)) {
                return t.codigo == Parser.C_NUMENTERO || t.codigo == Parser.C_NUMFRACC;
            }
        }
        return false;
    }
    
}
