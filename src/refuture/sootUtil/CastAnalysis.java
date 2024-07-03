package refuture.sootUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.CastExpression;

import refuture.astvisitor.AllVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.Future2Completable;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JimpleLocal;

public class CastAnalysis {
	public static Set<JimpleLocal> useCastofFuture;
	public static boolean initStaticField() {
		useCastofFuture = new HashSet<JimpleLocal>();
		return true;
	}
	
	public static void init() {
		List<CastExpression> castNodes = AllVisiter.getInstance().getCastResult();
		for(CastExpression castNode:castNodes) {
			String qName = castNode.resolveTypeBinding().getBinaryName().toString();
			if(ExecutorSubclass.getStringInSootClassSet(ExecutorSubclass.getAllRelationSubClasses(ExecutorSubclass.allFutureSubClasses)).contains(qName)&&!qName.equals("java.util.concurrent.Future")&&!qName.equals("java.lang.Object")) {
				Stmt stmt = AdaptAst.getJimpleStmt(castNode);
				List<ValueBox> boxes = stmt.getUseBoxes();
				for(ValueBox box : boxes) {
					if(box instanceof ImmediateBox) {
						JimpleLocal local = (JimpleLocal)box.getValue();
						useCastofFuture.add(local);
					}
				}
			}
		}
		AnalysisUtils.debugPrint("useCastofFuture:"+useCastofFuture);
	}
	
	public static boolean useCast(Stmt castStmt) {
		if(useCastofFuture.isEmpty()) return false;
		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
		JimpleLocal local =(JimpleLocal)castStmt.getDefBoxes().get(0).getValue();
		PointsToSet ptset = pa.reachingObjects(local);
		for(JimpleLocal ioLocal:useCastofFuture) {
			if(ptset.hasNonEmptyIntersection(pa.reachingObjects(ioLocal))) {
				Future2Completable.FutureCanotC++;
				AnalysisUtils.debugPrint("Future因使用强制类型转换而被排除");
				return true;
			}
		}
		return false;
	}
	
}
