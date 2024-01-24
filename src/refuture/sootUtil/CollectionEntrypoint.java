package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import refuture.astvisitor.AllVisiter;
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
	
	/**
	 * 这个方法虽然名称是入口点初始化，但是与指针分析的入口点已经没关系了。
	 * 1.判断方法调用的名称为execute/submit()
	 * 2.判断方法调用的receiverObject的类型为Executor或其子类.
	 * 3.判断方法调用的实参是否包含类型为Runnable/Callable的参数。
	 * 此时若都符合，则属于maybeRefactoringNode.
	 * 4.对execute方法调用限制个数为1,对submit方法调用限制个数为1/2.（这个其实并不安全，且应该放在这里判断嘛？最终是为了对应到我们要重构的4种方法）
	 * 5.对于submit情况下，判断接受Future对象的变量是否定义为Future，并且该对象是否流入intanceof判断。（需要再限制强制类型转换嘛？）
	 */
	public static void entryPointInit(List<ICompilationUnit> allJavaFiles){
		List<MethodInvocation> invocationNodes = AllVisiter.getInstance().getMInvocResult();
		for(MethodInvocation invocationNode:invocationNodes) {
			ICompilationUnit cu = (ICompilationUnit) ((CompilationUnit) invocationNode.getRoot()).getJavaElement();
			List<Expression> arguExps =  invocationNode.arguments();
			if(!invocationNode.getName().toString().equals("execute")&&!invocationNode.getName().toString().equals("submit")) continue;
			boolean isTask = false;
			if(!expIsExecutor(invocationNode)) {
//					AnalysisUtils.debugPrint("[entryPointInit]receiverObject不属于Executor Family.");
				continue;
			}
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
			Set<String> methodSubSignature = new HashSet<>();
			methodSubSignature.add("execute(java.lang.Runnable)");
			methodSubSignature.add("submit(java.lang.Runnable)");
			methodSubSignature.add("submit(java.util.concurrent.Callable)");
			methodSubSignature.add("submit(java.lang.Runnable,java.lang.Object)");
			boolean ssFlag = false;
			IMethodBinding imb = invocationNode.resolveMethodBinding();
			ITypeBinding[] itbs =imb.getParameterTypes();
			String typeArgument = null;
			for(ITypeBinding itbT :imb.getReturnType().getTypeArguments()) {
				typeArgument = itbT.getQualifiedName();
			}
			List<String> arguNameList = new ArrayList<>();
			for(int i = 0;i<itbs.length;i++) {
				String name = itbs[i].getErasure().getQualifiedName();
				if(i>0&&typeArgument!= null&&name.equals(typeArgument)) {
					name = "java.lang.Object";
				}
				arguNameList.add(name);
			}
			String arguName = String.join(",", arguNameList);
			String subSignature = imb.getName()+"("+arguName+")";
			for(String ss:methodSubSignature) {
				if(subSignature.contains(ss)) {
					ssFlag = true;
				}
			}
			if(!ssFlag) {
				Future2Completable.methodOverload++;
				continue;
			}
			if(!futureType(invocationNode)) {
				continue;
			}
			if(!invocNodeMap.containsKey(cu)){
				invocNodeMap.put(cu, new ArrayList<MethodInvocation>());
			}
			invocNodeMap.get(cu).add(invocationNode);
			Future2Completable.canRefactoringNode++;
		}
	}
		
	private static boolean expIsExecutor(MethodInvocation invocationNode) {
		Expression exp = invocationNode.getExpression();
		Set <String> allSubNames = ExecutorSubclass.getAllExecutorSubClassesName();
		Set <String> allSubServiceNames = ExecutorSubclass.getAllExecutorServiceSubClassesName();
		if(exp==null){
			// 判断invocationNode所在类是否是子类，若是子类，则任务提交点+1.
//			AnalysisUtils.debugPrint("[entryPointInit]receiverObject为this，继续判断");
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
				return true;
			}else if(invocationNode.getName().toString().equals("submit")&&allSubServiceNames.contains(typeName)) {
				return true;
			}else if(typeName == "java.lang.Object") {
				throw new RefutureException(invocationNode,"typeBinding为Object,精度不够");
			}
			return false;
		}
		String typeName = AnalysisUtils.getTypeName4Exp(exp);
		if(typeName==null){
			throw new RefutureException(invocationNode,"typeBinding为null，应该是eclipse环境下的源码没有调试好，这里卡");
		}
		if(invocationNode.getName().toString().equals("execute")&&(allSubNames.contains(typeName))) {
			return true;
		}
		else if(invocationNode.getName().toString().equals("submit")&&(allSubServiceNames.contains(typeName))) {
			return true;
		}else if(typeName == "java.lang.Object") {
			throw new RefutureException(invocationNode,"typeBinding为Object,精度不够");
		}
		return false;
	}
	
	private static boolean futureType(MethodInvocation mInvoc) {
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
				throw new RefutureException(mInvoc);
			}
		}else if (parentNode instanceof ReturnStatement ) {
			MethodDeclaration md = AnalysisUtils.getMethodDeclaration4node(parentNode);
			if(md.getReturnType2().resolveBinding().getErasure().getName().equals("Future")) {
				if(Instanceof.useInstanceofFuture(stmt)||CastAnalysis.useCast(stmt)) {
					return false;
				}
				return true;
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
				throw new RefutureException(mInvoc);
			}
		}
		return true;
	}	
		
}
