package refuture.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import refuture.astvisitor.MethodInvocationVisiter;
import refuture.sootUtil.AdaptAst;
import refuture.sootUtil.ExecutorSubclass;
import soot.SootClass;
import soot.jimple.Stmt;

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
		int i = 1;
		int j = 1;
		for(ICompilationUnit cu : allJavaFiles) {
			int invocNum = 1;
			boolean printClassFlag = false;
			IFile source = (IFile) cu.getResource();
//			System.out.println(source.getName());//输出所有的类文件名称
			ASTParser parser = ASTParser.newParser(AST.JLS11);
			parser.setResolveBindings(true);
			parser.setStatementsRecovery(true);
			parser.setBindingsRecovery(true);
			parser.setSource(cu);
			CompilationUnit astUnit = (CompilationUnit) parser.createAST(null);
			MethodInvocationVisiter miv = new MethodInvocationVisiter();
			astUnit.accept(miv);
			List<MethodInvocation> invocationNodes = miv.getResult();
			// 2023.0706 
//			ForTask.FindGet(invocationNodes);
			for(MethodInvocation invocationNode:invocationNodes) {
				if(!invocationNode.getName().toString().equals("execute")&&!invocationNode.getName().toString().equals("submit")) {
					continue;
				}
				
				
				if(!printClassFlag) {
					SootClass sc = AdaptAst.getSootClass4InvocNode(invocationNode);
					System.out.printf("--第%d个包含可能调用的类：%s分析开始------------------------------%n",j,sc.getName());
					printClassFlag =true;
					AnalysisUtils.debugPrint("[refactor]类中所有的方法签名"+sc.getMethods());
				}
				System.out.printf("**第%d个{execute或submit}调用分析开始**********************************************************%n",invocNum);
				
				
				//修改成先利用ast的类型绑定进行初次判断执行器变量的类型，排除一些非法的。已添加0712
				if(!AnalysisUtils.receiverObjectIsComplete(invocationNode)) {
					System.out.printf("**第%d个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕**%n",invocNum++);
					continue;
				}
				Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);
				if(invocStmt ==null) {
					System.out.printf("**第%d个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕**%n",invocNum++);
					continue;
				}
				if(!ExecutorSubclass.canRefactor(invocStmt)) {
					System.out.printf("**第%d个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕**%n",invocNum++);
					continue;
				}
				TextFileChange change = new TextFileChange("Future2Completable",source);
				
				boolean flag1 = refactorExecuteRunnable(invocStmt,invocationNode,change,cu);
				boolean flag2 = refactorffSubmitCallable(invocStmt,invocationNode,change,cu);
				boolean flag3 = refactorSubmitRunnable(invocStmt,invocationNode,change,cu);
				boolean flag4 = refactorSubmitRunnableNValue(invocStmt,invocationNode,change,cu);//suspend
				boolean flag5 = false;
				if(flag3 == false) {
					flag5 = refactorSubmitFutureTask(invocStmt,invocationNode,change,cu);
				}
				boolean flag6 = false;
				if(flag1 ==false) {
					flag6 = refactorExecuteFutureTask(invocStmt,invocationNode,change,cu);
				}
				if(flag1||flag2||flag3||flag4||flag5||flag6) {
					allChanges.add(change);
					MethodDeclaration outMD = AnalysisUtils.getMethodDeclaration4node(invocationNode);
					if(outMD != null) {
						ASTNode outTD = outMD.getParent();
						if(outTD instanceof AnonymousClassDeclaration) {
							System.out.printf("[Task->CF]:重构成功的第%d个，类名：%s，方法名：%s,行号：%d%n",i,((AnonymousClassDeclaration) outTD).resolveBinding().getQualifiedName(),outMD.resolveBinding().getName(),astUnit.getLineNumber(invocationNode.getStartPosition()));
						}else {
							System.out.printf("[Task->CF]:重构成功的第%d个，类名：%s，方法名：%s,行号：%d%n",i,((TypeDeclaration)outTD).resolveBinding().getQualifiedName(),outMD.resolveBinding().getName(),astUnit.getLineNumber(invocationNode.getStartPosition()));
						}
					}else {
						TypeDeclaration outTD = AnalysisUtils.getTypeDeclaration4node(invocationNode);
						System.out.printf("[Task->CF]:重构成功的第%d个，类名：%s，方法名：字段或者初始化块,行号：%d%n",i,outTD.resolveBinding().getQualifiedName(),astUnit.getLineNumber(invocationNode.getStartPosition()));
					}
					i = i+1;
				}
				System.out.printf("**第%d个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****%n",invocNum++);
			}
			if(printClassFlag) {
				System.out.printf("--第%d个可能包含调用的类分析完毕-----------------------------%n",j++);
			}
		}
	}
	

	/*
	 * es.execute(r);
	 * CompletableFuture.runAsync(r, es);
	 */
	private static boolean refactorExecuteRunnable(Stmt invocStmt, MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
			//得到execute方法的调用Node。
			if (invocationNode.getName().toString().equals("execute")&&ExecutorSubclass.canRefactorArgu(invocStmt, 2)) {
            	AST ast = invocationNode.getAST();
            	ASTRewrite rewriter = ASTRewrite.create(ast);
            	//重构逻辑
            	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
            	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
            	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
            	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
            	
            	TextEdit edits = rewriter.rewriteAST();
            	change.setEdit(edits);
           	 ImportRewrite ir = ImportRewrite.create(cu, true);
   			 ir.addImport("java.util.concurrent.CompletableFuture");
   			try {
   				TextEdit editsImport = ir.rewriteImports(null);
   				change.addEdit(editsImport);
   			} catch (CoreException e) {
   				e.printStackTrace();
   			}
   			AnalysisUtils.debugPrint("[refactorExecuteRunnable]refactor success!");
            	return true;
                
			}
		return false;
		
	}

	/**
	 * 第一版，不考虑定义的作用范围。
	 * Future f = es.submit(Callable);
	 * 直接转换为 Future f = (Future)CompletableFuture.supplyAsync(Callable,es);
	 * 
	 * 第二版，完善Callable转换到Supplier的情况。
	 * 需要改变AST ast = methodbody.ast。
	 * 构建一个BiFunction 和一个 Function的匿名类对象。
	 * 使用 Supplier 将 在submit中的callable对象进行包裹。
	 * 重构
	 * Future f = es.submit(call);
	 * ——>
	 * Future f = CompletableFuture.supplyAsync(call$Rf$,es).handle(bfun).thenCompose(fun3);
	 * 
	 * 第三版，直接使用lambda表达式，不搞那么多本地变量了。
	 * lambda形参需要改变，有一个错误需要修改。
	 * @param invocStmt 
	 * @param cu 
	 */
	private static boolean refactorffSubmitCallable(Stmt invocStmt, MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
		int flag = 0; //assignment = 1 ; Fragment = 2; ExpressionStatement = 3; MethodInvocation = 4
		AST ast = null;
		Assignment invocAssignment = null;
		VariableDeclarationFragment invocFragment = null;
		ExpressionStatement expressionStatement = null;
		MethodInvocation methodInvocation = null;
		if (invocationNode.getName().toString().equals("submit")&&ExecutorSubclass.canRefactorArgu(invocStmt, 1)) {
			if(invocationNode.getParent() instanceof Assignment) {
				flag = 1;
				invocAssignment = (Assignment)invocationNode.getParent();
				ast = invocAssignment.getAST();
			}else if(invocationNode.getParent() instanceof VariableDeclarationFragment ) {
				flag = 2;
				invocFragment = (VariableDeclarationFragment)invocationNode.getParent();
            	ast = invocFragment.getAST();
			}else if(invocationNode.getParent() instanceof ExpressionStatement ){
				flag = 3;
				expressionStatement = (ExpressionStatement)invocationNode.getParent();
				ast = expressionStatement.getAST();
			}else if(invocationNode.getParent() instanceof MethodInvocation ) {
				flag = 4;
				methodInvocation = (MethodInvocation)invocationNode.getParent();
				ast = methodInvocation.getAST();
			}
			if (ast == null) {
				throw new NullPointerException();
			}
        	ASTRewrite rewriter = ASTRewrite.create(ast);
        	//重构逻辑
        	//一、CompletableFuture.supplyAsync(Supplier,Executor)
        	MethodInvocation invocationSup = ast.newMethodInvocation();
        	rewriter.set(invocationSup, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
        	rewriter.set(invocationSup, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("supplyAsync"), null);
/*
 * 	()->{
		                try {
		                    return call.call();
		                } catch (Exception e) {
		                    throw new RuntimeException(e);
		                }
		            }
 */
        	Object callableObject = invocationNode.arguments().get(0);
        	// callable.call()；
        	MethodInvocation invocCall = ast.newMethodInvocation();
        	rewriter.set(invocCall, MethodInvocation.EXPRESSION_PROPERTY, callableObject, null);
        	rewriter.set(invocCall, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("call"), null);
        	//return callable.call();
        	ReturnStatement reSup = ast.newReturnStatement();
        	rewriter.set(reSup, ReturnStatement.EXPRESSION_PROPERTY, invocCall, null);
        	//{return callable.call();}
        	Block tryBlockFirst = ast.newBlock();
        	ListRewrite tryBlockFirstListRewrite = rewriter.getListRewrite(tryBlockFirst, Block.STATEMENTS_PROPERTY);
        	tryBlockFirstListRewrite.insertLast(reSup, null);
        	//try {return callable.call();}
        	TryStatement tryFirst = ast.newTryStatement();
        	rewriter.set(tryFirst, TryStatement.BODY_PROPERTY, tryBlockFirst, null);
        	//catch (Exception e){ throw new RuntimeException(e)};}
        	//Exception e
        	SingleVariableDeclaration exceptionE = ast.newSingleVariableDeclaration();
        	rewriter.set(exceptionE, SingleVariableDeclaration.TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName("Exception")), null);
        	rewriter.set(exceptionE, SingleVariableDeclaration.NAME_PROPERTY, ast.newSimpleName("e"), null);
        	//new RuntimeException(e)
        	ClassInstanceCreation exceptionInstance = ast.newClassInstanceCreation();
        	rewriter.set(exceptionInstance, ClassInstanceCreation.TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName("RuntimeException")), null);
        	ListRewrite listRewriterRe = rewriter.getListRewrite(exceptionInstance, ClassInstanceCreation.ARGUMENTS_PROPERTY);
        	listRewriterRe.insertLast(ast.newSimpleName("e"), null);
        	//throw new RuntimeException(e)
        	ThrowStatement throwStatement = ast.newThrowStatement();
        	rewriter.set(throwStatement, ThrowStatement.EXPRESSION_PROPERTY, exceptionInstance, null);
        	//{throw new RuntimeException(e)};
        	Block tryBlockSecond = ast.newBlock();
        	ListRewrite tryBlockSecondListRewrite = rewriter.getListRewrite(tryBlockSecond, Block.STATEMENTS_PROPERTY);
        	tryBlockSecondListRewrite.insertLast(throwStatement, null);
        	//catch (Exception e){ throw new RuntimeException(e)};}
        	CatchClause catchClause = ast.newCatchClause();
        	rewriter.set(catchClause, CatchClause.EXCEPTION_PROPERTY, exceptionE, null);
        	rewriter.set(catchClause, CatchClause.BODY_PROPERTY, tryBlockSecond, null);
        	/*
        	 * 	try {
	                    return call.call();
	                } catch (Exception e) {
	                    throw new RuntimeException(e);
	                }
        	 */
        	ListRewrite listRewriterTry = rewriter.getListRewrite(tryFirst, TryStatement.CATCH_CLAUSES_PROPERTY);
        	listRewriterTry.insertLast(catchClause, null);
        	//{ try... catch..}
        	Block lambdaBlockFirst = ast.newBlock();
        	ListRewrite lambdaBlockFirstListRewrite = rewriter.getListRewrite(lambdaBlockFirst, Block.STATEMENTS_PROPERTY);
        	lambdaBlockFirstListRewrite.insertLast(tryFirst, null);
        	//()->{}
        	LambdaExpression lambdaExpFirst = ast.newLambdaExpression();
        	rewriter.set(lambdaExpFirst, LambdaExpression.BODY_PROPERTY, lambdaBlockFirst, null);
        	//CompletableFuture.supplyAsync(Supplier,Executor)
        	ListRewrite listRewriteIvocFirst = rewriter.getListRewrite(invocationSup, MethodInvocation.ARGUMENTS_PROPERTY);
        	listRewriteIvocFirst.insertLast(lambdaExpFirst, null);
        	listRewriteIvocFirst.insertLast(invocationNode.getExpression(), null);
        	
        	//二、CompletableFuture.supplyAsync(Supplier,Executor).handle(LambdaExpression)
        	MethodInvocation invocationHandle = ast.newMethodInvocation();
        	rewriter.set(invocationHandle, MethodInvocation.EXPRESSION_PROPERTY, invocationSup, null);
        	rewriter.set(invocationHandle, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("handle"), null);
        	/*
        	 * (o$Rf$,o2$Rf$)-> {
	                if (o2$Rf$ != null){
	                    return o2$Rf$;
	                }else{
	                    return o$Rf$;
	                }
	            }
        	 */
        	//(o$Rf$,o2$Rf$)
        	VariableDeclarationFragment o1 = ast.newVariableDeclarationFragment();
        	rewriter.set(o1, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName("o$Rf$"), null);
        	VariableDeclarationFragment o2 = ast.newVariableDeclarationFragment();
        	rewriter.set(o2, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName("o2$Rf$"), null);
        	//(o2$Rf$ != null)
        	InfixExpression infixHandle = ast.newInfixExpression();
        	rewriter.set(infixHandle, InfixExpression.LEFT_OPERAND_PROPERTY, ast.newSimpleName("o2$Rf$"), null);
        	rewriter.set(infixHandle, InfixExpression.OPERATOR_PROPERTY, InfixExpression.Operator.NOT_EQUALS, null);
        	rewriter.set(infixHandle, InfixExpression.RIGHT_OPERAND_PROPERTY, ast.newNullLiteral(), null);
        	// return o2$Rf$
        	ReturnStatement reHanFirst = ast.newReturnStatement();
        	rewriter.set(reHanFirst, ReturnStatement.EXPRESSION_PROPERTY, ast.newSimpleName("o2$Rf$"), null);
        	//{return o2$Rf$;}
        	Block ifBlockHanFirst = ast.newBlock();
        	ListRewrite ifBlockHanFirstListRewrite = rewriter.getListRewrite(ifBlockHanFirst, Block.STATEMENTS_PROPERTY);
        	ifBlockHanFirstListRewrite.insertLast(reHanFirst, null);
        	//return o$Rf$
        	ReturnStatement reHanSecond = ast.newReturnStatement();
        	rewriter.set(reHanSecond, ReturnStatement.EXPRESSION_PROPERTY, ast.newSimpleName("o$Rf$"), null);
        	//{return o$Rf$;}
        	Block ifBlockHanSecond = ast.newBlock();
        	ListRewrite ifBlockHanSecondListRewrite = rewriter.getListRewrite(ifBlockHanSecond, Block.STATEMENTS_PROPERTY);
        	ifBlockHanSecondListRewrite.insertLast(reHanSecond, null);
        	//if(..){}else{}
        	IfStatement ifHandle = ast.newIfStatement();
        	rewriter.set(ifHandle, IfStatement.EXPRESSION_PROPERTY, infixHandle, null);
        	rewriter.set(ifHandle, IfStatement.THEN_STATEMENT_PROPERTY, ifBlockHanFirst, null);
        	rewriter.set(ifHandle, IfStatement.ELSE_STATEMENT_PROPERTY, ifBlockHanSecond, null);
        	//{if(..){}else{}}
        	Block lambdaHandleBlock = ast.newBlock();
        	ListRewrite lambdaHandleBlockListRewrite = rewriter.getListRewrite(lambdaHandleBlock, Block.STATEMENTS_PROPERTY);
        	lambdaHandleBlockListRewrite.insertLast(ifHandle, null);
        	//(o$Rf$,o2$Rf$)->{}
        	LambdaExpression lambdaHandle = ast.newLambdaExpression();
        	ListRewrite lrHanLambda = rewriter.getListRewrite(lambdaHandle, LambdaExpression.PARAMETERS_PROPERTY);
        	lrHanLambda.insertLast(o1, null);
        	lrHanLambda.insertLast(o2, null);
        	rewriter.set(lambdaHandle, LambdaExpression.BODY_PROPERTY, lambdaHandleBlock, null);
        	//CompletableFuture.supplyAsync(Supplier,Executor).handle(LambdaExpression)
        	ListRewrite invocationHandleListRewrite = rewriter.getListRewrite(invocationHandle, MethodInvocation.ARGUMENTS_PROPERTY);
        	invocationHandleListRewrite.insertLast(lambdaHandle, null);
        	
        	//三、CompletableFuture.supplyAsync(Supplier,Executor).handle(LambdaExpression).thenCompose(LambdaExpression）
        	MethodInvocation invocationCompose = ast.newMethodInvocation();
        	rewriter.set(invocationCompose, MethodInvocation.EXPRESSION_PROPERTY, invocationHandle, null);
        	rewriter.set(invocationCompose, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("thenCompose"), null);
        	
        	/*
        	 * (o$Rf$)->{
	                CompletableFuture cf$Rf$ = new CompletableFuture();
	                if(o$Rf$ instanceof CompletionException)
	                {
	                    RuntimeException runex$Rf$ = (RuntimeException) ((CompletionException) o$Rf$).getCause();
	                    cf.obtrudeException(runex$Rf$.getCause());
	                    return cf$Rf$;
	                }else{
	                    cf$Rf$.obtrudeValue(o$Rf$);
	                    return cf$Rf$;
	                }
	            }
        	 */
        	// (o$Rf$)
        	VariableDeclarationFragment o = ast.newVariableDeclarationFragment();
        	rewriter.set(o, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName("o$Rf$"), null);
        	// new CompletableFuture()
        	ClassInstanceCreation completablefuture = ast.newClassInstanceCreation();
        	rewriter.set(completablefuture, ClassInstanceCreation.TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName("CompletableFuture")), null);
        	// cf$Rf$ = new CompletableFuture()
        	VariableDeclarationFragment cfVDF = ast.newVariableDeclarationFragment();
        	rewriter.set(cfVDF, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName("cf$Rf$"), null);
        	rewriter.set(cfVDF, VariableDeclarationFragment.INITIALIZER_PROPERTY, completablefuture, null);
        	//CompletableFuture cf$Rf$ = new CompletableFuture()
        	VariableDeclarationStatement cfVDS = ast.newVariableDeclarationStatement(cfVDF);
        	rewriter.set(cfVDS, VariableDeclarationStatement.TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName("CompletableFuture")), null);
        	//o$Rf$ instanceof CompletionException
        	InstanceofExpression instanceofComp = ast.newInstanceofExpression();
        	rewriter.set(instanceofComp, InstanceofExpression.LEFT_OPERAND_PROPERTY, ast.newSimpleName("o$Rf$"), null);
        	rewriter.set(instanceofComp, InstanceofExpression.RIGHT_OPERAND_PROPERTY, ast.newSimpleType(ast.newSimpleName("CompletionException")), null);
        	// RuntimeException runex$Rf$ = (RuntimeException) ((CompletionException) o$Rf$).getCause()
        	//CompletionException) o$Rf$
        	CastExpression completionExpCast = ast.newCastExpression();
        	rewriter.set(completionExpCast, CastExpression.TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName("CompletionException")), null);
        	rewriter.set(completionExpCast, CastExpression.EXPRESSION_PROPERTY, ast.newSimpleName("o$Rf$"), null);
        	//(CompletionException) o$Rf$)
        	ParenthesizedExpression parentExp = ast.newParenthesizedExpression();
        	rewriter.set(parentExp, ParenthesizedExpression.EXPRESSION_PROPERTY, completionExpCast, null);
        	//(CompletionException) o$Rf$).getCauser()
        	MethodInvocation invocationCauseFirst = ast.newMethodInvocation();
        	rewriter.set(invocationCauseFirst, MethodInvocation.EXPRESSION_PROPERTY, parentExp, null);
        	rewriter.set(invocationCauseFirst, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("getCause"), null);
        	//(RuntimeException) ((CompletionException) o$Rf$).getCause()
        	CastExpression runtimeExpCast = ast.newCastExpression();
        	rewriter.set(runtimeExpCast, CastExpression.TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName("RuntimeException")), null);
        	rewriter.set(runtimeExpCast, CastExpression.EXPRESSION_PROPERTY, invocationCauseFirst, null);
        	//runex$Rf$ = (RuntimeException) ((CompletionException) o$Rf$).getCause()
        	VariableDeclarationFragment runtimeExpVDF = ast.newVariableDeclarationFragment();
        	rewriter.set(runtimeExpVDF, VariableDeclarationFragment.NAME_PROPERTY, ast.newSimpleName("runex$Rf$"), null);
        	rewriter.set(runtimeExpVDF, VariableDeclarationFragment.INITIALIZER_PROPERTY, runtimeExpCast, null);
        	//RuntimeException runex$Rf$ = (RuntimeException) ((CompletionException) o$Rf$).getCause()
        	VariableDeclarationStatement runtimeExpVDS = ast.newVariableDeclarationStatement(runtimeExpVDF);
        	rewriter.set(runtimeExpVDS, VariableDeclarationStatement.TYPE_PROPERTY, ast.newSimpleType(ast.newSimpleName("RuntimeException")), null);
        	//runex$Rf$.getCause()
        	MethodInvocation invocationCauseSecond = ast.newMethodInvocation();
        	rewriter.set(invocationCauseSecond, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("runex$Rf$"), null);
        	rewriter.set(invocationCauseSecond, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("getCause"), null);
        	//cf$Rf$.obtrudeException(runex$Rf$.getCause())
        	MethodInvocation invocationObtrudeExp = ast.newMethodInvocation();
        	rewriter.set(invocationObtrudeExp, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("cf$Rf$"), null);
        	rewriter.set(invocationObtrudeExp, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("obtrudeException"), null);
        	ListRewrite invocationObtrudeExpListRewriter = rewriter.getListRewrite(invocationObtrudeExp, MethodInvocation.ARGUMENTS_PROPERTY);
        	invocationObtrudeExpListRewriter.insertLast(invocationCauseSecond, null);
        	ExpressionStatement expStatement = ast.newExpressionStatement(invocationObtrudeExp);
        	//return cf$Rf$
        	ReturnStatement returnComposeFirst = ast.newReturnStatement();
        	rewriter.set(returnComposeFirst, ReturnStatement.EXPRESSION_PROPERTY, ast.newSimpleName("cf$Rf$"), null);
        	//{VariableDS;ExpresstionStatement;ReturnStatement;}
        	Block ifBlockComposeFirst = ast.newBlock();
        	ListRewrite ifBlockComposeFirstListRewrite = rewriter.getListRewrite(ifBlockComposeFirst, Block.STATEMENTS_PROPERTY);
        	ifBlockComposeFirstListRewrite.insertLast(runtimeExpVDS, null);
        	ifBlockComposeFirstListRewrite.insertLast(expStatement, null);
        	ifBlockComposeFirstListRewrite.insertLast(returnComposeFirst, null);
        	//cf$Rf$.obtrudeValue(o$Rf$);
        	MethodInvocation ivocationObtrudeValue = ast.newMethodInvocation();
        	rewriter.set(ivocationObtrudeValue, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("cf$Rf$"), null);
        	rewriter.set(ivocationObtrudeValue, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("obtrudeValue"), null);
        	ListRewrite ivocationObtrudeValueListRewrite = rewriter.getListRewrite(ivocationObtrudeValue, MethodInvocation.ARGUMENTS_PROPERTY);
        	ivocationObtrudeValueListRewrite.insertLast(ast.newSimpleName("o$Rf$"), null);
        	ExpressionStatement expStatementSecond = ast.newExpressionStatement(ivocationObtrudeValue);
        	//return cf$Rf$
        	ReturnStatement returnComposeSecond = ast.newReturnStatement();
        	rewriter.set(returnComposeSecond, ReturnStatement.EXPRESSION_PROPERTY, ast.newSimpleName("cf$Rf$"), null);
        	//{cf$Rf$.obtrudeValue(o$Rf$);return cf$Rf$;}
        	Block ifBlockComposeSecond = ast.newBlock();
        	ListRewrite ifBlockComposeSecondListRewrite = rewriter.getListRewrite(ifBlockComposeSecond, Block.STATEMENTS_PROPERTY);
        	ifBlockComposeSecondListRewrite.insertLast(expStatementSecond, null);
        	ifBlockComposeSecondListRewrite.insertLast(returnComposeSecond, null);
        	//if(){}else{}
        	IfStatement ifCompose = ast.newIfStatement();
        	rewriter.set(ifCompose, IfStatement.EXPRESSION_PROPERTY, instanceofComp, null);
        	rewriter.set(ifCompose, IfStatement.THEN_STATEMENT_PROPERTY, ifBlockComposeFirst, null);
        	rewriter.set(ifCompose, IfStatement.ELSE_STATEMENT_PROPERTY, ifBlockComposeSecond, null);
        	//->{}
        	Block labmdaBlockCompose = ast.newBlock();
        	ListRewrite labmdaBlockComposeListRewrite = rewriter.getListRewrite(labmdaBlockCompose, Block.STATEMENTS_PROPERTY);
        	labmdaBlockComposeListRewrite.insertLast(cfVDS, null);
        	labmdaBlockComposeListRewrite.insertLast(ifCompose, null);
        	//()->{};
        	LambdaExpression lambdaCompose = ast.newLambdaExpression();
        	ListRewrite lambdaComposeListRewrite = rewriter.getListRewrite(lambdaCompose, LambdaExpression.PARAMETERS_PROPERTY);
        	lambdaComposeListRewrite.insertLast(o, null);
        	rewriter.set(lambdaCompose, LambdaExpression.BODY_PROPERTY, labmdaBlockCompose, null);
        	//CompletableFuture.supplyAsync(Supplier,Executor).handle(LambdaExpression).thenCompose(LambdaExpression）
        	ListRewrite invocationComposeRewrite = rewriter.getListRewrite(invocationCompose, MethodInvocation.ARGUMENTS_PROPERTY);
        	invocationComposeRewrite.insertLast(lambdaCompose, null);
        	
        	if(flag == 1) {
        		rewriter.set(invocAssignment, Assignment.RIGHT_HAND_SIDE_PROPERTY, invocationCompose, null);
        	}else if(flag ==2){
            	rewriter.set(invocFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, invocationCompose, null);
        	}else if(flag ==3){
        		rewriter.set(expressionStatement, ExpressionStatement.EXPRESSION_PROPERTY, invocationCompose, null);
        	}else if(flag ==4){
        		rewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invocationCompose, null);
        	}else {
        		throw new IllegalArgumentException(new Integer(flag).toString());
        	}
        	
        	TextEdit edits = rewriter.rewriteAST();
        	change.setEdit(edits);
       	 ImportRewrite ir = ImportRewrite.create(cu, true);
		 ir.addImport("java.util.concurrent.CompletableFuture");
		try {
			TextEdit editsImport = ir.rewriteImports(null);
			change.addEdit(editsImport);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		AnalysisUtils.debugPrint("[refactorffSubmitCallable]refactor success!");
        	return true;
		}
		return false;
	}
	
	/*
	 * Future f = es.submit(()->{});
	 * Future f = CompletableFuture.runAsync(()->{},es);
	 */
	private static boolean refactorSubmitRunnable(Stmt invocStmt, MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {

		if (invocationNode.getName().toString().equals("submit")&&ExecutorSubclass.canRefactorArgu(invocStmt, 2)) {
        	AST ast = invocationNode.getAST();
        	ASTRewrite rewriter = ASTRewrite.create(ast);
        	//重构逻辑
        	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
        	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
        	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
        	if(invocationNode.getExpression() instanceof ArrayAccess) {
        		ArrayAccess oldArrayAcc = (ArrayAccess)invocationNode.getExpression();
        		ArrayAccess arrayAcc = ast.newArrayAccess();
        		rewriter.set(arrayAcc, ArrayAccess.ARRAY_PROPERTY, oldArrayAcc.getArray(), null);
        		rewriter.set(arrayAcc, ArrayAccess.INDEX_PROPERTY, oldArrayAcc.getIndex(), null);
        		listRewriter.insertLast(arrayAcc, null);
        	}else {
        		listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
        	}
        	TextEdit edits = rewriter.rewriteAST();
        	change.setEdit(edits);
        	
       	 ImportRewrite ir = ImportRewrite.create(cu, true);
		 ir.addImport("java.util.concurrent.CompletableFuture");
		try {
			TextEdit editsImport = ir.rewriteImports(null);
			change.addEdit(editsImport);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		AnalysisUtils.debugPrint("[refactorSubmitRunnable]refactor success!");
        	return true;
		}
		return false;
	}
	
	/*
	 * FutureTask ft = ...;
	 * future f = es.submit(ft);
	 * 转换
	 * future f = CompletableFuture.runAsync(ft,es);
	 */
	private static boolean refactorSubmitFutureTask(Stmt invocStmt, MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
		if (invocationNode.getName().toString().equals("submit")&&ExecutorSubclass.canRefactorArgu(invocStmt, 3)) {
        	AST ast = invocationNode.getAST();
        	//hello
        	ASTRewrite rewriter = ASTRewrite.create(ast);
        	//重构逻辑
        	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
        	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
        	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
        	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
        	TextEdit edits = rewriter.rewriteAST();
        	change.setEdit(edits);
        	
       	 ImportRewrite ir = ImportRewrite.create(cu, true);
		 ir.addImport("java.util.concurrent.CompletableFuture");
		try {
			TextEdit editsImport = ir.rewriteImports(null);
			change.addEdit(editsImport);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		AnalysisUtils.debugPrint("[refactorSubmitFutureTask]refactor success!");
        	return true;
		}
		return false;
	}
	/*
	 * 		Future f = es.submit(r, value);
	 * 
		 FutureTask f = new FutureTask (r,value)；6.1改成了Future f = new ...
		 CompletableFuture.runAsync(f,es);
		 7.25:
		 first: Callable f$rf$ = Executors.callable(r, v);
		 Future f = es.submit(f$rf);
		 ->submit(callable)->CaompletableFuture;
		 
	 * 
	 */
	private static boolean refactorSubmitRunnableNValue(Stmt invocStmt, MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
		if (invocationNode.getName().toString().equals("submit")&&ExecutorSubclass.canRefactorArgu(invocStmt, 4)) {
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
        	rewriter.set(vds, VariableDeclarationStatement.TYPE_PROPERTY, ast.newSimpleName("Future"), null);
        	rewriter.set(vdf, VariableDeclarationFragment.INITIALIZER_PROPERTY, cic, null);
        	
        	MethodInvocation newMiv = ast.newMethodInvocation();
        	rewriter.set(newMiv, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
        	rewriter.set(newMiv, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"),null);
        	ListRewrite  newListRewriter = rewriter.getListRewrite(newMiv, MethodInvocation.ARGUMENTS_PROPERTY);
        	newListRewriter.insertLast(variableName, null);
        	newListRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
        	ExpressionStatement exps = ast.newExpressionStatement(newMiv);
        	ListRewrite lastListRewrite = rewriter.getListRewrite(b, Block.STATEMENTS_PROPERTY);
        	lastListRewrite.insertAfter(exps, vds, null);
        	TextEdit edits = rewriter.rewriteAST();
        	change.setEdit(edits);

       	 ImportRewrite ir = ImportRewrite.create(cu, true);
		 ir.addImport("java.util.concurrent.CompletableFuture");
		try {
			TextEdit editsImport = ir.rewriteImports(null);
			change.addEdit(editsImport);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		AnalysisUtils.debugPrint("[refactorSubmitRunnableNValue]refactor success!");
        	return true;
		}
		return false;
	}
	
	/*
	 * FutureTask f = ... ；
	 * 
	 * es.excute(f);
	 * CompletableFuture.runAsync(f,es);
	 */
	private static boolean refactorExecuteFutureTask(Stmt invocStmt, MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
		if (invocationNode.getName().toString().equals("execute")&&ExecutorSubclass.canRefactorArgu(invocStmt, 3)) {
        	AST ast = invocationNode.getAST();
        	ASTRewrite rewriter = ASTRewrite.create(ast);
        	//重构逻辑
        	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
        	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
        	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
        	listRewriter.insertLast(ast.newName(invocationNode.getExpression().toString()), null);
        	TextEdit edits = rewriter.rewriteAST();
        	change.setEdit(edits);
        	 ImportRewrite ir = ImportRewrite.create(cu, true);
			 ir.addImport("java.util.concurrent.CompletableFuture");
			try {
				TextEdit editsImport = ir.rewriteImports(null);
				change.addEdit(editsImport);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			AnalysisUtils.debugPrint("[refactorExecuteFutureTask]refactor success!");
        	return true;
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
