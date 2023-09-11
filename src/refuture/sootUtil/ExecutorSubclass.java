package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.ltk.core.refactoring.Change;

import refuture.refactoring.AnalysisUtils;
import soot.Hierarchy;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.Type;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt.LinkedVariableBox;
import soot.jimple.internal.JimpleLocalBox;
// TODO: Auto-generated Javadoc
/**
 * The Class ExecutorSubclass.
 */
//负责从所有的Executor子类中，筛选出能够重构的子类。
public class ExecutorSubclass {
	//7.8记录，目前的实现是，将ThreadPoolExecutor 的子类中没有修改过s相关类的加入到completesubclass，然后判断
	// 将ExecutorService的子类进行判断，将不是jdk存在的加入到附加的类集合中，然后若不是上一行判断过的complete的，就直接
	//加入到污染类集合中。对包装类的判断还未实现。
	
	/** The complete executor sub class. */
	private static Set<SootClass>completeExecutorSubClass;
	
	/** The all dirty classes. */
	private static Set<SootClass>allDirtyClasses;
	
	/**
	 * Inits the static field.
	 *
	 * @return true, if successful
	 */
	public static boolean initStaticField() {
		completeExecutorSubClass = new HashSet<SootClass>();
		allDirtyClasses = new HashSet<SootClass>();
		return true;
	}
	

