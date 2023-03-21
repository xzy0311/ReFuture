package refuture.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
 * 此类是重构的动作类 重构的预览可能是Wizard的功能吧。
 */
public class FutureTaskRefactoring extends Refactoring {
	// 所有的重构变化
	List<Change> allChanges;
	// 项目所有的java文件,将IJavaElement改成了IcompilationUnit,这是由它的初始化过程决定的。
	List<ICompilationUnit> allJavaFiles;
	// 项目的import中包含Callable,Runnable,Executorservice,Future,FutureTask等类的情况。可以作为分析的切入点。
	List<IJavaElement> potentialJavaFiles;

	public FutureTaskRefactoring(IProject select) {		
		allJavaFiles = AnalysisUtils.collectFromSelect(select);
		allChanges = new ArrayList<Change>();
		potentialJavaFiles = new ArrayList<IJavaElement>();
		
	}


	@Override
	public String getName() {
		return "reFutureMain";
	}


	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		
		
		if (allJavaFiles.isEmpty()) {
			return RefactoringStatus.createFatalErrorStatus("Find zero java file");
		}
		//查找潜在的异步任务定义文件.
		for(ICompilationUnit javafile: allJavaFiles) {
			if(AnalysisUtils.searchImport(javafile,AnalysisUtils.IMPORT_ConCurrent))
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
		
		return null;
	}


	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		Change[] changes = new Change[allChanges.size()];
		System.arraycopy(allChanges.toArray(), 0, changes, 0, allChanges.size());
		CompositeChange change = new CompositeChange("refuture 待更改", changes);
		return change;
		
	}





}
