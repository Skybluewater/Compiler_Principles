package bit.minisys.minicc.icgen;

import java.util.*;

import bit.minisys.minicc.parser.ast.*;
// һ����������ֻʵ���˼ӷ�

enum Type{
	INT,CHAR,FLOAT,DOUBLE,VOID
}

class FuncArray{
	public String name;
	public int parNums;
	public Type type;
	public int parent;
	public VarArray[] arr = new VarArray[10];
	public boolean ifReturned=false;
	public FuncArray(){
		for(int i=0;i<arr.length;i++){
			arr[i] = new VarArray();
		}
	}
}

class VarArray{
	public String name;
	public Type type;
	public int parent;
}

public class ExampleICBuilder implements ASTVisitor{

	private Map<ASTNode, ASTNode> map;				// ʹ��map�洢�ӽڵ�ķ���ֵ��key��Ӧ�ӽڵ㣬value��Ӧ����ֵ��valueĿǰ������ASTIdentifier,ASTIntegerConstant,TemportaryValue...
	private List<Quat> quats;						// ���ɵ���Ԫʽ�б�
	private Integer tmpId;							// ��ʱ�������
	StringBuilder sb = new StringBuilder();
	private Stack<String> operands = new Stack<>();
	private Stack<Integer> operandType = new Stack<>();
	private Stack<String> selectStmt = new Stack<>();
	private Integer selectionId;
	private Integer localSelection = 0;
	private Stack<Integer> localSelectId = new Stack<>();
	private Stack<Integer> globalSelectId = new Stack<>();
	//int tmpID=0;
	private Object ASTIntegerConstant;
	private int globalIdentifier = 0;
	private int globalSelectionId = 0;
	private int localLoopId = 0;
	private int globalLoop = 0;
	private Stack<Integer> localLoop = new Stack<>();
	//copied from Syntax Analyzing
	public int funclevel=0;
	public int funCount=0;
	public int currentFun=0;
	public int currentFun2=0;//func match return
	public int previousIsBinary=0;
	public FuncArray[] funcArray = new FuncArray[10];
	public FuncArray[] funcDeclared = new FuncArray[10];
	public int funcDecl = 0;
	public int[] varCount = new int[10];
	public VarArray[][] varArray = new VarArray[10][10];
	public int ifFound=0;
	public int ifFoundGlobal=0;

	public ExampleICBuilder() {
		map = new HashMap<ASTNode, ASTNode>();
		quats = new LinkedList<Quat>();
		tmpId = 0;
		selectionId = 0;
	}

	public List<Quat> getQuats() {
		return quats;
	}

	public StringBuilder ICGenerator(ASTCompilationUnit program) throws Exception {
		for(int i=0;i<funcArray.length;i++){
			funcArray[i] = new FuncArray();
			funcDeclared[i] = new FuncArray();
		}
		for(int i=0;i<10;i++){
			for(int k=0;k<10;k++){
				varArray[i][k] = new VarArray();
			}
		}
		for(int i=0;i<10;i++){
			varCount[i]=0;
		}
		visit(program);
		return sb;
	}

	@Override
	public void visit(ASTCompilationUnit program) throws Exception {
		for (ASTNode node : program.items) {
			if(node instanceof ASTFunctionDefine) {
				visit((ASTFunctionDefine) node);
				currentFun++;
			}else if(node instanceof ASTDeclaration){
				globalIdentifier=1;
				visit((ASTDeclaration)node);
				globalIdentifier=0;
			}
		}
	}

