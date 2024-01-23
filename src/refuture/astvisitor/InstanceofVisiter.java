package refuture.astvisitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.InstanceofExpression;
@Deprecated
public class InstanceofVisiter extends ASTVisitor {

	List<InstanceofExpression> expNodes;
	
	public InstanceofVisiter() {
		expNodes=new ArrayList<InstanceofExpression>();
	}
	
	@Override
	public boolean visit(InstanceofExpression node) {
		expNodes.add(node);
		return true;
	}
	
	public List<InstanceofExpression> getResult(){
		return expNodes;
	}
}
