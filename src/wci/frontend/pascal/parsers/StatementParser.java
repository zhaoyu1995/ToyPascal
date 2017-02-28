package wci.frontend.pascal.parsers;

import wci.frontend.EofToken;
import wci.frontend.Token;
import wci.frontend.TokenType;
import wci.frontend.pascal.PascalErrorCode;
import wci.frontend.pascal.PascalParserTD;
import wci.frontend.pascal.PascalTokenType;
import wci.intermediate.ICodeFactory;
import wci.intermediate.ICodeNode;

import java.util.EnumSet;

import static wci.frontend.pascal.PascalErrorCode.MISSING_COLON;
import static wci.frontend.pascal.PascalErrorCode.MISSING_SEMICOLON;
import static wci.frontend.pascal.PascalErrorCode.UNEXPECTED_TOKEN;
import static wci.frontend.pascal.PascalTokenType.*;
import static wci.intermediate.icodeimpl.ICodeKeyImpl.LINE;
import static wci.intermediate.icodeimpl.ICodeNodeTypeImpl.NO_OP;

/**
 * <h1>StatementParser</h1>
 *
 * <p>Parse a Pascal statement.</p>
 */
public class StatementParser extends PascalParserTD {
    /**
     * Constructor.
     * @param parent the parent parser.
     */
    public StatementParser(PascalParserTD parent) {
        super(parent);
    }

    // Synchronization set for starting a statement.
    protected static final EnumSet<PascalTokenType> STMT_START_SET =
            EnumSet.of(BEGIN, CASE, FOR, PascalTokenType.IF, REPEAT, WHILE, IDENTIFIER, SEMICOLON);
    // Synchronization set for following a statement.
    protected static final EnumSet<PascalTokenType> STMT_FOLLOW_SET =
            EnumSet.of(SEMICOLON, END, ELSE, UNTIL, DOT);

    /**
     * Parse a statement.
     * To be overridden by the specialized statement parser subclass.
     * @param token the initial token.
     * @return the root node of the generated parse tree.
     * @throws Exception if an error occurred.
     */
    public ICodeNode parse(Token token) throws Exception {
        ICodeNode statementNode = null;

        switch ((PascalTokenType) token.getType()) {
            case BEGIN: {
                CompoundStatementParser compoundParser = new CompoundStatementParser(this);
                statementNode = compoundParser.parse(token);
                break;
            }
            // An assignment statement begins with a variable's identifier.
            case IDENTIFIER: {
                AssignmentStatementParser assignmentParser = new AssignmentStatementParser(this);
                statementNode = assignmentParser.parse(token);
                break;
            }
            case REPEAT: {
                RepeatStatementParser repeatParser = new RepeatStatementParser(this);
                statementNode = repeatParser.parse(token);
                break;
            }
            case WHILE: {
                WhileStatementParser whileParser = new WhileStatementParser(this);
                statementNode = whileParser.parse(token);
                break;
            }
            case FOR: {
                ForStatementParser forParser = new ForStatementParser(this);
                statementNode = forParser.parse(token);
                break;
            }
            case IF: {
                IfStatementParser ifParser = new IfStatementParser(this);
                statementNode = ifParser.parse(token);
                break;
            }
            case CASE: {
                CaseStatementParser caseParser = new CaseStatementParser(this);
                statementNode = caseParser.parse(token);
                break;
            }
            default: {
                statementNode = ICodeFactory.createICodeNode(NO_OP);
                break;
            }
        }

        // Set the current line number as an attribute.
        setLineNumber(statementNode, token);

        return statementNode;
    }


    protected void setLineNumber(ICodeNode node, Token token) {
        if (node != null) {
            node.setAttribute(LINE, token.getLineNumber());
        }
    }

    /**
     * Parse a statement list.
     * @param token the current token.
     * @param parentNode the parent node of the statement list.
     * @param terminator the token type of the node that terminates the list.
     * @param errorCode the error code if the terminator token if missing.
     * @throws Exception if an error occurred.
     */
    protected void parseList(Token token, ICodeNode parentNode,
                             PascalTokenType terminator,
                             PascalErrorCode errorCode) throws Exception {
        // Synchronization set for the terminator.
        EnumSet<PascalTokenType> terminatorSet = STMT_START_SET.clone();
        terminatorSet.add(terminator);

        // Loop to parse each statement until the END token or the end of the source line.
        while (!(token instanceof EofToken) && (token.getType() != terminator)) {
            // Parse a statement.The parent node adopts the statement node.
            ICodeNode statementNode = parse(token);
            parentNode.addChild(statementNode);

            token = currentToken();
            TokenType tokenType = token.getType();

            // Look for the semicolon between statements.
            if (tokenType == SEMICOLON) {
                token = nextToken();    // consume the ;
            } else if (STMT_START_SET.contains(tokenType)) {
                // If at the start of the next statement, then missing a semicolon.
                errorHandler.flag(token, MISSING_SEMICOLON, this);
            }

            // Synchronize at the start of the next statement or at the terminator.
            token = synchronize(terminatorSet);
        }

        // Look for the terminator token.
        if (token.getType() == terminator) {
            token = nextToken();    // consume the terminator token
        } else {
            errorHandler.flag(token, errorCode, this);
        }
    }
}
