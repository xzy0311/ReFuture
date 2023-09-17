package refuture.refactoring;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import refuture.astvisitor.MethodInvocationVisiter;
import refuture.sootUtil.AdaptAst;
import refuture.sootUtil.ExecutorSubclass;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.jimple.Stmt;

//第二阶段实验，开始。0706
// 待添加，增加一个get是否在callable或者Runnable中。
public class ForTask {
	private static List<Change> allChanges;
	public static boolean initStaticField() {
		allChanges = new ArrayList<Change>();
		return true;
	}
	public static void refactor(List<ICompilationUnit> allJavaFiles) {
		List<SootClass> allFutureSubClasses = ForTask.getAllFutureAndItsSubClasses();
		System.out.println("【所有Future实现类及其子类：】"+allFutureSubClasses);
		int isDoneNumber = 0;
		int getNumber = 0;
		for(ICompilationUnit cu : allJavaFiles) {
			IFile source = (IFile) cu.getResource();
			TextFileChange change = new TextFileChange("ForTask",source);
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
			//开始逻辑
			List<MethodInvocation> futureIsDones = findIsDone(invocationNodes, allFutureSubClasses, change);
			List<MethodInvocation> futureGets =findGet(invocationNodes,allFutureSubClasses,change);
			List<MethodInvocation> EsSubmitOExecutes =ForTask.findEsSubmitOExecute(invocationNodes);
			//start in task.
			for(MethodInvocation invocationNode:futureGets) {
				getNumber++;
				if(ForTask.inSubmitExecuteArg(invocationNode)) {
					System.out.printf("❤[ForTask:getInCallable]这个get可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(invocationNode).resolveBinding().getBinaryName(),
							astUnit.getLineNumber(invocationNode.getStartPosition()));
				}
			}
			for(MethodInvocation invocationNode:futureIsDones) {
				isDoneNumber++;
				if(ForTask.inSubmitExecuteArg(invocationNode)) {
					System.out.printf("❤[ForTask:getInCallable]这个isDone可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(invocationNode).resolveBinding().getBinaryName(),
							astUnit.getLineNumber(invocationNode.getStartPosition()));
				}
			}
//			//end in task
//			//start not in task 
//			for(MethodInvocation getNode:futureGets) {
//				Block getBlock = AnalysisUtils.getBlockNode(getNode);
//				isDoneNumber++;
//				for(MethodInvocation esNode : EsSubmitOExecutes) {
//					ASTNode astNode = (ASTNode)esNode;
//					Block oldBlock = null;
//					Block esBlock = null;
//					while(true) {
//						if(astNode instanceof TypeDeclaration) {
//							TypeDeclaration type = (TypeDeclaration)astNode;
//							if(type.resolveBinding().isTopLevel() ) {
//								break;
//							}
//						}
//						if(astNode instanceof MethodDeclaration) {
//							break;
//						}
//						oldBlock = esBlock;
//						esBlock = AnalysisUtils.getBlockNode(astNode);
//						if(esBlock == null) {
//							System.out.println("错误错误，获得esBlock为Null");
//							break;
//						}
//						if(getBlock.equals(esBlock)) {
//							System.out.printf("※[ForTask:get&Submit]这个get可能可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(getNode).resolveBinding().getBinaryName(),astUnit.getLineNumber(getNode.getStartPosition()));
//							if(astUnit.getLineNumber(getNode.getStartPosition()) < astUnit.getLineNumber(esNode.getStartPosition())) {
//								System.out.printf("❤[ForTask:get&Submit]这个get可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(getNode).resolveBinding().getBinaryName(),
//										astUnit.getLineNumber(getNode.getStartPosition()));
//							}
//						}
//						astNode = astNode.getParent();
//					}
//				}
//			}//end get()&submit()；
//			
//			for(MethodInvocation IsDonesNode:futureIsDones) {
//				Block isDoneBlock = AnalysisUtils.getBlockNode(IsDonesNode);
//				getNumber++;
//				for(MethodInvocation esNode : EsSubmitOExecutes) {
//					ASTNode astNode = (ASTNode)esNode;
//					Block oldBlock = null;
//					Block esBlock = null;
//					while(true) {
//						if(astNode instanceof TypeDeclaration) {
//							TypeDeclaration type = (TypeDeclaration)astNode;
//							if(type.resolveBinding().isTopLevel() ) {
//								break;
//							}
//						}
//						if(astNode instanceof MethodDeclaration) {
//							break;
//						}
//						oldBlock = esBlock;
//						esBlock = AnalysisUtils.getBlockNode(astNode);
//						if(esBlock == null) {
//							System.out.println("错误错误，获得esBlock为Null");
//							break;
//						}
//						if(isDoneBlock.equals(esBlock)) {
//							System.out.printf("※[ForTask:get&Submit]这个isDone可能可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(IsDonesNode).resolveBinding().getBinaryName(),astUnit.getLineNumber(IsDonesNode.getStartPosition()));
//							if(astUnit.getLineNumber(IsDonesNode.getStartPosition()) < astUnit.getLineNumber(esNode.getStartPosition())) {
//								System.out.printf("❤[ForTask:get&Submit]这个isDone可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(IsDonesNode).resolveBinding().getBinaryName(),
//										astUnit.getLineNumber(IsDonesNode.getStartPosition()));
//							}
//						}
//						astNode = astNode.getParent();
//					}
//				}
//			}//end isdone()&submit()
			
		}
		System.out.println("该项目中所有的isdone()数为："+isDoneNumber);
		System.out.println("该项目中所有的get()数为："+getNumber);
	}
	
