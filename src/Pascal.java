import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import wci.backend.interpreter.DebuggerType;
import wci.frontend.*;
import wci.intermediate.*;
import wci.backend.*;
import wci.message.*;
import wci.util.CrossReferencer;
import wci.util.ParseTreePrinter;

import static wci.backend.interpreter.DebuggerType.COMMAND_LINE;
import static wci.backend.interpreter.DebuggerType.GUI;
import static wci.ide.IDEControl.*;
import static wci.intermediate.symtabimpl.SymTabKeyImpl.ROUTINE_ICODE;

/**
 * Pascal
 *
 * Compile or interpret a Pascal source program.
 */
public class Pascal
{
    private Parser parser;              // language-independent parser
    private Source source;              // generic input source
    private ICode iCode;                // generated intermediate code
    private SymTabStack symTabStack;    // symbol table stack
    private Backend backend;            // backend

    private boolean intermediate;       // true to print intermediate code.
    private boolean xref;               // true to print cross-reference listing.

    private DebuggerType debuggerType;

    /**
     * Compile or interpret a Pascal source program.
     * @param operation either "compile" or "execute".
     * @param sourcePath the source file path.
     * @param inputPath the input file path.
     * @param flags the command line flags.
     */
    public Pascal(String operation, String debugger,String sourcePath, String inputPath, String flags) {
        try {
            if (debugger.equals("GUI")) {
                debuggerType = GUI;
            } else {
                debuggerType = COMMAND_LINE;
            }
            intermediate= flags.indexOf('i') > -1;
            xref        = flags.indexOf('x') > -1;

            source = new Source(new BufferedReader(new FileReader(sourcePath)));
            source.addMessageListener(new SourceMessageListener());

            parser = FrontendFactory.createParser("Pascal", "top-down", source);
            parser.addMessageListener(new ParserMessageListener());

            backend = BackendFactory.createBackend(operation, debuggerType, inputPath);
            backend.addMessageListener(new BackendMessageListener());

            parser.parse();
            source.close();

           if (parser.getErrorCount() == 0) {
                symTabStack = parser.getSymTabStack();

                SymTabEntry programId = symTabStack.getProgramId();
                iCode = (ICode) programId.getAttribute(ROUTINE_ICODE);

                if (xref) {
                    CrossReferencer crossReferencer = new CrossReferencer();
                    crossReferencer.print(symTabStack);
                }
                if (intermediate) {
                    ParseTreePrinter treePrinter = new ParseTreePrinter(System.out);
                    treePrinter.print(symTabStack);
                }
                backend.process(iCode, symTabStack);
           }
        } catch (Exception ex) {
            System.out.println("***** Internal translator error. *****");
            ex.printStackTrace();
        }
    }

    private static final String FLAGS = "[-ixlafcr]";
    private static final String USAGE = "Usage: Pascal execute|compile " + FLAGS +
            " <source file path>" + " [ <input file path> ]";

    /**
     * The main method.
     * @param args command-line arguments: "compile" or "execute" followed by
     *             optional flags followed by the source file path followed by
     *             an optional runtime input data file path.
     */
    public static void main(String args[]) {
        try {
            String operation = args[0];

            // Operation.
            if (!(   operation.equalsIgnoreCase("compile")
                  || operation.equalsIgnoreCase("execute"))) {
                throw new Exception();
            }

            int i = 0;
            String debugger = "COMMAND";
            if (++i < args.length) {
                if ("GUI".equals(args[i])) {
                    debugger = "GUI";
                } else {
                    --i;
                }
            }

            String flags = "";
            // Flags.
            while ((++i < args.length) && (args[i].charAt(0) == '-')) {
                flags += args[i].substring(1);
            }
            String sourcePath = null;
            String inputPath = null;
            // Source path.
            if (i < args.length) {
               sourcePath = args[i];
            } else {
                throw new Exception();
            }
           // Runtime input data file path.
            if (++i < args.length) {
                inputPath = args[i];
                File inputFile = new File(inputPath);
                if (!inputFile.exists()) {
                    System.out.println("Input file '"+inputPath+"' does not exist.");
                    throw new Exception();
                }
            }
            new Pascal(operation, debugger, sourcePath, inputPath, flags);
        }
        catch (Exception ex) {
            System.out.println(USAGE);
        }
    }

    private static final String SOURCE_LINE_FORMAT = "%03d %s";
    private static final String TAG_SOURCE_LINE_FORMAT = LISTING_TAG + "%03d %s";

