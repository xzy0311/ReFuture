package refuture.sootUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.InstanceofExpression;

import refuture.astvisitor.AllVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.Future2Completable;
import soot.Body;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
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
		List<InstanceofExpression> insOfNodes = AllVisiter.getInstance().getInsResult();
		for(InstanceofExpression insOfNode:insOfNodes) {
			String qName = insOfNode.getRightOperand().resolveBinding().getQualifiedName();
			if(ExecutorSubclass.runnablesubClasses.contains(qName)&&!qName.equals("java.lang.Runnable")) {
				Stmt stmt = AdaptAst.getJimpleStmt(insOfNode);
				List<ValueBox> boxes = stmt.getUseBoxes();
				for(ValueBox box : boxes) {
					if(box instanceof ImmediateBox) {
						Value v = box.getValue();
						if(v instanceof JimpleLocal) {
							JimpleLocal local = (JimpleLocal)v;
							useInstanceofRunnable.add(local);
						}
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
	public static boolean useInstanceofRunnable(SootMethod executeMethod) {
		if(useInstanceofRunnable.isEmpty()) return false;
		Body body = executeMethod.retrieveActiveBody();
		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
		Local la1 = body.getParameterLocals().get(0);
		PointsToSet ptset = pa.reachingObjects(la1);
		for(JimpleLocal ioLocal:useInstanceofRunnable) {
			PointsToSet ptsetIO = pa.reachingObjects(ioLocal);
			if(ptset.hasNonEmptyIntersection(ptsetIO)) {
				return true;
			}
		}
		return false;
	}
	public static boolean useInstanceofFuture(Stmt invocStmt) {
		if(useInstanceofFuture.isEmpty()) return false;
		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
		JimpleLocal local =(JimpleLocal)invocStmt.getDefBoxes().get(0).getValue();
			PointsToSet ptset = pa.reachingObjects(local);
			for(JimpleLocal ioLocal:useInstanceofFuture) {
				if(ptset.hasNonEmptyIntersection(pa.reachingObjects(ioLocal))) {
					Future2Completable.FutureCanotI++;
					AnalysisUtils.debugPrint("Future可能调用intanceof而被排除");
					return true;
				}
			}
		return false;
	}
}
