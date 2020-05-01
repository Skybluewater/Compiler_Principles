package bit.minisys.minicc.semantic;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Spliterators;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import bit.minisys.minicc.parser.ast.ASTVisitor;

enum Type{
    INT,CHAR,FLOAT,DOUBLE,
    VOID
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

public class ExampleSemantic implements IMiniCCSemantic,ASTVisitor{

    public int level=0;
    public int funCount=0;
    public int currentFun=0;
    public int currentFun2=0;//func match return
    public int parCount=0;
    public Type binaryType;
    public int binaryFunc;
    public int binaryVar;
    public int ifFound;
    public boolean ifFoundParams;
    public boolean ifIterationStmts;
    public Type expectedType;
    public FuncArray[] funcArray = new FuncArray[10];
    public FuncArray[] funcDeclared = new FuncArray[10];
    public int funcDecl = 0;
    public int[] varCount = new int[10];
    public VarArray[][] varArray = new VarArray[10][10];

    @Override
    public String run(String ifile) throws Exception{
        ifIterationStmts=false;
        ObjectMapper mapper = new ObjectMapper();
        ASTCompilationUnit prog= mapper.readValue(new File(ifile), ASTCompilationUnit.class);
        System.out.println("4. Running Semantic");
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
        //for(ASTNode item:prog.items){
        /*    if (item instanceof ASTDeclaration) {
                level = 0;
                this.visit((ASTDeclaration)item);
            } else if (item instanceof ASTFunctionDefine) {
                level = 0;
                this.visit((ASTFunctionDefine)item);
            }*/
        //}
        for(ASTNode item:prog.items){
            if (item instanceof ASTDeclaration) {
                level = 0;
                this.visit((ASTDeclaration) item);
            } else if (item instanceof ASTFunctionDefine) {
                level = 0;
                this.visit((ASTFunctionDefine) item);
                this.visit2((ASTFunctionDefine)item);
                if(!funcArray[currentFun].ifReturned&&funcArray[currentFun].type!=Type.VOID){
                    System.out.printf("ES08 %s function lack of return stmts\n",funcArray[currentFun].name);
                }
                currentFun++;
            } else {
                System.out.println("ES09 NOT AN ORDINARY COMPILE UNIT");
            }
        }
        if(funcDecl!=0){
            System.out.print("ES10 Function not defined\n");
        }
        return null;
    }

    public void visit2(ASTFunctionDefine functionDefine) throws Exception {
        this.visit((ASTCompoundStatement) functionDefine.body);
    }

    @Override
    public void visit(ASTCompilationUnit program) throws Exception {

    }

