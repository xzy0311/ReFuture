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
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
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
			ForTask.FindGet(invocationNodes,allFutureSubClasses,change);
		}
	}
	/**
	 * //增加一个找异步任务中存在get()的情况。第一步：找到get()，判断调用者对象是否是Future或者它的子类。第二步：输出定义它所在的类以及所有的父类。
	 * @param allFutureSubClasses 
	 * @param change 
	 */
	public static void FindGet(List<MethodInvocation> invocationNodes, List<SootClass> allFutureSubClasses, TextFileChange change) {

		for(MethodInvocation invocationNode:invocationNodes) {
			if(invocationNode.getName().toString().equals("get")) {
				TypeDeclaration outTD = AnalysisUtils.getTypeDeclaration4node(invocationNode);
				CompilationUnit astUnit = (CompilationUnit)outTD.getRoot();
				System.out.printf("[ForTask:FindGet]找到get,它在%s,行号为%d%n",outTD.resolveBinding().getBinaryName(),
						astUnit.getLineNumber(invocationNode.getStartPosition()));
				if(invocationNode.getExpression()==null) {
					System.out.println("	[ForTask:FindGet]这个没有接收器对象，排除");
					continue;
				}
				String a =invocationNode.getExpression().resolveTypeBinding().getBinaryName();
		        int index = a.indexOf("<");
		        // 如果找到了"<"符号，则删除该符号及之后的所有字符
		        if (index != -1) {
		            a = a.substring(0, index);
		        }
				System.out.println("	[ForTask:FindGet]:接收器对象类型:"+a);
				SootClass sootClass = Scene.v().getSootClass(a);
				if(sootClass.isPhantom()) {throw new NullPointerException(a);}
				if(allFutureSubClasses.contains(sootClass)) {
					System.out.println("※[ForTask:FindGet]上面这个get是Future.get(),LOC是："+
							astUnit.getLineNumber(invocationNode.getStartPosition()));
					if(ForTask.inSubmitInvocations(invocationNode)) {
						System.out.println("❤[ForTask:FindGet]这个get可以进行重构！");
					}
					allChanges.add(change);
				}else {
					System.out.println("	[ForTask:FindGet]:不是Future的get,排除");
				}
				
				
				
				
			}
		}
	}


	public static boolean inSubmitInvocations(ASTNode node) {
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
					Stmt invocStmt = AdaptAst.getJimpleInvocStmt(method);
					if(ExecutorSubclass.canRefactor(invocStmt)) {
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
