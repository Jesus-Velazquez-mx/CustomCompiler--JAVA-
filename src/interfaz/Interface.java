package interfaz;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import data.Parser;
import data.Scanner;
import data.Token;
import data.Semantic;
import data.Intermediate; // <-- NUEVO: generador en formato Intel
import data.Objectt;

public class Interface extends JFrame {

    private static final long serialVersionUID = 1L;

    // Áreas
    private JTextArea areaCodigo = new JTextArea();
    private JTextArea areaErrores = new JTextArea();
    private JTextArea areaCI = new JTextArea();
    private JTextArea areaCO = new JTextArea();

    // Tabla de símbolos
    private DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Tipo", "Token"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private JTable tablaTokens = new JTable(modeloTabla);

    // Botones
    private JButton btnParser;
    private JButton btnSemantico;
    private JButton btnCodigoIntermedio; // botón CI (arriba del panel CI)
    private JButton btnCodigoObjeto; // botón CI (arriba del panel CI)


    // Estado
    private List<Token> ultimaLista;     // tokens del último análisis léxico
    private File archivoActual = null;
    private List<Semantic.Simbolo> ultimaTablaSimbolos = null; // <-- NUEVO: tabla de símbolos del semántico OK

    public Interface() {
        super("Mini-Compilador — Analizador Léxico");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initComponents();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);

        // Cada vez que cambia el código, se invalida el estado de Parser/Semántico/CI
        areaCodigo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { bloquearParser(); }
            public void removeUpdate(DocumentEvent e) { bloquearParser(); }
            public void changedUpdate(DocumentEvent e) { bloquearParser(); }

