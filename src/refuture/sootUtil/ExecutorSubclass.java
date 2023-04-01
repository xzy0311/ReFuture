package refuture.sootUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class ExecutorSubclass.
 */
//负责从所有的Executor子类中，筛选出能够重构的子类。
public class ExecutorSubclass {

	//jdk1.8 内置的Executor的子类。
	private static List<String> getJDKExecutorSubClass(){
		List<String>setExecutorSubClass = new ArrayList<String>();
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
	public static List<String> getCompleteExecutor() {
		List<String>completeExecutorSubClass = new ArrayList<String>();
		completeExecutorSubClass.add("java.util.concurrent.ThreadPoolExecutor");
		completeExecutorSubClass.add("java.util.concurrent.Executors$FinalizableDelegatedExecutorService");
		completeExecutorSubClass.add("java.util.concurrent.Executors$DelegatedExecutorService");
		return completeExecutorSubClass;
	}

	
	
	public static List<String> getAdditionalExecutorClass(){
		//检查Executor的继承关系。
		List<String> executorSubClassesName = ClassHierarchy.getSubClassInJDK("java.util.concurrent.Executor");
		List<String> setExecutorSubClass = getJDKExecutorSubClass();
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
}