    @Override
    public void visit(ASTDeclaration declaration) throws Exception {
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
        for(ASTInitList items:declaration.initLists){
            if(items.declarator instanceof ASTFunctionDeclarator){
                this.visit((ASTFunctionDeclarator)items.declarator);
            } else {
                ASTIdentifier id = new ASTIdentifier();
                ASTVariableDeclarator decl3 = new ASTVariableDeclarator();
                decl3 = (ASTVariableDeclarator) items.declarator;
                id = decl3.identifier;
                String name = id.value;
                for (int k = 0; k < funcArray[currentFun].parNums; k++) {
                    if (funcArray[currentFun].arr[k].name.equals(name)) {
                        System.out.printf("ES02 %s variable define duplicated\n", name);
                    }
                }
                for (int k = 0; k < varCount[currentFun]; k++) {
                    if (varArray[currentFun][k].name.equals(name)) {
                        System.out.printf("ES02 %s variable define duplicated\n", name);
                    }
                }
            /*int tempCurrent = currentFun;
            while(funcArray[currentFun].parent!=10){
                currentFun=funcArray[currentFun].parent;
                for(int k=0;k<funcArray[currentFun].parNums;k++){
                    if(funcArray[currentFun].arr[k].name.equals(name)){
                        System.out.printf("ES02 %s variable define duplicated\n",name);
                    }
                }
                for(int k=0;k<varCount[currentFun];k++){
                    if(varArray[currentFun][k].name.equals(name)){
                        System.out.printf("ES02 %s variable define duplicated\n",name);

                    }
                }
            }
            for(int k=0;k<varCount[9];k++){
                if(varArray[9][k].name.equals(name)){
                    System.out.printf("ES02 %s variable define duplicated\n",name);
                }
            }
            currentFun = tempCurrent;*/
                varArray[currentFun][varCount[currentFun]].name = name;
                for (ASTExpression item : items.exprs) {
                    if (item instanceof ASTFunctionCall) {
                        expectedType = varArray[currentFun][varCount[currentFun]].type;
                        this.visit((ASTFunctionCall) item);
                    } else if (item instanceof ASTBinaryExpression) {
                        binaryType = varArray[currentFun][varCount[currentFun]].type;
                        this.visit((ASTBinaryExpression) item);
                    } else if (item instanceof ASTIntegerConstant) {
                        if (varArray[currentFun][varCount[currentFun]].type != Type.INT) {
                            System.out.printf("ES05 %s var not match\n", varArray[currentFun][varCount[currentFun]].name);
                        }
                    } else if (item instanceof ASTCharConstant) {
                        if (varArray[currentFun][varCount[currentFun]].type != Type.CHAR) {
                            System.out.printf("ES05 %s var not match\n", varArray[currentFun][varCount[currentFun]].name);
                        }
                    } else if (item instanceof ASTFloatConstant) {
                        if (varArray[currentFun][varCount[currentFun]].type != Type.FLOAT) {
                            System.out.printf("ES05 %s var not match\n", varArray[currentFun][varCount[currentFun]].name);
                        }
                    }
                }
                varCount[currentFun]++;
            }
        }
    }

    @Override
    public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {

    }

    @Override
    public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {

    }

    @Override
    public void visit(ASTFunctionDeclarator functionDeclarator) throws Exception {
        ASTFunctionDeclarator decl = new ASTFunctionDeclarator();
        decl = functionDeclarator;
        ASTVariableDeclarator decl2 = new ASTVariableDeclarator();
        decl2= (ASTVariableDeclarator) decl.declarator;
        ASTIdentifier id = new ASTIdentifier();
        id=decl2.identifier;
        funcDeclared[funcDecl].type=varArray[currentFun][0].type;
        funcDeclared[funcDecl].name=id.value;
        funcDeclared[funcDecl].parNums=decl.params.size();
        for(int i=0;i<decl.params.size();i++){
            ASTParamsDeclarator parms = new ASTParamsDeclarator();
            parms = decl.params.get(i);
            for(int a=0;a<parms.specfiers.size();a++){
                ASTToken b = parms.specfiers.get(a);
                if(b.value.equals("int")){
                    funcDeclared[funcDecl].arr[i].type=Type.INT;
                } else if(b.value.equals("char")){
                    funcDeclared[funcDecl].arr[i].type=Type.CHAR;
                }
            }
            ASTVariableDeclarator decl3 = new ASTVariableDeclarator();
            decl3 = (ASTVariableDeclarator)parms.declarator;
            id = decl3.identifier;
            funcDeclared[funcDecl].arr[i].name=id.value;
        }
        funcDecl++;
    }

    @Override
    public void visit(ASTParamsDeclarator paramsDeclarator) throws Exception {

    }

    @Override
    public void visit(ASTArrayAccess arrayAccess) throws Exception {

    }

