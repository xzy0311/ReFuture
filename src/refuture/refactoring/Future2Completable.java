package refuture.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import refuture.astvisitor.MethodInvocationVisiter;
import refuture.sootUtil.AdaptAst;
import refuture.sootUtil.ClassHierarchy;
import refuture.sootUtil.ExecutorSubclass;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.toolkits.base.ExceptionCheckerError;

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
	
	private static List<Change> allChanges;
	
	public static boolean initStaticField() {
		status = true;
		errorCause = "没有错误";
		allChanges = new ArrayList<Change>();
		return true;
	}
	public static void refactor(List<ICompilationUnit> allJavaFiles) throws JavaModelException {
		for(ICompilationUnit cu : allJavaFiles) {
			IFile source = (IFile) cu.getResource();
			TextFileChange change = new TextFileChange("Future2Completable",source);
			ASTParser parser = ASTParser.newParser(AST.JLS11);
			parser.setResolveBindings(true);
			parser.setStatementsRecovery(true);
			parser.setBindingsRecovery(true);
			parser.setSource(cu);
			CompilationUnit astUnit = (CompilationUnit) parser.createAST(null);
			MethodInvocationVisiter miv = new MethodInvocationVisiter();
			astUnit.accept(miv);
			List<MethodInvocation> invocationNodes = miv.getResult();
			change = refactorExecuteRunnable(invocationNodes,change);
			if(change !=null) {
				allChanges.add(change);
			}
		}
	}
	
	private static TextFileChange refactorExecuteRunnable(List<MethodInvocation> invocationNodes, TextFileChange change) throws JavaModelException, IllegalArgumentException {
		for(MethodInvocation invocationNode:invocationNodes) {
			//得到execute方法的调用Node。
			if (invocationNode.getName().toString().equals("execute")) {
//				System.out.println("[refactorexecute:]"+invocationNode.resolveMethodBinding());
				Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);

				if (invocStmt == null) {
					setErrorStatus();
					setErrorCause("[refactorexecute]获取调用节点对应的Stmt出错");
				}
                if (ExecutorSubclass.canRefactor(invocStmt)) {
                	AST ast = invocationNode.getAST();
                	ASTRewrite rewriter = ASTRewrite.create(ast);
                	//重构逻辑
//                	MethodInvocation newMethodInvocation = ast.newMethodInvocation();
                	
                	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
                	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
                	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
//                	ASTNode firstArgu = rewriter.createCopyTarget((ASTNode) invocationNode.arguments().get(0));
//                	listRewriter.insertLast(ast.newSimpleName(invocationNode.arguments().get(0).toString()), null);
                	listRewriter.insertLast(ast.newSimpleName(invocationNode.getExpression().toString()), null);
//                	rewriter.replace(invocationNode, newMethodInvocation, null);
                	TextEdit edits = rewriter.rewriteAST();
                	change.setEdit(edits);
                	return change;
                }
			}
		}
		return null;
		
	}
	
	
	
	public static List<Change>  getallChanges() {
		return allChanges;
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
