package bit.minisys.minicc.parser.ast;

import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonTypeName;
@JsonTypeName("SelectionStatement")
public class ASTSelectionStatement extends ASTStatement{
	
	public LinkedList<ASTExpression> cond;
	public ASTStatement then;
	public ASTStatement otherwise;
	
	public ASTSelectionStatement() {
		super("SelectionStatement");
		this.cond = new LinkedList<ASTExpression> ();
		this.otherwise = null;
	}
	public ASTSelectionStatement(LinkedList<ASTExpression> cond,ASTStatement then,ASTStatement otherwise) {
		super("SelectionStatement");
		this.cond = cond;
		this.then = then;
		this.otherwise = otherwise;
	}
	@Override
	public void accept(ASTVisitor visitor) throws Exception {
		visitor.visit(this);
	}

}