    @Override
    public void visit(ASTBinaryExpression binaryExpression) throws Exception {
        ASTExpression exp1=binaryExpression.expr1;
        ASTExpression exp2=binaryExpression.expr2;
        VarArray id1 = new VarArray();
        VarArray id2 = new VarArray();
        ifFound=0;
        ifFoundParams=false;
        if(exp1 instanceof ASTIdentifier){
            this.visit((ASTIdentifier) exp1);
            if (ifFound==1&&!ifFoundParams) {
                id1.type = varArray[binaryFunc][binaryVar].type;
                id1.name = varArray[binaryFunc][binaryVar].name;
            } else if(ifFound==1){
                id1.type=funcArray[binaryFunc].arr[binaryVar].type;
                id1.name=funcArray[binaryFunc].arr[binaryVar].name;
            } else{
                return;
            }
        }
        ifFound=0;
        ifFoundParams=false;
        if(exp2 instanceof ASTIdentifier){
            this.visit((ASTIdentifier) exp2);
            if (ifFound==1&&!ifFoundParams) {
                id2.type = varArray[binaryFunc][binaryVar].type;
                id2.name = varArray[binaryFunc][binaryVar].name;
            }else if(ifFound==1) {
                id2.type=funcArray[binaryFunc].arr[binaryVar].type;
                id2.name=funcArray[binaryFunc].arr[binaryVar].name;
            } else {
                return;
            }
        } else if(exp2 instanceof ASTFunctionCall){
            expectedType=id1.type;
            this.visit((ASTFunctionCall) exp2);
        }
        if(binaryExpression.op.value.equals("+")){
            if(exp1 instanceof ASTIdentifier&&exp2 instanceof ASTIdentifier){
                if(id1.type==id2.type){
                    if(binaryType!=id1.type){
                        System.out.print("ES05 opnd not match\n");
                    }
                }else {
                    System.out.print("ES05 opnd not match\n");
                }
            }else if(exp1 instanceof ASTIdentifier&&exp2 instanceof ASTIntegerConstant){
                if(id1.type==Type.INT){
                    if(binaryType!=id1.type){
                        System.out.print("ES05 opnd not match\n");
                    }
                }else{
                    System.out.println("ES05 operand not match");
                }
            }else if(exp1 instanceof ASTIntegerConstant&&exp2 instanceof ASTIntegerConstant){
                if(binaryType!=Type.INT){
                    System.out.print("ES05 opnd not match\n");
                }
            }else if(exp1 instanceof ASTIntegerConstant&&exp2 instanceof ASTIdentifier){
                if(id2.type==Type.INT){
                    if(binaryType!=id2.type){
                        System.out.print("ES05 opnd not match\n");
                    }
                }else{
                    System.out.println("ES05 operand not match");
                }
            }
        }else if(binaryExpression.op.value.equals("<<")){
            if(exp1 instanceof ASTIdentifier&&exp2 instanceof ASTIdentifier){
                if(id2.type==Type.INT&&(id1.type==Type.FLOAT||id1.type==Type.INT)){
                    if(binaryType!=id1.type){
                        System.out.print("ES05 opnd not match\n");
                    }
                }else {
                    System.out.print("ES05 opnd not match\n");
                }
            }else if(exp1 instanceof ASTIdentifier&&exp2 instanceof ASTIntegerConstant){
                if(id1.type==Type.INT||id1.type==Type.FLOAT){
                    if(binaryType!=id1.type){
                        System.out.print("ES05 opnd not match\n");
                    }
                }else{
                    System.out.println("ES05 operand not match");
                }
            }else if(exp1 instanceof ASTIntegerConstant&&exp2 instanceof ASTIntegerConstant){
                if(binaryType!=Type.INT){
                    System.out.print("ES05 opnd not match\n");
                }
            }else if(exp1 instanceof ASTIntegerConstant&&exp2 instanceof ASTIdentifier){
                if(id2.type==Type.INT){
                    if(binaryType!=Type.INT){
                        System.out.print("ES05 opnd not match\n");
                    }
                }else{
                    System.out.println("ES05 operand not match");
                }
            }
        }else if(binaryExpression.op.value.equals("=")){
            if(exp1 instanceof ASTIdentifier&&exp2 instanceof ASTIdentifier){
                if(id2.type!=id1.type){
                    System.out.print("ES05 opnd not match\n");
                }
            }else if(exp1 instanceof ASTIdentifier&&exp2 instanceof ASTIntegerConstant){
                if(id1.type!=Type.INT&&id1.type!=Type.FLOAT){
                    System.out.print("ES05 opnd not match\n");
                }
            }else if(exp1 instanceof ASTIdentifier && exp2 instanceof ASTFunctionCall){
                int parNums=((ASTFunctionCall) exp2).argList.size();
                ASTIdentifier id = (ASTIdentifier)((ASTFunctionCall) exp2).funcname;
                int flag=0;
                currentFun2=0;
                for(int i=0;i<funCount;i++){
                    if(funcArray[i].name.equals(id.value)&&funcArray[i].parNums==parNums){
                        flag=1;
                        currentFun2=i;
                        break;
                    }
                }
                if(flag==1){
                    if(funcArray[currentFun2].type!=Type.INT){
                        System.out.print("ES05 opnd not match\n");
                    }
                }
            }
        }
    }

