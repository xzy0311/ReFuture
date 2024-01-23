package refuture.sootUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.CastExpression;

import refuture.astvisitor.AllVisiter;
import refuture.astvisitor.CastVisiter;
import refuture.refactoring.AnalysisUtils;
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
			String qName = castNode.getType().toString();
			if(ExecutorSubclass.allFutureSubClasses.contains(qName)&&!qName.equals("java.util.concurrent.Future")) {
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
	}
	
}