	/**
	 *
	 *5.12尝试完善。
	 *5.15发现新问题，有一些Executor子类，它们是包装器，虽然重新Override了这几个相关的方法，但是它们只是简单的调用了的执行器字段的。这种直接判断是安全的吗？
	 *
	 * @return the complete executor
	 */
	public static void threadPoolExecutorSubClassAnalysis() {
		
		SootClass threadPoolExecutorClass = Scene.v().getSootClass("java.util.concurrent.ThreadPoolExecutor");
		
		completeExecutorSubClass.add(threadPoolExecutorClass);//是安全的。
		completeExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$FinalizableDelegatedExecutorService"));
		completeExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedExecutorService"));
		
		Set<SootClass>dirtyClasses = new HashSet<SootClass>();
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		List<SootClass> tPESubClasses = hierarchy.getSubclassesOf(threadPoolExecutorClass);//若子类没有重写execute，newTaskFor，和submit方法，则直接判断为安全
		if(tPESubClasses.isEmpty()) {
			System.out.println("[threadPoolExecutorSubClassAnalysis]：子类为空");
			return;
		}
		System.out.println("[threadPoolExecutorSubClassAnalysis]：所有的子类："+tPESubClasses);
		for(SootClass tPESubClass : tPESubClasses) {
			//判断是否是dirtyClass
			boolean flag1 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)");
			boolean flag2 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)");
			boolean flag3 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable)");
			boolean flag4 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.util.concurrent.Callable)");
			boolean flag5 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.lang.Runnable,java.lang.Object)");
			boolean flag6 = tPESubClass.declaresMethod("void execute(java.lang.Runnable)");
			if(flag1||flag2||flag3||flag4||flag5||flag6) {
				dirtyClasses.add(tPESubClass);
				System.out.println("[threadPoolExecutorSubClassAnalysis]：这个类是不安全的："+tPESubClass);
			}else {
				completeExecutorSubClass.add(tPESubClass);
				System.out.println("[threadPoolExecutorSubClassAnalysis]：这个类是安全的："+tPESubClass);
			}
		}
		for(SootClass currentDirtyClass:dirtyClasses) {
			allDirtyClasses.addAll(hierarchy.getSubclassesOfIncluding(currentDirtyClass));
		}
	}

	/**
	 * 这个类用来分析额外的ExecutorService子类，这些类不属于JDK，是用户定义的新类，判断这些类是否是包装类。.
	 */
	public static void additionalExecutorServiceSubClassAnalysis() {
		Set<SootClass>setExecutorSubClass = new HashSet<SootClass>();
		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedScheduledExecutorService"));
		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.ScheduledThreadPoolExecutor"));
		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.ForkJoinPool"));
		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.ThreadPoolExecutor"));
		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$FinalizableDelegatedExecutorService"));
		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedExecutorService"));
		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.AbstractExecutorService"));
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		SootClass executorServiceClass = Scene.v().getSootClass("java.util.concurrent.ExecutorService");
		List<SootClass> executorSubClasses = hierarchy.getImplementersOf(executorServiceClass);
		List<SootClass> additionalExecutorServiceClass = new ArrayList<SootClass>();
		for(SootClass executorSubClass:executorSubClasses) {
			if(!setExecutorSubClass.contains(executorSubClass)) {
				additionalExecutorServiceClass.add(executorSubClass);
			}
		}
		
		// 待   实现对额外包装类的判断。
		for(SootClass additionalClass:additionalExecutorServiceClass) {
			if(!completeExecutorSubClass.contains(additionalClass)) {
				allDirtyClasses.add(additionalClass);
			}
		}
	}
	
	public static Set<SootClass> getCompleteExecutorSubClass(){
		return completeExecutorSubClass;
	}
	
	public static Set<String> getCompleteExecutorSubClassName(){
		Set<SootClass> completeSetType = getCompleteExecutorSubClass();
		Set<String> completeSetTypeStrings = new HashSet<>();
		for(SootClass completeSC:completeSetType) {
			completeSetTypeStrings.add(completeSC.getName());
		}
		return completeSetTypeStrings;
	}
	/**
	 * Gets the all dirty executor sub class.
	 *
	 * @return the all dirty executor sub class
	 */
	public static Set<SootClass> getallDirtyExecutorSubClass(){
		return allDirtyClasses;
	}
	
	
	/**
	 * 是否可以安全的重构，就是判断调用提交异步任务方法的变量是否是安全提交的几种执行器的对象之一。.
	 *
	 * @param invocStmt the invoc stmt
	 * @return 1, 如果可以进行重构;0,不可重构;2,需要得到绑定并进行判断.
	 */
	public static int canRefactor(Stmt invocStmt) {
		if(invocStmt == null) return 0;
		List<ValueBox> lvbs = invocStmt.getUseBoxes();
			Iterator<ValueBox> it =lvbs.iterator();
        	while(it.hasNext()) {
        		Object o = it.next();
        		if (o instanceof JimpleLocalBox) {
        			//Soot会在JInvocStmt里放入InvocExprBox,里面有JInterfaceInvokeExpr,里面有argBoxes和baseBox,分别存放ImmediateBox,JimpleLocalBox。
        			JimpleLocalBox jlb = (JimpleLocalBox) o;
        			Local local = (Local)jlb.getValue();
        			PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
        			PointsToSet ptset = pa.reachingObjects(local);
        			Set<Type> typeSet = ptset.possibleTypes();

        			Set<String> typeSetStrings = new HashSet<>();
        			for (Type obj : typeSet) {
        				typeSetStrings.add(obj.toString()); // 将每个对象转换为字符串类型并添加到 Set<String> 中
        			}
        			Set<String> completeSetTypeStrings = getCompleteExecutorSubClassName();
        			if(typeSetStrings.isEmpty()) {
        				//说明没有被访问到，可以进行排除
        				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]程序中没有访问到,进一步判断");
        				return 2;
        			}else if(completeSetTypeStrings.containsAll(typeSetStrings)) {
        				//是安全重构的子集，就可以进行重构了。
//        				System.out.println("[ExecutorSubclass:canRefactor:typeSetStrings]"+typeSetStrings);
        				return 1;
        			}
        		}	
        	}
        	AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]不是安全重构的子集，进行排除");
        	return 0;
	}


	/**
	 * 判断参数的类型是否复合要求。.
	 *
	 * @param invocStmt the invoc stmt
	 * @param argType   为1,代表是callable;为2,代表Runnable;为3,代表FutureTask;为4,代表两个参数。
	 * @return true, if successful
	 */
	public static boolean canRefactorArgu(Stmt invocStmt,int argType) {
		/*这里已经限制了调用的方法是submit或者execute，所以第一个参数一定是：callable、Runnable或者，FutureTask。
		 * 我只分析invocStmt第一个参数，根据argType进行判断，为1,则判断是否是callable的子类，为2或者3,则判断是否是FutureTask,
		 * 若不是，再判断是否是Runnable。lambda表达式也可以正常的分析，因为在Jinple中，lambda表达式会首先由一个Local变量指向它代表的对象。
		 */
		InvokeExpr ivcExp = invocStmt.getInvokeExpr();
		List<Value> lv =ivcExp.getArgs();
		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
		if(lv.size() == 0) {
			AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:未传入实参，排除");
			return false;
		}
		if(lv.size() == 2) {
			if(argType == 4) {
				return true;
			}
			return false;
		}
		if(lv.get(0)instanceof Local) {
			Local la1 = (Local) lv.get(0);
			PointsToSet ptset = pa.reachingObjects(la1);
			Set<Type> typeSet = ptset.possibleTypes();
			Hierarchy hierarchy = Scene.v().getActiveHierarchy();
			SootClass callable = Scene.v().getSootClass("java.util.concurrent.Callable");
			List<SootClass> callableImplementers =hierarchy.getImplementersOf(callable);
			SootClass futureTask = Scene.v().getSootClass("java.util.concurrent.FutureTask");
			SootClass runnable = Scene.v().getSootClass("java.lang.Runnable");
			List<SootClass> runnableImplementers =hierarchy.getImplementersOf(runnable);
			switch (argType) {
			case 1://是否是Callable的子类.
				for(Type type:typeSet) {
					SootClass sc = Scene.v().getSootClass(type.getEscapedName());
					if(sc.isPhantom()) {
						AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:传入的实参类型无法获得SootClass，排除");
						return false;
					}
					if(callableImplementers.contains(sc)) {
						return true;
					}
				}
				break;
			case 2:		
				for(Type type:typeSet) {
					SootClass sc = Scene.v().getSootClass(type.getEscapedName());
					if(sc.isPhantom()) {
						AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:传入的实参类型无法获得SootClass，排除");
						return false;
					}
					if(runnableImplementers.contains(sc)) {
						return true;
					}
				}
				break;
			case 3:		
				for(Type type:typeSet) {
					SootClass sc = Scene.v().getSootClass(type.getEscapedName());
					if(sc.isPhantom()) {
						AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:传入的实参类型无法获得SootClass，排除");
						return false;
					}
					if(hierarchy.isClassSuperclassOfIncluding(futureTask, sc)) {
						return true;
					}
				}
				break;
			case 4:		
				if(lv.size()==2) {
				return true;
			}
				break;
			default:
				throw new IllegalArgumentException("Invalid number");
		}
			return false;
		}
		return false;
	}
	
	/**
	 * 判断是否有返回值，若没有返回值则直接返回true，否则判断等号左边的变量的类型，是否是Future.
	 *
	 * @param invocationNode the invocation node
	 * @return true, 如果可以重构
	 */
	public static boolean objectIsFuture(MethodInvocation invocationNode) {
		if(AnalysisUtils.isDeclarOrAssign(invocationNode)) {
			Stmt invocStmt = AdaptAst.getJimpleInvocStmt(invocationNode);
			List<ValueBox> lvbs = invocStmt.getDefBoxes();
			Iterator<ValueBox> it =lvbs.iterator();
        	while(it.hasNext()) {
        		Object o = it.next();
        		if (o instanceof LinkedVariableBox) {
        			LinkedVariableBox jlb = (LinkedVariableBox) o;
        			Local local = (Local)jlb.getValue();
        			PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
        			PointsToSet ptset = pa.reachingObjects(local);
        			Set<Type> typeSet = ptset.possibleTypes();
        			if(typeSet.size()!=1||typeSet.iterator().next().toString()!="java.util.concurrent.FutureTask") {
        				return false;
        			}
        		}
        	}
		}
		return true;
	}
	
	/**
	 * Initial check for class hierarchy.
	 *
	 * @return the list
	 */
	public static List<SootClass> initialCheckForClassHierarchy() {
		
		Set<SootClass> dirtyExecutorClass =getallDirtyExecutorSubClass();
		List<SootClass> additionalDirtyExecutorClass = new ArrayList<SootClass>();
		for(SootClass appDirtyExecutorClass:dirtyExecutorClass) {
			if(appDirtyExecutorClass.isApplicationClass()) {
				additionalDirtyExecutorClass.add(appDirtyExecutorClass);
			}
		}
		return additionalDirtyExecutorClass;
	}


	
}