    @Override
    public void visit(ASTBreakStatement breakStat) throws Exception {

    }

    @Override
    public void visit(ASTContinueStatement continueStatement) throws Exception {

    }

    @Override
    public void visit(ASTCastExpression castExpression) throws Exception {

    }

    @Override
    public void visit(ASTCharConstant charConst) throws Exception {
        if(funcArray[currentFun2].arr[parCount].type==Type.CHAR){
            return;
        }else{
            System.out.printf("E4 %s function argument not match because the needed parms type is %s\n",charConst.value,funcArray[currentFun2].arr[parCount].type.name());
        }
    }

    @Override
    public void visit(ASTCompoundStatement compoundStat) throws Exception {
        for(ASTNode items:compoundStat.blockItems) {
            if(items instanceof ASTDeclaration){
                this.visit((ASTDeclaration) items);
            } else if (items instanceof ASTReturnStatement) {
                this.visit((ASTReturnStatement) items);
            } else if (items instanceof ASTExpressionStatement) {
                this.visit((ASTExpressionStatement) items);
            } else if(items instanceof ASTCompoundStatement) {
                funcArray[funCount].parent=funCount-1;
                funCount++;
                currentFun++;
                this.visit((ASTCompoundStatement) items);
                currentFun--;
            } else if(items instanceof ASTIterationStatement) {
                ifIterationStmts = true;
                this.visit((ASTIterationStatement) items);
                ifIterationStmts = false;
            } else if(items instanceof ASTBreakStatement) {
                if(!ifIterationStmts){
                    System.out.println("ES03 break stat not in loop");
                }
            }
        }
    }

    @Override
    public void visit(ASTConditionExpression conditionExpression) throws Exception {

    }

    @Override
    public void visit(ASTExpression expression) throws Exception {

    }

    @Override
    public void visit(ASTExpressionStatement expressionStat) throws Exception {
        for(ASTExpression exp:expressionStat.exprs){
            if(exp instanceof ASTBinaryExpression){
                this.visit((ASTBinaryExpression) exp);
            }
        }
    }

    @Override
    public void visit(ASTFloatConstant floatConst) throws Exception {
        if(funcArray[currentFun2].arr[parCount].type==Type.DOUBLE||funcArray[currentFun2].arr[parCount].type==Type.FLOAT){
            return;
        }else{
            System.out.printf("ES04 function arguments not match because the needed type is %s but the given is %s\n",funcArray[currentFun2].arr[parCount].type.name(),floatConst.value);
        }
    }