    /**
     * Listener for source messages.
     */
    private class SourceMessageListener implements MessageListener {
        /**
         * Called by the source whenever it produces a message.
         * @param message the message.
         */
        public void messageReceived(Message message) {
            MessageType type = message.getType();
            Object body[] = (Object []) message.getBody();

            switch (type) {

                case SOURCE_LINE: {
                    int lineNumber = (Integer) body[0];
                    String lineText = (String) body[1];
                    if (debuggerType.equals(GUI)) {
                        System.out.println(String.format(TAG_SOURCE_LINE_FORMAT, lineNumber, lineText));
                    } else {
                        System.out.println(String.format(SOURCE_LINE_FORMAT, lineNumber, lineText));
                    }
                    break;
                }
            }
        }
    }

    private static final String PARSER_SUMMARY_FORMAT =
        "\n%,20d source lines." +
        "\n%,20d syntax errors." +
        "\n%,20.2f seconds total parsing time.\n";
    private static final String TAG_PARSER_SUMMARY_FORMAT =
            PARSER_TAG + "%,d source lines, %,d syntax errors, " +
                    "%,.2f seconds total parsing time.\n";



    private static final int PREFIX_WIDTH = 5;
    /**
     * Listener for parser messages.
     */
    private class ParserMessageListener implements MessageListener {
        /**
         * Called by the parser whenever it produces a message.
         * @param message the message.
         */
        public void messageReceived(Message message) {
            MessageType type = message.getType();

            switch (type) {
                case PARSER_SUMMARY: {
                    Number body[] = (Number[]) message.getBody();
                    int statementCount = (Integer) body[0];
                    int syntaxErrors = (Integer) body[1];
                    float elapsedTime = (Float) body[2];

                    if (debuggerType.equals(GUI)) {
                        System.out.printf(TAG_PARSER_SUMMARY_FORMAT, statementCount, syntaxErrors, elapsedTime);
                    } else {
                        System.out.printf(PARSER_SUMMARY_FORMAT, statementCount, syntaxErrors, elapsedTime);
                    }
                    break;
                }
                case SYNTAX_ERROR: {
                    Object body[] = (Object []) message.getBody();
                    int lineNumber = (Integer) body[0];
                    int position = (Integer) body[1];
                    String tokenText = (String) body[2];
                    String errorMessage = (String) body[3];

                    StringBuilder flagBuffer = new StringBuilder();
                    if (debuggerType.equals(COMMAND_LINE)) {
                        int spaceCount = PREFIX_WIDTH + position;

                        // Spaces up to the error position.
                        for (int i = 1; i < spaceCount; ++i) {
                            flagBuffer.append(' ');
                        }

                        // A pointer to the error followed by the error message.
                        flagBuffer.append("^\n*** ").append(errorMessage);
                    } else if (debuggerType.equals(GUI)) {
                        flagBuffer.append(String.format(SYNTAX_TAG + "%d: %s", lineNumber, errorMessage));
                    }

                    // Text, if any, of the bad token.
                    if (tokenText != null) {
                        flagBuffer.append(" [at \"").append(tokenText).append("\"]");
                    }

                    System.out.println(flagBuffer.toString());
                    break;
                }
            }
        }
    }

    private static final String INTERPRETER_SUMMARY_FORMAT =
        "\n%,20d statements executed." +
        "\n%,20d runtime errors." +
        "\n%,20.2f seconds total execution time.\n";
    private static final String TAG_INTERPRETER_SUMMARY_FORMAT =
            INTERPRETER_TAG + "%,d statements executed, %,d runtime errors, " +
                    "%,.2f seconds total execution time.\n";

    private static final String COMPILER_SUMMARY_FORMAT =
        "\n%,20d instructions generated." +
        "\n%,20.2f seconds total code generation time.\n";

    /**
     * Listener for back end messages.
     */
    private class BackendMessageListener implements MessageListener {
        /**
         * Called by the back end whenever it produces a message.
         * @param message the message.
         */
        public void messageReceived(Message message) {
            MessageType type = message.getType();

            switch (type) {

                case INTERPRETER_SUMMARY: {
                    Number body[] = (Number[]) message.getBody();
                    int executionCount = (Integer) body[0];
                    int runtimeErrors = (Integer) body[1];
                    float elapsedTime = (Float) body[2];

                    if (debuggerType.equals(GUI)) {
                        System.out.printf(TAG_INTERPRETER_SUMMARY_FORMAT,
                            executionCount, runtimeErrors,
                            elapsedTime);
                    } else {
                        System.out.printf(INTERPRETER_SUMMARY_FORMAT,
                            executionCount, runtimeErrors,
                            elapsedTime);
                    }
                    break;
                }

                case COMPILER_SUMMARY: {
                    Number body[] = (Number[]) message.getBody();
                    int instructionCount = (Integer) body[0];
                    float elapsedTime = (Float) body[1];

                    System.out.printf(COMPILER_SUMMARY_FORMAT,
                            instructionCount, elapsedTime);
                    break;
                }
            }
        }
    }
}