	@Override
	public void visit(ASTDeclaration declaration) throws Exception {
		// TODO Auto-generated method stub
		if (globalIdentifier == 0) {
			int g=varCount[currentFun];
			for(int a=0;a<declaration.specifiers.size();a++){
				ASTToken b = declaration.specifiers.get(a);
				switch (b.value) {
					case "int":
						for(int i=0;i<declaration.initLists.size();i++){
							varArray[currentFun][g+i].type = Type.INT;
						}
						break;
					case "char":
						for(int i=0;i<declaration.initLists.size();i++){
							varArray[currentFun][g+i].type = Type.CHAR;
						}
						break;
					case "double":
						for(int i=0;i<declaration.initLists.size();i++){
							varArray[currentFun][g+i].type = Type.FLOAT;
						}
						break;
				}
			}
			StringBuilder temp = new StringBuilder();
			for (ASTToken tk : declaration.specifiers) {
				if (tk.value.equals("int")) {
					temp.append(" i32\n");
					break;
				}
			}
			int i=0;
			varCount[currentFun] += declaration.initLists.size();
			for (ASTInitList init : declaration.initLists) {
				if (init.declarator instanceof ASTVariableDeclarator) {
					sb.append("var " + "%").append(((ASTVariableDeclarator) init.declarator).identifier.value).append(temp);
					varArray[currentFun][g+i].name=((ASTVariableDeclarator) init.declarator).identifier.value;
					tmpId++;
				}else if(init.declarator instanceof ASTArrayDeclarator){
					int tempG=0;
					tempG=g+i;
					funclevel=tempG;
					visit((ASTArrayDeclarator) init.declarator);
					sb.append(">");
					if(declaration.specifiers.get(0).value.equals("int")){
						varArray[currentFun][g+i].type=Type.INT;
					}
				}
				for(ASTExpression exp : init.exprs){
					if(exp instanceof ASTBinaryExpression){
						operands.push(((ASTVariableDeclarator)init.declarator).identifier.value);
						operandType.push(0);
						visit((ASTBinaryExpression) exp);
						String k1=operands.pop();
						Integer k1Type = operandType.pop();
						String k0 = operands.pop();
						Integer k0Type = operandType.pop();
						if(k1Type == 1)
							sb.append("dassign %").append(k0).append("(regread i32 %").append(k1).append(")\n");
						else{
							sb.append("dassign %").append(k0).append("(dread i32 %").append(k1).append(")\n");
						}
					}else if(exp instanceof ASTIntegerConstant){
						tmpId++;
						sb.append("dassign %").append(tmpId.toString()).append("(constval i32 ").append(((ASTIntegerConstant) exp).value).append(")\n");
						sb.append("dassign %").append(((ASTVariableDeclarator) init.declarator).identifier.value).append("(regread i32 %").append(tmpId.toString()).append(")\n");
					}else if(exp instanceof ASTIdentifier){
						sb.append("dassign %").append(((ASTVariableDeclarator) init.declarator).identifier.value).append("(dread i32 %").append(((ASTIdentifier) exp).value).append(")\n");
					}
				}
				i++;
			}
		}else{
			StringBuilder temp = new StringBuilder();
			int i=varCount[9];
			varCount[9]+=declaration.initLists.size();
			for(ASTInitList init:declaration.initLists){
				funclevel=i;
				if(init.declarator instanceof ASTVariableDeclarator){
					sb.append("var $").append(((ASTVariableDeclarator) init.declarator).identifier.value);
					varArray[9][i].name=((ASTVariableDeclarator) init.declarator).identifier.value;
					if(declaration.specifiers.get(0).value.equals("int"))
						varArray[9][i].type=Type.INT;
					tmpId++;
				}else if(init.declarator instanceof ASTArrayDeclarator){
					visit((ASTArrayDeclarator) init.declarator);
					sb.append(">");
					if(declaration.specifiers.get(0).value.equals("int")){
						varArray[9][i].type=Type.INT;
					}
				}
				i++;
			}
			for(ASTToken specs:declaration.specifiers){
				if(specs.value.equals("int"))
					sb.append(" i32\n");
			}
		}
	}

