package refuture.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import refuture.sootUtil.ExecutorSubclass;
import refuture.sootUtil.SootConfig;
import soot.SootClass;


// TODO: Auto-generated Javadoc
/**
 * 此类是重构的动作类 重构的预览可能是Wizard的功能吧。.
 */
public class RefutureRefactoring extends Refactoring {
	
	/** The all changes. */
	// 所有的重构变化
	List<Change> allChanges;
	
	/** The all java files. */
	// 项目所有的java文件,将IJavaElement改成了IcompilationUnit,这是由它的初始化过程决定的。
	List<ICompilationUnit> allJavaFiles;
	
	/** The potential java files. */
	// 包含异步任务的定义
	List<IJavaElement> potentialJavaFiles;

	/**
	 * Instantiates a new future task refactoring.
	 *
	 * @param selectProject the select project
	 */
	public RefutureRefactoring(IJavaProject selectProject) {		
		allJavaFiles = AnalysisUtils.collectFromSelect(selectProject);
		allChanges = new ArrayList<Change>();
		potentialJavaFiles = new ArrayList<IJavaElement>();
		InitAllStaticfield.init();//初始化所有的静态字段。
		SootConfig.setupSoot();//配置初始化soot,用来分析类层次结构
		
	}


	@Override
	public String getName() {
		return "reFutureMain";
	}


	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
//		return RefactoringStatus.createFatalErrorStatus("Find zero java file");
		List<SootClass> additionalExecutorClass = ExecutorSubclass.initialCheckForClassHierarchy();
		if(!additionalExecutorClass.isEmpty()) {
			int i = 1;
			additionalExecutorClass.forEach((e)->{System.out.printf("initialConditions|额外的不可重构子类:%s%n",e.getName());});
			return RefactoringStatus.createErrorStatus("有额外的不可重构ThreadPoolExecutor子类需要注意:"+additionalExecutorClass);
		}
		if (allJavaFiles.isEmpty()) {
			return RefactoringStatus.createFatalErrorStatus("Find zero java file");
		}

		return RefactoringStatus.createInfoStatus("Ininal condition has been checked");
		
	}


	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		Future2Completable.refactor(allJavaFiles);
		if(!Future2Completable.getStatus()) {
			return RefactoringStatus.createErrorStatus(Future2Completable.getErrorCause());
		}
		return null;
	}


	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		allChanges.addAll(Future2Completable.getallChanges());
		
		Change[] changes = new Change[allChanges.size()];
		System.arraycopy(allChanges.toArray(), 0, changes, 0, allChanges.size());
		CompositeChange change = new CompositeChange("refuture 待更改", changes);
		return change;
		
	}





}
