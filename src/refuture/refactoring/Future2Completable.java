package refuture.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
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
			ASTParser parser = ASTParser.newParser(AST.JLS11);
			parser.setResolveBindings(true);
			parser.setStatementsRecovery(true);
			parser.setBindingsRecovery(true);
			parser.setSource(cu);
			CompilationUnit astUnit = (CompilationUnit) parser.createAST(null);
			MethodInvocationVisiter miv = new MethodInvocationVisiter();
			astUnit.accept(miv);
			List<MethodInvocation> invocationNodes = miv.getResult();
			for(MethodInvocation invocationNode:invocationNodes) {
				TextFileChange change = new TextFileChange("Future2Completable",source);
				boolean flag1 = refactorExecuteRunnable(invocationNode,change);
				boolean flag2 = refactorffSubmitCallable(invocationNode,change);
				boolean flag3 = refactorSubmitRunnable(invocationNode,change);
				boolean flag4 = refactorSubmitFutureTask(invocationNode,change);
				boolean flag5 = refactorSubmitRunnableNValue(invocationNode,change);
				boolean flag6 = refactorExecuteFutureTask(invocationNode,change);
				if(flag1||flag2||flag3||flag4||flag5||flag6) {
					allChanges.add(change);
				}
			}
			
			

		}
	}
	
	/*
	 * es.execute(r);
	 * CompletableFuture.runAsync(r, es);
	 */
	private static boolean refactorExecuteRunnable(MethodInvocation invocationNode, TextFileChange change) throws JavaModelException, IllegalArgumentException {
			//得到execute方法的调用Node。
			if (invocationNode.getName().toString().equals("execute")) {
//				System.out.println("[refactorexecute:]"+invocationNode.resolveMethodBinding());
				Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);

				if (invocStmt == null) {
					setErrorStatus();
					setErrorCause("[refactorexecute]获取调用节点对应的Stmt出错");
				}
                if (ExecutorSubclass.canRefactor(invocStmt)&&ExecutorSubclass.canRefactorArgu(invocStmt, 2)) {
                	AST ast = invocationNode.getAST();
                	ASTRewrite rewriter = ASTRewrite.create(ast);
                	//重构逻辑
//                	MethodInvocation newMethodInvocation = ast.newMethodInvocation();
                	
                	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
                	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
                	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
//                	ASTNode firstArgu = rewriter.createCopyTarget((ASTNode) invocationNode.arguments().get(0));
//                	listRewriter.insertLast(ast.newSimpleName(invocationNode.arguments().get(0).toString()), null);
                	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
//                	rewriter.replace(invocationNode, newMethodInvocation, null);
                	TextEdit edits = rewriter.rewriteAST();
                	change.setEdit(edits);
                	return true;
                }
			}
		return false;
		
	}
	/*
	 *  
	 * 第一版，不考虑定义的作用范围。
	 * Future f = es.submit(Callable);
	 * 直接转换为 Future f = (Future)CompletableFuture.supplyAsync(Callable,es);
	 * 
	 */
	private static boolean refactorffSubmitCallable(MethodInvocation invocationNode, TextFileChange change) throws JavaModelException, IllegalArgumentException {
		
		if (invocationNode.getName().toString().equals("submit")) {
//			System.out.println("[refactorexecute:]"+invocationNode.resolveMethodBinding());
			Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);

			if (invocStmt == null) {
				setErrorStatus();
				setErrorCause("[refactorexecute]获取调用节点对应的Stmt出错");
			}
            if (ExecutorSubclass.canRefactor(invocStmt)&&ExecutorSubclass.canRefactorArgu(invocStmt, 1)) {
            	AST ast = invocationNode.getAST();
            	ASTRewrite rewriter = ASTRewrite.create(ast);
            	//重构逻辑
//            	MethodInvocation newMethodInvocation = ast.newMethodInvocation();
            	
            	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
            	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("supplyAsync"), null);
            	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
//            	ASTNode firstArgu = rewriter.createCopyTarget((ASTNode) invocationNode.arguments().get(0));
//            	listRewriter.insertLast(ast.newSimpleName(invocationNode.arguments().get(0).toString()), null);
            	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
//            	rewriter.replace(invocationNode, newMethodInvocation, null);
            	TextEdit edits = rewriter.rewriteAST();
            	change.setEdit(edits);
            	return true;
            }
		}
		return false;
	}
	
	/*
	 * Future f = es.submit(()->{});
	 * Future f = CompletableFuture.runAsync(()->{},es);
	 */
	private static boolean refactorSubmitRunnable(MethodInvocation invocationNode, TextFileChange change) throws JavaModelException, IllegalArgumentException {

		if (invocationNode.getName().toString().equals("submit")) {
//			System.out.println("[refactorexecute:]"+invocationNode.resolveMethodBinding());
			Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);

			if (invocStmt == null) {
				setErrorStatus();
				setErrorCause("[refactorexecute]获取调用节点对应的Stmt出错");
			}
            if (ExecutorSubclass.canRefactor(invocStmt)&&ExecutorSubclass.canRefactorArgu(invocStmt, 2)) {
            	AST ast = invocationNode.getAST();
            	ASTRewrite rewriter = ASTRewrite.create(ast);
            	//重构逻辑
//            	MethodInvocation newMethodInvocation = ast.newMethodInvocation();
            	
            	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
            	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
            	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
//            	ASTNode firstArgu = rewriter.createCopyTarget((ASTNode) invocationNode.arguments().get(0));
//            	listRewriter.insertLast(ast.newSimpleName(invocationNode.arguments().get(0).toString()), null);
            	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
//            	rewriter.replace(invocationNode, newMethodInvocation, null);
            	TextEdit edits = rewriter.rewriteAST();
            	change.setEdit(edits);
            	return true;
            }
		}
		return false;
	}
	
	/*
	 * FutureTask ft = ...;
	 * future f = es.submit(ft);
	 * 转换
	 * future f = CompletableFuture.runAsync(ft);
	 */
	private static boolean refactorSubmitFutureTask(MethodInvocation invocationNode, TextFileChange change) throws JavaModelException, IllegalArgumentException {
		if (invocationNode.getName().toString().equals("submit")) {
//			System.out.println("[refactorexecute:]"+invocationNode.resolveMethodBinding());
			Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);

			if (invocStmt == null) {
				setErrorStatus();
				setErrorCause("[refactorexecute]获取调用节点对应的Stmt出错");
			}
            if (ExecutorSubclass.canRefactor(invocStmt)&&ExecutorSubclass.canRefactorArgu(invocStmt, 3)) {
            	AST ast = invocationNode.getAST();
            	ASTRewrite rewriter = ASTRewrite.create(ast);
            	//重构逻辑
//            	MethodInvocation newMethodInvocation = ast.newMethodInvocation();
            	
            	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
            	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
            	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
//            	ASTNode firstArgu = rewriter.createCopyTarget((ASTNode) invocationNode.arguments().get(0));
//            	listRewriter.insertLast(ast.newSimpleName(invocationNode.arguments().get(0).toString()), null);
            	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
//            	rewriter.replace(invocationNode, newMethodInvocation, null);
            	TextEdit edits = rewriter.rewriteAST();
            	change.setEdit(edits);
            	return true;
            }
		}
		return false;
	}
	/*
	 * 		Future f = es.submit(r, value);
	 * 
		 FutureTask f = new FutureTask (r,value)；
		 CompletableFuture.runAsync(f,es);
	 * 
	 */
	private static boolean refactorSubmitRunnableNValue(MethodInvocation invocationNode, TextFileChange change) throws JavaModelException, IllegalArgumentException {
		if (invocationNode.getName().toString().equals("submit")) {
//			System.out.println("[refactorexecute:]"+invocationNode.resolveMethodBinding());
			Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);

			if (invocStmt == null) {
				setErrorStatus();
				setErrorCause("[refactorexecute]获取调用节点对应的Stmt出错");
			}
            if (ExecutorSubclass.canRefactor(invocStmt)&&ExecutorSubclass.canRefactorArgu(invocStmt, 4)) {
            	//重构逻辑
            	VariableDeclarationFragment vdf = (VariableDeclarationFragment) invocationNode.getParent();
            	VariableDeclarationStatement vds = (VariableDeclarationStatement) vdf.getParent();
            	Block b = (Block)vds.getParent();
            	AST ast = b.getAST();
            	SimpleName variableName = vdf.getName();
            	List arguList = invocationNode.arguments();
            	ASTRewrite rewriter = ASTRewrite.create(ast);
            	ClassInstanceCreation cic = ast.newClassInstanceCreation();
            	rewriter.set(cic, ClassInstanceCreation.TYPE_PROPERTY, ast.newSimpleName("FutureTask"), null);
            	ListRewrite listRewriter = rewriter.getListRewrite(cic, ClassInstanceCreation.ARGUMENTS_PROPERTY);
            	listRewriter.insertLast((ASTNode) arguList.get(0), null);
            	listRewriter.insertLast((ASTNode) arguList.get(1), null);
            	rewriter.set(vds, VariableDeclarationStatement.TYPE_PROPERTY, ast.newSimpleName("FutureTask"), null);
            	rewriter.set(vdf, VariableDeclarationFragment.INITIALIZER_PROPERTY, cic, null);
            	
            	MethodInvocation newMiv = ast.newMethodInvocation();
            	rewriter.set(newMiv, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
            	rewriter.set(newMiv, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"),null);
            	ListRewrite  newListRewriter = rewriter.getListRewrite(newMiv, MethodInvocation.ARGUMENTS_PROPERTY);
            	newListRewriter.insertLast(variableName, null);
            	
            	ExpressionStatement exps = ast.newExpressionStatement(newMiv);
            	ListRewrite lastListRewrite = rewriter.getListRewrite(b, Block.STATEMENTS_PROPERTY);
            	lastListRewrite.insertAfter(exps, vds, null);
            	
            	
            	
            	TextEdit edits = rewriter.rewriteAST();
            	change.setEdit(edits);
            	return true;
            }
		}
		return false;
	}
	
	/*
	 * FutureTask f = ... ；
	 * 
	 * es.excute(f);
	 * CompletableFuture.runAsync(f,es);
	 */
	private static boolean refactorExecuteFutureTask(MethodInvocation invocationNode, TextFileChange change) throws JavaModelException, IllegalArgumentException {
		if (invocationNode.getName().toString().equals("execute")) {
//			System.out.println("[refactorexecute:]"+invocationNode.resolveMethodBinding());
			Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);

			if (invocStmt == null) {
				setErrorStatus();
				setErrorCause("[refactorexecute]获取调用节点对应的Stmt出错");
			}
            if (ExecutorSubclass.canRefactor(invocStmt)&&ExecutorSubclass.canRefactorArgu(invocStmt, 3)) {
            	AST ast = invocationNode.getAST();
            	ASTRewrite rewriter = ASTRewrite.create(ast);
            	//重构逻辑
//            	MethodInvocation newMethodInvocation = ast.newMethodInvocation();
            	
            	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
            	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
            	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
//            	ASTNode firstArgu = rewriter.createCopyTarget((ASTNode) invocationNode.arguments().get(0));
//            	listRewriter.insertLast(ast.newSimpleName(invocationNode.arguments().get(0).toString()), null);
            	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
//            	rewriter.replace(invocationNode, newMethodInvocation, null);
            	TextEdit edits = rewriter.rewriteAST();
            	change.setEdit(edits);
            	return true;
            }
		}
		return false;
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
