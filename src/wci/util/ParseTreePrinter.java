package wci.util;

import wci.intermediate.ICode;
import wci.intermediate.ICodeKey;
import wci.intermediate.ICodeNode;
import wci.intermediate.SymTabEntry;
import wci.intermediate.icodeimpl.ICodeNodeImpl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <h1>ParseTreePrinter</h1>
 *
 * <p>Print a parse tree.</p>
 */
public class ParseTreePrinter {
    private static final int INDENT_WIDTH = 4;
    private static final int LINE_WIDTH = 80;
    private PrintStream ps;         // output print stream
    private int length;             // output line length
    private String indent;          // indent spaces
    private String indentation;     // indentation of a line;
    private StringBuffer line;      // output line

    /**
     * Constructor.
     * @param ps the output print stream.
     */
    public ParseTreePrinter(PrintStream ps) {
        this.ps = ps;
        this.length = 0;
        this.indentation = "";
        this.line = new StringBuffer();

        // The indent is INDENT_WIDTH spaces.
        this.indent = "";
        for (int i = 0; i < INDENT_WIDTH; ++i) {
            this.indent += " ";
        }
    }

    /**
     * Print the intermediate code as a parse tree.
     * @param iCode the intermediate code.
     */
    public void print(ICode iCode) {
        ps.println("\n===== INTERMEDIATE CODE =====\n");

        printNode((ICodeNodeImpl) iCode.getRoot());
        printLine();
    }

    /**
     * Print a parse tree node.
     * @param node the parse tree node.
     */
    private void printNode(ICodeNodeImpl node) {
        // Opening tag
        append(indentation);
        append("<" + node.toString());

        printAttributes(node);
        printTypeSpec(node);

        ArrayList<ICodeNode> childNodes = node.getChildren();

        // Print the node's children followed by the closing tag.
        if ((childNodes != null) && (childNodes.size() > 0)) {
            append(">");
            printLine();

            printChildNodes(childNodes);
            append(indentation);
            append("</" + node.toString() + ">");
        } else {    // No children: Close off the tag.
            append(" ");
            append("/>");
        }

        printLine();
    }

    /**
     * Print a parse tree node's attributes.
     * @param node the parse tree node.
     */
    private void printAttributes(ICodeNodeImpl node) {
        String saveIndentation = indentation;
        indentation += indent;

        Set<Map.Entry<ICodeKey, Object>> attributes = node.entrySet();
        Iterator<Map.Entry<ICodeKey, Object>> it = attributes.iterator();

        // Iterate to print each attribute.
        while (it.hasNext()) {
            Map.Entry<ICodeKey, Object> attribute = it.next();
            printAttribute(attribute.getKey().toString(), attribute.getValue());
        }

        indentation = saveIndentation;
    }

    /**
     * Print a node attribute as key="value".
     * @param keyString the key string.
     * @param value the value.
     */
    private void printAttribute(String keyString, Object value) {
        // If the value is a symbol table entry,use the identifier's name.
        // Else just use the value string.
        boolean isSymTabEntry = value instanceof SymTabEntry;
        String valueString = isSymTabEntry ? ((SymTabEntry) value).getName()
                                           : value.toString();

        String text = keyString.toLowerCase() + "=\"" + valueString + "\"";
        append(" ");
        append(text);

        // Include an identifier's nesting level.
        if (isSymTabEntry) {
            int level = ((SymTabEntry) value).getSymTab().getNestingLevel();
            printAttribute("LEVEL", level);
        }
    }

    /**
     * Print a parse tree node's child nodes.
     * @param childNodes
     */
    private void printChildNodes(ArrayList<ICodeNode> childNodes) {
        String saveIndentation = indentation;
        indentation += indent;

        for (ICodeNode child : childNodes) {
            printNode((ICodeNodeImpl) child);
        }

        indentation = saveIndentation;
    }

    /**
     * Print a parse tree node's type specification.
     * @param node
     */
    private void printTypeSpec(ICodeNodeImpl node) {}

    /**
     * Append text to the output line.
     * @param text the text to append.
     */
    private void append(String text) {
        int textLength = text.length();
        boolean lineBreak = false;

        // Wrap lines that are too long.
        if (length + textLength > LINE_WIDTH) {
            printLine();
            line.append(indentation);
            length = indentation.length();
            lineBreak = true;
        }

        // Append the text.
        if (!(lineBreak && text.equals(" "))) {
            line.append(text);
            length += textLength;
        }
    }

    /**
     * Print an output line.
     */
    private void printLine() {
        if (length > 0) {
            ps.println(line);
            line.setLength(0);
            length = 0;
        }
    }
}
