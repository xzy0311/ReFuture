package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.ArrayType;
import soot.Hierarchy;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocalBox;

/**
 * The Class ExecutorSubclass.
 */
//负责从所有的Executor子类中，筛选出能够重构的子类。
public class ExecutorSubclass {

	//jdk1.8 内置的Executor的子类。
	private static Set<String> getJDKExecutorSubClass(){
		Set<String>setExecutorSubClass = new HashSet<String>();
		setExecutorSubClass.add("java.util.concurrent.Executors$DelegatedScheduledExecutorService");
		setExecutorSubClass.add("java.util.concurrent.ScheduledThreadPoolExecutor");
		setExecutorSubClass.add("java.util.concurrent.ForkJoinPool");
		setExecutorSubClass.add("java.util.concurrent.ThreadPoolExecutor");
		setExecutorSubClass.add("java.util.concurrent.Executors$FinalizableDelegatedExecutorService");
		setExecutorSubClass.add("java.util.concurrent.Executors$DelegatedExecutorService");
		setExecutorSubClass.add("java.util.concurrent.AbstractExecutorService");
		setExecutorSubClass.add("sun.nio.ch.AsynchronousChannelGroupImpl");
		return setExecutorSubClass;
	}
	
	/**
	 * 目前不具备分析额外的Executor子类的能力，只能先手动筛选能够返回FutureTask类型，且不具备ForkJoin
	 * 和Schedule特性的执行器。.
	 *
	 * @return the complete executor
	 */
	public static Set<String> getCompleteExecutor() {
		Set<String>completeExecutorSubClass = new HashSet<String>();
		completeExecutorSubClass.add("java.util.concurrent.ThreadPoolExecutor");
		completeExecutorSubClass.add("java.util.concurrent.Executors$FinalizableDelegatedExecutorService");
		completeExecutorSubClass.add("java.util.concurrent.Executors$DelegatedExecutorService");
		return completeExecutorSubClass;
	}

	
	
	public static List<String> getAdditionalExecutorClass(){
		//检查Executor的继承关系。
		List<String> executorSubClassesName = ClassHierarchy.getSubClassInJDK("java.util.concurrent.Executor");
		List<String> setExecutorSubClass = new ArrayList<String>(getJDKExecutorSubClass());
		List<String> additionalExecutorClass = new ArrayList<String>();
		for(String executorSubClassName:executorSubClassesName) {
			if(!setExecutorSubClass.contains(executorSubClassName)) {
				additionalExecutorClass.add(executorSubClassName);
			}
		}
//		System.out.println("[executorSubClassesName]"+executorSubClassesName);
//		System.out.println("[setExecutorSubClass]"+setExecutorSubClass);
//		System.out.println("[additionalExecutorClass]"+additionalExecutorClass);
		return additionalExecutorClass;
	}
	
	/**
	 * 是否可以安全的重构，就是判断调用提交异步任务方法的变量是否是安全提交的几种执行器的对象之一。
	 *
	 * @param stmt 必须是提交异步任务方法的调用语句。没有考虑Thread.start()。
	 * @return true, 如果可以进行重构
	 */
	public static boolean canRefactor(Stmt invocStmt) {
			Iterator it =invocStmt.getUseBoxes().iterator();
        	while(it.hasNext()) {
        		Object o = it.next();
        		if (o instanceof JimpleLocalBox) {
        			//Soot会在JInvocStmt里放入InvocExprBox,里面有JInterfaceInvokeExpr,里面有argBoxes和baseBox,分别存放ImmediateBox,JimpleLocalBox。
        			JimpleLocalBox jlb = (JimpleLocalBox) o;
        			Local local = (Local)jlb.getValue();
        			PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
        			PointsToSet ptset = pa.reachingObjects(local);
        			Set typeSet = ptset.possibleTypes();

        			Set<String> typeSetStrings = new HashSet<>();
        			for (Object obj : typeSet) {
        				typeSetStrings.add(obj.toString()); // 将每个对象转换为字符串类型并添加到 Set<String> 中
        			}
        			
        			Set completeSetType = ExecutorSubclass.getCompleteExecutor();
        			if(completeSetType.containsAll(typeSetStrings)) {
        				//是安全重构的子集，就可以进行重构了。
        				return true;
        			}
        		}	
        		
        	}
        	return false;
	}


	/**
	 * 	判断参数的类型是否复合要求。
	 *
	 * @param invocStmt the invoc stmt
	 * @param argType   为1,代表是callable;为2,代表Runnable;为3,代表FutureTask。
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
		Local la1 = (Local) lv.get(0);
		PointsToSet ptset = pa.reachingObjects(la1);
		Set<Type> typeSet = ptset.possibleTypes();
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		switch (argType) {
		case 1://是否是Callable的子类.
			for(Type type:typeSet) {
				SootClass sc = Scene.v().getSootClass(type.getEscapedName());
				SootClass callable = Scene.v().getSootClass("java.util.concurrent.Callable");
				if(sc.isPhantom()||callable.isPhantom()) {
					return false;
				}
				List<SootClass> implementers =hierarchy.getImplementersOf(callable);
				return implementers.contains(sc);
			}
			break;
		default://判断是否是FutureTask类型，若是则判断是3则返回true，其他情况返回false；若不是则2返回true,其他情况返回false;
			for(Type type:typeSet) {
				SootClass sc = Scene.v().getSootClass(type.getEscapedName());
				SootClass futureTask = Scene.v().getSootClass("java.util.concurrent.FutureTask");
				SootClass runnable = Scene.v().getSootClass("java.lang.Runnable");
				if(sc.isPhantom()||futureTask.isPhantom()) {
					return false;
				}
				if(hierarchy.isClassSuperclassOfIncluding(futureTask, sc)) {
					if(argType == 3) {
						return true;
					}
				}else {
					if(argType ==2) {
						List<SootClass> implementers =hierarchy.getImplementersOf(runnable);
						return implementers.contains(sc);

					}
				}
			}
			return false;
		}
		System.out.println("over");
		return false;
	}
}

