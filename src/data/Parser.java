package data;

import java.util.List;

public class Parser {
    private List<Token> listaTokens;
    private int posicionActual = 0;
    private StringBuilder mensajesError = new StringBuilder();

    public static final int C_CLASS = 1;
    public static final int C_BOOLEAN = 2;
    public static final int C_INT = 3;
    public static final int C_FLOAT = 4;
    public static final int C_WHILE = 5;
    public static final int C_PRINT = 6;
    public static final int C_TRUE = 7;
    public static final int C_FALSE = 8;
    public static final int C_PUNTOCOMA = 9;
    public static final int C_LLAVEABRE = 10;
    public static final int C_LLAVECIERRA = 11;
    public static final int C_PARENTABRE = 12;
    public static final int C_PARENTCIERRA = 13;
    public static final int C_OPMAS = 14;
    public static final int C_OPMENOS = 15;
    public static final int C_OPMULTI = 16;
    public static final int C_CMPMAY = 17;
    public static final int C_CMPMEN = 18;
    public static final int C_ASIGNACION = 19;
    public static final int C_IDENTIFICADOR = 20;
    public static final int C_NUMENTERO = 21;
    public static final int C_NUMFRACC = 22;
    public static final int C_IF = 23;
    public static final int C_ELSE = 24;
    public static final int C_AX = 25;
    public static final int C_EAX = 26;
    public static final int C_DX = 27;
    public static final int C_EDX = 28;



    public Parser(List<Token> tokens) { this.listaTokens = tokens; }

    public void asignarCodigos() {
        for (Token token : listaTokens) {
            switch (token.tipo) {
                case PalabraReservada: token.codigo = codigoReservada(token.valor); break;
                case Identificador:
                    int cod = codigoReservada(token.valor); 
                    if (cod != 0) {
                        token.codigo = cod;
                    } else {
                        token.codigo = C_IDENTIFICADOR;
                    }
                    break;
                case NumEntero: token.codigo = C_NUMENTERO; break;
                case NumFracc: token.codigo = C_NUMFRACC; break;
                case PuntoComa: token.codigo = C_PUNTOCOMA; break;
                case LlaveAbre: token.codigo = C_LLAVEABRE; break;
                case LlaveCierra: token.codigo = C_LLAVECIERRA; break;
                case ParentAbre: token.codigo = C_PARENTABRE; break;
                case ParentCierra: token.codigo = C_PARENTCIERRA; break;
                case OpMAS: token.codigo = C_OPMAS; break;
                case OpMENOS: token.codigo = C_OPMENOS; break;
                case OpMULTI: token.codigo = C_OPMULTI; break;
                case CMPMAY: token.codigo = C_CMPMAY; break;
                case CMPMEN: token.codigo = C_CMPMEN; break;
                case Asignacion: token.codigo = C_ASIGNACION; break;
                default: token.codigo = 0; break;
            }
        }
    }

    private int codigoReservada(String lexema) {
        switch (lexema) {
            case "class": return C_CLASS;
            case "boolean": return C_BOOLEAN;
            case "int": return C_INT;
            case "float": return C_FLOAT;
            case "while": return C_WHILE;
            case "print": return C_PRINT;
            case "true": return C_TRUE;
            case "false": return C_FALSE;
            case "if": return C_IF;
            case "else": return C_ELSE;
        }
        String l = lexema.toLowerCase();
        switch (l) {
            case "ax":  return C_AX;
            case "eax": return C_EAX;
            case "dx":  return C_DX;
            case "edx": return C_EDX;
            default:    return 0;
        }
    }


    public boolean analizar() {
        asignarCodigos();
        boolean resultado = parsearProgram();
        return resultado;
    }