    @Override
    public void visit(ASTFunctionCall funcCall) throws Exception {
        ASTIdentifier id=(ASTIdentifier)funcCall.funcname;
        int parNums = funcCall.argList.size();
        int flag=0;
        currentFun2=0;
        if(funcDecl>0) {
            for (int i = 0; i < funcDecl; i++) {
                if (funcDeclared[i].name.equals(id.value) && funcArray[i].parNums == parNums) {
                    flag = 1;
                    currentFun2 = i;
                    break;
                }
            }
            if (flag == 1) {
                parCount=0;
                if(expectedType!=funcDeclared[currentFun2].type){
                    System.out.printf("ES05 %s function return not matched\n",funcDeclared[currentFun2].name);
                }
                for(ASTExpression item:funcCall.argList){
                    if(item instanceof ASTIntegerConstant) {
                        if(funcDeclared[currentFun2].arr[parCount].type!=Type.INT){
                            System.out.printf("ES04 function arguments not match because the needed type is %s but the given is %s\n",funcArray[currentFun2].arr[parCount].type.name(),((ASTIntegerConstant) item).value);
                        }
                    } else if(item instanceof ASTFloatConstant) {
                        if(funcDeclared[currentFun2].arr[parCount].type!=Type.FLOAT){
                            System.out.printf("ES04 function arguments not match because the needed type is %s but the given is %s\n",funcArray[currentFun2].arr[parCount].type.name(),((ASTFloatConstant) item).value);
                        }
                    } else if(item instanceof ASTCharConstant) {
                        if (funcDeclared[currentFun2].arr[parCount].type!=Type.CHAR){
                            System.out.printf("ES04 function arguments not match because the needed type is %s but the given is %s\n",funcArray[currentFun2].arr[parCount].type.name(),((ASTCharConstant) item).value);
                        }
                    }
                    parCount++;
                }
            }
            return;
        }
        for(int i=0;i<funCount;i++){
            if(funcArray[i].parent==10&&funcArray[i].name.equals(id.value)&&funcArray[i].parNums==parNums){
                flag=1;
                currentFun2=i;
                break;
            }
        }
        if(flag==0){
            System.out.printf("ES01 %s function not defined\n",id.value);
            return;
        }
        parCount=0;
        if(expectedType!=funcArray[currentFun2].type){
            System.out.printf("ES05 %s function return not matched\n",funcArray[currentFun2].name);
        }
        for(ASTExpression item:funcCall.argList){
            if(item instanceof ASTIntegerConstant) {
                this.visit((ASTIntegerConstant) item);
            } else if(item instanceof ASTFloatConstant) {
                this.visit((ASTFloatConstant) item);
            } else if(item instanceof ASTCharConstant) {
                this.visit((ASTCharConstant) item);
            }
            parCount++;
        }
    }

    @Override
    public void visit(ASTGotoStatement gotoStat) throws Exception {

    }

