package wci.frontend.pascal.parsers;

import wci.frontend.Token;
import wci.frontend.pascal.PascalParserTD;
import wci.frontend.pascal.PascalTokenType;
import wci.intermediate.ICodeFactory;
import wci.intermediate.ICodeNode;
import wci.intermediate.TypeSpec;
import wci.intermediate.icodeimpl.ICodeNodeTypeImpl;
import wci.intermediate.symtabimpl.Predefined;
import wci.intermediate.typeimpl.TypeChecker;

import java.util.EnumSet;

import static wci.frontend.pascal.PascalErrorCode.INCOMPATIBLE_TYPES;
import static wci.frontend.pascal.PascalErrorCode.MISSING_THEN;
import static wci.frontend.pascal.PascalTokenType.ELSE;
import static wci.frontend.pascal.PascalTokenType.THEN;

public class IfStatementParser extends StatementParser {
    // Synchronization set for THEN.
    private static final EnumSet<PascalTokenType> THEN_SET =
            StatementParser.STMT_START_SET.clone();

    static {
        THEN_SET.add(THEN);
        THEN_SET.addAll(StatementParser.STMT_FOLLOW_SET);
    }

    /**
     * Constructor.
     *
     * @param parent the parent parser.
     */
    public IfStatementParser(PascalParserTD parent) {
        super(parent);
    }

    /**
     * Parse an IF statement.
     *
     * @param token the initial token.
     * @return the root node of the generated tree.
     * @throws Exception if an error occurred.
     */
    @Override
    public ICodeNode parse(Token token) throws Exception {
        token = nextToken();    // consume the IF

        // Create an IF node.
        ICodeNode ifNode = ICodeFactory.createICodeNode(ICodeNodeTypeImpl.IF);

        // Parse the expression.
        // The IF node adopts the expression subtree as its first child.
        ExpressionParser expressionParser = new ExpressionParser(this);
        ICodeNode exprNode = expressionParser.parse(token);
        ifNode.addChild(exprNode);

        // Type check: The expression type must be boolean.
        TypeSpec exprType = exprNode != null ? exprNode.getTypeSpec() : Predefined.undefinedType;
        if (!TypeChecker.isBoolean(exprType)) {
            errorHandler.flag(token, INCOMPATIBLE_TYPES, this);
        }

        // Synchronize at the THEN.
        token = synchronize(THEN_SET);
        if (token.getType() == THEN) {
            token = nextToken();    // consume the THEN
        } else {
            errorHandler.flag(token, MISSING_THEN, this);
        }

        // Parse the THEN statement.
        // The IF node adopts the statement subtree as its second child.
        StatementParser statementParser = new StatementParser(this);
        ifNode.addChild(statementParser.parse(token));
        token = currentToken();

        // Look for an ELSE.
        if (token.getType() == ELSE) {
            token = nextToken();    // consume the ELSE

            // Parse the ELSE Statement.
            // The IF node adopts the statement subtree as its third child.
            ifNode.addChild(statementParser.parse(token));
        }

        return ifNode;
    }
}
