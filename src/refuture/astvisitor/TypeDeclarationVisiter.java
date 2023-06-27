package refuture.astvisitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TypeDeclarationVisiter extends ASTVisitor{


	
	/** The declaration nodes. */
	List<TypeDeclaration> declarationNodes;
	
	/**
	 * Instantiates a new method declaration visiter.
	 */
	public TypeDeclarationVisiter() {
		declarationNodes=new ArrayList<TypeDeclaration>();
	}
	@Override
	public boolean visit(TypeDeclaration node) {
		
		declarationNodes.add(node);
		return true;
	}
	
	public List<TypeDeclaration> getResult(){
		return declarationNodes;
	}
	
}
