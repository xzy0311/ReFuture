package refuture.sootUtil;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
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
	 * 从方法调用的ASTNode得到对应的Soot的Jimple Stmt.
	 *
	 * @param miv the miv
	 * @return the jimple invoc stmt
	 */
	public static Stmt getJimpleInvocStmt(MethodInvocation miv) {
		
		CompilationUnit cu = (CompilationUnit)miv.getRoot();
		int lineNumber = cu.getLineNumber(miv.getStartPosition());//行号
		String ivcMethodName = miv.getName().toString();//调用的方法的名称，只包含名称
		String methodSootName = AnalysisUtils.getSootMethodName(miv);//得到soot中用到的subsignature。
		SootClass sc = getSootClass4InvocNode(miv);
		AnalysisUtils.debugPrint("[AdaptAST.getJimpleInvocStmt]:包含submit/execute方法调用的类："+sc.getName()+"方法名"+methodSootName);
		SootMethod sm = sc.getMethod(methodSootName);
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
//		throw new IllegalStateException("[getJimpleInvocStmt]获取调用节点对应的Stmt出错");
        System.out.println("@error[AdaptAST.getJimpleInvocStmt]:获取调用节点对应的Stmt出错");
        return null;
		//这里后期需要修改为返回null,可以增加程序健壮性。不过走到这里，肯定有程序的源代码，所以应该要有它的class文件的，
		//也就是说正常情况下不应该出错。
	}

	public static SootClass getSootClass4InvocNode(MethodInvocation incovNode) {
		ITypeBinding itb;
		String typeFullName;
		ASTNode astNode = AnalysisUtils.getMethodDeclaration4node(incovNode);
		if(astNode == null) {
			TypeDeclaration td = AnalysisUtils.getTypeDeclaration4node(incovNode);
			itb = td.resolveBinding();
		}else if(astNode.getParent() instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration ad = (AnonymousClassDeclaration)astNode.getParent();
			itb = ad.resolveBinding();
		}else {
			TypeDeclaration td=(TypeDeclaration)astNode
					.getParent();//MethodDeclaration 节点的父节点就是TypeDeclaration
			itb = td.resolveBinding();//得到FullName,必须是用绑定。
		}
		if(itb.isNested()) {
			typeFullName = itb.getBinaryName();
		}else {
			typeFullName = itb.getQualifiedName();
		}
		SootClass sc = Scene.v().getSootClass(typeFullName);
		if(sc.isPhantom()) {
			System.out.println("@error[AdaptAst.getSootClass4InvocNode]:调用了虚幻类，请检查soot ClassPath");
			throw new NullPointerException("@error[AdaptAst.getSootClass4InvocNode]:调用了虚幻类:"+typeFullName+"，请检查soot ClassPath:");
		}
		return sc;
	}
	
}
