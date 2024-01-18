package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.InstanceofExpression;

import refuture.astvisitor.InstanceofVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.Future2Completable;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JimpleLocal;

public class Instanceof {
	public static Set<JimpleLocal> useInstanceofRunnable;
	public static Set<JimpleLocal> useInstanceofFuture;
	public static boolean initStaticField() {
		useInstanceofRunnable = new HashSet<JimpleLocal>();
		useInstanceofFuture = new HashSet<JimpleLocal>();
		return true;
	}
	
	public static void init() {
		for(CompilationUnit astUnit : AnalysisUtils.allAST) {
			InstanceofVisiter insOf = new InstanceofVisiter();
			astUnit.accept(insOf);
			List<InstanceofExpression> insOfNodes = insOf.getResult();
			for(InstanceofExpression insOfNode:insOfNodes) {
				String qName = insOfNode.getRightOperand().resolveBinding().getQualifiedName();
				if(ExecutorSubclass.runnablesubClasses.contains(qName)&&!qName.equals("java.lang.Runnable")) {
					Stmt stmt = AdaptAst.getJimpleStmt(insOfNode);
					List<ValueBox> boxes = stmt.getUseBoxes();
					for(ValueBox box : boxes) {
						if(box instanceof ImmediateBox) {
							JimpleLocal local = (JimpleLocal)box.getValue();
							useInstanceofRunnable.add(local);
						}
					}
				}else if(ExecutorSubclass.allFutureSubClasses.contains(qName)&&!qName.equals("java.util.concurrent.Future")) {
					Stmt stmt = AdaptAst.getJimpleStmt(insOfNode);
					List<ValueBox> boxes = stmt.getUseBoxes();
					for(ValueBox box : boxes) {
						if(box instanceof ImmediateBox) {
							JimpleLocal local = (JimpleLocal)box.getValue();
							useInstanceofFuture.add(local);
						}
					}
				}
			}
		}
	}
	public static boolean useInstanceofRunnable(Stmt invocStmt) {
		if(useInstanceofRunnable.isEmpty()) return false;
		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
		InvokeExpr ivcExp = invocStmt.getInvokeExpr();
		List<Value> lv =ivcExp.getArgs();
		if(lv.get(0)instanceof Local) {
			Local la1 = (Local) lv.get(0);
			PointsToSet ptset = pa.reachingObjects(la1);
			for(JimpleLocal ioLocal:useInstanceofRunnable) {
				if(ptset.hasNonEmptyIntersection(pa.reachingObjects(ioLocal))) {
					Future2Completable.useInstanceof++;
					AnalysisUtils.debugPrint("因使用 instanceof而被排除");
					return true;
				}
			}
		}
		return true;
	}
	public static boolean useInstanceofFuture(Stmt invocStmt) {
		if(useInstanceofFuture.isEmpty()) return false;
		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
		JimpleLocal local =(JimpleLocal)invocStmt.getDefBoxes().get(0).getValue();
			PointsToSet ptset = pa.reachingObjects(local);
			for(JimpleLocal ioLocal:useInstanceofFuture) {
				if(ptset.hasNonEmptyIntersection(pa.reachingObjects(ioLocal))) {
					Future2Completable.useInstanceof++;
					AnalysisUtils.debugPrint("因使用 instanceof而被排除");
					return true;
				}
			}
		return false;
	}
}