    /** Program → class Identifier { DeclarationList StatementList } EOF */
    public boolean parsearProgram() {
        int inicio = posicionActual;
        if (tokenActualEs(C_CLASS)) {
            posicionActual++;
            if (tokenActualEs(C_IDENTIFICADOR)) {
                posicionActual++;
                if (tokenActualEs(C_LLAVEABRE)) {
                    posicionActual++;
                    if (parsearDeclarationList()) {
                        if (parsearStatementList()) {
                            if (tokenActualEs(C_LLAVECIERRA)) {
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

    /** DeclarationList → [[VarDeclaration]]* */
    public boolean parsearDeclarationList() {
        boolean encontrado = false;
        while (parsearVarDeclaration()) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** VarDeclaration → Type Identifier ; */
    public boolean parsearVarDeclaration() {
        int inicio = posicionActual;
        if (parsearType()) {
            posicionActual++;
            if (tokenActualEs(C_IDENTIFICADOR)) {
                posicionActual++;
                if (tokenActualEs(C_PUNTOCOMA)) {
                    posicionActual++;
                    return true;
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** StatementList → [[Statement]]* */
    public boolean parsearStatementList() {
        boolean encontrado = false;
        while (parsearStatement()) encontrado = true;
        if (!encontrado) return true;
        return true;
    }

    /** Statement → while (BoolExpression){StatementList} | if (BoolExpression) {StatementList} [else { StatementList}] 
     * | Identifier=Expression; | Identifier=BoolExpression; */
    public boolean parsearStatement() {
        int inicio = posicionActual;
        if (tokenActualEs(C_WHILE)) { if (parsearWhile()) return true; }
        posicionActual = inicio;
        if (tokenActualEs(C_IF)) { if (parsearif()) return true; }
        posicionActual = inicio;
        if (tokenActualEs(C_IDENTIFICADOR)) { if (parsearAsignacion() || parsearAsignacionBooleana()) return true; }
        posicionActual = inicio;
        return false;
    }

    /** while ( ExpressionBooleana ) { StatementList } */
    public boolean parsearWhile() {
        int inicio = posicionActual;
        if (tokenActualEs(C_WHILE)) {
            posicionActual++;
            if (tokenActualEs(C_PARENTABRE)) {
                posicionActual++;
                if (parsearExpresionBooleana()) {
                    if (tokenActualEs(C_PARENTCIERRA)) {
                        posicionActual++;
                        if (tokenActualEs(C_LLAVEABRE)) {
                            posicionActual++;
                            if (parsearStatementList()) {
                                if (tokenActualEs(C_LLAVECIERRA)) {
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
    public boolean parsearif() {
        int inicio = posicionActual;
        if (tokenActualEs(C_IF)) {
            posicionActual++;
            if (tokenActualEs(C_PARENTABRE)) {
                posicionActual++;
                if (parsearExpresionBooleana()) {
                    if (tokenActualEs(C_PARENTCIERRA)) {
                        posicionActual++;
                        if (tokenActualEs(C_LLAVEABRE)) {
                            posicionActual++;
                            if (parsearStatementList()) {
                                if (tokenActualEs(C_LLAVECIERRA)) {
                                    posicionActual++;
                                    if (tokenActualEs(C_ELSE)) {
                                        posicionActual++;
                                        if (tokenActualEs(C_LLAVEABRE)) {
                                            posicionActual++;
                                            if (parsearStatementList()) {
                                                if (tokenActualEs(C_LLAVECIERRA)) {
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
    public boolean parsearAsignacion() {
        int inicio = posicionActual;
        if (tokenActualEs(C_IDENTIFICADOR)) {
            posicionActual++;
            if (tokenActualEs(C_ASIGNACION)) {
                posicionActual++;
                if (parsearExpresion()) {
                    if (tokenActualEs(C_PUNTOCOMA)) {
                        posicionActual++;
                        return true;
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Identifier = ExpressionBooleana ; */
    public boolean parsearAsignacionBooleana() {
        int inicio = posicionActual;
        if (tokenActualEs(C_IDENTIFICADOR)) {
            posicionActual++;
            if (tokenActualEs(C_ASIGNACION)) {
                posicionActual++;
                if (parsearExpresionBooleana()) {
                    if (tokenActualEs(C_PUNTOCOMA)) {
                        posicionActual++;
                        return true;
                    }
                }
            }
        }
        posicionActual = inicio;
        return false;
    }

    /** Expression → Termino (OP Termino)* */
    public boolean parsearExpresion() {
        int inicio=posicionActual;
        if (parsearTermino()) {
            posicionActual++;
            while (parsearOperador()) {
                posicionActual++;
                if (parsearTermino()) {
                    posicionActual++;
                } else {
                    posicionActual=inicio;
                    return false;
                }
            }
            return true;
        }
        posicionActual=inicio;
        return false;
    }

    /** Termino → Identifier | NumEntero | NumFracc */
    public boolean parsearTermino() { return tokenActualEs(C_IDENTIFICADOR) || tokenActualEs(C_NUMENTERO) || tokenActualEs(C_NUMFRACC); }


    /** BoolExpression → Termino CMP Termino | true | false */
    public boolean parsearExpresionBooleana() {
        int inicio = posicionActual;
        if (tokenActualEs(C_TRUE) || tokenActualEs(C_FALSE)) { posicionActual++; return true; }
        if (parsearTermino()) {
            posicionActual++;
            if (parsearComparador()) {
                posicionActual++;
                if (parsearTermino())
                    posicionActual++;
                	return true;
            }
        }
        posicionActual = inicio;
        return false;
    }


    /** OP → + | - | * */
    public boolean parsearOperador() { return tokenActualEs(C_OPMAS) || tokenActualEs(C_OPMENOS) || tokenActualEs(C_OPMULTI); }

    /** CMP → > | < */
    public boolean parsearComparador() { return tokenActualEs(C_CMPMAY) || tokenActualEs(C_CMPMEN); }

    /** Type → int | boolean | float */
    public boolean parsearType() { return tokenActualEs(C_INT) || tokenActualEs(C_BOOLEAN) || tokenActualEs(C_FLOAT); }

    private boolean tokenActualEs(int codigoEsperado) {
        if (posicionActual < listaTokens.size()) return listaTokens.get(posicionActual).codigo == codigoEsperado;
        return false;
    }

    private boolean esEOF() {
        return posicionActual >= listaTokens.size() || (posicionActual < listaTokens.size() && listaTokens.get(posicionActual).codigo == 0);
    }

    public String getErrores() { return mensajesError.toString(); }
    public List<Token> getTokens() { return listaTokens; }
}
