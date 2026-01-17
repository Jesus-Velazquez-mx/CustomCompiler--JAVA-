package data;

import java.util.ArrayList;
import java.util.List;

public class Scanner {
    private final List<Token> listaTokens = new ArrayList<>();
    private int indiceActual = 0;
    private String codigoFuente, mensajeError;
    private boolean hayError;

    // Listado de palabras reservadas
    private static String[] PALABRAS_RESERVADAS = {
        "print", "class", "boolean", "int", "float", "while", "true", "false", "if", "else"
    };

    public Scanner(String codigo) {
        hayError = false;
        codigoFuente = (codigo != null) ? codigo : "";
        analizar();
    }

    // Recorre el texto y hace la lista de tokens
    private void analizar() {
        int posicion = 0, longitud = codigoFuente.length();

        while (posicion < longitud) {
            char caracter = codigoFuente.charAt(posicion);

            // 1) Saltar espacios en blanco
            if (Character.isWhitespace(caracter)) { 
                posicion++; 
                continue; 
            }

            // 2) Identificador o reservada: [a-zA-Z][a-zA-Z0-9]*
            if (Character.isLetter(caracter)) {
                int j = posicion + 1;
                while (j < longitud && Character.isLetterOrDigit(codigoFuente.charAt(j))) j++;
                String palabra = codigoFuente.substring(posicion, j);

                // Si hay símbolo inválido pegado al final, marcar todo hasta el siguiente espacio como inválido
                if (j < longitud) {
                    char sig = codigoFuente.charAt(j);
                    if (!Character.isWhitespace(sig) && !Character.isLetterOrDigit(sig) && !esSimboloValido(sig)) {
                        int k = j + 1;
                        while (k < longitud && !Character.isWhitespace(codigoFuente.charAt(k))) k++;
                        listaTokens.add(new Token(Token.TokenTipo.Invalido, codigoFuente.substring(posicion, k)));
                        posicion = k;
                        continue;
                    }
                }

                Token.TokenTipo tipoReservada = obtenerTipoReservada(palabra);
                if (tipoReservada != Token.TokenTipo.Invalido) {
                    listaTokens.add(new Token(tipoReservada, palabra));
                } else {
                    listaTokens.add(new Token(Token.TokenTipo.Identificador, palabra));
                }
                posicion = j;
                continue;
            }

            // 3) Número entero o fraccional
            if (Character.isDigit(caracter)) {
                int j = posicion + 1;
                while (j < longitud && Character.isDigit(codigoFuente.charAt(j))) j++;

                // Letras pegadas al número = inválido hasta espacio
                if (j < longitud && Character.isLetter(codigoFuente.charAt(j))) {
                    int k = j + 1;
                    while (k < longitud && !Character.isWhitespace(codigoFuente.charAt(k))) k++;
                    listaTokens.add(new Token(Token.TokenTipo.Invalido, codigoFuente.substring(posicion, k)));
                    posicion = k;
                    continue;
                }

                // Punto decimal
                if (j < longitud && codigoFuente.charAt(j) == '.') {
                    int k = j + 1;

                    // Requiere al menos un dígito después del punto
                    if (k < longitud && Character.isDigit(codigoFuente.charAt(k))) {
                        while (k < longitud && Character.isDigit(codigoFuente.charAt(k))) k++;

                        // Letras pegadas después del punto = inválido hasta espacio
                        if (k < longitud && Character.isLetter(codigoFuente.charAt(k))) {
                            int m = k + 1;
                            while (m < longitud && !Character.isWhitespace(codigoFuente.charAt(m))) m++;
                            listaTokens.add(new Token(Token.TokenTipo.Invalido, codigoFuente.substring(posicion, m)));
                            posicion = m;
                            continue;
                        }

                        // Símbolo inválido después del punto = inválido hasta espacio
                        if (k < longitud) {
                            char s = codigoFuente.charAt(k);
                            if (!Character.isWhitespace(s) && !Character.isLetterOrDigit(s) && !esSimboloValido(s)) {
                                int m = k + 1;
                                while (m < longitud && !Character.isWhitespace(codigoFuente.charAt(m))) m++;
                                listaTokens.add(new Token(Token.TokenTipo.Invalido, codigoFuente.substring(posicion, m)));
                                posicion = m;
                                continue;
                            }
                        }

                        listaTokens.add(new Token(Token.TokenTipo.NumFracc, codigoFuente.substring(posicion, k)));
                        posicion = k;
                        continue;
                    } else {
                        // Punto sin dígito = inválido hasta espacio
                        int k2 = j + 1;
                        while (k2 < longitud && !Character.isWhitespace(codigoFuente.charAt(k2))) k2++;
                        listaTokens.add(new Token(Token.TokenTipo.Invalido, codigoFuente.substring(posicion, k2)));
                        posicion = k2;
                        continue;
                    }
                }

                // Símbolo inválido pegado a entero = inválido hasta espacio
                if (j < longitud) {
                    char s = codigoFuente.charAt(j);
                    if (!Character.isWhitespace(s) && !Character.isLetterOrDigit(s) && !esSimboloValido(s)) {
                        int k = j + 1;
                        while (k < longitud && !Character.isWhitespace(codigoFuente.charAt(k))) k++;
                        listaTokens.add(new Token(Token.TokenTipo.Invalido, codigoFuente.substring(posicion, k)));
                        posicion = k;
                        continue;
                    }
                }

                // Entero válido
                listaTokens.add(new Token(Token.TokenTipo.NumEntero, codigoFuente.substring(posicion, j)));
                posicion = j;
                continue;
            }

            // 4A) Operadores aritméticos (+ - *)
            if (procesarOperador(caracter)) { 
                posicion++; 
                continue; 
            }

            // 4B) Comparadores (< > =)
            if (procesarComparador(caracter)) { 
                posicion++; 
                continue; 
            }

            // 4C) Otros símbolos (; { } ( ))
            if (procesarOtrosSimbolos(caracter)) { 
                posicion++; 
                continue; 
            }

            // 5) Símbolo desconocido: inválido hasta el siguiente espacio
            int k = posicion;
            while (k < longitud && !Character.isWhitespace(codigoFuente.charAt(k))) k++;
            listaTokens.add(new Token(Token.TokenTipo.Invalido, codigoFuente.substring(posicion, k)));
            posicion = k;
        }

        // EOF
        listaTokens.add(new Token(Token.TokenTipo.EOF, ""));
    }

