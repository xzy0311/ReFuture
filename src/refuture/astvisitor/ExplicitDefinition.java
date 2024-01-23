package refuture.astvisitor;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;


// TODO: Auto-generated Javadoc
/**
 * 得到显式定义的，继承Thread,Runnable,Callable及其子类，并覆写Run/Call方法的ASTNode,这个无用
 */
@Deprecated
public class ExplicitDefinition extends ASTVisitor {
	
	/** The sub thread. */
	List<TypeDeclaration> subThread;
	
	/** The sub runnable. */
	List<TypeDeclaration> subRunnable;
	
	/** The sub callable. */
	List<TypeDeclaration> subCallable;
	
	/**
	 * Instantiates a new explicit definition.
	 *
	 * @param subThread   the sub thread
	 * @param subRunnable the sub runnable
	 * @param subCallable the sub callable
	 */
	public ExplicitDefinition(List subThread,List subRunnable,List subCallable) {
		this.subThread = subThread;
		this.subRunnable = subRunnable;
		this.subCallable = subCallable;
	}
	public boolean visit(TypeDeclaration node) {
		//找重写Thread.run的定义

		node.getName();
		return true;
	}

}
