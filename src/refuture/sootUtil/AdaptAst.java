package refuture.sootUtil;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import refuture.refactoring.AnalysisUtils;
import soot.Body;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.util.Chain;


/*
 * 这个工具类是用来适配AST的，在AST上进行交互时，需要的方法。
 */
public class AdaptAst {


	/**
	 * 从方法调用的ASTNode得到对应的Soot的Jimple Stmt,若没有可能返回null。
	 *
	 * @param miv the miv
	 * @return the jimple invoc stmt
	 */
	public static Stmt getJimpleInvocStmt(MethodInvocation miv) {
		
		CompilationUnit cu = (CompilationUnit)miv.getRoot();
		int lineNumber = cu.getLineNumber(miv.getStartPosition());//行号
		String ivcMethodName = miv.getName().toString();//调用的方法的名称，只包含名称
//		System.out.println("[getJimpleInvocStmt:]"+ivcMethodName);
		String methodSootName = AnalysisUtils.getSootMethodName(miv);//得到soot中用到的subsignature。
//		System.out.println("[getJimpleInvocStmt:]"+methodSootName);
		ITypeBinding itb;
		if(AnalysisUtils.getMethodDeclaration4node(miv).getParent() instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration ad = (AnonymousClassDeclaration)AnalysisUtils.getMethodDeclaration4node(miv).getParent();
			itb = ad.resolveBinding();
		}else {
			TypeDeclaration td=(TypeDeclaration)AnalysisUtils.getMethodDeclaration4node(miv).getParent();//MethodDeclaration 节点的父节点就是TypeDeclaration
			itb = td.resolveBinding();//得到FullName,必须是用绑定。
		}
		String typeFullName;
		if(itb.isNested()) {
			typeFullName = itb.getBinaryName();
		}else {
			typeFullName = itb.getQualifiedName();
		}
		SootClass sc = Scene.v().getSootClass(typeFullName);
		if(sc.isPhantom()) {
			throw new IllegalStateException("虚拟类错误");
		}
		if(AnalysisUtils.debugFlag) {
			System.out.println("[AdaptAST.getJimpleInvocStmt:]当前处理的是否是虚拟类："+sc.isPhantom()+"当前方法名称为:"+methodSootName+"所有的方法有："+sc.getMethods());
		}
		SootMethod sm = sc.getMethod(methodSootName);
		//test
		if(AnalysisUtils.debugFlag) {
			System.out.println("[AdaptAST.getJimpleInvocStmt:]包含submit/execute方法调用的类："+sc.getName()+"方法名"+sm.getName());
		}
		Body body =sm.retrieveActiveBody();
		
        Iterator<Unit> i=body.getUnits().snapshotIterator();
        while(i.hasNext())
        {
            Stmt stmt=(Stmt) i.next();
            if(stmt.toString().contains(ivcMethodName)) {
            	if(stmt.getJavaSourceStartLineNumber()==lineNumber) {
            		return stmt;
            	}
            }
        }
		return null;
	}
}
