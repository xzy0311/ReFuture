package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import refuture.refactoring.AnalysisUtils;
import soot.Body;
import soot.Hierarchy;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
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
	
	//9月14日记录，目前所有记录的安全的，污染的，附加的类都是用于遇到submit()方法时，判断是否符合重构条件。待添加包装类判断
	/** The all subclasses. */
	private static Set<SootClass>allExecutorSubClasses;
	
	/** 包含我手动添加的jdk中自带的执行器类型,以及完全没有重写关键方法子类. */
	private static Set<SootClass>mayCompleteExecutorSubClasses;//存入可能可以重构的类型以及包装类。
	
	/** The all dirty classes. */
	private static Set<SootClass>allDirtyClasses;
	
	/** The all appendent classes. */
	private static Set<SootClass>allAdditionalClasses;
	
	/**
	 * Inits the static field.
	 *
	 * @return true, if successful
	 */
	public static boolean initStaticField() {
		mayCompleteExecutorSubClasses = new HashSet<SootClass>();
		allDirtyClasses = new HashSet<SootClass>();
		allAdditionalClasses = new HashSet<SootClass>();
		allExecutorSubClasses= new HashSet<SootClass>();
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
		SootClass executorServiceClass = Scene.v().getSootClass("java.util.concurrent.ExecutorService");
		mayCompleteExecutorSubClasses.add(executorServiceClass);//是安全的。
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.AbstractExecutorService"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.ThreadPoolExecutor"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.Executors$FinalizableDelegatedExecutorService"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedExecutorService"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.ScheduledThreadPoolExecutor"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.ForkJoinPool"));
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		allExecutorSubClasses.addAll(hierarchy.getImplementersOf(executorServiceClass));
		allExecutorSubClasses.add(executorServiceClass);
		for(SootClass tPESubClass : allExecutorSubClasses) {
			if(mayCompleteExecutorSubClasses.contains(tPESubClass)||allDirtyClasses.contains(tPESubClass)) {
				continue;
			}
			// 首先判定它继承的父类，有没有污染类
			List<SootClass> superClasses = hierarchy.getSuperclassesOfIncluding(tPESubClass);
			boolean isDirty = false;
			for(SootClass superClass : superClasses) {
				if(allDirtyClasses.contains(superClass)){
					allDirtyClasses.add(tPESubClass);
					isDirty = true;
					break;
				}
			}
			if(isDirty) {continue;}
			//判断是否是dirtyClass
			boolean flag1 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)");
			boolean flag2 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)");
			boolean flag3 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable)");
			boolean flag4 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.util.concurrent.Callable)");
			boolean flag5 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.lang.Runnable,java.lang.Object)");
			if(flag1||flag2||flag3) {
				allDirtyClasses.add(tPESubClass);//先不想逻辑试试看
				// 重新定义了submit(),需要对内部调用的方法进行判断，是否是私有或者保护方法，不能移植到异步任务中。
//				if(flag4||flag5) {
//					
//				}
//				
//				if(flag1) {
//					SootMethod sm = tPESubClass.getMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)");
//					Body body = sm.retrieveActiveBody();
//					UnitPatchingChain uc = body.getUnits();
//					for(Unit unit : uc){
//						Stmt j = (Stmt) unit;
//						if(j.containsInvokeExpr()) {
//							InvokeExpr invokeExp = j.getInvokeExpr();
//							String invocMethodName = invokeExp.getMethodRef().getSubSignature().toString();
//							SootMethod invocMethod = Scene.v().getMethod(invocMethodName);
//							if(invocMethodName.contains("java.util.concurrent.Callable")&&invocMethod.isConstructor()) {
//								//代表了包装callable的Task类。
//								SootClass taskClass = invocMethod.getDeclaringClass();
//								if(!taskClass.isPublic()) {
//									allDirtyClasses.add(tPESubClass);
//								}
//								
//							}
//							
//							
//							
//						}
//						
//					}
//				}else if(flag2) {
//					
//				}else if(flag3) {
//					
//				}else {
//					throw new IllegalStateException("判断子类，出现不应该有的错误。");
//				}
//				
			}else {
				// 没有重新定义submit
				if(flag4||flag5) {//重新定义newTaskof
					allDirtyClasses.add(tPESubClass);//先不想逻辑试试看
					
				}else {//没有重新定义newTaskof
					mayCompleteExecutorSubClasses.add(tPESubClass);
				}
			}
		}

	}

	/**
	 * 这个类用来分析额外的ExecutorService子类，这些类不属于JDK，是用户定义的新类，判断这些类是否是包装类。
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
		for(SootClass executorSubClass:executorSubClasses) {
			if(!setExecutorSubClass.contains(executorSubClass)) {
				allAdditionalClasses.add(executorSubClass);
			}
		}
		
		System.out.println("allAdditionalClasses: " + allAdditionalClasses);
	}
	
	/**
	 * 是否可以安全的重构，就是判断调用提交异步任务方法的变量是否是安全提交的几种执行器的对象之一。.
	 *
	 * @param invocStmt the invoc stmt
	 * 	@param	isSubmit 输入当前需要判断的函数名词是否为submit，否则是execute。
	 * @return 1, 如果可以进行重构;0,不可重构;2,需要得到绑定并进行判断.
	 */
	public static boolean canRefactor(MethodInvocation mInvocation,Stmt invocStmt , boolean isSubmit) {
		if(invocStmt == null) return false;
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
        			Set<String> completeSetTypeStrings;
        			if(isSubmit) {
        				completeSetTypeStrings = getCompleteExecutorSubClassesName();
        			}else {
        				completeSetTypeStrings = getAllExecutorSubClassesName();
        			}
        			if(typeSetStrings.isEmpty()) {
        				//说明没有被访问到，可以进行AST判断
        				
        				Expression exp = mInvocation.getExpression();
        				ITypeBinding typeBinding = exp.resolveTypeBinding();
        				String typeName = typeBinding.getQualifiedName();
        				if(typeBinding.isNested()) {
        					typeName = typeBinding.getBinaryName();
        				}
        				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]程序中没有访问到,进一步判断,类型为："+typeName);
        				if(completeSetTypeStrings.contains(typeName)) {
        					AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据ASTtypeBinding 可以重构");
        					return true;
        				}
        			}else if(completeSetTypeStrings.containsAll(typeSetStrings)) {
        				//是安全重构的子集，就可以进行重构了。
        				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据sootClass 可以重构");
        				return true;
        			}
        		}	
        	}
        	AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]不是安全重构的子集，进行排除");
        	return false;
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
	
	
	public static Set<String> getCompleteExecutorSubClassesName(){
		Set<String> completeSetTypeStrings = new HashSet<>();
		for(SootClass completeSC:mayCompleteExecutorSubClasses) {
			completeSetTypeStrings.add(completeSC.getName());
		}
		return completeSetTypeStrings;
	}

	public static Set<String> getAllDirtyExecutorSubClassName(){
		Set<String> allDirtyClassesStrings = new HashSet<>();
		for(SootClass allDirtyClass:allDirtyClasses) {
			allDirtyClassesStrings.add(allDirtyClass.getName());
		}
		return allDirtyClassesStrings;
	}
	
	public static Set<String> getAllExecutorSubClassesName(){
		Set<String> allExecutorSubClassesStrings = new HashSet<>();
		for(SootClass allExecutorSubClass:allExecutorSubClasses) {
			allExecutorSubClassesStrings.add(allExecutorSubClass.getName());
		}
		return allExecutorSubClassesStrings;
	}

	
}