    @Override
    public void visit(ASTIdentifier identifier) throws Exception {
        int flag=0;
        for(int k=0;k<funcArray[currentFun].parNums;k++){
            if(funcArray[currentFun].arr[k].name.equals(identifier.value)){
                flag=1;
                binaryFunc=currentFun;
                binaryVar=k;
                ifFoundParams=true;
                break;
            }
        }
        if(flag==0){
            for(int k=0;k<varCount[currentFun];k++){
                if(varArray[currentFun][k].name.equals(identifier.value)){
                    binaryFunc=currentFun;
                    binaryVar=k;
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
                        binaryFunc=currentFun;
                        binaryVar=k;
                        ifFoundParams=true;
                        flag=1;
                        break;
                    }
                }
                for(int k=0;k<varCount[currentFun];k++){
                    if(varArray[currentFun][k].name.equals(identifier.value)){
                        binaryFunc=currentFun;
                        binaryVar=k;
                        flag=1;
                        break;
                    }
                }
            }
            for(int k=0;k<varCount[9];k++){
                if(varArray[9][k].name.equals(identifier.value)){
                    binaryFunc=9;
                    binaryVar=k;
                    flag=1;
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

    @Override
    public void visit(ASTInitList initList) throws Exception {

    }

    @Override
    public void visit(ASTIntegerConstant intConst) throws Exception {
        if(funcArray[currentFun2].arr[parCount].type==Type.INT){
            return;
        }else{
            System.out.printf("ES04 function arguments not match because the needed type is %s but the given is %s\n",funcArray[currentFun2].arr[parCount].type.name(),intConst.value);
        }
    }

    @Override
    public void visit(ASTIterationDeclaredStatement iterationDeclaredStat) throws Exception {

    }

    @Override
    public void visit(ASTIterationStatement iterationStat) throws Exception {
        this.visit(iterationStat.stat);
    }

    @Override
    public void visit(ASTLabeledStatement labeledStat) throws Exception {

    }

    @Override
    public void visit(ASTMemberAccess memberAccess) throws Exception {

    }

    @Override
    public void visit(ASTPostfixExpression postfixExpression) throws Exception {

    }

    @Override
    public void visit(ASTReturnStatement returnStat) throws Exception {
        for(ASTExpression exp:returnStat.expr){
            if(exp instanceof ASTIntegerConstant){
                if(funcArray[currentFun].type==Type.INT){
                    funcArray[currentFun].ifReturned=true;
                    return;
                } else {
                    System.out.printf("ES10 %s func return not matched\n",((ASTIntegerConstant) exp).value);
                }
            } else if(exp instanceof ASTBinaryExpression){
                binaryType = funcArray[currentFun].type;
                visit((ASTBinaryExpression) exp);
            }
            funcArray[currentFun].ifReturned=true;
        }
    }

    @Override
    public void visit(ASTSelectionStatement selectionStat) throws Exception {

    }

    @Override
    public void visit(ASTStringConstant stringConst) throws Exception {

    }

    @Override
    public void visit(ASTTypename typename) throws Exception {

    }

    @Override
    public void visit(ASTUnaryExpression unaryExpression) throws Exception {

    }

    @Override
    public void visit(ASTUnaryTypename unaryTypename) throws Exception {

    }

    @Override
    public void visit(ASTFunctionDefine functionDefine) throws Exception {
        for(int a=0;a<functionDefine.specifiers.size();a++){
            ASTToken b = functionDefine.specifiers.get(a);
            if(b.value.equals("int")){
                funcArray[funCount].type=Type.INT;
            } else if(b.value.equals("char")){
                funcArray[funCount].type=Type.CHAR;
            } else if(b.value.equals("double")){
                funcArray[funCount].type=Type.FLOAT;
            }
        }
        funcArray[funCount].parent=10;
        ASTFunctionDeclarator decl = new ASTFunctionDeclarator();
        decl = (ASTFunctionDeclarator)functionDefine.declarator;
        ASTVariableDeclarator decl2 = new ASTVariableDeclarator();
        decl2= (ASTVariableDeclarator) decl.declarator;
        ASTIdentifier id = new ASTIdentifier();
        id = decl2.identifier;
        funcArray[funCount].name = id.value;
        funcArray[funCount].parNums = decl.params.size();
        for(int i=0;i<funCount;i++){
            if(funcArray[i].parent==10&&funcArray[i].name.equals(id.value)&&funcArray[i].parNums==funcArray[funCount].parNums){
                System.out.printf("ES02 %s function define duplicated\n",id.value);
            }
        }
        if(funcDecl>0) {
            for (int i = 0; i < funcDecl; i++) {
                if (funcDeclared[i].name.equals(id.value) && funcDeclared[i].parNums == funcArray[funCount].parNums) {
                    funcDecl--;
                }
            }
        }
        for(int i=0;i<decl.params.size();i++){
            ASTParamsDeclarator parms = new ASTParamsDeclarator();
            parms = decl.params.get(i);
            for(int a=0;a<parms.specfiers.size();a++){
                ASTToken b = parms.specfiers.get(a);
                //functionDefine.specifiers.get(a);
                if(b.value.equals("int")){
                    funcArray[funCount].arr[i].type=Type.INT;
                } else if(b.value.equals("char")){
                    funcArray[funCount].arr[i].type=Type.CHAR;
                } else if(b.value.equals("double")){
                    funcArray[funCount].arr[i].type=Type.FLOAT;
                }
            }
            ASTVariableDeclarator decl3 = new ASTVariableDeclarator();
            decl3 = (ASTVariableDeclarator)parms.declarator;
            id = decl3.identifier;
            funcArray[funCount].arr[i].name=id.value;
        }
        funCount++;
    }

    @Override
    public void visit(ASTDeclarator declarator) throws Exception {

    }

    @Override
    public void visit(ASTStatement statement) throws Exception {
        if(statement instanceof ASTCompoundStatement){
            this.visit((ASTCompoundStatement) statement);
        }
    }

    @Override
    public void visit(ASTToken token) throws Exception {

    }
}
