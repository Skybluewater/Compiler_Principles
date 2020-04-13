package bit.minisys.minicc.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.sun.tools.attach.AgentInitializationException;
import org.antlr.v4.gui.TreeViewer;

import com.fasterxml.jackson.databind.ObjectMapper;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.*;
import org.python.antlr.AST;

/*
 * PROGRAM     --> FUNC_LIST
 * FUNC_LIST   --> FUNC FUNC_LIST | e
 * FUNC        --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
 * TYPE        --> INT
 * ARGS   	   --> e | ARG_LIST
 * ARG_LIST    --> ARG ',' ARGLIST | ARG
 * ARG    	   --> TYPE ID
 * CODE_BLOCK  --> '{' STMTS '}'
 * STMTS       --> STMT STMTS | e
 * STMT        --> RETURN_STMT
 *
 * RETURN STMT --> RETURN EXPR ';'
 *
 * EXPR        --> TERM EXPR'  ***** TERM -> ID BINARY-> ALL OF BINARY CALCULATON // exp->assignment_exp exp'
 * EXPR'       --> '+' TERM EXPR' | '-' TERM EXPR' | e // exp'=',' assignment_exp exp' | e
 *
 * assignment_exp -> unary_exp assignment_operator assignment_exp |
 *
 * TERM        --> FACTOR TERM'
 * TERM'       --> '*' FACTOR TERM' | e
 *
 * FACTOR      --> ID
 *
 */

class ScannerToken {
    public String lexme;
    public String type;
    public int line;
    public int column;
}

public class ExampleParser implements IMiniCCParser {

    private ArrayList<ScannerToken> tknList;
    private int tokenIndex;
    private ScannerToken nextToken;

    @Override
    public String run(String iFile) throws Exception {
        System.out.println("Parsing...");

        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_PARSER_OUTPUT_EXT;
        String tFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;

        tknList = loadTokens(tFile);
        tokenIndex = 0;

        ASTNode root = program();


        String[] dummyStrs = new String[16];
        TreeViewer viewr = new TreeViewer(Arrays.asList(dummyStrs), root);
        viewr.open();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(oFile), root);

        //TODO: write to file


