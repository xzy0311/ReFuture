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

import refuture.sootUtil.SootConfig;


/**
 * 此类是重构的动作类 重构的预览可能是Wizard的功能吧。
 */
public class FutureTaskRefactoring extends Refactoring {
	// 所有的重构变化
	List<Change> allChanges;
	// 项目所有的java文件,将IJavaElement改成了IcompilationUnit,这是由它的初始化过程决定的。
	List<ICompilationUnit> allJavaFiles;
	// 包含异步任务的定义
	List<IJavaElement> potentialJavaFiles;

	public FutureTaskRefactoring(IJavaProject selectProject) {		
		allJavaFiles = AnalysisUtils.collectFromSelect(selectProject);
		allChanges = new ArrayList<Change>();
		potentialJavaFiles = new ArrayList<IJavaElement>();
//		SootConfig.setupSoot(AnalysisUtils.getSootClassPath());//配置soot,用来分析类层次结构
		
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

		return RefactoringStatus.createInfoStatus("Ininal condition has been checked");
		
		
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
