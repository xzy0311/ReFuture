package refuture.astvisitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;


// TODO: Auto-generated Javadoc
/**
 * 得到ASTNode中所有的方法调用。
 */
@Deprecated
public class MethodInvocationVisiter extends ASTVisitor {
	
	/** The declaration nodes. */
	List<MethodInvocation> invocationNodes;
	
	/**
	 * Instantiates a new method declaration visiter.
	 */
	public MethodInvocationVisiter() {
		invocationNodes=new ArrayList<MethodInvocation>();
	}
	@Override
	public boolean visit(MethodInvocation node) {
		invocationNodes.add(node);
		return true;
	}
	public List<MethodInvocation> getResult(){
		return invocationNodes;
	}
}