	public static List<MethodInvocation> findIsDone(List<MethodInvocation> invocationNodes, List<SootClass> allFutureSubClasses, TextFileChange change) {
		List<MethodInvocation> futureIsDone = new ArrayList<MethodInvocation>();
		for(MethodInvocation invocationNode:invocationNodes) {
			if(invocationNode.getName().toString().equals("isDone")) {
				if(invocationNode.getExpression()==null) {
					continue;
				}
				String a =invocationNode.getExpression().resolveTypeBinding().getBinaryName();
		        int index = a.indexOf("<");
		        // 如果找到了"<"符号，则删除该符号及之后的所有字符
		        if (index != -1) {
		            a = a.substring(0, index);
		        }
				SootClass sootClass = Scene.v().getSootClass(a);
				if(sootClass.isPhantom()) {continue;}
				if(allFutureSubClasses.contains(sootClass)) {
					AnalysisUtils.debugPrint("[ForTask.findIsDone;BinaryName:]"+a);
					futureIsDone.add(invocationNode);
				}
			}			
		}
		return futureIsDone;
	}
	
	/**
	 * //增加一个找异步任务中存在get()的情况。第一步：找到get()，判断调用者对象是否是Future或者它的子类。第二步：输出定义它所在的类以及所有的父类。
	 * @param allFutureSubClasses 
	 * @param change 
	 */
	public static List<MethodInvocation> findGet(List<MethodInvocation> invocationNodes, List<SootClass> allFutureSubClasses, TextFileChange change) {
		List<MethodInvocation> futureGet = new ArrayList<MethodInvocation>();
		for(MethodInvocation invocationNode:invocationNodes) {
			if(invocationNode.getName().toString().equals("get")) {
				if(invocationNode.getExpression()==null) {
					continue;
				}
				String a =invocationNode.getExpression().resolveTypeBinding().getBinaryName();
		        int index = a.indexOf("<");
		        // 如果找到了"<"符号，则删除该符号及之后的所有字符
		        if (index != -1) {
		            a = a.substring(0, index);
		        }
				SootClass sootClass = Scene.v().getSootClass(a);
				if(sootClass.isPhantom()) {continue;}
				if(allFutureSubClasses.contains(sootClass)) {
					AnalysisUtils.debugPrint("[ForTask.findGet;BinaryName:]"+a);
					futureGet.add(invocationNode);
				}
			}
		}
		return futureGet;
	}
	
	public static List<MethodInvocation> findEsSubmitOExecute(List<MethodInvocation> invocationNodes){
		List<MethodInvocation> futureSOE = new ArrayList<MethodInvocation>();
		for(MethodInvocation method:invocationNodes) {
			if(method.getName().toString().equals("submit")||method.getName().toString().equals("execute")) {
				if(!AnalysisUtils.receiverObjectIsComplete(method)) {
					continue;
				}
				Stmt invocStmt = AdaptAst.getJimpleInvocStmt(method);
				boolean returnValue;
				if(method.getName().toString().equals("submit")) {
					returnValue = ExecutorSubclass.canRefactor(method,invocStmt,true);
				}else {
					returnValue = ExecutorSubclass.canRefactor(method,invocStmt,false);
				}
				if(returnValue) {
					futureSOE.add(method);
				}
			}
		}
		return futureSOE;
	}


	public static boolean inSubmitExecuteArg(ASTNode node) {
		while(true) {
			if(node instanceof TypeDeclaration) {
				TypeDeclaration type = (TypeDeclaration)node;
				if(type.resolveBinding().isTopLevel() ) {
					break;
				}
			}
			if(node instanceof MethodInvocation) {
				MethodInvocation method = (MethodInvocation)node;
				if(method.getName().toString().equals("submit")||method.getName().toString().equals("execute")) {
					Expression exp = method.getExpression();
					if(exp==null){
						continue;
					}
					Stmt invocStmt = AdaptAst.getJimpleInvocStmt(method);
					boolean returnValue = ExecutorSubclass.canRefactor(method,invocStmt,false);
					if(returnValue) {
						return true;
					}
				}
			}
			node = node.getParent();
		}
		return false;
	}
	
	private static List<SootClass> getAllFutureAndItsSubClasses(){
		SootClass futureSootClass = Scene.v().getSootClass("java.util.concurrent.Future");
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		List<SootClass> futureImpClasses =hierarchy.getImplementersOf(futureSootClass);
		List<SootClass> allFutureSubclasses = new ArrayList<SootClass>();
		for(SootClass sc : futureImpClasses) {
			allFutureSubclasses.addAll(hierarchy.getSubclassesOfIncluding(sc));
		}
		allFutureSubclasses.addAll(hierarchy.getSubinterfacesOfIncluding(futureSootClass));
		return allFutureSubclasses;
	}
	
	public static Collection<? extends Change> getallChanges() {
		return allChanges;
	}
}
