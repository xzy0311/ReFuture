package refuture.refactoring;

import java.util.ArrayList;
import java.util.Date;
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

import refuture.sootUtil.Cancel;
import refuture.sootUtil.ExecutorSubclass;
import refuture.sootUtil.SootConfig;


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
	
	int refactorPattern;

	boolean disableCancelPattern;
	public static int time = 0;
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
		this.refactorPattern = 1;
		this.disableCancelPattern = false;
	}

	public boolean setRefactorPattern(int pattern) {
		this.refactorPattern = pattern;
		return true;
	}
	
	public boolean setDisableCancelPattern(boolean pattern) {
		this.disableCancelPattern = pattern;
		return true;
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
		if(refactorPattern ==1) {
			System.out.println("Future重构模式");
			System.out.println("hello xzy ,this is "+ time++ +" times run this model.");
			if(time == 1) {
				SootConfig.setupSoot();//配置初始化soot,用来分析类层次结构
			}
			ExecutorSubclass.taskTypeAnalysis();
	        ExecutorSubclass.threadPoolExecutorSubClassAnalysis();
	        ExecutorSubclass.additionalExecutorServiceSubClassAnalysis();
	        if(!this.disableCancelPattern) {
		        Cancel.initCancel(allJavaFiles);
	        }
			Future2Completable.refactor(allJavaFiles);
		}else if(refactorPattern == 2) {
//			ForTask.refactor(allJavaFiles);   10月11日，暂时取消ForTask尝试。待添加寻找Thread相关代码，以及关闭soot的方法。
			FindThread.find(allJavaFiles);
		}
		Date endTime = new Date();
		System.out.println("The current ent time is "+ endTime +"已花费:" + ((endTime.getTime()-SootConfig.startTime.getTime())/1000)+"s");
		return null;
	}


	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if(refactorPattern ==1) {
			allChanges.addAll(Future2Completable.getallChanges());
		}else if(refactorPattern == 2) {
//			allChanges.addAll(ForTask.getallChanges());
		}
		Change[] changes = new Change[allChanges.size()];
		System.arraycopy(allChanges.toArray(), 0, changes, 0, allChanges.size());
		CompositeChange change = new CompositeChange("refuture 待更改", changes);
		return change;
		
	}





}