	@Override
	public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {
		if(arrayDeclarator.declarator instanceof ASTArrayDeclarator){
			visit((ASTArrayDeclarator) arrayDeclarator.declarator);
		}else if(arrayDeclarator.declarator instanceof ASTVariableDeclarator && globalIdentifier== 1){
			sb.append("var $").append(((ASTVariableDeclarator) arrayDeclarator.declarator).identifier.value).append(" <");
			int g=funclevel;
			varArray[9][g].name=((ASTVariableDeclarator) arrayDeclarator.declarator).identifier.value;
			tmpId++;
		}else if(arrayDeclarator.declarator instanceof ASTVariableDeclarator && globalIdentifier == 0){
			sb.append("var %").append(((ASTVariableDeclarator) arrayDeclarator.declarator).identifier.value).append(" <");
			int g=funclevel;
			varArray[currentFun][g].name=((ASTVariableDeclarator) arrayDeclarator.declarator).identifier.value;
			tmpId++;
		}
		if(arrayDeclarator.expr instanceof ASTIntegerConstant){
			sb.append("[").append(((ASTIntegerConstant) arrayDeclarator.expr).value).append("]");
		}
	}

	@Override
	public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTFunctionDeclarator functionDeclarator) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTParamsDeclarator paramsDeclarator) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTArrayAccess arrayAccess) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTBinaryExpression binaryExpression) throws Exception {
		String op = binaryExpression.op.value;
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		if(op.equals("=")) {
			operands.push(((ASTIdentifier) binaryExpression.expr1).value);
			operandType.push(0);
			if (binaryExpression.expr2 instanceof ASTIntegerConstant) {
				tmpId++;
				operands.push(tmpId.toString());
				operandType.push(1);
				sb.append("dassign %").append(tmpId.toString()).append("(constval i32 ").append(((ASTIntegerConstant) binaryExpression.expr2).value).append(")\n");
			} else if (binaryExpression.expr2 instanceof ASTBinaryExpression) {
				visit(binaryExpression.expr2);
			} else if (binaryExpression.expr2 instanceof ASTIdentifier) {
				operands.push(((ASTIdentifier) binaryExpression.expr2).value);
				operandType.push(0);
			} else if (binaryExpression.expr2 instanceof ASTUnaryExpression) {
				visit((ASTUnaryExpression) binaryExpression.expr2);
			} else if (binaryExpression.expr2 instanceof ASTPostfixExpression) {
				tmpId++;
				operands.push(tmpId.toString());
				operandType.push(1);
				sb.append("dassign %").append(tmpId.toString()).append("(dread i32 %").append(((ASTIdentifier) ((ASTPostfixExpression) binaryExpression.expr2).expr).value).append(")\n");
				visit((ASTPostfixExpression) binaryExpression.expr2);
			} else if (binaryExpression.expr2 instanceof ASTFunctionCall) {
				previousIsBinary=1;
				visit((ASTFunctionCall) binaryExpression.expr2);
			}
			previousIsBinary=0;
			String k1 = operands.pop();
			String k0 = operands.pop();
			Integer k1Type = operandType.pop();
			Integer k0Type = operandType.pop();
			if (k1Type == 1) {
				sb.append("dassign %").append(k0).append("(regread i32 %").append(k1).append(")\n");
			}
			else if (k1Type == 0) {
				sb.append("dassign %").append(k0).append("(dread i32 %").append(k1).append(")\n");
			}
		}else {
			if (binaryExpression.expr1 instanceof ASTBinaryExpression) {
				visit(binaryExpression.expr1);
			} else if (binaryExpression.expr1 instanceof ASTIdentifier) {
				findId((ASTIdentifier) binaryExpression.expr1);
				operands.push(((ASTIdentifier) binaryExpression.expr1).value);
				operandType.push(0);
			} else if (binaryExpression.expr1 instanceof ASTIntegerConstant) {
				tmpId++;
				operands.push(tmpId.toString());
				operandType.push(1);
				sb.append("dassign %").append(tmpId.toString()).append("(constval i32 ").append(binaryExpression.expr1.toString()).append(")\n");
			}

			if (binaryExpression.expr2 instanceof ASTIntegerConstant) {
				tmpId++;
				operands.push(tmpId.toString());
				operandType.push(1);
				sb.append("dassign %").append(tmpId.toString()).append("(constval i32 ").append(((ASTIntegerConstant) binaryExpression.expr2).value.toString()).append(")\n");
			} else if (binaryExpression.expr2 instanceof ASTIdentifier) {
				operands.push(((ASTIdentifier) binaryExpression.expr2).value);
				operandType.push(0);
			} else if (binaryExpression.expr2 instanceof ASTBinaryExpression) {
				visit(binaryExpression.expr2);
			} else if (binaryExpression.expr2 instanceof ASTFunctionCall) {
				tmpId++;
				operands.push(tmpId.toString());
				operandType.push(1);
				previousIsBinary=1;
				visit((ASTFunctionCall) binaryExpression.expr2);
				previousIsBinary=0;
			}
			String k2 = operands.pop();
			Integer k2Type = operandType.pop();
			String k1 = operands.pop();
			Integer k1Type = operandType.pop();
			tmpId++;
			sb.append("dassign %").append(tmpId.toString()).append("(\n");
			operands.push(tmpId.toString());
			operandType.push(1);
			if (op.equals("+")) {
				// 以下是+判断操作;
				if (k1Type == 1 && k2Type == 1) {
					sb.append("	   add i32(regread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type == 1) {
					sb.append("    add i32(dread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 1 && k2Type == 0) {
					sb.append("    add i32(regread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				} else {
					sb.append("    add i32(dread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				}
			} else if (op.equals("%")){
				if (k1Type == 1 && k2Type == 1) {
					sb.append("	   rem i32(regread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type == 1) {
					sb.append("    rem i32(dread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 1 && k2Type == 0) {
					sb.append("    rem i32(regread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				} else {
					sb.append("    rem i32(dread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				}
			} else if (op.equals("<<")){
				if(k1Type == 1 && k2Type ==1){
					sb.append("	   shl i32(regread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				}else if(k1Type == 1 && k2Type == 0){
					sb.append("	   shl i32(regread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				}else if(k1Type == 0 && k2Type == 1){
					sb.append("	   shl i32(dread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				}else{
					sb.append("    shl i32(dread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				}
			} else if(op.equals("&&")) {
				if (k1Type == 1 && k2Type == 1) {
					sb.append("    land i32(regread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 1 && k2Type == 0) {
					sb.append("    land i32(regread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type == 1){
					sb.append("	   land i32(dread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type ==0) {
					sb.append("    land i32(dread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				}
			} else if (op.equals("+=")) {
				if (k1Type == 1 && k2Type == 1) {
					sb.append("    add i32(regread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 1 && k2Type == 0) {
					sb.append("    add i32(regread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type == 1){
					sb.append("    add i32(dread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type ==0) {
					sb.append("    add i32(dread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				}
				sb.append("dassign %").append(k1).append("(regread i32 %").append(tmpId.toString()).append(")\n");
				return;
			} else if (op.equals("<")){
				if (k1Type == 1 && k2Type == 1) {
					sb.append("    lt i32(regread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 1 && k2Type == 0) {
					sb.append("    lt i32(regread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type == 1){
					sb.append("	   lt i32(dread i32 %").append(k1).append(",regread i32 %").append(k2).append("))\n");
				} else if (k1Type == 0 && k2Type ==0) {
					sb.append("    lt i32(dread i32 %").append(k1).append(",dread i32 %").append(k2).append("))\n");
				}
			}
			tmpId++;
			String k = operands.pop();
			Integer kType = operandType.pop();
			sb.append("dassign %").append(tmpId.toString()).append("(regread i32 %").append(k).append(")\n");
			operands.push(tmpId.toString());
			operandType.push(1);
		}
	}

	@Override
	public void visit(ASTBreakStatement breakStat) throws Exception {
		// TODO Auto-generated method stub
		sb.append("goto <@").append(localLoopId).append("LoopEndLabel>\n");
	}

	@Override
	public void visit(ASTContinueStatement continueStatement) throws Exception {
		// TODO Auto-generated method stub
		sb.append("goto <@").append(localLoopId).append("LoopStepLabel>\n");
	}

	@Override
	public void visit(ASTCastExpression castExpression) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTCharConstant charConst) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTCompoundStatement compoundStat) throws Exception {
		for (ASTNode node : compoundStat.blockItems) {
			if(node instanceof ASTDeclaration) {
				visit((ASTDeclaration)node);
			}else if (node instanceof ASTStatement) {
				visit((ASTStatement)node);
			}
		}

	}

	@Override
	public void visit(ASTConditionExpression conditionExpression) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTExpression expression) throws Exception {
		if(expression instanceof ASTArrayAccess) {
			visit((ASTArrayAccess)expression);
		}else if(expression instanceof ASTBinaryExpression) {
			visit((ASTBinaryExpression)expression);
		}else if(expression instanceof ASTCastExpression) {
			visit((ASTCastExpression)expression);
		}else if(expression instanceof ASTCharConstant) {
			visit((ASTCharConstant)expression);
		}else if(expression instanceof ASTConditionExpression) {
			visit((ASTConditionExpression)expression);
		}else if(expression instanceof ASTFloatConstant) {
			visit((ASTFloatConstant)expression);
		}else if(expression instanceof ASTFunctionCall) {
			visit((ASTFunctionCall)expression);
		}else if(expression instanceof ASTIdentifier) {
			visit((ASTIdentifier)expression);
		}else if(expression instanceof ASTIntegerConstant) {
			visit((ASTIntegerConstant)expression);
		}else if(expression instanceof ASTMemberAccess) {
			visit((ASTMemberAccess)expression);
		}else if(expression instanceof ASTPostfixExpression) {
			visit((ASTPostfixExpression)expression);
		}else if(expression instanceof ASTStringConstant) {
			visit((ASTStringConstant)expression);
		}else if(expression instanceof ASTUnaryExpression) {
			visit((ASTUnaryExpression)expression);
		}else if(expression instanceof ASTUnaryTypename){
			visit((ASTUnaryTypename)expression);
		}
	}

	@Override
	public void visit(ASTExpressionStatement expressionStat) throws Exception {
		for (ASTExpression node : expressionStat.exprs) {
			if(node instanceof ASTFunctionCall){
				visit((ASTFunctionCall) node);
			}else
				visit((ASTExpression)node);
		}
	}

	@Override
	public void visit(ASTFloatConstant floatConst) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTFunctionCall funcCall) throws Exception {
		// TODO Auto-generated method stub
		for(ASTNode ast:funcCall.argList){
			if(ast instanceof ASTStringConstant){
				visit((ASTStringConstant) ast);
			}else if(ast instanceof ASTIntegerConstant){
				visit((ASTIntegerConstant) ast);
			}else if(ast instanceof ASTIdentifier){
				visit((ASTIdentifier) ast);
			}
		}
		if(previousIsBinary==1) {
			tmpId++;
			sb.append("dassign %").append(tmpId.toString()).append("(\n    ");
		}
		sb.append("call ").append(((ASTIdentifier)funcCall.funcname).value).append("(");
		ArrayList<String> str=new ArrayList<>();
		ArrayList<Integer> itr=new ArrayList<>();
		int i=0;
		while(i<funcCall.argList.size()){
			String k=operands.pop();
			Integer kType = operandType.pop();
			str.add(i,k);
			itr.add(i++,kType);
		}
		i--;
		while(i>=0){
			if(itr.get(i)==0){
				sb.append("dread i32 %").append(str.get(i));
			}else if(itr.get(i)==1){
				sb.append("regread i32 %").append(str.get(i));
			}else if(itr.get(i)==2){
				sb.append("regread a32 %").append(str.get(i));
			}else if(itr.get(i)==3){
				sb.append("dread i32 $").append(str.get(i));
			}
			i--;
			if(!(i<0)){
				sb.append(",");
			}
		}
		if(previousIsBinary==1) {
			operands.push(tmpId.toString());
			operandType.push(1);
			sb.append("))\n");
		}else{
			sb.append(")\n");
		}
	}

	@Override
	public void visit(ASTGotoStatement gotoStat) throws Exception {
		// TODO Auto-generated method stub
		sb.append("goto <@").append(gotoStat.label.value).append(">\n");
	}

	@Override
	public void visit(ASTIdentifier identifier) throws Exception {
		//map.put(identifier, identifier);
		findId((ASTIdentifier)identifier);
		if(ifFoundGlobal==1){
			sb.append("dassign $").append(identifier.value).append("(\n    iread i32(constval i32 800))\n");
			operands.push(identifier.value);
			operandType.push(3);
			return;
		}
		operands.push(identifier.value);
		operandType.push(0);
	}

	@Override
	public void visit(ASTInitList initList) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTIntegerConstant intConst) throws Exception {
		tmpId++;
		sb.append("dassign %").append(tmpId.toString()).append("(constval i32 ").append(intConst.value).append(")\n");
		operands.push(tmpId.toString());
		operandType.push(1);
		//map.put(intConst, intConst);
	}

	@Override
	public void visit(ASTIterationDeclaredStatement iterationDeclaredStat) throws Exception {
		// TODO Auto-generated method stub
		if(iterationDeclaredStat.init!=null){
			visit(iterationDeclaredStat.init);
		}
		sb.append("@").append(localLoopId).append("LoopCheckLabel:\n");
		if(iterationDeclaredStat.cond!=null){
			for(ASTExpression exp:iterationDeclaredStat.cond){
				visit(exp);
			}
			String k0=operands.pop();
			Integer k0T=operandType.pop();
			if(k0T==1)
				sb.append("brfalse <@").append(localLoopId).append("LoopEndLabel>(regread i32 %").append(k0).append(")\n");
			else
				sb.append("brfalse <@").append(localLoopId).append("LoopEndLabel>(dread i32 %").append(k0).append(")\n");
		}
		if(iterationDeclaredStat.stat!=null){
			visit(iterationDeclaredStat.stat);
		}
		sb.append("@").append(localLoopId).append("LoopStepLabel:\n");
		if(iterationDeclaredStat.step!=null){
			for(ASTExpression exr:iterationDeclaredStat.step){
				if(exr instanceof ASTPostfixExpression){
					tmpId++;
					operands.push(tmpId.toString());
					operandType.push(1);
					sb.append("dassign %").append(tmpId.toString()).append("(dread i32 %").append(((ASTIdentifier)((ASTPostfixExpression) exr).expr).value).append(")\n");
					visit(exr);
				}
			}
		}
		sb.append("goto <@").append(localLoopId).append("LoopCheckLabel>\n").append("@").append(localLoopId).append("LoopEndLabel:\n");
	}

	@Override
	public void visit(ASTIterationStatement iterationStat) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTLabeledStatement labeledStat) throws Exception {
		// TODO Auto-generated method stub
		sb.append("@").append(labeledStat.label.value).append(":\n");
		visit((ASTStatement)labeledStat.stat);
	}

	@Override
	public void visit(ASTMemberAccess memberAccess) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTPostfixExpression postfixExpression) throws Exception {
		// TODO Auto-generated method stub
		tmpId++;
		sb.append("dassign %").append(tmpId.toString()).append("(constval i32 1)\n");
		sb.append("dassign %").append(((ASTIdentifier)postfixExpression.expr).value).append("(\n");
		sb.append("    add i32(dread i32 %").append(((ASTIdentifier)postfixExpression.expr).value).append(",regread i32 %").append(tmpId.toString()).append("))\n");
	}

	@Override
	public void visit(ASTReturnStatement returnStat) throws Exception {
		// TODO Auto-generated method stub
		if(returnStat.expr==null){
			sb.append("return ()}\n");
		} else {
			for (ASTExpression expr : returnStat.expr) {
				if (expr instanceof ASTIntegerConstant) {
					tmpId++;
					sb.append("dassign %").append(tmpId.toString()).append("(constval i32 ").append(((ASTIntegerConstant) expr).value).append(")\n");
					sb.append("return (regread i32 %").append(tmpId.toString()).append(")}\n");
				} else if (expr instanceof ASTIdentifier) {
					sb.append("return (dread i32 %").append(((ASTIdentifier) expr).value).append(")}\n");
				}
			}
		}
	}

	@Override
	public void visit(ASTSelectionStatement selectionStat) throws Exception {
		// TODO Auto-generated method stub
		if(localSelection!=0){
			sb.append(selectStmt.pop()).append("\n");
		}
		for(ASTExpression cond:selectionStat.cond){
			visit(cond);
		}
		localSelection++;
		String k=operands.pop();
		operandType.pop();
		selectStmt.push("@"+selectionId.toString()+"otherwise"+localSelection.toString()+":");
		sb.append("brfalse <@").append(selectionId.toString()).append("otherwise").append(localSelection.toString()).append(">(regread i32 %").append(k).append(")\n");
		visit(selectionStat.then);
		sb.append("goto <@").append(selectionId.toString()).append("endif>\n");
		if(selectionStat.otherwise!=null){
			if(selectionStat.otherwise instanceof ASTSelectionStatement) {
				visit((ASTSelectionStatement) selectionStat.otherwise);
			}else{
				sb.append(selectStmt.pop()).append("\n");
				visit((ASTStatement) selectionStat.otherwise);
				sb.append(selectStmt.pop()).append("\n");
			}
		}
	}

	@Override
	public void visit(ASTStringConstant stringConst) throws Exception {
		// TODO Auto-generated method stub
		tmpId++;
		sb.append("dassign %").append(tmpId.toString()).append("(addrof a32 _1sc)\n");
		operands.push(tmpId.toString());
		operandType.push(2);
	}

	@Override
	public void visit(ASTTypename typename) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTUnaryExpression unaryExpression) throws Exception {
		// TODO Auto-generated method stub
		if(unaryExpression.op.value.equals("!")){
			tmpId++;
			sb.append("dassign %").append(tmpId.toString()).append("(\n    lnot i32(");
			if(unaryExpression.expr instanceof ASTIdentifier){
				sb.append("dread i32 %").append(((ASTIdentifier) unaryExpression.expr).value).append("))\n");
			}
			operands.push(tmpId.toString());
			operandType.push(1);
		}else if(unaryExpression.op.value.equals("~")){
			tmpId++;
			sb.append("dassign %").append(tmpId.toString()).append("(\n    neg i32(");
			if(unaryExpression.expr instanceof ASTIdentifier){
				sb.append("dread i32 %").append(((ASTIdentifier)unaryExpression.expr).value).append("))\n");
			}
			operands.push(tmpId.toString());
			operandType.push(1);
		}else if(unaryExpression.op.value.equals("++")) {
			tmpId++;
			sb.append("dassign %").append(tmpId.toString()).append("(").append("constval i32 1)\n");
			sb.append("dassign %").append(((ASTIdentifier)unaryExpression.expr).value).append("(\n    ");
			sb.append("add i32(dread i32 %").append(((ASTIdentifier)unaryExpression.expr).value).append(",regread i32 %").append(tmpId.toString()).append("))\n");
			tmpId++;
			operands.push(tmpId.toString());
			operandType.push(1);
			sb.append("dassign %").append(tmpId.toString()).append("(").append("dread i32 %").append(((ASTIdentifier)unaryExpression.expr).value).append(")\n");
		}
	}

	@Override
	public void visit(ASTUnaryTypename unaryTypename) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTFunctionDefine functionDefine) throws Exception {
		StringBuilder temp= new StringBuilder();
		temp.append("func &");
		funcArray[funCount].parent=10;
		ASTDeclarator decl = functionDefine.declarator;
		if(decl instanceof ASTFunctionDeclarator){
			ASTVariableDeclarator decl2 = (ASTVariableDeclarator) ((ASTFunctionDeclarator) decl).declarator;
			temp.append(decl2.identifier.value);
			funcArray[funCount].name = decl2.identifier.value;
			funcArray[funCount].parNums = ((ASTFunctionDeclarator) decl).params.size();
			temp.append("(");
			int i=0;
			for(ASTParamsDeclarator param:((ASTFunctionDeclarator) decl).params){
				ASTVariableDeclarator var = (ASTVariableDeclarator)param.declarator;
				temp.append("var %");
				temp.append(var.identifier.value);
				ASTToken paramsToken = param.specfiers.get(0);
				if(paramsToken.value.equals("int")) {
					temp.append(" i32");
					funcArray[funCount].arr[i].type=Type.INT;
					funcArray[funCount].arr[i].name=var.identifier.value;
				}
				if(++i<((ASTFunctionDeclarator) decl).params.size()){
					temp.append(", ");
				}
				tmpId++;
			}
		}
		for(ASTToken a:functionDefine.specifiers){
			if(a.value.equals("int")){
				temp.append(") i32{\n");
				funcArray[funCount].type=Type.INT;
			}else if(a.value.equals("void")){
				temp.append(") void{\n");
			}
		}
		funCount++;
		sb.append(temp);
		visit(functionDefine.body);
		sb.append("\n");
	}

	@Override
	public void visit(ASTDeclarator declarator) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ASTStatement statement) throws Exception {
		if(statement instanceof ASTIterationDeclaredStatement) {
			globalLoop++;
			localLoopId = globalLoop;
			localLoop.push(localLoopId);
			visit((ASTIterationDeclaredStatement)statement);
			localLoopId = localLoop.pop();
		}else if(statement instanceof ASTIterationStatement) {
			visit((ASTIterationStatement)statement);
		}else if(statement instanceof ASTCompoundStatement) {
			visit((ASTCompoundStatement)statement);
		}else if(statement instanceof ASTSelectionStatement) {
			globalSelectionId++;
			globalSelectId.push(selectionId);
			selectionId = globalSelectionId;
			localSelectId.push(localSelection);
			localSelection = 0;
			selectStmt.push("@"+selectionId.toString()+"endif:");
			visit((ASTSelectionStatement)statement);
			localSelection = localSelectId.pop();
			selectionId = globalSelectId.pop();
		}else if(statement instanceof ASTExpressionStatement) {
			visit((ASTExpressionStatement)statement);
		}else if(statement instanceof ASTBreakStatement) {
			visit((ASTBreakStatement)statement);
		}else if(statement instanceof ASTContinueStatement) {
			visit((ASTContinueStatement)statement);
		}else if(statement instanceof ASTReturnStatement) {
			visit((ASTReturnStatement)statement);
		}else if(statement instanceof ASTGotoStatement) {
			visit((ASTGotoStatement)statement);
		}else if(statement instanceof ASTLabeledStatement) {
			visit((ASTLabeledStatement)statement);
		}
	}

	@Override
	public void visit(ASTToken token) throws Exception {
		// TODO Auto-generated method stub

	}

	public void findId(ASTIdentifier identifier){
		int flag=0;
		ifFound=0;
		ifFoundGlobal=0;
		for(int k=0;k<funcArray[currentFun].parNums;k++){
			if(funcArray[currentFun].arr[k].name.equals(identifier.value)){
				flag=1;
				break;
			}
		}
		if(flag==0){
			for(int k=0;k<varCount[currentFun];k++){
				if(varArray[currentFun][k].name.equals(identifier.value)){
					flag=1;
					break;
				}
			}
		}
		if(flag==0){
			int tempCurrent = currentFun;
			while(funcArray[currentFun].parent!=10){
				currentFun=funcArray[currentFun].parent;
				for(int k=0;k<funcArray[currentFun].parNums;k++){
					if(funcArray[currentFun].arr[k].name.equals(identifier.value)){
						flag=1;
						break;
					}
				}
				for(int k=0;k<varCount[currentFun];k++){
					if(varArray[currentFun][k].name.equals(identifier.value)){
						flag=1;
						break;
					}
				}
			}
			for(int k=0;k<varCount[9];k++){
				if(varArray[9][k].name.equals(identifier.value)){
					flag=1;
					ifFoundGlobal = 1;
					break;
				}
			}
			currentFun = tempCurrent;
		}
		if(flag==0){
			System.out.printf("ES01 %s variable not defined\n", identifier.value);
		}else{
			ifFound=1;
		}
	}

}
