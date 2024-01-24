package refuture.refactoring;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;

import refuture.astvisitor.AllVisiter;
import refuture.astvisitor.MethodInvocationVisiter;

public class FindThread {

	public static void find(List<ICompilationUnit> allJavaFiles) {
		List<MethodInvocation> invocationNodes = AllVisiter.getInstance().getMInvocResult();
		for(MethodInvocation invocationNode:invocationNodes) {
			if(invocationNode.getName().toString().equals("start")) {
				System.out.printf("❤[FindThread]这个Thread可以进行重构！它在%s,行号为%d%n", AnalysisUtils.getTypeDeclaration4node(invocationNode).resolveBinding().getBinaryName(),
						((CompilationUnit) invocationNode.getRoot()).getLineNumber(invocationNode.getStartPosition()));
			}
			
		}
	}

	
}