        return oFile;
    }


    private ArrayList<ScannerToken> loadTokens(String tFile) {
        tknList = new ArrayList<ScannerToken>();

        ArrayList<String> tknStr = MiniCCUtil.readFile(tFile);

        for (String str : tknStr) {
            if (str.trim().length() <= 0) {
                continue;
            }

            ScannerToken st = new ScannerToken();
            //[@0,0:2='int',<'int'>,1:0]
            String[] segs;
            if (str.indexOf("<','>") > 0) {
                str = str.replace("','", "'DOT'");

                segs = str.split(",");
                segs[1] = "=','";
                segs[2] = "<','>";

            } else {
                segs = str.split(",");
            }
            st.lexme = segs[1].substring(segs[1].indexOf("=") + 1);
            st.type = segs[2].substring(segs[2].indexOf("<") + 1, segs[2].length() - 1);
            String[] lc = segs[3].split(":");
            st.line = Integer.parseInt(lc[0]);
            st.column = Integer.parseInt(lc[1].replace("]", ""));

            tknList.add(st);
        }

        return tknList;
    }

    private ScannerToken getToken(int index) {
        if (index < tknList.size()) {
            return tknList.get(index);
        }
        return null;
    }

    public void matchToken(String type) {
        if (tokenIndex < tknList.size()) {
            ScannerToken next = tknList.get(tokenIndex);
            if (!next.type.equals(type)) {
                System.out.println("[ERROR]Parser: unmatched token, expected = " + type + ", "
                        + "input = " + next.type);
            } else {
                tokenIndex++;
            }
        }
    }

    //PROGRAM --> FUNC_LIST
    public ASTNode program() {
        ASTCompilationUnit p = new ASTCompilationUnit();
        ArrayList<ASTNode> fl = funcList();
        if (fl != null) {
            //p.getSubNodes().add(fl);
            p.items.addAll(fl);
        }
        p.children.addAll(p.items);
        return p;
    }

    //FUNC_LIST --> FUNC FUNC_LIST | e
    public ArrayList<ASTNode> funcList() {
        ArrayList<ASTNode> fl = new ArrayList<ASTNode>();

        nextToken = tknList.get(tokenIndex);
        if (nextToken.type.equals("EOF")) {
            return null;
        } else {
            ASTNode f = func();
            fl.add(f);
            ArrayList<ASTNode> fl2 = funcList();
            if (fl2 != null) {
                fl.addAll(fl2);
            }
            return fl;
        }
    }

    //FUNC --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
    public ASTNode func() {
        ASTFunctionDefine fdef = new ASTFunctionDefine();

        ASTToken s = type();

        fdef.specifiers.add(s);
        fdef.children.add(s);

        ASTFunctionDeclarator fdec = new ASTFunctionDeclarator();

        ASTIdentifier id = new ASTIdentifier();
        id.tokenId = tokenIndex;
        matchToken("Identifier");

        fdef.children.add(id);

        matchToken("'('");
        ArrayList<ASTParamsDeclarator> pl = arguments();
        matchToken("')'");

        //fdec.identifiers.add(id);
        if (pl != null) {
            fdec.params.addAll(pl);
            fdec.children.addAll(pl);
        }

        ASTCompoundStatement cs = new ASTCompoundStatement();
        codeBlock(cs);

        fdef.declarator = fdec;
        if(fdec!=null)
            fdef.children.add(fdec);
        if(cs!=null) {
            fdef.children.add(cs);
            fdef.body = cs;
        }
        return fdef;
    }

    //TYPE --> INT |FLOAT | CHART
    public ASTToken type() {
        ScannerToken st = tknList.get(tokenIndex);

        ASTToken t = new ASTToken();
        if (st.type.equals("'int'") || st.type.equals("'float'") || st.type.equals("'double'") || st.type.equals("'char'")) {
            t.tokenId = tokenIndex;
            t.value = st.lexme;
            tokenIndex++;
        }
        return t;
    }

    //ARGUMENTS --> e | ARG_LIST
    public ArrayList<ASTParamsDeclarator> arguments() {
        nextToken = tknList.get(tokenIndex);
        if (nextToken.type.equals("')'")) { //ending
            return null;
        } else {
            ArrayList<ASTParamsDeclarator> al = argList();
            return al;
        }
    }

    //ARG_LIST --> ARGUMENT ',' ARGLIST | ARGUMENT
    public ArrayList<ASTParamsDeclarator> argList() {
        ArrayList<ASTParamsDeclarator> pdl = new ArrayList<ASTParamsDeclarator>();
        ASTParamsDeclarator pd = argument();
        pdl.add(pd);

        nextToken = tknList.get(tokenIndex);
        if (nextToken.type.equals("','")) {
            matchToken("','");
            ArrayList<ASTParamsDeclarator> pdl2 = argList();
            pdl.addAll(pdl2);
        }

        return pdl;
    }

    //ARGUMENT --> TYPE ID | ID
    public ASTParamsDeclarator argument() {
        ASTParamsDeclarator pd = new ASTParamsDeclarator();
        ASTToken t = type();
        pd.specfiers.add(t);

        ASTIdentifier id = new ASTIdentifier();
        id.tokenId = tokenIndex;
        matchToken("Identifier");

        ASTVariableDeclarator vd = new ASTVariableDeclarator();
        vd.identifier = id;
        pd.declarator = vd;

        return pd;
    }

    //CODE_BLOCK --> '{' STMTS '}'
    public void codeBlock(ASTCompoundStatement par) {
        matchToken("'{'");
        nextToken = tknList.get(tokenIndex);
        if(nextToken.type.equals("'int'")||nextToken.type.equals("'double'")||nextToken.type.equals("'char'")||nextToken.type.equals("'float'")){
            par.children.add(decl());
        }
        stmts(par);
        matchToken("'}'");
    }

    public ASTDeclaration decl() {
        ASTDeclaration decl=new ASTDeclaration();
        decl.specifiers = new ArrayList<ASTToken>();
        decl.initLists = new ArrayList<ASTInitList>();
        ASTToken tkn = new ASTToken();
        tkn.tokenId=tokenIndex;
        tokenIndex++;
        decl.specifiers.add(tkn);
        if(tkn!=null)
            decl.children.add(tkn);
        init2(decl);
        return decl;
    }

    public ASTInitList init(ASTDeclaration par){
        ASTInitList init = new ASTInitList();
        nextToken=getToken(tokenIndex);
        if(nextToken.type.equals("','")){
            matchToken("','");
            if(getToken(tokenIndex+1).type.equals("','")||getToken(tokenIndex+1).type.equals("';'")){
                init.exprs=null;
                ASTVariableDeclarator var=var_decl();
                init.declarator=var;
                if(var!=null)
                    init.children.add(var);
            } else if (getToken(tokenIndex+1).type.equals("'['")){
                init.exprs = null;
                ASTArrayDeclarator arr = arr_decl();
                init.declarator = arr;
                if(arr!=null)
                    init.children.add(arr);
            } else{
                init.exprs = new ArrayList<ASTExpression>();
                init.declarator=null;
                ASTExpression exp = expr();
                init.exprs.add(exp);
                if(init!=null)
                    init.children.add(exp);
            }
            par.initLists.add(init);
            if(par!=null)
                par.children.add(init);
            init(par);
            return init;
        } else {
            matchToken("';'");
            return null;
        }
    }

    public void init2(ASTDeclaration par){
        ASTInitList init = new ASTInitList();
        if(getToken(tokenIndex+1).type.equals("','")||getToken(tokenIndex+1).type.equals("';'")){
            init.exprs=null;
            ASTVariableDeclarator var = var_decl();
            init.declarator=var;
            if(var!=null)
              init.children.add(var);
        } else if (getToken(tokenIndex+1).type.equals("'['")){
            init.exprs = null;
            ASTArrayDeclarator arr = arr_decl();
            init.declarator = arr;
            if(arr!=null)
              init.children.add(arr);
        }
        else{
            init.declarator=null;
            init.exprs = new ArrayList<ASTExpression>();
            ASTExpression exp = expr();
            init.exprs.add(exp);
            if(exp!=null)
                init.children.add(exp);
        }
        par.initLists.add(init);
        if(init!=null)
            par.children.add(init);
        //par.add
        init(par);
    }

    public ASTVariableDeclarator var_decl(){
        ASTVariableDeclarator var_decl = new ASTVariableDeclarator();
        ASTIdentifier id=new ASTIdentifier();
        id.tokenId=tokenIndex;
        tokenIndex++;
        var_decl.identifier=id;
        return var_decl;
    }

    public ASTArrayDeclarator arr_decl() {
        ASTArrayDeclarator arr = new ASTArrayDeclarator();
        ASTVariableDeclarator var = var_decl();
        matchToken("'['");
        ASTExpression exp = expr();
        matchToken("']'");
        arr.declarator = var;
        arr.expr = exp;
        if(var!=null)
            arr.children.add(var);
        if(exp!=null)
            arr.children.add(exp);
        return arr;
    }

    //STMTS --> STMT STMTS | e
    public ASTCompoundStatement stmts(ASTCompoundStatement par) {
        nextToken = tknList.get(tokenIndex);
        if (nextToken.type.equals("'}'"))
            return null;
        else if(nextToken.type.equals("'int'")||nextToken.type.equals("'double'")||nextToken.type.equals("'char'")||nextToken.type.equals("'float'")){
            ASTDeclaration decl=decl();
            par.children.add(decl);
            par.blockItems.add(decl);
            ASTCompoundStatement cs2 = stmts(par);
            return null;
        } else {
            ASTCompoundStatement cs = new ASTCompoundStatement();
            ASTNode s = stmt();
            par.blockItems.add(s);
            par.children.add(s);
            ASTCompoundStatement cs2 = stmts(par);
            return cs;
        }
    }

    //STMT --> ASSIGN_STMT | RETURN_STMT | DECL_STMT | FUNC_CALL
    public ASTStatement stmt() {
        nextToken = tknList.get(tokenIndex);
        if (nextToken.type.equals("'return'")) {
            return returnStmt();
        } else if (nextToken.type.equals("'for'")) {
            return IterationStmt();
        } else if (nextToken.type.equals("'}'")) {
            return null;
        } else if (nextToken.type.equals("'if'") || nextToken.type.equals("'switch'")) {
            return selectionStmt();
        } else if (nextToken.type.equals("'goto'")||nextToken.type.equals("'continue'")||nextToken.type.equals("'break'")){
            return jump();
        } else {
            return exp_stat();
        }
    }

    public ASTExpressionStatement exp_stat(){
        if(nextToken.type.equals("';'")){
            matchToken("';'");
            return null;
        }else{
            ASTExpression exp=expr();
            LinkedList<ASTExpression> exp2=new LinkedList<ASTExpression>();
            exp2.add(exp);
            ASTExpressionStatement exp_stat=new ASTExpressionStatement(exp2);
            exp_stat.exprs.add(exp);
            exp_stat.children.add(exp);
            matchToken("';'");
            return exp_stat;
        }
    }

    public ASTSelectionStatement selectionStmt() {
        matchToken("'if'");
        matchToken("'('");
        ASTExpression e1 = expr();
        LinkedList<ASTExpression> e11 = new LinkedList<ASTExpression>();
        e11.add(e1);
        matchToken("')'");
        ASTStatement s3;
        ASTStatement s4;
        nextToken = getToken(tokenIndex);
        if(nextToken.type.equals("'{'")) {
            matchToken("'{'");
            ASTCompoundStatement s = new ASTCompoundStatement();
            stmts(s);
            s3=s;
            matchToken("'}'");
        }else{
            s3 = stmt();
        }
        if (nextToken.type.equals("'else'")) {
            tokenIndex++;
            nextToken = getToken(tokenIndex);
            if(nextToken.type.equals("'{'")) {
                matchToken("'{'");
                ASTCompoundStatement s = new ASTCompoundStatement();
                stmts(s);
                s4=s;
                matchToken("'}'");
            }else{
                s4 = stmt();
            }
            ASTSelectionStatement s2 = new ASTSelectionStatement(e11,s3,s4);
            s2.then = s3;
            s2.otherwise = s4;
            if(e1!=null) {
                s2.cond.add(e1);
                s2.children.add(e1);
            }
            if(s3!=null)
                s2.children.add(s3);
            if(s4!=null)
                s2.children.add(s4);
            return s2;
        } else {
            ASTSelectionStatement s2 = new ASTSelectionStatement(e11,s3, null);
            s2.then = s3;
            if(e1!=null) {
                s2.cond.add(e1);
                s2.children.add(e1);
            }
            if(s3!=null) {
                s2.children.add(s3);
            }
            return s2;
        }
    }

    public ASTIterationStatement IterationStmt() {
        matchToken("'for'");
        matchToken("'('");
        ASTExpression e1 = expr();
        LinkedList<ASTExpression> e11 = new LinkedList<ASTExpression>();
        e11.add(e1);
        matchToken("';'");
        ASTExpression e2 = expr();
        LinkedList<ASTExpression> e21 = new LinkedList<ASTExpression>();
        e21.add(e2);
        matchToken("';'");
        ASTExpression e3 = expr();
        LinkedList<ASTExpression> e31 = new LinkedList<ASTExpression>();
        e31.add(e3);
        matchToken("')'");
        nextToken = getToken(tokenIndex);
        ASTStatement s;
        if (nextToken.type.equals("'{'")) {
            matchToken("'{'");
            ASTCompoundStatement s2 = new ASTCompoundStatement();
            stmts(s2);
            s = s2;
            matchToken("'}'");
        } else {
            s = stmt();
        }
        /*matchToken("'{'");
        ASTCompoundStatement s = new ASTCompoundStatement();
        stmts(s);
        matchToken("'}'");*/
        ASTIterationStatement is = new ASTIterationStatement(e11, e21, e31, s);
        if (e1 != null) {
            is.children.add(e1);
        }
        if (e2 != null) {
            is.children.add(e2);
        }
        if (e3 != null) {
            is.children.add(e3);
        }
        if (s != null) {
            is.children.add(s);
        }
        return is;
    }

    //RETURN_STMT --> RETURN EXPR ';'
    public ASTReturnStatement returnStmt() {
        matchToken("'return'");
        ASTReturnStatement rs = new ASTReturnStatement();
        ASTExpression e = expr();
        if (e != null) {
            rs.children.add(e);
        }
        matchToken("';'");
        rs.expr.add(e);
        return rs;
    }

    //EXPR --> TERM EXPR'
    public ASTExpression expr() {
        ASTExpression term = unary();
        nextToken=getToken(tokenIndex);
        if(nextToken.type.equals("'='")||nextToken.type.equals("'*='")||nextToken.type.equals("'%='")||nextToken.type.equals("'+='")||nextToken.type.equals("'/='")||nextToken.type.equals("'<<='")||nextToken.type.equals("'>>='")||nextToken.type.equals("'^='")||nextToken.type.equals("'|='")||nextToken.type.equals("'&='")){
            ASTBinaryExpression binary = new ASTBinaryExpression();
            ASTToken tkn = new ASTToken();
            tkn.tokenId=tokenIndex;
            tokenIndex++;
            nextToken = this.getToken(this.tokenIndex);
            binary.op=tkn;
            binary.expr1=term;
            binary.expr2 = expr();
            binary.children.add(term);
            binary.children.add(tkn);
            binary.children.add(binary.expr2);
            return binary;
        } else if (nextToken.type.equals("'+'")||nextToken.type.equals("'-'")||nextToken.type.equals("'*'")||nextToken.type.equals("'/'")||nextToken.type.equals("'%'")||nextToken.type.equals("'||'")||nextToken.type.equals("'&&'")||nextToken.type.equals("'>'")||nextToken.type.equals("'<'")||nextToken.type.equals("'<='")||nextToken.type.equals("'>='")) {
            ASTBinaryExpression binary = new ASTBinaryExpression();
            ASTToken tkn = new ASTToken();
            tkn.tokenId=tokenIndex;
            tokenIndex++;
            nextToken = this.getToken(this.tokenIndex);
            binary.op=tkn;
            binary.expr1=term;
            binary.expr2=expr2();
            binary.children.add(term);
            binary.children.add(tkn);
            binary.children.add(binary.expr2);
            return binary;
        } else {
            return term;
        }
    }

    //EXPR' --> '+' TERM EXPR' | '-' TERM EXPR' | e
    public ASTExpression expr2() {
        ASTExpression term = unary();
        if (nextToken.type.equals("'+'")||nextToken.type.equals("'-'")||nextToken.type.equals("'*'")||nextToken.type.equals("'/'")||nextToken.type.equals("'%'")||nextToken.type.equals("'||'")||nextToken.type.equals("'&&'")) {
            ASTBinaryExpression binary = new ASTBinaryExpression();
            ASTToken tkn = new ASTToken();
            tkn.tokenId = tokenIndex;
            tokenIndex++;
            binary.op = tkn;
            binary.expr1 = term;
            binary.expr2 = expr2();
            binary.children.add(term);
            binary.children.add(tkn);
            binary.children.add(binary.expr2);
            return binary;
        } else {
            return term;
        }
        //nextToken = tknList.get(tokenIndex);
        /*if (nextToken.type.equals("';'"))
            return null;
        else if (nextToken.type.equals("'('")||nextToken.type.equals("'++'")||nextToken.type.equals("'--'")||nextToken.type.equals("'sizeof'")||nextToken.type.equals("'&'")||nextToken.type.equals("'*'")||nextToken.type.equals("'~'")||nextToken.type.equals("'+'")||nextToken.type.equals("'-'")||nextToken.type.equals("'!'")) {
            ASTUnaryExpression unary = new ASTUnaryExpression();
            unary = unary();
            ASTBinaryExpression binary = new ASTBinaryExpression();
            binary.expr1=unary.expr;
            if(nextToken.type.equals("'='")||nextToken.type.equals("'*='")||nextToken.type.equals("'%='")||nextToken.type.equals("'+='")||nextToken.type.equals("'/='")||nextToken.type.equals("'<<='")||nextToken.type.equals("'>>='")||nextToken.type.equals("'^='")||nextToken.type.equals("'|='")||nextToken.type.equals("'&='")) {
                ASTToken tkn = new ASTToken();
                tkn.tokenId=tokenIndex;
                tokenIndex++;
                binary.op=tkn;
            }else{
                System.out.println("[ERROR]Parser: unreachable stmt!");
            }
            binary.expr2= unary();
            return binary;
        } else {
            return null;
        }*/
        /*if (nextToken.type.equals("'+'")) {
            ASTBinaryExpression be = new ASTBinaryExpression();
            ASTToken tkn = new ASTToken();
            tkn.tokenId = tokenIndex;
            matchToken("'+'");

            be.op = tkn;
            be.expr2 = term();

            ASTBinaryExpression expr = expr2();
            if (expr != null) {
                expr.expr1 = be;
                return expr;
            }

            return be;
        } else {
            return null;
        }*/
    }

    public ASTCastExpression cast() {
        if (nextToken.type.equals("'('")) {
            matchToken("'('");
            if (nextToken.type.equals("'const'") || nextToken.type.equals("'volatile'")) {
				ASTTypename typename = new ASTTypename();
				typename.declarator=null;
				ASTToken tkn = new ASTToken();
                tkn.tokenId = tokenIndex;
				typename.specfiers.add(tkn);
				ASTExpression tkn2=expr();
				tokenIndex++;
                return new ASTCastExpression(typename,tkn2);
            } else if (nextToken.type.equals("'void'") || nextToken.type.equals("'char'") || nextToken.type.equals("'int'") || nextToken.type.equals("'short'") || nextToken.type.equals("'float'") || nextToken.type.equals("'double'") || nextToken.type.equals("'long'") || nextToken.type.equals("'unsigned'") || nextToken.type.equals("'signed'")) {
                ASTTypename typename = new ASTTypename();
                typename.declarator=null;
                ASTToken tkn = new ASTToken();
                tkn.tokenId = tokenIndex;
                typename.specfiers.add(tkn);
                ASTExpression tkn2=expr();
                ASTCastExpression cast=new ASTCastExpression(typename,tkn2);
                tokenIndex++;
                return cast;
            } else{
                ASTCastExpression cast=new ASTCastExpression();
                ASTExpression exp;
                exp=expr();
                cast.expr=exp;
                return cast;
            }
        }else{
            ASTCastExpression exp=new ASTCastExpression();
            exp.expr= unary();
            return exp;
        }
    }

    public ASTUnaryExpression unary(){
        if(nextToken.type.equals("'++'")||nextToken.type.equals("'--'")||nextToken.type.equals("'sizeof'")){
            ASTToken tkn = new ASTToken();
            tkn.tokenId = tokenIndex;
            tokenIndex++;
            nextToken=getToken(tokenIndex);
            ASTExpression exp=expr();
            ASTUnaryExpression unary = new ASTUnaryExpression();
            unary.expr = exp;
            if(exp!=null)
                unary.children.add(exp);
            return unary;
        }else if(nextToken.type.equals("'&'")||nextToken.type.equals("'*'")||nextToken.type.equals("'~'")||nextToken.type.equals("'+'")||nextToken.type.equals("'-'")||nextToken.type.equals("'!'")){
            ASTToken tkn = new ASTToken();
            tkn.tokenId = tokenIndex;
            tokenIndex++;
            nextToken=getToken(tokenIndex);
            ASTCastExpression exp=cast();
            ASTUnaryExpression unary = new ASTUnaryExpression();
            unary.expr = exp;
            if(exp!=null)
                unary.children.add(exp);
            return unary;
        }else{
            ASTUnaryExpression unary = new ASTUnaryExpression();
            ASTPostfixExpression postfix = postfix();
            unary.expr=postfix();
            if(postfix!=null)
                unary.children.add(postfix);
            return unary;
        }
    }

    public ASTPostfixExpression postfix() {
        ASTExpression exp=factor();
        ASTPostfixExpression postfix = new ASTPostfixExpression();
        nextToken = getToken(tokenIndex);
        if (nextToken.type.equals("'('")) {
            matchToken("'('");
            ASTFunctionCall func = new ASTFunctionCall();
            func.argList = new ArrayList<ASTExpression>();
            ASTExpression expr = expr();
            matchToken("')'");
            func.argList.add(expr);
            func.funcname = exp;
            if(func!=null)
                postfix.children.add(func);
            postfix.expr = func;
            return postfix;
        } else if (nextToken.type.equals("'++'")||nextToken.type.equals("'--'")){
            ASTToken tkn = new ASTToken();
            tkn.tokenId=tokenIndex;
            tokenIndex++;
            postfix.op = tkn;
            nextToken=getToken(tokenIndex);
            postfix.expr = exp;
            if(exp!=null)
                postfix.children.add(exp);
            return postfix;
        } else if (nextToken.type.equals("'['")){
            matchToken("'['");
            ASTArrayAccess Arr = new ASTArrayAccess();
            Arr.arrayName = exp;
            Arr.elements = new ArrayList<>();
            Arr.elements.add(expr());
            postfix.expr = Arr;
            matchToken("']'");
            return postfix;
        }
        else {
            postfix.expr = exp;
            if(exp!=null)
                postfix.children.add(exp);
            return postfix;
        }
    }

    public ASTGotoStatement jump(){
        ASTIdentifier id=new ASTIdentifier();
        ASTGotoStatement g=new ASTGotoStatement();
        id.tokenId=tokenIndex;
        tokenIndex++;
        nextToken=getToken(tokenIndex);
        g.label=id;
        return g;
    }

    //FACTOR --> '(' EXPR // FACTOR ( AGU_EX_LIS) ')' | ID | CONST | FUNC_CALL primary_expression
    public ASTExpression factor() {
        nextToken = tknList.get(tokenIndex);
        if (nextToken.type.equals("Identifier")) {
            ASTIdentifier id = new ASTIdentifier();
            id.tokenId = tokenIndex;
            matchToken("Identifier");
            return id;
        } else if(nextToken.type.equals("IntegerConstant")||nextToken.type.equals("StringLiteral")) {
            ASTIntegerConstant inn=new ASTIntegerConstant();
            inn.tokenId=tokenIndex;
            tokenIndex++;
            return inn;
        } else {
            return null;
        }
    }
}