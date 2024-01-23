package refuture.astvisitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class AllVisiter extends ASTVisitor{

	private static AllVisiter instance = null;

	private List<MethodInvocation> invocationNodes;
	private List<InstanceofExpression> insNodes;
	private List<CastExpression> castNodes;

	/**
	 * Private constructor to prevent instantiation from outside the class.
	 */
	private AllVisiter() {
		invocationNodes = new ArrayList<MethodInvocation>();
		insNodes = new ArrayList<InstanceofExpression>();
		castNodes = new ArrayList<CastExpression>();
	}

	/**
	 * Returns the single instance of AllVisiter.
	 * @return the single instance of AllVisiter
	 */
	public static AllVisiter getInstance() {
		if (instance == null) {
			instance = new AllVisiter();
		}
		return instance;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		invocationNodes.add(node);
		return true;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		insNodes.add(node);
		return true;
	}

	@Override
	public boolean visit(CastExpression node) {
		castNodes.add(node);
		return true;
	}

	public List<CastExpression> getCastResult(){
		return castNodes;
	}

	public List<MethodInvocation> getMInvocResult(){
		return invocationNodes;
	}

	public List<InstanceofExpression> getInsResult(){
		return insNodes;
	}

}
