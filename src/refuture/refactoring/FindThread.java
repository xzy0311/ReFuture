package refuture.refactoring;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;

import refuture.astvisitor.MethodInvocationVisiter;

public class FindThread {

	public static void find(List<ICompilationUnit> allJavaFiles) {
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
				if(invocationNode.getName().toString().equals("start")) {
					System.out.printf("❤[FindThread]这个Thread可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(invocationNode).resolveBinding().getBinaryName(),
							astUnit.getLineNumber(invocationNode.getStartPosition()));
				}
				
			}
		}
	}

	
}
