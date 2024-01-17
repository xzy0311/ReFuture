package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import refuture.astvisitor.MethodInvocationVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.Future2Completable;
import refuture.refactoring.RefutureException;
import soot.jimple.Stmt;

public class CollectionEntrypoint {
	public static HashMap<ICompilationUnit,List<MethodInvocation>> invocNodeMap;
	public static void initStaticField() {
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
				
				if(invocationNode.getName().toString().equals("execute")) {
					if(arguExps.size() != 1) {
						AnalysisUtils.debugPrint("[entryPointInit]:execute的参数不为1排除");
						Future2Completable.methodOverload++;
						continue;
					}
				}else if(invocationNode.getName().toString().equals("submit")) {
					if(arguExps.size() == 0||arguExps.size()>2) {
						AnalysisUtils.debugPrint("[entryPointInit]:参数个数为0或大于2execute的参数不为1排除");
						Future2Completable.methodOverload++;
						continue;
					}
					if(!futureType(invocationNode)) { continue;}
				}

				//到这里,invocation是submit/execute(task...);
				Expression exp = invocationNode.getExpression();
				Set <String> allSubNames = ExecutorSubclass.getAllExecutorSubClassesName();
				Set <String> allSubServiceNames = ExecutorSubclass.getAllExecutorServiceSubClassesName();
				if(exp==null){
					// 判断invocationNode所在类是否是子类，若是子类，则任务提交点+1.
					AnalysisUtils.debugPrint("[entryPointInit]receiverObject为this，继续判断");
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
						AnalysisUtils.debugPrint("[entryPointInit]receiverObject为this，判断失败，当前类不是子类。typename:"+typeName);
					}
					continue;
				}
				String typeName = AnalysisUtils.getTypeName4Exp(exp);
				if(typeName==null){
					throw new RefutureException(invocationNode,"typeBinding为null，应该是eclipse环境下的源码没有调试好，这里卡");
				}
				if(invocationNode.getName().toString().equals("execute")&&(allSubNames.contains(typeName))) {
					Future2Completable.canRefactoringNode++;
					AnalysisUtils.debugPrint("[entryPointInit]初步ast判定可以,这里不卡");
					taskPointList.add(invocationNode);
				}
				else if(invocationNode.getName().toString().equals("submit")&&(allSubServiceNames.contains(typeName))) {
					Future2Completable.canRefactoringNode++;
					AnalysisUtils.debugPrint("[entryPointInit]ast判定可以,这里不卡");
					taskPointList.add(invocationNode);
				}else if(typeName == "java.lang.Object") {
					throw new RefutureException(invocationNode,"typeBinding为Object,精度不够");
				}else{
					AnalysisUtils.debugPrint("[entryPointInite]ast判定这个调用对象的类不是子类,这个对象的类型名为:"+typeName);
					Future2Completable.useNotExecutorSubClass++;
				}
			}
		invocNodeMap.put(cu, taskPointList);
		 allTaskPointList.addAll(taskPointList);
		}
		}
		
	private static boolean futureType(MethodInvocation mInvoc) {
		ASTNode astNode = (ASTNode) mInvoc;
		ASTNode parentNode = astNode.getParent();
		Stmt stmt = AdaptAst.getJimpleInvocStmt(mInvoc);
		if(parentNode instanceof MethodInvocation) {
			MethodInvocation parentInvocation = (MethodInvocation)parentNode;
			if(parentInvocation.getExpression() == mInvoc) {
				return true;
			}else if(parentInvocation.arguments().contains(mInvoc)) {
				for(ITypeBinding paraTypeBinding : parentInvocation.resolveMethodBinding().getParameterTypes()) {
					if(paraTypeBinding.getErasure().getName().equals("Future")) {
						if(Instanceof.useInstanceofFuture(stmt)) {
							return false;
						}
						return true;
					}
				}
				return false;
			}
		}else if(parentNode instanceof ExpressionStatement) {
			return true;
		}else if(parentNode instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment parentDeclarationFragment = (VariableDeclarationFragment)parentNode;
			VariableDeclarationStatement parentDeclarationStatement = (VariableDeclarationStatement)parentDeclarationFragment.getParent();
			if(parentDeclarationStatement.getType().resolveBinding().getErasure().getName().equals("Future")) {
				if(Instanceof.useInstanceofFuture(stmt)) {
					return false;
				}
				return true;
			}else {
				return false;
			}
		}else if (parentNode instanceof ReturnStatement ) {
			MethodDeclaration md = AnalysisUtils.getMethodDeclaration4node(parentNode);
			if(md.getReturnType2().resolveBinding().getErasure().getName().equals("Future")) {
				if(Instanceof.useInstanceofFuture(stmt)) {
					return false;
				}
				return true;
			}else {
				return false;
			}
		}else if(parentNode instanceof Assignment) {
			Assignment parentAssignment = (Assignment)parentNode;
			if(parentAssignment.getLeftHandSide().resolveTypeBinding().getErasure().getName().equals("Future")) {
				if(Instanceof.useInstanceofFuture(stmt)) {
					return false;
				}
				return true;
			}else {
				return false;
			}
		}
		return true;
	}	
		
}
