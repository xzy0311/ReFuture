package refuture.refactoring;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jface.text.Document;

import refuture.astvisitor.MethodInvocationVisiter;

/**
 * 这个类保存着重构future到completableFuture的方法。
 * 在主类Future2Completable.java中调用这个类中的方法，以完成单个future的重构。
 * 逻辑与步骤在笔记本中已记好。
 * 
 * 主类将所有的ICompilationUnit传入，由本类完成AST的修改，等一切工作。
 * 
 */
public class Future2Completable {

	private static boolean status = true;

	private static String errorCause;
	
	public static boolean initStaticField() {
		status = true;
		errorCause="没有错误";
		return true;
	}
	public static void refactor(List<ICompilationUnit> allJavaFiles) throws JavaModelException {
		for(ICompilationUnit cu : allJavaFiles) {
			String source = cu.getSource();
			Document document = new Document(source);
			
			ASTParser parser = ASTParser.newParser(AST.JLS11);
			parser.setResolveBindings(true);
			parser.setStatementsRecovery(true);
			parser.setBindingsRecovery(true);
			parser.setSource(cu);
			CompilationUnit astUnit = (CompilationUnit) parser.createAST(null);

			MethodInvocationVisiter miv = new MethodInvocationVisiter();
			
			astUnit.accept(miv);
			
			List<MethodInvocation> invocationNodes = miv.getResult();
			refactorexecute(invocationNodes);
			
		}
	}
	
	private static void refactorexecute(List<MethodInvocation> invocationNodes) {
		for(MethodInvocation invocationNode:invocationNodes) {
			//得到execute方法的调用Node。
			if (invocationNode.getName().toString().equals("execute")) {
				System.out.println(invocationNode.resolveMethodBinding());
			}
		}
		
	}
	
	
	
	
	
	
	private static void setErrorStatus() {
		status = false;
	}
	public static boolean getStatus() {
		return status;
	}
	
	private static void setErrorCause(String cause) {
		errorCause = cause;
	}
	public static String getErrorCause() {
		return errorCause;
	}
	
	
	
}