            private void bloquearParser() {
                btnParser.setEnabled(false);
                btnSemantico.setEnabled(false);
                btnCodigoObjeto.setEnabled(false);
                btnCodigoIntermedio.setEnabled(false);
                if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
                areaErrores.setText("");
                areaCO.setText("");
                areaCI.setText("");
                areaErrores.setForeground(Color.BLACK);
                areaErrores.setFont(new Font("Consolas", Font.PLAIN, 22));
                ultimaTablaSimbolos = null; // al cambiar código, invalida tabla semántica previa
            }
        });
    }

    private void initComponents() {
        // Título y menú
        JLabel titulo = new JLabel("SuperCompiler");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 32));
        titulo.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JMenuBar menu = crearMenu();
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(titulo, BorderLayout.NORTH);
        norte.add(menu, BorderLayout.SOUTH);

        // Fila superior (Programa, Tokens, Errores)
        JPanel filaSuperior = new JPanel(new GridLayout(1, 3, 10, 10));

        // Programa
        JPanel panelPrograma = new JPanel(new BorderLayout());
        panelPrograma.setBorder(new TitledBorder("Programa"));
        ((TitledBorder)panelPrograma.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));
        areaCodigo.setFont(new Font("Consolas", Font.PLAIN, 22));
        panelPrograma.add(new JScrollPane(areaCodigo), BorderLayout.CENTER);

        // Tokens
        JPanel panelTokens = new JPanel(new BorderLayout());
        panelTokens.setBorder(new TitledBorder("Tabla de símbolos"));
        ((TitledBorder)panelTokens.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel barraTokens = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnTokens = new JButton("Tokens");
        btnTokens.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnTokens.addActionListener(e -> analizarLexico());
        barraTokens.add(btnTokens);
        panelTokens.add(barraTokens, BorderLayout.NORTH);

        tablaTokens.setFillsViewportHeight(true);
        tablaTokens.setFont(new Font("Consolas", Font.PLAIN, 22));
        tablaTokens.setRowHeight(28);
        tablaTokens.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 22));
        tablaTokens.setDefaultRenderer(Object.class, new ValidaRenderer());
        panelTokens.add(new JScrollPane(tablaTokens), BorderLayout.CENTER);

        // Errores
        JPanel panelErrores = new JPanel(new BorderLayout());
        panelErrores.setBorder(new TitledBorder("Errores"));
        ((TitledBorder)panelErrores.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel barraErrores = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnParser = new JButton("Parser");
        btnParser.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnParser.setEnabled(false);
        btnParser.addActionListener(e -> ejecutarParser());
        barraErrores.add(btnParser);

        btnSemantico = new JButton("Semántico");
        btnSemantico.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnSemantico.setEnabled(false); // Se habilita cuando el parser está OK
        btnSemantico.addActionListener(e -> ejecutarSemantico());
        barraErrores.add(btnSemantico);
        panelErrores.add(barraErrores, BorderLayout.NORTH);

        areaErrores.setEditable(false);
        areaErrores.setFont(new Font("Consolas", Font.PLAIN, 22));
        panelErrores.add(new JScrollPane(areaErrores), BorderLayout.CENTER);

        filaSuperior.add(panelPrograma);
        filaSuperior.add(panelTokens);
        filaSuperior.add(panelErrores);

        // Fila inferior (CI, CO)
        JPanel filaInferior = new JPanel(new GridLayout(1, 2, 10, 10));

        // Panel CI con botón "código intermedio" ARRIBA
        JPanel panelCI = new JPanel(new BorderLayout());
        panelCI.setBorder(new TitledBorder("CI"));
        ((TitledBorder)panelCI.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));
        areaCI.setEditable(false);
        areaCI.setFont(new Font("Consolas", Font.PLAIN, 22));

        // Botón arriba del área de CI
        JPanel barraCI = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnCodigoIntermedio = new JButton("Código intermedio");
        btnCodigoIntermedio.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnCodigoIntermedio.setEnabled(false); // se habilita solo tras semántico OK
        btnCodigoIntermedio.addActionListener(e -> generarCodigoIntermedio());
        barraCI.add(btnCodigoIntermedio);
        panelCI.add(barraCI, BorderLayout.NORTH);

        panelCI.add(new JScrollPane(areaCI), BorderLayout.CENTER);

        // Panel CO
        JPanel panelCO = new JPanel(new BorderLayout());
        panelCO.setBorder(new TitledBorder("CO"));
        ((TitledBorder)panelCO.getBorder()).setTitleFont(new Font("SansSerif", Font.BOLD, 22));
        areaCO.setEditable(false);
        areaCO.setFont(new Font("Consolas", Font.PLAIN, 22));
        panelCO.add(new JScrollPane(areaCO), BorderLayout.CENTER);
        
        // Botón arriba del área de CO
        JPanel barraCO = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        btnCodigoObjeto = new JButton("Código Objeto");
        btnCodigoObjeto.setFont(new Font("SansSerif", Font.BOLD, 22));
        btnCodigoObjeto.setEnabled(false); // se habilita solo tras semántico OK
        btnCodigoObjeto.addActionListener(e -> generarCodigoObjeto());
        barraCO.add(btnCodigoObjeto);
        panelCO.add(barraCO, BorderLayout.NORTH);

        filaInferior.add(panelCI);
        filaInferior.add(panelCO);

        // Centro
        JPanel centro = new JPanel(new GridLayout(2, 1, 10, 10));
        centro.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        centro.add(filaSuperior);
        centro.add(filaInferior);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(norte, BorderLayout.NORTH);
        getContentPane().add(centro, BorderLayout.CENTER);
    }

    private JMenuBar crearMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu mArchivo = new JMenu("Archivo");
        mArchivo.setFont(new Font("SansSerif", Font.BOLD, 22));

        JMenuItem itNuevo = new JMenuItem("Nuevo");
        itNuevo.setFont(new Font("SansSerif", Font.PLAIN, 22));
        itNuevo.addActionListener(e -> {
            areaCodigo.setText("");
            modeloTabla.setRowCount(0);
            areaErrores.setText("");
            areaErrores.setForeground(Color.BLACK);
            areaErrores.setFont(new Font("Consolas", Font.PLAIN, 22));
            areaCI.setText("");
            areaCO.setText("");
            ultimaLista = null;
            ultimaTablaSimbolos = null; // <-- reset
            btnParser.setEnabled(false);
            btnSemantico.setEnabled(false);
            if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
            archivoActual = null;
            setTitle("Mini-Compilador — Analizador Léxico");
        });

        JMenuItem itAbrir = new JMenuItem("Abrir");
        itAbrir.setFont(new Font("SansSerif", Font.PLAIN, 22));
        itAbrir.addActionListener(e -> abrirArchivo());

        JMenuItem itGuardar = new JMenuItem("Guardar");
        itGuardar.setFont(new Font("SansSerif", Font.PLAIN, 22));
        itGuardar.addActionListener(e -> guardar(false));

        JMenuItem itGuardarComo = new JMenuItem("Guardar como…");
        itGuardarComo.setFont(new Font("SansSerif", Font.PLAIN, 22));
        itGuardarComo.addActionListener(e -> guardar(true));

        JMenuItem itSalir = new JMenuItem("Salir");
        itSalir.setFont(new Font("SansSerif", Font.PLAIN, 22));
        itSalir.addActionListener(e -> dispose());

        mArchivo.add(itNuevo);
        mArchivo.add(itAbrir);
        mArchivo.addSeparator();
        mArchivo.add(itGuardar);
        mArchivo.add(itGuardarComo);
        mArchivo.addSeparator();
        mArchivo.add(itSalir);

        bar.add(mArchivo);
        return bar;
    }

    private void abrirArchivo() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Abrir programa");
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoActual = ch.getSelectedFile();
            try {
                String contenido = new String(
                        java.nio.file.Files.readAllBytes(archivoActual.toPath()),
                        StandardCharsets.UTF_8
                );
                areaCodigo.setText(contenido);
                setTitle("Mini-Compilador — " + archivoActual.getName());
                modeloTabla.setRowCount(0);
                areaErrores.setText("");
                areaErrores.setForeground(Color.BLACK);
                areaErrores.setFont(new Font("Consolas", Font.PLAIN, 22));
                areaCI.setText("");
                areaCO.setText("");
                ultimaLista = null;
                ultimaTablaSimbolos = null; // <-- reset
                btnParser.setEnabled(false);
                btnSemantico.setEnabled(false);
                if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo abrir: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardar(boolean forzarDialogo) {
        try {
            if (forzarDialogo || archivoActual == null) {
                JFileChooser ch = new JFileChooser();
                ch.setDialogTitle("Guardar programa");
                ch.setSelectedFile(new File("programa.txt"));
                if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
                archivoActual = ch.getSelectedFile();
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(archivoActual), StandardCharsets.UTF_8)) {
                w.write(areaCodigo.getText());
            }
            setTitle("Mini-Compilador — " + archivoActual.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Ejecuta análisis léxico; habilita Parser si no hay errores. */
    private void analizarLexico() {
        modeloTabla.setRowCount(0);
        areaErrores.setText("");
        areaCI.setText("");
        areaErrores.setForeground(Color.BLACK);
        areaErrores.setFont(new Font("Consolas", Font.PLAIN, 22));
        if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
        ultimaTablaSimbolos = null; // <-- al relanzar léxico, invalidamos tabla previa

        String codigo = areaCodigo.getText();
        Scanner analizador = new Scanner(codigo);

        int errores = 0;
        StringBuilder sb = new StringBuilder();
        List<Token> tmp = new ArrayList<>();

        while (true) {
            Token tk = analizador.siguienteToken();
            tmp.add(tk);

            // Agregamos TODAS las filas, incluyendo EOF (se pintará gris en el renderer)
            modeloTabla.addRow(new Object[]{ tk.tipo, tk.valor });

            if (tk.tipo == Token.TokenTipo.Invalido) {
                errores++;
                sb.append("Error léxico: token inválido -> ").append(tk.valor).append('\n');
            }
            if (tk.tipo == Token.TokenTipo.EOF) break;
        }

        ultimaLista = tmp;
        if (errores > 0) {
            areaErrores.setForeground(Color.RED);
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText(sb.toString());
            btnParser.setEnabled(false);
            btnSemantico.setEnabled(false);
            if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
            JOptionPane.showMessageDialog(this,
                    "Se detectaron " + errores + " error(es) léxico(s).",
                    "Análisis con errores",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            areaErrores.setForeground(new Color(0, 128, 0));
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("Análisis léxico correcto.\n");
            btnParser.setEnabled(true);
            btnSemantico.setEnabled(false);
            if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
            JOptionPane.showMessageDialog(this,
                    "Análisis léxico correcto.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }

        tablaTokens.repaint();
    }

    /** Llama a Parser.analizar() y muestra resultado en CI y Errores. */
    private void ejecutarParser() {
        if (ultimaLista == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero ejecuta Tokens.",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Parser p = new Parser(ultimaLista);
        boolean exito = p.analizar();

        if (exito) {
            areaErrores.setForeground(new Color(0, 128, 0));
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("Análisis sintáctico correcto.\n");
            btnSemantico.setEnabled(true); // habilitar semántico solo cuando el parser está bien
            // NO habilitar CI aquí (solo tras semántico OK)
            JOptionPane.showMessageDialog(this,
                    "El análisis sintáctico se completó sin errores.",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } else {
            areaErrores.setForeground(Color.RED);
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("SYNTAX ERROR\n\n" + p.getErrores());
            btnSemantico.setEnabled(false);
            if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
            JOptionPane.showMessageDialog(this,
                    "Se encontraron errores sintácticos.",
                    "Errores", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Llama a Semantic.analizar() y guarda la tabla para CI si todo está OK. */
    private void ejecutarSemantico() {
        if (ultimaLista == null) {
            JOptionPane.showMessageDialog(this,
                    "Primero ejecuta Tokens.",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Reutilizamos los mismos tokens que ya fueron codificados por Parser en ejecutarParser()
        Semantic s = new Semantic(ultimaLista);
        boolean ok = s.analizar(); // dentro de Semantic se puede mostrar UI adicional

        if (ok) {
            areaErrores.setForeground(new Color(0, 128, 0));
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("Análisis semántico correcto.\n");
            // Guardamos la tabla de símbolos para el generador intermedio
            ultimaTablaSimbolos = s.getTablaSimbolos();
            if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(true); // habilitar CI solo aquí
            JOptionPane.showMessageDialog(this,
                    "El análisis semántico se completó sin errores.",
                    "Semántico", JOptionPane.INFORMATION_MESSAGE);
        } else {
            areaErrores.setForeground(Color.RED);
            areaErrores.setFont(new Font("Consolas", Font.BOLD, 22));
            areaErrores.setText("SEMANTIC ERROR\n\n" + s.getErrores());
            ultimaTablaSimbolos = null; // no usar tabla si semántico falló
            if (btnCodigoIntermedio != null) btnCodigoIntermedio.setEnabled(false);
            JOptionPane.showMessageDialog(this,
                    "Se encontraron errores semánticos.",
                    "Semántico", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Genera y muestra el código intermedio (Intel) en el área CI cuando se presiona el botón. */
    private void generarCodigoIntermedio() {
        if (ultimaLista == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay tokens disponibles. Ejecuta Tokens/Parser/Semántico primero.",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (ultimaTablaSimbolos == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay tabla de símbolos válida. Ejecuta el análisis semántico sin errores.",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Crear y ejecutar el generador en formato Intel
        Intermediate inter = new Intermediate(ultimaLista, ultimaTablaSimbolos, areaCI);
        inter.imprimirTodo();
        
        if ( areaCI != null) {
        	btnCodigoObjeto.setEnabled(true);

    }
        
    
            JOptionPane.showMessageDialog(this,
                    "Código intermedio generado.",
                    "CI", JOptionPane.INFORMATION_MESSAGE);
        
    }
    
    private void generarCodigoObjeto() {

        // Por si acaso, validar que ya haya CI
        if (areaCI.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay código intermedio. Genera primero el CI.",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Ahora le pasas areaCI y areaCO
        Objectt obj = new Objectt(ultimaTablaSimbolos, areaCI, areaCO);
        obj.imprimirTodo();

        JOptionPane.showMessageDialog(this,
                "Código objeto generado.",
                "CO", JOptionPane.INFORMATION_MESSAGE);
    }


    /** Renderer: verde claro válidos, rojo claro inválidos, gris para EOF. */
    private static class ValidaRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final Color VERDE_CLARO = new Color(214, 245, 214);
        private static final Color ROJO_CLARO  = new Color(248, 215, 218);
        private static final Color GRIS_CLARO  = new Color(230, 230, 230);

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            Object tipo = table.getModel().getValueAt(row, 0);
            boolean esInvalido = (tipo instanceof Token.TokenTipo)
                    && ((Token.TokenTipo) tipo) == Token.TokenTipo.Invalido;
            boolean esEOF = (tipo instanceof Token.TokenTipo)
                    && ((Token.TokenTipo) tipo) == Token.TokenTipo.EOF;

            if (!isSelected) {
                if (esEOF) c.setBackground(GRIS_CLARO);
                else c.setBackground(esInvalido ? ROJO_CLARO : VERDE_CLARO);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
