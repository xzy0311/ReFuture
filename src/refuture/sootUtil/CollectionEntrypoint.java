package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import refuture.astvisitor.MethodInvocationVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.Future2Completable;
import refuture.refactoring.RefutureException;
import soot.SootMethod;

public class CollectionEntrypoint {
	public static Set<SootMethod> entryPointSet;
	public static HashMap<ICompilationUnit,List<MethodInvocation>> invocNodeMap;
	public static void initStaticField() {
		entryPointSet = new HashSet<>();
		invocNodeMap =  new HashMap<>();
	}
	public static void entryPointInit(List<ICompilationUnit> allJavaFiles){
		List<MethodInvocation> allTaskPointList = new ArrayList<MethodInvocation>();
		//调用cancel的方法定义(可选）
		//调用submit/execute的方法定义|submit得到的定义变量可替代cancel的方法定义。
		for(CompilationUnit astUnit : AnalysisUtils.allAST) {
			List<MethodInvocation> taskPointList = new ArrayList<MethodInvocation>();
			ICompilationUnit cu = (ICompilationUnit) astUnit.getJavaElement();
			MethodInvocationVisiter miv = new MethodInvocationVisiter();
			astUnit.accept(miv);
			List<MethodInvocation> invocationNodes = miv.getResult();
			for(MethodInvocation invocationNode:invocationNodes) {
				List<Expression> arguExps =  invocationNode.arguments();
				if(!invocationNode.getName().toString().equals("execute")&&!invocationNode.getName().toString().equals("submit")) continue;
				if(arguExps.size() == 0) continue;
				boolean isTask = false;
				for(Expression arguExp : arguExps) {
					String arguTypeName =AnalysisUtils.getTypeName4Exp(arguExp);
					if(arguTypeName == null) {
						throw new RefutureException(invocationNode,"得不到类型绑定");
					}else if(arguTypeName == "java.lang.Object") {throw new RefutureException(invocationNode,"得到了object");}
					if(ExecutorSubclass.callableSubClasses.contains(arguTypeName)|| ExecutorSubclass.runnablesubClasses.contains(arguTypeName)) {
						isTask = true;
					}
				}
				if(isTask) {Future2Completable.maybeRefactoringNode++;}else {continue;}
				
				//到这里,invocation是submit/execute(task...);
				Expression exp = invocationNode.getExpression();
				Set <String> allSubNames = ExecutorSubclass.getAllExecutorSubClassesName();
				Set <String> allSubServiceNames = ExecutorSubclass.getAllExecutorServiceSubClassesName();
				if(exp==null){
					AnalysisUtils.debugPrint("[AnalysisUtils.receiverObjectIsComplete]receiverObject为this，继续重构。");
					// 判断invocationNode所在类是否是子类，若是子类，则任务提交点+1.
					ASTNode aboutTypeDeclaration = (ASTNode) invocationNode;
					while(!(aboutTypeDeclaration instanceof TypeDeclaration)&&!(aboutTypeDeclaration instanceof AnonymousClassDeclaration)) {
						aboutTypeDeclaration = aboutTypeDeclaration.getParent();
					}
					String typeName = null;
					if(aboutTypeDeclaration instanceof TypeDeclaration) {
						TypeDeclaration td = (TypeDeclaration)aboutTypeDeclaration;
						ITypeBinding tdBinding = td.resolveBinding();
						typeName = tdBinding.getQualifiedName();
						if(tdBinding.isNested()) {
							typeName = tdBinding.getBinaryName();
						}
					}else if(aboutTypeDeclaration instanceof AnonymousClassDeclaration) {
						AnonymousClassDeclaration acd = (AnonymousClassDeclaration)aboutTypeDeclaration;
						ITypeBinding tdBinding = acd.resolveBinding();
						typeName = tdBinding.getQualifiedName();
						if(tdBinding.isNested()) {
							typeName = tdBinding.getBinaryName();
						}
					}else {
						throw new RefutureException(invocationNode,"迭代,未得到类型定义或者匿名类定义");
					}
					typeName = typeName.replaceAll("<[^>]*>", "");
					if(invocationNode.getName().toString().equals("execute")&&allSubNames.contains(typeName)) {
						Future2Completable.canRefactoringNode++;
						taskPointList.add(invocationNode);
					}else if(invocationNode.getName().toString().equals("submit")&&allSubServiceNames.contains(typeName)) {
						Future2Completable.canRefactoringNode++;
						taskPointList.add(invocationNode);
					}else if(typeName == "java.lang.Object") {
						throw new RefutureException(invocationNode,"typeBinding为Object,精度不够");
					}else {
						Future2Completable.useNotExecutorSubClass++;
					}
					continue;
				}
				String typeName = AnalysisUtils.getTypeName4Exp(exp);
				if(typeName==null){
					throw new RefutureException(invocationNode,"typeBinding为null，应该是eclipse环境下的源码没有调试好，这里卡");
				}
				if(invocationNode.getName().toString().equals("execute")&&(allSubNames.contains(typeName))) {
					Future2Completable.canRefactoringNode++;
					AnalysisUtils.debugPrint("[AnalysisUtils.receiverObjectIsComplete]初步ast判定可以,这里不卡");
					taskPointList.add(invocationNode);
				}
				else if(invocationNode.getName().toString().equals("submit")&&(allSubServiceNames.contains(typeName))) {
					Future2Completable.canRefactoringNode++;
					AnalysisUtils.debugPrint("[AnalysisUtils.receiverObjectIsComplete]ast判定可以,这里不卡");
					taskPointList.add(invocationNode);
				}else if(typeName == "java.lang.Object") {
					throw new RefutureException(invocationNode,"typeBinding为Object,精度不够");
				}else{
					AnalysisUtils.debugPrint("[AnalysisUtils.receiverObjectIsComplete]ast判定这个调用对象的类不是子类,这个对象的类型名为:"+typeName);
					Future2Completable.useNotExecutorSubClass++;
				}
			}
		invocNodeMap.put(cu, taskPointList);
		 allTaskPointList.addAll(taskPointList);
		}
		for(MethodInvocation node:allTaskPointList) {
			SootMethod sm = AdaptAst.getSootMethod4invocNode(node);
			if(AdaptAst.invocInLambda(node)>0) {
				sm = AdaptAst.getSootRealFunction4InLambda(node);
			}
			entryPointSet.add(sm);
		}
	}

		
		
	
	
}
