package refuture.refactoring;

import java.util.List;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;

//第二阶段实验，开始。0706
public class ForTask {

	/**
	 * //增加一个找异步任务中存在get()的情况。第一步：找到get()，判断调用者对象是否是Future或者它的子类。第二步：输出定义它所在的类以及所有的父类。
	 */
	public static void FindGet(List<MethodInvocation> invocationNodes) {
		for(MethodInvocation invocationNode:invocationNodes) {
			if(invocationNode.getName().toString().equals("get")) {
				TypeDeclaration outTD = AnalysisUtils.getTypeDeclaration4node(invocationNode);
				CompilationUnit astUnit = (CompilationUnit)outTD.getRoot();
				System.out.printf("[ForTask:FindGet]找到get,它在%s,行号为%d%n",outTD.resolveBinding().getQualifiedName(),
						astUnit.getLineNumber(invocationNode.getStartPosition()));
				
				if(invocationNode.getExpression()==null) {
					System.out.println("[ForTask:FindGet]这个没有接收器对象，跳过");
					continue;
				}
				String a =invocationNode.getExpression().resolveTypeBinding().getQualifiedName();
				SootClass sootClass = Scene.v().getSootClass(a);
				SootClass futureSootClass = Scene.v().getSootClass("java.util.concurrent.Future");
				Hierarchy hierarchy = Scene.v().getActiveHierarchy();
				List<SootClass> futureSubClasses =hierarchy.getImplementersOf(futureSootClass);
				
				if(futureSubClasses.contains(sootClass)||futureSootClass.equals(sootClass)) {
					System.out.println("[ForTask:FindGet=bingo=]上面这个get是Future.get(),LOC是："+
							astUnit.getLineNumber(invocationNode.getStartPosition()));
				}
			}
		}
	}
}
