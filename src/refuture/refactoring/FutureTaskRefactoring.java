package refuture.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

/**
 * 此类是重构的动作类 重构的预览也是通过此类完成
 */
public class FutureTaskRefactoring extends Refactoring {
	// 所有的重构变化
	List<Change> allChanges = new ArrayList<Change>();
	// 项目所有的java文件
	List<IJavaElement> allJavaFiles = new ArrayList<IJavaElement>();
	// 项目的import中包含Callable,Runnable,Executorservice,Future,FutureTask,CompletableFuture,Executors等类的情况。
	List<IJavaElement> potentialJavaFiles = new ArrayList<IJavaElement>();

	public FutureTaskRefactoring(IJavaElement select) {		
		allJavaFiles = AnalysisUtils.collectFromSelect(select);
	}


	@Override
	public String getName() {
		return "reFutureMain";
	}


	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		for(IJavaElement javafile: allJavaFiles) {
			if(AnalysisUtils.CtlImport(javafile,AnalysisUtils.IMPORTCONCURRENT))
				potentialJavaFiles.add(javafile);
		}
		if(potentialJavaFiles.isEmpty()) {
			return RefactoringStatus.createFatalErrorStatus("No potential java file can refactoring!");
		}else {
			return RefactoringStatus.createInfoStatus("Ininal condition has been checked");
		}
		
	}


	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}





}
