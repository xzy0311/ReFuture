package refuture.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
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
import org.eclipse.jdt.core.dom.Statement;
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

import refuture.sootUtil.AdaptAst;
import refuture.sootUtil.Cancel;
import refuture.sootUtil.CastAnalysis;
import refuture.sootUtil.CollectionEntrypoint;
import refuture.sootUtil.ExecutorSubclass;
import refuture.sootUtil.Instanceof;
import refuture.sootUtil.NeedTestMethods;
import soot.SootClass;
import soot.SootMethod;
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
	
	private static List<Change> allChanges;
	
	public static int FutureCanotC;
	
	public static int maybeRefactoringNode;
	
	public static int FutureCanot;
	
	public static int FutureCanotI;
	
	public static int methodOverload;
	
	public static boolean fineRefactoring;
	public static boolean debugFlag;
	public static String debugClassName;
	public static String debugMethodName;
	public static int debugLineNumber;
	public static int debugUsePoint2num;
	
	
	public static boolean initStaticField() {
		allChanges = new ArrayList<Change>();
		FutureCanotC = 0;
		FutureCanot = 0;
		FutureCanotI = 0;
		maybeRefactoringNode =0;
		fineRefactoring = false;
		debugFlag = false;
		methodOverload = 0;
		debugUsePoint2num = 0;
		return true;
	}
	
	public static void refactor() throws JavaModelException {
		int i = 1;
		int j = 1;
		int illExecutor = 0;
		int illExecutorService = 0;
		int useCancelTrue = 0;	
		HashMap<String,Integer> flagMap = new HashMap<String,Integer>();
		flagMap.put("ExecuteRunnable", 0);
		flagMap.put("SubmitCallable", 0);
		flagMap.put("SubmitRunnable", 0);
		flagMap.put("SubmitRunnableNValue", 0);
		HashMap<String,Integer> flagMaybeMap = new HashMap<String,Integer>();
		flagMaybeMap.put("ExecuteRunnable", 0);
		flagMaybeMap.put("SubmitCallable", 0);
		flagMaybeMap.put("SubmitRunnable", 0);
		flagMaybeMap.put("SubmitRunnableNValue", 0);
		
		
		HashMap<ICompilationUnit,List<MethodInvocation>> invocNodeMap = CollectionEntrypoint.invocNodeMap;
		for(ICompilationUnit cu : invocNodeMap.keySet()) {
			int invocNum = 1;
			int[] invocSubmitNum = {1};
			boolean printClassFlag = false;
			IFile source = (IFile) cu.getResource();
			TextFileChange classChange = null;
			if(!fineRefactoring) {
				classChange= new TextFileChange(cu.getElementName(),source);
			}
			boolean scClassflag = false;
			boolean sRVClassflag = false;
			for(MethodInvocation invocationNode:invocNodeMap.get(cu)) {
				CompilationUnit astUnit = (CompilationUnit)invocationNode.getRoot();
				int lineNumber = astUnit.getLineNumber(invocationNode.getStartPosition());
				if((lineNumber == debugLineNumber)&&(cu.getElementName().equals(debugClassName))) {
					MethodDeclaration deMd = (MethodDeclaration)AnalysisUtils.getMethodDeclaration4node(invocationNode);
					if(debugMethodName.equals(deMd.getName().toString())) {
						System.out.println("已到达指定位置");
					}
				}
				TextFileChange change;
				if(fineRefactoring) {
					change= new TextFileChange("Future2Completable",source);
				}else {
					change = classChange;
				}
				if(!printClassFlag&&debugFlag) {
					SootClass sc = AdaptAst.getSootClass4InvocNode(invocationNode);
					AnalysisUtils.debugPrint("--第"+j+"个包含可能调用的类："+sc.getName()+"分析开始------------------------------%n");
					printClassFlag =true;
					AnalysisUtils.debugPrint("[refactor]类中所有的方法签名"+sc.getMethods());
				}
				AnalysisUtils.debugPrint("**第"+invocNum+"个{execute或submit}调用分析开始**********************************************************%n");
				SootMethod sm = AdaptAst.getSM4ASTNode(invocationNode);
				Stmt invocStmt = AdaptAst.getJimpleStmt(sm,invocationNode);
				boolean returnValue;
				int tempNum = debugUsePoint2num;
				int refactorMode = ExecutorSubclass.arguModel(invocationNode);
				boolean flag = true;
				boolean scflag = false;
				boolean sRVflag = false;
				switch (refactorMode) {
			    case 1:
			    	flagMaybeMap.put("ExecuteRunnable",flagMaybeMap.get("ExecuteRunnable")+1 );
			    	returnValue = ExecutorSubclass.canRefactor(invocationNode,invocStmt,refactorMode);
					if(returnValue == false) {
						AnalysisUtils.debugPrint("Executor类型失败");
						illExecutor++;
						AnalysisUtils.debugPrint("**第"+invocNum+++"个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕**%n");
						continue;
					}
			    	refactorExecuteRunnable(invocationNode, change, cu);
			    	flagMap.put("ExecuteRunnable",flagMap.get("ExecuteRunnable")+1 );
			        break;
			    case 2:
			    	flagMaybeMap.put("SubmitCallable",flagMaybeMap.get("SubmitCallable")+1 );
			    	returnValue = ExecutorSubclass.canRefactor(invocationNode,invocStmt,refactorMode);
					if(returnValue == false) {
						AnalysisUtils.debugPrint("失败");
						//因执行器类型不安全，不能重构。
						illExecutorService++;
						AnalysisUtils.debugPrint("**第"+invocNum+++"个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕**%n");
						continue;
					}
					if(!futureType(invocationNode)) {
						continue;
					}
					if (Cancel.futureUseCancelTure(invocationNode, invocStmt)) {
			            useCancelTrue++;
			            continue;
			           }
			    	refactorffSubmitCallable(invocationNode, change, cu, invocSubmitNum);
			    	flagMap.put("SubmitCallable",flagMap.get("SubmitCallable")+1 );
			    	scflag = true;
			    	scClassflag = true;
			        break;
			    case 3:
			    	flagMaybeMap.put("SubmitRunnable",flagMaybeMap.get("SubmitRunnable")+1 );
			    	returnValue = ExecutorSubclass.canRefactor(invocationNode,invocStmt,refactorMode);
					if(returnValue == false) {
						AnalysisUtils.debugPrint("失败");
						//因执行器类型不安全，不能重构。
						illExecutorService++;
						AnalysisUtils.debugPrint("**第"+invocNum+++"个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕**%n");
						continue;
					}
					if(!futureType(invocationNode)) {
						continue;
					}
					if (Cancel.futureUseCancelTure(invocationNode, invocStmt)) {
			            useCancelTrue++;
			            continue;
			           }
			    	refactorSubmitRunnable(invocationNode, change, cu);
			    	flagMap.put("SubmitRunnable",flagMap.get("SubmitRunnable")+1 );
			        break;
			    case 4:
			    	flagMaybeMap.put("SubmitRunnableNValue",flagMaybeMap.get("SubmitRunnableNValue")+1 );
			    	returnValue = ExecutorSubclass.canRefactor(invocationNode,invocStmt,refactorMode);
					if(returnValue == false) {
						AnalysisUtils.debugPrint("失败");
						//因执行器类型不安全，不能重构。
						illExecutorService++;
						AnalysisUtils.debugPrint("**第"+invocNum+++"个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕**%n");
						continue;
					}
					if(!futureType(invocationNode)) {
						continue;
					}
					if (Cancel.futureUseCancelTure(invocationNode, invocStmt)) {
			            useCancelTrue++;
			            continue;
			           }
			    	refactorSubmitRunnableNValue(invocationNode, change, cu);
			    	flagMap.put("SubmitRunnableNValue",flagMap.get("SubmitRunnableNValue")+1 );
			    	sRVClassflag = true;
			    	sRVflag = true;
			        break;
			    default:
			    	flag = false;
			        break;
				}
				if(flag) {
					if(fineRefactoring) {
						ImportRewrite ir = ImportRewrite.create(cu, true);
						ir.addImport("java.util.concurrent.CompletableFuture");
						if(scflag) {
							ir.addImport("java.util.concurrent.CompletionException");
						}
						if(sRVflag) {
							ir.addImport("java.util.concurrent.Executors");
						}
						try {
							TextEdit editsImport = ir.rewriteImports(null);
							change.addEdit(editsImport);
						} catch (CoreException e) {
							e.printStackTrace();
						}
						allChanges.add(change);
					}
					System.out.printf("[Task->CF]:重构成功的第%d个，方法名：%s,行号：%d,Points未命中:%d%n",i,sm.getSignature(),lineNumber,debugUsePoint2num-tempNum);
					NeedTestMethods.getInstance().addRefactoringMethods(sm);
					i = i+1;
				}else {
					System.out.printf("[Task->CF]:重构失败方法名：%s,行号：%d%n",sm.getSignature(),lineNumber);
				}
				AnalysisUtils.debugPrint("**第"+ invocNum++ +"个调用分析完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****完毕****%n");
			}// 一个类中所有的调用分析完毕
			if(!fineRefactoring&&classChange.getEdit() != null) {
				ImportRewrite ir = ImportRewrite.create(cu, true);
				ir.addImport("java.util.concurrent.CompletableFuture");
				if(scClassflag) {
					ir.addImport("java.util.concurrent.CompletionException");
				}
				if(sRVClassflag) {
					ir.addImport("java.util.concurrent.Executors");
				}
				try {
					TextEdit editsImport = ir.rewriteImports(null);
					classChange.addEdit(editsImport);
				} catch (CoreException e) {
					e.printStackTrace();
				}
				allChanges.add(classChange);
			}
			if(printClassFlag) {AnalysisUtils.debugPrint("--第"+j+++"个可能包含调用的类分析完毕-----------------------------%n");}
		}//所有的类分析完毕
		
		System.out.println("重构前（"+maybeRefactoringNode+"个）：ExecuteRunnable:"+flagMaybeMap.get("ExecuteRunnable")+"个；   SubmitCallable:"+flagMaybeMap.get("SubmitCallable")+"个；   SubmitRunnable:"+
				flagMaybeMap.get("SubmitRunnable")+"个；   SubmitRunnableNValue:"+flagMaybeMap.get("SubmitRunnableNValue"));
		
		System.out.println("重构成功（"+(flagMap.get("ExecuteRunnable")+flagMap.get("SubmitCallable")+flagMap.get("SubmitRunnable")+flagMap.get("SubmitRunnableNValue"))+"个）：ExecuteRunnable:"+
		flagMap.get("ExecuteRunnable")+"个；   SubmitCallable:"+flagMap.get("SubmitCallable")+"个；   SubmitRunnable:"+flagMap.get("SubmitRunnable")+"个；   SubmitRunnableNValue:"+flagMap.get("SubmitRunnableNValue"));
		
		System.out.println("重构失败（"+(methodOverload+FutureCanot+FutureCanotI+FutureCanotC+illExecutor+illExecutorService+useCancelTrue)+"个）： 提交方法重载："+methodOverload+
				"个；   Future变量声明类型不是Future接口"+FutureCanot+"个；   Future变量调用instanceof"+FutureCanotI+"个；   Future变量强制类型转换："+FutureCanotC+
		"个；\n            execute执行器类型不安全："+illExecutor+"个；    submit执行器类型不安全："+illExecutorService+"个；   因调用cancel(true)不能重构的个数为："+useCancelTrue+"个。");
		System.out.println("Pointo未命中："+debugUsePoint2num);
	}
	

	/*
	 * es.execute(r);
	 * CompletableFuture.runAsync(r, es);
	 */
	private static void refactorExecuteRunnable(MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
			//得到execute方法的调用Node。
    	AST ast = invocationNode.getAST();
    	ASTRewrite rewriter = ASTRewrite.create(ast);
    	//重构逻辑
    	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
    	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
    	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
    	Expression expression = invocationNode.getExpression();
    	if(expression == null) {
    		listRewriter.insertLast(ast.newThisExpression(), null);
    	}else {
    		listRewriter.insertLast(expression, null);
    	}
    	TextEdit edits = rewriter.rewriteAST();
    	if(change.getEdit() == null) {
        	change.setEdit(edits);
    	}else {
        	change.addEdit(edits);
    	}

		AnalysisUtils.debugPrint("[refactorExecuteRunnable]refactor success!");
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
	private static void refactorffSubmitCallable(MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu,int[] invocNum) throws JavaModelException, IllegalArgumentException {
		int flag = 0; //assignment = 1 ; Fragment = 2; ExpressionStatement = 3; MethodInvocation = 4
		AST ast = null;
		Assignment invocAssignment = null;
		VariableDeclarationFragment invocFragment = null;
		ExpressionStatement expressionStatement = null;
		MethodInvocation methodInvocation = null;
		ReturnStatement returnStatement = null;
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
		}else if(invocationNode.getParent() instanceof ReturnStatement ) {
			flag = 5;
			returnStatement = (ReturnStatement)invocationNode.getParent();
			ast = returnStatement.getAST();
		}
		if (ast == null) {
			return;
		}
    	ASTRewrite rewriter = ASTRewrite.create(ast);
    	//重构逻辑
    	//一、CompletableFuture.supplyAsync(Supplier,Executor)
    	MethodInvocation invocationSup = ast.newMethodInvocation();
    	rewriter.set(invocationSup, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
    	rewriter.set(invocationSup, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("supplyAsync"), null);

    	Object callableObject = invocationNode.arguments().get(0);
    	LambdaExpression lambdaExpFirst = ast.newLambdaExpression();
    	if(callableObject instanceof LambdaExpression) {
    		/*原本:
    		    ()->{ele};/
    		    ()->exp;
    		  -->>
    			()->{try{ele}catch (Exception e){runtimeException(e);}}/
    			()->{try{exp}catch (Exception e){runtimeException(e);}}
    		*/
    		
    		//得到{ele}
			LambdaExpression lambdaExpression = (LambdaExpression)callableObject;
			ASTNode lambdaBody = lambdaExpression.getBody();
			Block blockFirst;
			if(lambdaBody instanceof Expression) {
				// return exp;
				ReturnStatement reExp = ast.newReturnStatement();
	        	rewriter.set(reExp, ReturnStatement.EXPRESSION_PROPERTY, lambdaBody, null);
				blockFirst = ast.newBlock();
				ListRewrite tryBlockFirstListRewrite = rewriter.getListRewrite(blockFirst, Block.STATEMENTS_PROPERTY);
	        	tryBlockFirstListRewrite.insertLast(reExp, null);
			}else {
				blockFirst = (Block) lambdaBody;
			}
	
	    	//try {ele}
	    	TryStatement tryFirst = ast.newTryStatement();
	    	rewriter.set(tryFirst, TryStatement.BODY_PROPERTY, blockFirst, null);
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
	                    ele
	                } catch (Exception e) {
	                    throw new RuntimeException(e);
	                }
	    	 */
	    	ListRewrite listRewriterTry = rewriter.getListRewrite(tryFirst, TryStatement.CATCH_CLAUSES_PROPERTY);
	    	listRewriterTry.insertLast(catchClause, null);
	    	
	    	// ()->{try}
	    	Block lambdaBlockFirst = ast.newBlock();
	    	ListRewrite lambdaBlockFirstListRewrite = rewriter.getListRewrite(lambdaBlockFirst, Block.STATEMENTS_PROPERTY);
	    	lambdaBlockFirstListRewrite.insertLast(tryFirst, null);
	    	//()->{}
            rewriter.set(lambdaExpFirst, LambdaExpression.BODY_PROPERTY, lambdaBlockFirst, null);
	//        		lambdaExpFirst = ((LambdaExpression)callableObject);
    	}else {
		/*
		 * 	()->{
				                try {
				                    return call.call();
				                } catch (Exception e) {
				                    throw new RuntimeException(e);
				                }
				            }
		 */
		
    	// callable.call()；
    		
    		
            MethodInvocation invocCall = ast.newMethodInvocation();
			if(callableObject instanceof SimpleName) {
				rewriter.set(invocCall, MethodInvocation.EXPRESSION_PROPERTY, callableObject, null);
        	}else {
    			//其他情况需要增加一行代码，防止final问题。
    			ASTNode stmtNode = (ASTNode)invocationNode;
    			while (!(stmtNode instanceof Statement)) {
    				stmtNode = stmtNode.getParent();
    			}
    			AST blockAst;
    			ASTNode blockNode =AnalysisUtils.getBlockNode (invocationNode);
    			if(blockNode == null) {throw new RefutureException(invocationNode);}
    			blockAst = blockNode.getAST();
    			VariableDeclarationFragment vdf = blockAst.newVariableDeclarationFragment();
    			String callableName = "callable$Rf$"+invocNum[0]++;
    			ASTRewrite blockRewriter = ASTRewrite.create(blockAst);
    			blockRewriter.set(vdf, VariableDeclarationFragment.NAME_PROPERTY, blockAst.newSimpleName(callableName), null);
    			blockRewriter.set(vdf, VariableDeclarationFragment.INITIALIZER_PROPERTY, callableObject, null);
    			VariableDeclarationStatement callableStmt = blockAst.newVariableDeclarationStatement(vdf);
    			blockRewriter.set(callableStmt, VariableDeclarationStatement.TYPE_PROPERTY, blockAst.newSimpleType(blockAst.newSimpleName("Callable")), null);
    			ListRewrite blockListRewrite = blockRewriter.getListRewrite(blockNode, Block.STATEMENTS_PROPERTY);
    			blockListRewrite.insertBefore(callableStmt, stmtNode, null);
    			TextEdit edits = blockRewriter.rewriteAST();
            	if(change.getEdit() == null) {
                	change.setEdit(edits);
            	}else {
                	change.addEdit(edits);
            	}
    			rewriter.set(invocCall, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName(callableName), null);
        	}
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
        	rewriter.set(lambdaExpFirst, LambdaExpression.BODY_PROPERTY, lambdaBlockFirst, null);
    	}
    	//CompletableFuture.supplyAsync(Supplier,Executor)
    	ListRewrite listRewriteIvocFirst = rewriter.getListRewrite(invocationSup, MethodInvocation.ARGUMENTS_PROPERTY);
    	listRewriteIvocFirst.insertLast(lambdaExpFirst, null);
    	Expression expression = invocationNode.getExpression();
    	if(expression == null) {
    		listRewriteIvocFirst.insertLast(ast.newThisExpression(), null);
    	}else {
    		listRewriteIvocFirst.insertLast(expression, null);
    	}
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
    	//cf$Rf$.completeExceptionally(runex$Rf$.getCause())
    	MethodInvocation invocationObtrudeExp = ast.newMethodInvocation();
    	rewriter.set(invocationObtrudeExp, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("cf$Rf$"), null);
    	rewriter.set(invocationObtrudeExp, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("completeExceptionally"), null);
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
    	//cf$Rf$.complete(o$Rf$);
    	MethodInvocation ivocationObtrudeValue = ast.newMethodInvocation();
    	rewriter.set(ivocationObtrudeValue, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("cf$Rf$"), null);
    	rewriter.set(ivocationObtrudeValue, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("complete"), null);
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
    	}else if(flag == 2){
        	rewriter.set(invocFragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, invocationCompose, null);
    	}else if(flag == 3){
    		rewriter.set(expressionStatement, ExpressionStatement.EXPRESSION_PROPERTY, invocationCompose, null);
    	}else if(flag == 4){
    		if(methodInvocation.getExpression() == invocationNode) {
    			rewriter.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, invocationCompose, null);
    		}else if(methodInvocation.arguments().contains(invocationNode)) {
    			ListRewrite invocationArguRewrite = rewriter.getListRewrite(methodInvocation, MethodInvocation.ARGUMENTS_PROPERTY);
    			invocationArguRewrite.replace(invocationNode, invocationCompose, null);
    		}else {
    			throw new ExceptionInInitializerError("parent:"+methodInvocation+"currentInvocation:"+invocationNode);
    		}
    	}else if(flag == 5) {
    		rewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, invocationCompose, null);
    	}else {
    		throw new IllegalArgumentException(new Integer(flag).toString());
    	}
	    	
    	TextEdit edits = rewriter.rewriteAST();
    	if(change.getEdit() == null) {
        	change.setEdit(edits);
    	}else {
        	change.addEdit(edits);
    	}
 
		AnalysisUtils.debugPrint("[refactorffSubmitCallable]refactor success!");
	}
	
	/*
	 * Future f = es.submit(()->{});
	 * Future f = CompletableFuture.runAsync(()->{},es);
	 */
	private static void refactorSubmitRunnable(MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
    	AST ast = invocationNode.getAST();
    	ASTRewrite rewriter = ASTRewrite.create(ast);
    	//重构逻辑
    	rewriter.set(invocationNode, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("CompletableFuture"), null);
    	rewriter.set(invocationNode, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("runAsync"), null);
    	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
    	Expression expression = invocationNode.getExpression();
    	if(expression instanceof ArrayAccess) {
    		ArrayAccess oldArrayAcc = (ArrayAccess)expression;
    		ArrayAccess arrayAcc = ast.newArrayAccess();
    		rewriter.set(arrayAcc, ArrayAccess.ARRAY_PROPERTY, oldArrayAcc.getArray(), null);
    		rewriter.set(arrayAcc, ArrayAccess.INDEX_PROPERTY, oldArrayAcc.getIndex(), null);
    		listRewriter.insertLast(arrayAcc, null);
    	}else if(expression == null){
    		listRewriter.insertLast(ast.newThisExpression(), null);
    	}else {
//        		Expression exp = invocationNode.getExpression();
//        		listRewriter.insertLast((Expression)ASTNode.copySubtree(ast, exp), null);
    		listRewriter.insertLast(expression, null);
    	}
    	TextEdit edits = rewriter.rewriteAST();
    	if(change.getEdit() == null) {
        	change.setEdit(edits);
    	}else {
        	change.addEdit(edits);
    	}
    	
		AnalysisUtils.debugPrint("[refactorSubmitRunnable]refactor success!");
	}
	
	/*
	 * 		Future f = es.submit(r, value);
	 * 
		 7.25:
	 *	 first: Callable f$rf$ = Executors.callable(r, v);
		 Future f = es.submit(Executors.callable(r,v));
		 ->submit(callable)->CaompletableFuture;
		 
	 * 
	 */
	private static void refactorSubmitRunnableNValue(MethodInvocation invocationNode, TextFileChange change, ICompilationUnit cu) throws JavaModelException, IllegalArgumentException {
		AST ast = invocationNode.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		MethodInvocation newMiv = ast.newMethodInvocation();
    	rewriter.set(newMiv, MethodInvocation.EXPRESSION_PROPERTY, ast.newSimpleName("Executors"), null);
    	rewriter.set(newMiv, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("callable"),null);
    	ListRewrite  newListRewriter = rewriter.getListRewrite(newMiv, MethodInvocation.ARGUMENTS_PROPERTY);
    	newListRewriter.insertLast((ASTNode)invocationNode.arguments().get(0),null);
    	newListRewriter.insertLast((ASTNode)invocationNode.arguments().get(1),null);
    	ListRewrite listRewriter = rewriter.getListRewrite(invocationNode, MethodInvocation.ARGUMENTS_PROPERTY);
    	listRewriter.replace((ASTNode)invocationNode.arguments().get(0), newMiv, null);
    	listRewriter.remove((ASTNode)invocationNode.arguments().get(1), null);
    	TextEdit edits = rewriter.rewriteAST();
    	if(change.getEdit() == null) {
        	change.setEdit(edits);
    	}else {
        	change.addEdit(edits);
    	}

   	 	AnalysisUtils.debugPrint("[refactorSubmitRunnableNValue]refactor success!");
	}
	
	public static List<Change>  getallChanges() {
		return allChanges;
	}
	public static boolean futureType(MethodInvocation mInvoc) {
		ASTNode astNode = (ASTNode) mInvoc;
		ASTNode parentNode = astNode.getParent();
		Stmt stmt = AdaptAst.getJimpleStmt(mInvoc);
		if(parentNode instanceof MethodInvocation) {
			MethodInvocation parentInvocation = (MethodInvocation)parentNode;
			if(parentInvocation.getExpression() == mInvoc) {
				return true;
			}else if(parentInvocation.arguments().contains(mInvoc)) {
				for(ITypeBinding paraTypeBinding : parentInvocation.resolveMethodBinding().getParameterTypes()) {
					if(paraTypeBinding.getErasure().getName().equals("Future")) {
						if(Instanceof.useInstanceofFuture(stmt)||CastAnalysis.useCast(stmt)) {
							return false;
						}
						return true;
					}
				}
				throw new RefutureException(mInvoc);
			}
		}else if(parentNode instanceof ExpressionStatement) {
			return true;
		}else if(parentNode instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment parentDeclarationFragment = (VariableDeclarationFragment)parentNode;
			VariableDeclarationStatement parentDeclarationStatement = (VariableDeclarationStatement)parentDeclarationFragment.getParent();
			if(parentDeclarationStatement.getType().resolveBinding().getErasure().getName().equals("Future")) {
				if(Instanceof.useInstanceofFuture(stmt)||CastAnalysis.useCast(stmt)) {
					return false;
				}
				return true;
			}else {
				FutureCanot++;
				return false;
			}
		}else if (parentNode instanceof ReturnStatement ) {
			while (!(parentNode instanceof TypeDeclaration)) {
				if (parentNode instanceof MethodDeclaration) {
					break;
				}else if(parentNode instanceof LambdaExpression) {
					break;
				}
				parentNode = parentNode.getParent();
			}
			if(parentNode == null) {
//				return false;
				throw new RefutureException(mInvoc);
			}
			if(parentNode instanceof MethodDeclaration) {
				MethodDeclaration md = (MethodDeclaration) parentNode;
				if(md.getReturnType2().resolveBinding().getErasure().getName().equals("Future")) {
					if(Instanceof.useInstanceofFuture(stmt)||CastAnalysis.useCast(stmt)) {
						return false;
					}
					return true;
				}
			}else if(parentNode instanceof LambdaExpression) {
				LambdaExpression le = (LambdaExpression)parentNode;
				if(le.resolveMethodBinding().getReturnType().getErasure().getName().equals("Future")) {
					if(Instanceof.useInstanceofFuture(stmt)||CastAnalysis.useCast(stmt)) {
						return false;
					}
					return true;
				}
			}else {
				throw new RefutureException(mInvoc);
			}
		}else if(parentNode instanceof Assignment) {
			Assignment parentAssignment = (Assignment)parentNode;
			if(parentAssignment.getLeftHandSide().resolveTypeBinding().getErasure().getName().equals("Future")) {
				if(Instanceof.useInstanceofFuture(stmt)||CastAnalysis.useCast(stmt)) {
					return false;
				}
				return true;
			}else {
				FutureCanot++;
				return false;
			}
		}
		return true;
	}	
	
}
