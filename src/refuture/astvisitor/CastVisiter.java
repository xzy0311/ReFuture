package refuture.astvisitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
@Deprecated
public class CastVisiter extends ASTVisitor {

	List<CastExpression> expNodes;
	
	public CastVisiter() {
		expNodes=new ArrayList<CastExpression>();
	}
	
	@Override
	public boolean visit(CastExpression node) {
		expNodes.add(node);
		return true;
	}
	
	public List<CastExpression> getResult(){
		return expNodes;
	}
}