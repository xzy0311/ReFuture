package refuture.refactoring;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		SootClass futureSootClass = Scene.v().getSootClass("java.util.concurrent.Future");
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		List<SootClass> futureImpClasses =hierarchy.getImplementersOf(futureSootClass);
//		System.out.println("[ForTask:FindGet]futureSubClasses"+futureSubClasses);
		List<SootClass> futureSubClasses = hierarchy.getSubclassesOfIncluding(futureSootClass);
		for(MethodInvocation invocationNode:invocationNodes) {
			if(invocationNode.getName().toString().equals("get")) {
				TypeDeclaration outTD = AnalysisUtils.getTypeDeclaration4node(invocationNode);
				CompilationUnit astUnit = (CompilationUnit)outTD.getRoot();
				System.out.printf("[ForTask:FindGet]找到get,它在%s,行号为%d%n",outTD.resolveBinding().getBinaryName(),
						astUnit.getLineNumber(invocationNode.getStartPosition()));
				if(invocationNode.getExpression()==null) {
					System.out.println("[ForTask:FindGet]这个没有接收器对象，跳过");
					continue;
				}
				String a =invocationNode.getExpression().resolveTypeBinding().getBinaryName();
		        int index = a.indexOf("<");
		        // 如果找到了"<"符号，则删除该符号及之后的所有字符
		        if (index != -1) {
		            a = a.substring(0, index);
		        }
				System.out.println("[ForTask:FindGet]:ExpressionQualifiedName:"+a);
				SootClass sootClass = Scene.v().getSootClass(a);
				if(sootClass.isPhantom()) {throw new NullPointerException(a);}
				if(futureImpClasses.contains(sootClass)||futureSubClasses.contains(sootClass)) {
					System.out.println("[ForTask:FindGet=bingo=]上面这个get是Future.get(),LOC是："+
							astUnit.getLineNumber(invocationNode.getStartPosition()));
				}
			}
		}
	}
}
