package bit.minisys.minicc.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.antlr.v4.gui.TreeViewer;

import com.fasterxml.jackson.databind.ObjectMapper;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.*;

import java.util.concurrent.TimeUnit;

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
 * EXPR        --> TERM EXPR'
 * EXPR'       --> '+' TERM EXPR' | '-' TERM EXPR' | e
 *
 * TERM        --> FACTOR TERM'
 * TERM'       --> '*' FACTOR TERM' | e
 *
 * FACTOR      --> ID  
 * 
 */

class ScannerToken {
	public String lexme; // 值
	public String type; // 类型
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

//		String[] dummyStrs = new String[16];
//		TreeViewer viewr = new TreeViewer(Arrays.asList(dummyStrs), root);
//	    viewr.open();

		// TODO: write to file
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File(oFile), root);

		return oFile;
	}

	// 提取属性流到tknList，无需修改
	private ArrayList<ScannerToken> loadTokens(String tFile) {
		tknList = new ArrayList<ScannerToken>();

		ArrayList<String> tknStr = MiniCCUtil.readFile(tFile);

		for (String str : tknStr) {
			if (str.trim().length() <= 0) {
				continue;
			}

			ScannerToken st = new ScannerToken();
			// [@0,0:2='int',<'int'>,1:0]
			String[] segs;
			if (str.indexOf("<','>") > 0) {
				str = str.replace("','", "'DOT'");
				segs = str.split(",");
				segs[1] = "=','";
				segs[2] = "<','>";
			} else {
				segs = str.split(",");
			}
			st.lexme = segs[1].substring(segs[1].indexOf("=") + 2, segs[1].length() - 1);
			st.type = segs[2].substring(segs[2].indexOf("<") + 1, segs[2].length() - 1);
			String[] lc = segs[3].split(":");
			st.line = Integer.parseInt(lc[0]);
			st.column = Integer.parseInt(lc[1].replace("]", ""));

			// System.out.println(st.lexme + " " + st.type + " " + st.line + " " +
			// st.column);
			tknList.add(st);
		}

		return tknList;
	}

	// 暂时没有用上
	private ScannerToken getToken(int index) {
		if (index < tknList.size()) {
			return tknList.get(index);
		}
		return null;
	}

	// 如果传入的type是当前属性流的type，tokenIndex++，否则报错
	public void matchToken(String type) {
		if (tokenIndex < tknList.size()) {
			ScannerToken next = tknList.get(tokenIndex);
			if (!next.type.equals(type)) {
				System.out
						.println("match wrong: " + next.lexme + " " + next.type + " " + next.line + " " + next.column);
				System.out
						.println("[ERROR]Parser: unmatched token, expected = " + type + ", " + "input = " + next.type);
				System.out.println();
			} else {
				tokenIndex++;
			}
		}
	}

	// PROGRAM --> FUNC_LIST
	public ASTNode program() {
		ASTCompilationUnit p = new ASTCompilationUnit();
		ArrayList<ASTNode> fl = funcList();
		if (fl != null) {
			p.items.addAll(fl);
		}
		return p;
	}

	// FUNC_LIST --> FUNC FUNC_LIST | e
	public ArrayList<ASTNode> funcList() {
		ArrayList<ASTNode> fl = new ArrayList<ASTNode>();

		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("EOF")) {
			return null;
		} else {
			ASTNode f = func();
			fl.add(f);
//			ArrayList<ASTNode> fl2 = funcList();
//			if (fl2 != null) {
//				fl.addAll(fl2);
//			}
			return fl;
		}
	}

	// FUNC --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
	public ASTNode func() {
		ASTFunctionDefine fdef = new ASTFunctionDefine();
		ASTToken s = type();
		fdef.specifiers.add(s);

		ASTFunctionDeclarator fdec = new ASTFunctionDeclarator();
		ASTVariableDeclarator vd = new ASTVariableDeclarator();
		ASTIdentifier id = new ASTIdentifier();

		ScannerToken st = tknList.get(tokenIndex);
		id.value = st.lexme;
		id.tokenId = tokenIndex;

		matchToken("Identifier");
		vd.identifier = id;
		fdec.declarator = vd;
		fdef.declarator = fdec;

		matchToken("'('");
		ArrayList<ASTParamsDeclarator> pl = arguments();
		matchToken("')'");

		if (pl != null) {
			fdec.params.addAll(pl);
		} else {
			fdec.params = null;
		}

		ASTCompoundStatement cs = codeBlock();
		fdef.body = cs;

		return fdef;
	}

	public ASTToken type() {
		ScannerToken st = tknList.get(tokenIndex);

		ASTToken t = new ASTToken();
		if (st.type.equals("'int'")) {
			t.tokenId = tokenIndex;
			t.value = st.lexme;
			tokenIndex++;
		} else if (st.type.equals("'void'")) {
			t.tokenId = tokenIndex;
			t.value = st.lexme;
			tokenIndex++;
		} else {
			t.tokenId = tokenIndex;
			t.value = st.lexme;
			tokenIndex++;
		}

		return t;
	}

	// ARGUMENTS --> e | ARG_LIST
	public ArrayList<ASTParamsDeclarator> arguments() {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("')'")) { // ending
			return null;
		} else {
			ArrayList<ASTParamsDeclarator> al = argList();
			return al;
		}
	}

	// ARG_LIST --> ARGUMENT ',' ARGLIST | ARGUMENT
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

	// ARGUMENT --> TYPE ID
	public ASTParamsDeclarator argument() {
		ASTParamsDeclarator pd = new ASTParamsDeclarator();
		ASTToken t = type(); // pd.type == ParamsDeclarator
		pd.specfiers.add(t);

		ASTIdentifier id = new ASTIdentifier();
		ScannerToken st = tknList.get(tokenIndex);
		id.tokenId = tokenIndex;
		id.value = st.lexme;

		matchToken("Identifier");

		ASTVariableDeclarator vd = new ASTVariableDeclarator();
		vd.identifier = id;
		pd.declarator = vd;

		return pd;
	}

	// CODE_BLOCK --> '{' STMTS '}'
	public ASTCompoundStatement codeBlock() {
		matchToken("'{'");
		ASTCompoundStatement cs = stmts();
		matchToken("'}'");

		return cs;
	}

	// STMTS --> STMT STMTS | e
	public ASTCompoundStatement stmts() {
		ASTCompoundStatement cs = new ASTCompoundStatement();
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'}'"))
			return null;
		while (true) {
			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals("'}'"))
				return cs;
			if (nextToken.type.equals("'return'")) {
				ASTStatement s = returnStmt();
				cs.blockItems.add(s);
			} else if (nextToken.type.equals("'int'")) {
				ASTDeclaration s = intStmt();
				cs.blockItems.add(s);
			} else if (nextToken.type.equals("Identifier")) {
				ASTExpressionStatement s = expStmt();
				cs.blockItems.add(s);
			} else if (nextToken.type.equals("'if'")) {
				ASTSelectionStatement s = ifStmt();
				cs.blockItems.add(s);

			}

		}
	}

	public ASTSelectionStatement ifStmt() {
		matchToken("'if'");
		ASTSelectionStatement ss = new ASTSelectionStatement();
		matchToken("'('");
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("Identifier")) {
			ASTIdentifier id = new ASTIdentifier();
			id.value = nextToken.lexme;
			id.tokenId = tokenIndex;
			tokenIndex++;

			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals("'['")) {
				ASTArrayAccess aa = new ASTArrayAccess();
				aa.arrayName = id;
				matchToken("'['");

				nextToken = tknList.get(tokenIndex);
				ASTIdentifier id2 = new ASTIdentifier();
				id2.tokenId = tokenIndex;
				id2.value = nextToken.lexme;
				tokenIndex++;

				nextToken = tknList.get(tokenIndex);
				if (nextToken.type.equals("']'")) {
					aa.elements.add(id2);
					ASTBinaryExpression es = bExpr();
					es.expr1 = aa;
					ss.cond.add(es);
				}

			} else {
				tokenIndex--;
				ASTBinaryExpression es = bExpr();
				es.expr1 = id;
				ss.cond.add(es);
			}
		}
		matchToken("')'");
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'{'")) {
			ASTCompoundStatement cs = new ASTCompoundStatement();
			matchToken("'{'");
			nextToken = tknList.get(tokenIndex);
			while (!nextToken.type.equals("'}'")) {
				// TimeUnit.SECONDS.sleep(1);
				// System.out.println("aaa: " + nextToken.lexme + " " + nextToken.type + " " +
				// nextToken.line + " " + nextToken.column);
				ASTExpressionStatement s = expStmt();
				tokenIndex--;
				matchToken("';'");
				cs.blockItems.add(s);
				nextToken = tknList.get(tokenIndex);
			}
			matchToken("'}'");
			ss.then = cs;
		} else {
			if (nextToken.type.equals("'return'")) {
				ASTStatement s = returnStmt();
				ss.then = s;
			}
		}
		return ss;

	}

	// RETURN_STMT --> RETURN EXPR ';'
	public ASTReturnStatement returnStmt() {
		matchToken("'return'");
		ASTReturnStatement rs = new ASTReturnStatement();
		ASTExpression e = expr();

		matchToken("';'");
		if (e != null) {
			rs.expr.add(e);
		} else {
			rs.expr = null;
		}

		return rs;
	}

	public ASTExpressionStatement expStmt() {
		ScannerToken st = tknList.get(tokenIndex);
		ASTIdentifier id = new ASTIdentifier();
		id.value = st.lexme;
		id.tokenId = tokenIndex;

		String next = tknList.get(tokenIndex + 1).type;
		ASTExpressionStatement es = new ASTExpressionStatement();

		if (next.equals("'['")) {
			tokenIndex++;
			ASTArrayAccess aa = new ASTArrayAccess();
			aa.arrayName = id;
			matchToken("'['");

			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals("Identifier")) {
				ASTIdentifier id2 = new ASTIdentifier();
				id2.value = nextToken.lexme;
				id2.tokenId = tokenIndex;
				tokenIndex++;

				nextToken = tknList.get(tokenIndex);
				if (nextToken.type.equals("']'")) {
					aa.elements.add(id2);
					matchToken("']'");
				} else if (nextToken.type.equals("'+'")) {
					ASTBinaryExpression be = new ASTBinaryExpression();
					ASTToken t = type();
					be.op = t;
					nextToken = tknList.get(tokenIndex);
					if (nextToken.type.equals("IntegerConstant")) {
						ASTIntegerConstant ic = new ASTIntegerConstant();
						ic.tokenId = tokenIndex;
						ic.value = Integer.parseInt(nextToken.lexme);
						tokenIndex++;
						be.expr1 = id2;
						be.expr2 = ic;
						aa.elements.add(be);
						matchToken("']'");
					}
				}
			}
			tokenIndex--;
			nextToken = tknList.get(tokenIndex + 1);
			if (nextToken.type.equals("'='") || nextToken.type.equals("'>'")) {
				ASTBinaryExpression be = bExpr();
				be.expr1 = aa;
				es.exprs.add(be);
			}
		} else if (next.equals("'('")) {
			ASTFunctionCall fc = funCall();
			fc.funcname = id;
			es.exprs.add(fc);

		} else if (next.equals("'='") || next.equals("'*='") || next.equals("'<'") || next.equals("'>'")) {
			ASTBinaryExpression be = bExpr();
			be.expr1 = id;
			es.exprs.add(be);

		} else if (next.equals("'++'")) {
			ASTPostfixExpression pfe = pfExpr();
			pfe.expr = id;
			es.exprs.add(pfe);
		}

		tokenIndex++;
		return es;

	}

	public ASTBinaryExpression bExpr() {
		ASTBinaryExpression be = new ASTBinaryExpression();
		tokenIndex++;

		ASTToken t = type();
		be.op = t;

		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("IntegerConstant")) {
			ASTIntegerConstant ic = new ASTIntegerConstant();
			ic.tokenId = tokenIndex;
			ic.value = Integer.parseInt(nextToken.lexme);
			tokenIndex++;

			nextToken = tknList.get(tokenIndex);
			if (nextToken.type.equals("'-'")) {
				ASTToken tkn = type();
				nextToken = tknList.get(tokenIndex);
				if (nextToken.type.equals("Identifier")) {
					ASTBinaryExpression be2 = new ASTBinaryExpression();
					ASTIdentifier id = new ASTIdentifier();
					id.tokenId = tokenIndex;
					id.value = nextToken.lexme;
					be2.op = tkn;
					be2.expr1 = ic;
					be2.expr2 = id;
					be.expr2 = be2;
					tokenIndex++;
				}
			} else if (nextToken.type.equals("';'")) {
				be.expr2 = ic;
			} else if (nextToken.type.equals("')'")) {
				be.expr2 = ic;
			}

		} else if (nextToken.type.equals("Identifier")) {
			ASTIdentifier id = new ASTIdentifier();
			id.value = nextToken.lexme;
			id.tokenId = tokenIndex;

			ScannerToken st = tknList.get(tokenIndex + 1);
			if (st.type.equals("'['")) {
				ASTArrayAccess aa = new ASTArrayAccess();
				aa.arrayName = id;
				tokenIndex++;
				matchToken("'['");

				nextToken = tknList.get(tokenIndex);
				ASTIdentifier id2 = new ASTIdentifier();
				id2.tokenId = tokenIndex;
				id2.value = nextToken.lexme;
				tokenIndex++;

				nextToken = tknList.get(tokenIndex);
				if (nextToken.type.equals("']'")) {
					aa.elements.add(id2);
					be.expr2 = aa;
					matchToken("']'");
				} else if (nextToken.type.equals("'+'")) {
					ASTToken tkn = type();
					nextToken = tknList.get(tokenIndex);
					if (nextToken.type.equals("IntegerConstant")) {
						ASTBinaryExpression be2 = new ASTBinaryExpression();
						ASTIntegerConstant ic = new ASTIntegerConstant();
						ic.tokenId = tokenIndex;
						ic.value = Integer.parseInt(nextToken.lexme);

						be2.op = tkn;
						be2.expr1 = id2;
						be2.expr2 = ic;
						aa.elements.add(be2);
						be.expr2 = aa;
						tokenIndex++;
						matchToken("']'");
					}
				}

			} else if (st.type.equals("';'")) {
				be.expr2 = id;
				tokenIndex++;
			} else if (st.type.equals("'('")) {
				ASTFunctionCall fc = funCall();
				fc.funcname = id;
				be.expr2 = fc;
			} else if (st.type.equals("'+'")) {
				tokenIndex++;
				ASTToken tkn = type();
				nextToken = tknList.get(tokenIndex);
				if (nextToken.type.equals("Identifier")) {
					ASTBinaryExpression be2 = new ASTBinaryExpression();
					ASTIdentifier id2 = new ASTIdentifier();
					id2.tokenId = tokenIndex;
					id2.value = nextToken.lexme;
					be2.op = tkn;
					be2.expr1 = id;
					be2.expr2 = id2;
					be.expr2 = be2;
					tokenIndex++;
				}

			}

		}

		return be;
	}

	public ASTPostfixExpression pfExpr() {
		ASTPostfixExpression pfe = new ASTPostfixExpression();
		tokenIndex++;

		nextToken = tknList.get(tokenIndex);
		pfe.op = type();
		return pfe;
	}

	public ASTDeclaration intStmt() {
		ASTDeclaration is = new ASTDeclaration();
		ASTToken s = type();
		is.specifiers.add(s);

		ArrayList<ASTInitList> il = initList();
		is.initLists.addAll(il);

		matchToken("';'");
		return is;
	}

	public ArrayList<ASTInitList> initList() {
		ArrayList<ASTInitList> il = new ArrayList<ASTInitList>();
		ASTInitList i = initVariable();
		il.add(i);

		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("','")) {
			matchToken("','");
			ArrayList<ASTInitList> il2 = initList();
			il.addAll(il2);
		}

		return il;
	}

	public ASTInitList initVariable() {
		ASTInitList i = new ASTInitList();

		ASTIdentifier id = new ASTIdentifier();
		ScannerToken st = tknList.get(tokenIndex);
		id.tokenId = tokenIndex;
		id.value = st.lexme;

		matchToken("Identifier");

		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'['")) {
			ASTArrayDeclarator ad = new ASTArrayDeclarator();
			matchToken("'['");

			ASTIntegerConstant ic = new ASTIntegerConstant();
			ScannerToken st2 = tknList.get(tokenIndex);
			ic.tokenId = tokenIndex;
			ic.value = Integer.parseInt(st2.lexme);
			tokenIndex++;

			ASTVariableDeclarator vd = new ASTVariableDeclarator();
			vd.identifier = id;

			ad.declarator = vd;
			ad.expr = ic;
			i.declarator = ad;
			i.exprs = null;
			matchToken("']'");
			return i;
		} else if (nextToken.type.equals("'='")) {
			ASTVariableDeclarator vd = new ASTVariableDeclarator();
			vd.identifier = id;
			i.declarator = vd;
			matchToken("'='");

			ScannerToken st2 = tknList.get(tokenIndex);
			if (st2.type.equals("IntegerConstant")) {
				ASTIntegerConstant ic = new ASTIntegerConstant();
				ic.tokenId = tokenIndex;
				ic.value = Integer.parseInt(st2.lexme);
				tokenIndex++;

				i.exprs.add(ic);
			} else if (st2.type.equals("Identifier")) {
				ASTIdentifier id2 = new ASTIdentifier();
				id2.value = st2.lexme;
				id2.tokenId = tokenIndex;

				if (tknList.get(tokenIndex + 1).type.equals("'('")) {
					ASTFunctionCall fc = funCall();
					fc.funcname = id2;
					i.exprs.add(fc);
				}
			}
			return i;

		} else {
			ASTVariableDeclarator vd = new ASTVariableDeclarator();
			vd.identifier = id;
			i.declarator = vd;
			i.exprs = null;
			return i;
		}

	}

	public ASTFunctionCall funCall() {
		ASTFunctionCall fc = new ASTFunctionCall();

		tokenIndex++;
		matchToken("'('");

		ScannerToken st = tknList.get(tokenIndex);
		if (st.type.equals("')'")) {
			fc.argList = null;
			matchToken("')'");
		} else if (st.type.equals("Identifier")) {
			if (tknList.get(tokenIndex + 1).type.equals("')'")) {
				ASTIdentifier id = new ASTIdentifier();
				id.value = st.lexme;
				id.tokenId = tokenIndex;

				fc.argList.add(id);
				tokenIndex++;
				matchToken("')'");
			}
		} else if (st.type.equals("StringLiteral")) {
			if (tknList.get(tokenIndex + 1).type.equals("')'")) {
				ASTStringConstant sc = new ASTStringConstant();
				sc.value = st.lexme;
				sc.tokenId = tokenIndex;

				fc.argList.add(sc);
				tokenIndex++;
				matchToken("')'");
			}
		}

		return fc;

	}

	// EXPR --> TERM EXPR'
	public ASTExpression expr() {
		ASTExpression term = term();
		ASTBinaryExpression expr = expr2();

		if (expr != null) {
			expr.expr1 = term;
			return expr;
		} else {
			return term;
		}
	}

	// EXPR' --> '+' TERM EXPR' | '-' TERM EXPR' | e
	public ASTBinaryExpression expr2() {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("';'"))
			return null;

		if (nextToken.type.equals("'+'")) {
			ASTBinaryExpression be = new ASTBinaryExpression();

			ScannerToken st = tknList.get(tokenIndex);
			ASTToken tkn = new ASTToken();
			tkn.value = st.lexme;
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
		}
	}

	// TERM --> FACTOR TERM2
	public ASTExpression term() {
		ASTExpression f = factor();
		ASTBinaryExpression term = term2();

		if (term != null) {
			term.expr1 = f;
			return term;
		} else {
			return f;
		}
	}

	// TERM'--> '*' FACTOR TERM' | '/' FACTOR TERM' | e
	public ASTBinaryExpression term2() {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("'*'")) {
			ASTBinaryExpression be = new ASTBinaryExpression();

			ScannerToken st = tknList.get(tokenIndex);

			ASTToken tkn = new ASTToken();
			tkn.value = st.lexme;
			tkn.tokenId = tokenIndex;

			matchToken("'*'");

			be.op = tkn;
			be.expr2 = factor();

			ASTBinaryExpression term = term2();
			if (term != null) {
				term.expr1 = be;
				return term;
			}
			return be;
		} else {
			return null;
		}
	}

	// FACTOR --> '(' EXPR ')' | ID | CONST | FUNC_CALL
	public ASTExpression factor() {
		nextToken = tknList.get(tokenIndex);
		if (nextToken.type.equals("Identifier")) {
			ScannerToken st = tknList.get(tokenIndex);

			ASTIdentifier id = new ASTIdentifier();
			id.value = st.lexme;
			id.tokenId = tokenIndex;

			matchToken("Identifier");
			return id;
		} else if (nextToken.type.equals("IntegerConstant")) {
			ScannerToken st = tknList.get(tokenIndex);

			ASTIntegerConstant ic = new ASTIntegerConstant();
			ic.value = Integer.parseInt(st.lexme);
			ic.tokenId = tokenIndex;

			matchToken("IntegerConstant");
			return ic;
		} else {
			return null;
		}
	}

}