    // Símbolos permitidos pegados a palabras/números
    private boolean esSimboloValido(char c) {
        switch (c) {
            case '+': case '-': case '*':
            case '<': case '>': case '=':
            case ';': case '{': case '}':
            case '(': case ')':
                return true;
            default:
                return false;
        }
    }

    // Si el lexema está en la lista, es PalabraReservada; sino Invalido
    private Token.TokenTipo obtenerTipoReservada(String lexema) {
        for (int i = 0; i < PALABRAS_RESERVADAS.length; i++) {
            if (PALABRAS_RESERVADAS[i].equals(lexema))
                return Token.TokenTipo.PalabraReservada;
        }
        return Token.TokenTipo.Invalido;
    }

    // OP: + - *
    private boolean procesarOperador(char c) {
        switch (c) {
            case '+': listaTokens.add(new Token(Token.TokenTipo.OpMAS, "+"));    return true;
            case '-': listaTokens.add(new Token(Token.TokenTipo.OpMENOS, "-"));  return true;
            case '*': listaTokens.add(new Token(Token.TokenTipo.OpMULTI, "*"));  return true;
            default:  return false;
        }
    }

    // CMP: < > =
    private boolean procesarComparador(char c) {
        switch (c) {
            case '<': listaTokens.add(new Token(Token.TokenTipo.CMPMEN, "<"));   return true;
            case '>': listaTokens.add(new Token(Token.TokenTipo.CMPMAY, ">"));   return true;
            case '=': listaTokens.add(new Token(Token.TokenTipo.Asignacion, "=")); return true;
            default:  return false;
        }
    }

    // Otros símbolos: ; { } ( )
    private boolean procesarOtrosSimbolos(char c) {
        switch (c) {
            case ';': listaTokens.add(new Token(Token.TokenTipo.PuntoComa, ";"));      return true;
            case '{': listaTokens.add(new Token(Token.TokenTipo.LlaveAbre, "{"));      return true;
            case '}': listaTokens.add(new Token(Token.TokenTipo.LlaveCierra, "}"));    return true;
            case '(': listaTokens.add(new Token(Token.TokenTipo.ParentAbre, "("));     return true;
            case ')': listaTokens.add(new Token(Token.TokenTipo.ParentCierra, ")"));   return true;
            default:  return false;
        }
    }

    // Entrega tokens en el orden en que fueron generados
    public Token siguienteToken() {
        if (indiceActual < listaTokens.size()) {
            Token t = listaTokens.get(indiceActual++);
            if (t.tipo == Token.TokenTipo.Invalido) {
                hayError = true;
                mensajeError = "Error léxico: token inválido -> " + t.valor;
            }
            return t;
        }
        return new Token(Token.TokenTipo.EOF, "");
    }

    public String getMensajeError() { return mensajeError; }
    public boolean hayError()       { return hayError; }
}
