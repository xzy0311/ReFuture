package refuture.sootUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import refuture.astvisitor.MethodInvocationVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.ForTask;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocalBox;

// 这个类就像ExecutorSubclass一样，在最开始初始化一次。找到所有的调用future.cancel()的local。
// 然后通过一个方法，能够得到当前输入的invocstmt 左值的local是否可能指向同一个对象，就行了。
public class Cancel {
	private static List <Local> invocCancelLocals;
	
	public static boolean initStaticField() {
		invocCancelLocals = new ArrayList<Local>();
		return true;
	}
	
	private static boolean isTrue(MethodInvocation cancelInvoc) {
		List argus = cancelInvoc.arguments();
		if(argus.size() == 1 && argus.get(0) instanceof BooleanLiteral) {
			BooleanLiteral boolExp = (BooleanLiteral) argus.get(0);
			if(boolExp.booleanValue() == true) {
				return true;
			}
		}
		
		return false;
	}
	
	public static Set<String> getAllFutureAndsubName(){
		List<SootClass> allFutureSubClasses = ForTask.getAllFutureAndItsSubClasses();
		Set<String> allFutureAndsubName = new HashSet<>();
		for(SootClass allFutureSubClass:allFutureSubClasses) {
			allFutureAndsubName.add(allFutureSubClass.getName());
		}
		return allFutureAndsubName;
	}
	
	public static void initCancel(List<ICompilationUnit> allJavaFiles) {
		Set<String> allFutureAndsubName = getAllFutureAndsubName();
		for(ICompilationUnit cu : allJavaFiles) {
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
				if(!invocationNode.getName().toString().equals("cancel") || !isTrue(invocationNode) || invocationNode.getExpression() == null) {
					continue;
				}
				//到这里都是cancel(true)了。根据astBinding 得到接收器类型。
				ITypeBinding typeBinding = invocationNode.getExpression().resolveTypeBinding();
				if(typeBinding == null) {
					continue;
				}
				String typeName = typeBinding.getQualifiedName();
				if(typeBinding.isNested()) {
					typeName = typeBinding.getBinaryName();
				}
				typeName = typeName.replaceAll("<[^>]*>", "");
				if(allFutureAndsubName.contains(typeName)) {
					//在这里确定了调用了future.cancel()。接下来开始将exp对应的sootlocal存入静态字段。
					System.out.println("存在调用Future.cancel(true)方法的语句");
					Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);
					if(invocStmt == null) {
						continue; 
					}
					List<ValueBox> lvbs = invocStmt.getUseBoxes();
					for(ValueBox vb : lvbs) {
						if(vb instanceof JimpleLocalBox) {
							JimpleLocalBox jlb = (JimpleLocalBox) vb;
							Local futureLocal = (Local)jlb.getValue();
							invocCancelLocals.add(futureLocal);
						}
					}
				}
				
			}
			
		}
	
	}
	
	public static boolean futureUseCancelTure(MethodInvocation invocationNode, Stmt invocStmt) {
		if(invocCancelLocals.isEmpty()) {
			//该程序没有调用cancel(true)。
			AnalysisUtils.debugPrint("[Cancel.futureUseCancelTure]没有调用cancel(true)");
			return false;
		}
		List<ValueBox> defBox = invocStmt.getDefBoxes();
		if(!defBox.isEmpty()) {
			Local futureLocal = (Local) defBox.get(0).getValue();
			PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
			PointsToSet futureLocalSet = pa.reachingObjects(futureLocal);
			for(Local cancelLocal: invocCancelLocals) {
				PointsToSet cancelLocalSet = pa.reachingObjects(cancelLocal);
				if(futureLocalSet.hasNonEmptyIntersection(cancelLocalSet)) {
					AnalysisUtils.debugPrint("[Cancel.futureUseCancelTure]根据别名分析，该future可能后继调用cancel(true),排除");
					return true;
				}
			}
		}
		return false;
	}
	
}
