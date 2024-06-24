package refuture.sootUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.Future2Completable;
import refuture.refactoring.RefutureException;
import soot.Body;
import soot.G;
import soot.Hierarchy;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.spark.sets.DoublePointsToSet;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
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
	private static Set<SootClass>allExecutorServiceSubClasses;
	private static Set<SootClass>allExecutorSubClasses;
	private static Set<SootClass>mayCompleteExecutorClasses;
	//mustDirtyClass排除了proxyClass，且内部只包含实际类
	private static HashMap<SootClass,ClassInfo> resultMap;
	
	private static Set<SootClass>mustDirtyClasses;
	
	private static Set<SootClass>executeDirtyClasses;
	
	private static Set<SootClass>submitRDirtyClasses;
	
	private static Set<SootClass>submitCDirtyClasses;
	
	private static Set<SootClass>submitRVDirtyClasses;
	
	/** 包含我手动添加的jdk中自带的执行器类型,以及完全没有重写关键方法子类. */
	private static Set<SootClass>mayCompleteExecutorSubClasses;//存入可能可以重构的类型以及包装类。
	
	private static Set<SootClass>proxySubmitRClass;
	private static Set<SootClass>proxySubmitCClass;
	private static Set<SootClass>proxySubmitRVClass;
	
	/** The all appendent classes. */
//	private static Set<SootClass>allAdditionalClasses;
	
	/** 包括子接口和所有实现类限定名. */
	public static Set<String> callableSubClasses;
	
	/** 包括子接口和所有实现类限定名. */
	public static Set<String> runnablesubClasses;
	
	public static Set<String>allFutureSubClasses;
	/**
	 * Inits the static field.
	 *
	 * @return true, if successful
	 */
	public static boolean initStaticField() {
		mayCompleteExecutorSubClasses = new HashSet<SootClass>();
		mayCompleteExecutorClasses = new HashSet<SootClass>();
		allFutureSubClasses = new HashSet<String>();
		resultMap = new HashMap<>();
		proxySubmitRClass = new HashSet<SootClass>();
		proxySubmitCClass = new HashSet<SootClass>();
		proxySubmitRVClass = new HashSet<SootClass>();
		mustDirtyClasses = new HashSet<SootClass>();
		executeDirtyClasses = new HashSet<SootClass>();
		submitRDirtyClasses = new HashSet<SootClass>();
		submitCDirtyClasses = new HashSet<SootClass>();
		submitRVDirtyClasses = new HashSet<SootClass>();
		allExecutorServiceSubClasses= new HashSet<SootClass>();
		allExecutorSubClasses = new HashSet<SootClass>();
		callableSubClasses = new HashSet<String>();
		runnablesubClasses = new HashSet<String>();
		return true;
	}
	public static void futureAnalysis() {
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		SootClass future = Scene.v().getSootClass("java.util.concurrent.Future");
		hierarchy.getImplementersOf(future).forEach((e)->{
			e = (SootClass)e;
			allFutureSubClasses.add(e.getName());
			});
		hierarchy.getSubinterfacesOfIncluding(future).forEach((e)->{
			e = (SootClass)e;
			allFutureSubClasses.add(e.getName());
			});
		AnalysisUtils.debugPrint("allFutureSubClasses:"+allFutureSubClasses.toString());
		
	}
	
	public static void taskTypeAnalysis() {
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		SootClass callable = Scene.v().getSootClass("java.util.concurrent.Callable");
		SootClass runnable = Scene.v().getSootClass("java.lang.Runnable");
		hierarchy.getImplementersOf(callable).forEach((e)->{
			e = (SootClass)e;
			callableSubClasses.add(e.getName());
			});
		hierarchy.getSubinterfacesOfIncluding(callable).forEach((e)->{
			e = (SootClass)e;
			callableSubClasses.add(e.getName());
			});
		hierarchy.getImplementersOf(runnable).forEach((e)->{
			e = (SootClass)e;
			runnablesubClasses.add(e.getName());
			});
		hierarchy.getSubinterfacesOfIncluding(runnable).forEach((e)->{
			e = (SootClass)e;
			runnablesubClasses.add(e.getName());
			});
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("CallableSubClasses:"+callableSubClasses.toString());
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("RunnableSubClasses:"+runnablesubClasses.toString());
	}

	public static void executorSubClassAnalysis() {
		SootClass executorClass = Scene.v().getSootClass("java.util.concurrent.Executor");
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		allExecutorSubClasses.addAll(hierarchy.getImplementersOf(executorClass));
		allExecutorSubClasses.addAll(hierarchy.getSubinterfacesOfIncluding(executorClass));
		for(SootClass sc : allExecutorSubClasses) {
			SootMethod sm = getMethodInHierarchy(sc,"void execute(java.lang.Runnable)");
			if(sm != null &&sm.isConcrete()) {
				if(Instanceof.useInstanceofRunnable(sm)) {
					mustDirtyClasses.add(sc);
					executeDirtyClasses.add(sc);
				}else {
					mayCompleteExecutorClasses.add(sc);
				}
			}
		}
		AnalysisUtils.debugPrint("Executor-mustDirtyClasses:"+mustDirtyClasses.toString());
	}
	
	public static SootMethod getMethodInHierarchy(SootClass sc ,String subsignature) {
		if(sc.declaresMethod(subsignature)) {
			return sc.getMethod(subsignature);
		}else {
			if(sc.hasSuperclass()) {
				return getMethodInHierarchy(sc.getSuperclass(),subsignature);
			}
			return null;
		}
		
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
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		List<SootClass> serviceSubImplementers = hierarchy.getImplementersOf(executorServiceClass);
		allExecutorServiceSubClasses.addAll(serviceSubImplementers);
		List<SootClass> serviceSubInterfaces = hierarchy.getSubinterfacesOfIncluding(executorServiceClass);
		allExecutorServiceSubClasses.addAll(serviceSubInterfaces);
		SootClass AbsExecutorServiceClass = Scene.v().getSootClass("java.util.concurrent.AbstractExecutorService");
		SootClass ForkJoinPoolClass = Scene.v().getSootClass("java.util.concurrent.ForkJoinPool");
		SootClass SThreadPoolExecutorClass = Scene.v().getSootClass("java.util.concurrent.ScheduledThreadPoolExecutor");
		mayCompleteExecutorSubClasses.add(ForkJoinPoolClass);
		mayCompleteExecutorSubClasses.add(SThreadPoolExecutorClass);
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.ThreadPoolExecutor"));
		List<SootClass> AbsExecutorServiceSubClasses = hierarchy.getSubclassesOf(AbsExecutorServiceClass);

		/** The all dirty classes. */
		Set<SootClass> allDirtyClasses = new HashSet<SootClass>(mustDirtyClasses);
		for(SootClass tPESubClass : AbsExecutorServiceSubClasses) {
			boolean confantFlag = true;
			boolean flag1,flag2,flag3,flag4,flag5 = false;
			if(mayCompleteExecutorSubClasses.contains(tPESubClass)||allDirtyClasses.contains(tPESubClass)) {
				continue;
			}
			//判断是否是dirtyClass
			if(hierarchy.isClassSubclassOf(tPESubClass, ForkJoinPoolClass)||hierarchy.isClassSubclassOf(tPESubClass, SThreadPoolExecutorClass)) {
				flag1 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)");
				flag2 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)");
				flag3 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable)");
				flag4 = tPESubClass.declaresMethod("void execute(java.lang.Runnable)");
			}else {
				flag1 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)");
				flag2 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)");
				flag3 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable)");
				flag4 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.util.concurrent.Callable)");
				flag5 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.lang.Runnable,java.lang.Object)");
			}
			if(flag1||flag2||flag3||flag4||flag5) {
				allDirtyClasses.add(tPESubClass);
				List<SootClass> dirtySubClasses = hierarchy.getSubclassesOf(tPESubClass);
				for(SootClass dirtyclass:dirtySubClasses) {
					allDirtyClasses.add(dirtyclass);
					if(mayCompleteExecutorSubClasses.contains(dirtyclass)) {
						mayCompleteExecutorSubClasses.remove(dirtyclass);
					}
				}
			}else {
				mayCompleteExecutorSubClasses.add(tPESubClass);
			}
		}
		//处理proxy类。
		//将这个类进行特殊处理。
		// 因为它肯定只会是安全的。
		SootClass fDEsc = Scene.v().getSootClass("java.util.concurrent.Executors$FinalizableDelegatedExecutorService");
		if(allDirtyClasses.contains(fDEsc)) {
			mayCompleteExecutorSubClasses.add(fDEsc);
		}
		//防止将这个类加入到mustDiryClasses
		mayCompleteExecutorSubClasses.add(AbsExecutorServiceClass);
		for(SootClass executorServiceImplemente:serviceSubImplementers) {
			if(mayCompleteExecutorSubClasses.contains(executorServiceImplemente)) {
				continue;
			}
			int count = 0;
			if(!proxySubmitRClass.contains(executorServiceImplemente)) {
				submitRDirtyClasses.add(executorServiceImplemente);
				count++;
			}
			if(!proxySubmitCClass.contains(executorServiceImplemente)) {
				submitCDirtyClasses.add(executorServiceImplemente);
				count++;
			}
			if(!proxySubmitRVClass.contains(executorServiceImplemente)) {
				submitRVDirtyClasses.add(executorServiceImplemente);
				count++;
			}
			if(count ==3) {
				mustDirtyClasses.add(executorServiceImplemente);
			}
			
		}
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("mayCompleteExecutorSubClasses:"+mayCompleteExecutorSubClasses.toString());
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("mustDirtyClasses:"+mustDirtyClasses.toString());
	}

	/**
	 * 这个类用来分析额外的ExecutorService子类，这些类不属于JDK，是用户定义的新类，判断这些类是否是包装类。
	 */
//	public static void additionalExecutorServiceSubClassAnalysis() {
//		Set<SootClass>setExecutorSubClass = new HashSet<SootClass>();
//		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedScheduledExecutorService"));
//		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.ScheduledThreadPoolExecutor"));
//		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.ForkJoinPool"));
//		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.ThreadPoolExecutor"));
//		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$FinalizableDelegatedExecutorService"));
//		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedExecutorService"));
//		setExecutorSubClass.add(Scene.v().getSootClass("java.util.concurrent.AbstractExecutorService"));
//		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
//		SootClass executorServiceClass = Scene.v().getSootClass("java.util.concurrent.ExecutorService");
//		List<SootClass> executorSubClasses = hierarchy.getImplementersOf(executorServiceClass);
//		for(SootClass executorSubClass:executorSubClasses) {
//			if(!setExecutorSubClass.contains(executorSubClass)) {
//				allAdditionalClasses.add(executorSubClass);
//			}
//		}
//	}
	
	public static void wrapperClassAnalysis() {
		//先通过工作列表算法，找到潜在的代理类。潜在的代理类是指，存在对应的字段和方法调用。但方法expression未判断是否实际指向字段，
		//因为当类不是完全的类时，可能无法准确判断。
//		HashMap<SootClass,ClassInfo> resultMap = new HashMap<>();
		Queue<SootClass> workList = new LinkedList<>();
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		SootClass executorSC = Scene.v().getSootClass("java.util.concurrent.Executor");
		List<String> allExecutorInterfacesName = new ArrayList<>();
		hierarchy.getSubinterfacesOfIncluding(executorSC).forEach((e)->{
			allExecutorInterfacesName.add(e.getName());
		});
		workList.add(executorSC); 
		while(!workList.isEmpty()) {
			SootClass currentClass = workList.poll();
			//首先是否包含未处理的Executor继承树上的直接父类。
			boolean containUnprocessSupClass = false;
			for(SootClass directSupClass4ExecutorFamily:getDirectSupClasses4ExecutorFamily(currentClass)) {
					if(!resultMap.containsKey(directSupClass4ExecutorFamily)) {
						containUnprocessSupClass = true;
						break;
					}
			}
			if(containUnprocessSupClass) {
//				workList.add(currentClass); 不需要再添加了，因为有其它父类处理完成时，自然会添加它。
				continue;
			}
			//若未含有未处理的继承树上的直接父类，则开始处理
			ClassInfo currentClassInfo = null;
			//1.处理继承问题，将父类的信息继承过来。
			//1.1 先处理父接口
			for(SootClass supInterface:getDirectSupInterfaces4ExecutorFamily(currentClass)) {
				ClassInfo supInterfaceInfo = resultMap.get(supInterface);
				currentClassInfo = processSupInterfaceInfo(currentClassInfo,supInterfaceInfo);
			}
			//1.2 处理父类
			SootClass supClass = getDirectSupCommonClass4ExecutorFamily(currentClass);
			if(supClass != null) {
				ClassInfo supClassInfo = resultMap.get(supClass);
				currentClassInfo = processSupClass(currentClassInfo,supClassInfo);
			}
			//2 开始分析当前类定义.
			if(currentClassInfo == null) currentClassInfo = new ClassInfo();//其实，这个只对应了Executor接口类型分析时，会运行。
			// 2.1 分析字段
			for(SootField currentField :currentClass.getFields()) {
				if(!currentField.isStatic()) {
					if(allExecutorInterfacesName.contains(currentField.getType().toQuotedString())) {
						currentClassInfo.declarField = true;
						currentClassInfo.fieldSignatures.add(currentField.getSignature()) ;
					}
				}
			}
			// 2.2 分析方法
			if(currentClass.declaresMethod("void execute(java.lang.Runnable)")) {
				currentClassInfo.delcarExecute = true;
				currentClassInfo.executeSignature = null;
				SootMethod currentMethod = currentClass.getMethod("void execute(java.lang.Runnable)") ;
				if(currentMethod.isConcrete()) {
					Body body = currentMethod.retrieveActiveBody();
					for(Unit e:body.getUnits()) {
						Stmt stmt = (Stmt) e;
						if(stmt.containsInvokeExpr()&&stmt.getInvokeExpr().getMethod().getSubSignature().equals("void execute(java.lang.Runnable)")){
							currentClassInfo.executeSignature = currentMethod.getSignature();
							break;
						}
					}
				}
			}
			
			if(currentClass.declaresMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)")){
				currentClassInfo.declarSubmitC = true;
				currentClassInfo.submitCSignature = null;
				SootMethod currentMethod = currentClass.getMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)") ;
				if(currentMethod.isConcrete()) {
					Body body = currentMethod.retrieveActiveBody();
					for(Unit e:body.getUnits()) {
						Stmt stmt = (Stmt) e;
						if(stmt.containsInvokeExpr()&&stmt.getInvokeExpr().getMethod().getSubSignature().equals("java.util.concurrent.Future submit(java.util.concurrent.Callable)")){
							currentClassInfo.submitCSignature = currentMethod.getSignature();
							break;
						}
					}
				}
			
			}
			if(currentClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)")) {
				currentClassInfo.declarSubmitRV = true;
				currentClassInfo.submitRVSignature = null;
				SootMethod currentMethod = currentClass.getMethod("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)") ;
				if(currentMethod.isConcrete()) {
					Body body = currentMethod.retrieveActiveBody();
					for(Unit e:body.getUnits()) {
						Stmt stmt = (Stmt) e;
						if(stmt.containsInvokeExpr()&&stmt.getInvokeExpr().getMethod().getSubSignature().equals("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)")){
							currentClassInfo.submitRVSignature = currentMethod.getSignature();
							break;
						}
					}
				}
			}
			if(currentClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable)")) {
				currentClassInfo.declarSubmitR = true;
				currentClassInfo.submitRSignature = null;
				SootMethod currentMethod = currentClass.getMethod("java.util.concurrent.Future submit(java.lang.Runnable)") ;
				if(currentMethod.isConcrete()) {
					Body body = currentMethod.retrieveActiveBody();
					for(Unit e:body.getUnits()) {
						Stmt stmt = (Stmt) e;
						if(stmt.containsInvokeExpr()&&stmt.getInvokeExpr().getMethod().getSubSignature().equals("java.util.concurrent.Future submit(java.lang.Runnable)")){
							currentClassInfo.submitRSignature = currentMethod.getSignature();
							break;
						}
					}
				}
			}
			//做完最后的处理，将info和SootClass加入resultMap。
			resultMap.put(currentClass, currentClassInfo);
			//将子类加入WorkList。
			for(SootClass directSubClass:getDirectSubClasses(currentClass)) {
				if (!resultMap.containsKey(directSubClass) ) {
					workList.add(directSubClass);
				}
			}
			
		}
		//现在开始处理是否指向字段，以及内部是否安全的代理方法。
		for(SootClass currentClass:resultMap.keySet()) {
			//0. 手动加入可以重构的内部类型
			if(currentClass.getName() == "java.util.concurrent.Executors$DelegatedExecutorService"||
					currentClass.getName() == "java.util.concurrent.Executors$DelegatedScheduledExecutorService") {
					proxySubmitRVClass.add(currentClass);
					proxySubmitCClass.add(currentClass);
					proxySubmitRClass.add(currentClass);
			}
			//1. 找到非抽象类进行判断。
			if(currentClass.isConcrete()) {
				ClassInfo currentClassInfo = resultMap.get(currentClass);
				//2.判断是否签名都齐全
				if(currentClassInfo.hasAllSignature()) {
					//3.挨个判断，符合要求，将其加入到对应的集合中，用于后续的判断。此时假定有4个集合，分别对应excute,submit(三种情况）
					Stack<String> currentFieldSignatures = currentClassInfo.fieldSignatures;
					// 3.1execute判断 execute不需要自己建立一个集合，但是execute是代理方法，是下面3个集合能够重构的基础，
					// 在这个基础上可以进行clone判断。
					SootMethod currentExecuteMethod = Scene.v().getMethod(currentClassInfo.executeSignature);
					Body executeBody = currentExecuteMethod.retrieveActiveBody();
					Stmt invocStmt = null;
					JimpleLocal realExecutor = null;
					for(Unit u : executeBody.getUnits()) {
						invocStmt = (Stmt) u;
						if(invocStmt.containsInvokeExpr()&&invocStmt.getInvokeExpr().getMethod().getSubSignature().equals(currentExecuteMethod.getSubSignature())) {
							realExecutor = getReceiverLocal4InvocStmt(invocStmt);
							if(realExecutor == null) {
								throw new RefutureException("出现错误，无法得到receiverLocal");
							}
							break;
						}
					}
					if(isFieldInvoc(executeBody,invocStmt,realExecutor,currentFieldSignatures)) {
						//此时，execute已经符合要求了。
						// 3.2submitR判断
						if(submitRcloneAnalysis(currentExecuteMethod,Scene.v().getMethod(currentClassInfo.submitRSignature))) {
							proxySubmitRClass.add(currentClass);
						}
						// 3.3submitC判断
						if(submitCcloneAnalysis(currentExecuteMethod,Scene.v().getMethod(currentClassInfo.submitCSignature))) {
							proxySubmitCClass.add(currentClass);
						}
						// 3.4submitRV判断
						if(submitRVcloneAnalysis(currentExecuteMethod,Scene.v().getMethod(currentClassInfo.submitRVSignature))) {
							proxySubmitRVClass.add(currentClass);
						}
					}
					
				}
			}
		}
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("proxySubmitRClass:"+proxySubmitRClass.toString());
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("proxySubmitRClass:"+proxySubmitCClass.toString());
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("proxySubmitRClass:"+proxySubmitRVClass.toString());
	}
	private static boolean submitRcloneAnalysis(SootMethod executeMethod,SootMethod submitMethod) {
		List<String> executeUnits = new ArrayList<>();
		HashMap<Unit,String> executeThisInvocMap = getRealThisInvocString(executeMethod);
		executeMethod.retrieveActiveBody().getUnits().forEach((e)->{
			//execute只需要将this的方法调用进行改变。
			String executeStmtString = e.toString();
			if(executeThisInvocMap.containsKey(e)) {
				executeStmtString = executeThisInvocMap.get(e);
			}
			executeUnits.add(executeStmtString);
		});
		//接下来将return  变量去除，然后将该变量的assign语句变成一个单纯的方法调用语句，也就是将这个变量和等号去除。
		List<String> submitUnits = new ArrayList<>();
		Body submitBody = submitMethod.retrieveActiveBody();
		ReturnStmt rs = getReturnStmt(submitMethod);
		JimpleLocal jl = (JimpleLocal)rs.getOp();
		List<Unit> defStmts = getDefsStmt(jl, rs, submitBody);
		HashMap<Unit,String> submitThisInvocMap = getRealThisInvocString(submitMethod);
		for(Unit u: submitBody.getUnits()) {
			Stmt ss = (Stmt)u;
			String unitString = u.toString();
			// 将this的方法调用更改为真实的。
			if(submitThisInvocMap.containsKey(u)) {
				unitString = submitThisInvocMap.get(u);
			}
			if(u == rs) {
				//return改变
				unitString = "return";
			}
			if(defStmts.contains(u)) {
				//去除左手变量和=号
				String[] parts = unitString.split("=");
				unitString = parts[parts.length -1].trim();
			}
			//将关键的方法调用的签名进行模糊。
			if(ss.containsInvokeExpr()&&unitString.contains("java.util.concurrent.Future submit(java.lang.Runnable)>")) {
				unitString = unitString.replace("java.util.concurrent.Future submit(java.lang.Runnable)>", "void execute(java.lang.Runnable)>");
			}
			submitUnits.add(unitString);
		}
		if(executeUnits.equals(submitUnits)) {
			return true;
		}
		return false;
	}
	
	private static boolean submitCcloneAnalysis(SootMethod executeMethod,SootMethod submitMethod) {
		List<String> executeUnits = new ArrayList<>();
		HashMap<Unit,String> executeThisInvocMap = getRealThisInvocString(executeMethod);
		executeMethod.retrieveActiveBody().getUnits().forEach((e)->{
			//execute只需要将this的方法调用进行改变。
			String executeStmtString = e.toString();
			if(executeThisInvocMap.containsKey(e)) {
				executeStmtString = executeThisInvocMap.get(e);
			}
			executeUnits.add(executeStmtString);
		});
		//接下来将return  变量去除，然后将该变量的assign语句变成一个单纯的方法调用语句，也就是将这个变量和等号去除。
		List<String> submitUnits = new ArrayList<>();
		Body submitBody = submitMethod.retrieveActiveBody();
		ReturnStmt rs = getReturnStmt(submitMethod);
		JimpleLocal jl = (JimpleLocal)rs.getOp();
		List<Unit> defStmts = getDefsStmt(jl, rs, submitBody);
		HashMap<Unit,String> submitThisInvocMap = getRealThisInvocString(submitMethod);
		for(Unit u: submitBody.getUnits()) {
			Stmt ss = (Stmt)u;
			String unitString = u.toString();
			//需要将形式参数中，callable进行修改为Runnable
			if(unitString.contains("@parameter0: java.util.concurrent.Callable")) {
				unitString = unitString.replace("java.util.concurrent.Callable", "java.lang.Runnable");
			}
			// 将this的方法调用更改为真实的。
			if(submitThisInvocMap.containsKey(u)) {
				unitString = submitThisInvocMap.get(u);
			}
			if(u == rs) {
				//return改变
				unitString = "return";
			}
			if(defStmts.contains(u)) {
				//去除左手变量和=号
				String[] parts = unitString.split("=");
				unitString = parts[parts.length -1].trim();
			}
			//将关键的方法调用的签名进行模糊。
			if(ss.containsInvokeExpr()&&unitString.contains("java.util.concurrent.Future submit(java.util.concurrent.Callable)>")) {
				unitString = unitString.replace("java.util.concurrent.Future submit(java.util.concurrent.Callable)>", "void execute(java.lang.Runnable)>");
			}

			submitUnits.add(unitString);
		}
		if(executeUnits.equals(submitUnits)) {
			return true;
		}
		return false;
	}
	private static boolean submitRVcloneAnalysis(SootMethod executeMethod,SootMethod submitMethod) {
		List<String> executeUnits = new ArrayList<>();
		HashMap<Unit,String> executeThisInvocMap = getRealThisInvocString(executeMethod);
		executeMethod.retrieveActiveBody().getUnits().forEach((e)->{
			//execute只需要将this的方法调用进行改变。
			String executeStmtString = e.toString();
			if(executeThisInvocMap.containsKey(e)) {
				executeStmtString = executeThisInvocMap.get(e);
			}
			executeUnits.add(executeStmtString);
		});
		//接下来将return  变量去除，然后将该变量的assign语句变成一个单纯的方法调用语句，也就是将这个变量和等号去除。
		List<String> submitUnits = new ArrayList<>();
		Body submitBody = submitMethod.retrieveActiveBody();
		ReturnStmt rs = getReturnStmt(submitMethod);
		JimpleLocal rjl = (JimpleLocal)rs.getOp();
		List<Unit> defStmts = getDefsStmt(rjl, rs, submitBody);
		HashMap<Unit,String> submitThisInvocMap = getRealThisInvocString(submitMethod);
		String para = new String();
		List<JimpleLocal> needChangeNameLocals = new ArrayList<>();
		for(Unit u: submitBody.getUnits()) {
			Stmt ss = (Stmt)u;
			String unitString = u.toString();
			// 去除额外的参数的初始化。
			if(ss instanceof JIdentityStmt&&unitString.contains("@parameter1:")) {
				JIdentityStmt jis = (JIdentityStmt)ss;
				JimpleLocal pjl = (JimpleLocal) jis.getLeftOp();
				Iterator it = submitBody.getLocals().snapshotIterator();
				boolean flag = false;
				while(it.hasNext()) {
					JimpleLocal current = (JimpleLocal) it.next();
					if(flag) {
						if(current != rjl&&needChangeName(pjl,current)) {
							needChangeNameLocals.add(current);
						}
					}else {
						if(current == pjl) {
							flag = true;
						}
					}
				}
				
				String[] parts = unitString.split(":=");
				para = ", "+parts[0].trim();
				continue;
			}
			// 将this的方法调用更改为真实的。
			if(submitThisInvocMap.containsKey(u)) {
				unitString = submitThisInvocMap.get(u);
			}
			if(u == rs) {
				//return改变
				unitString = "return";
			}
			if(defStmts.contains(u)) {
				//去除左手变量和=号
				String[] parts = unitString.split("=");
				unitString = parts[parts.length -1].trim();
			}
			//将关键的方法调用的签名进行模糊。
			if(ss.containsInvokeExpr()&&unitString.contains("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)>")) {
				unitString = unitString.replace("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)>", "void execute(java.lang.Runnable)>");
				unitString = unitString.replace(para, "");
			}
			//将数字-1
			for(JimpleLocal ncnl :needChangeNameLocals) {
				if(localInTheStmt(ncnl, ss)) {
					String oldLocal = Matcher.quoteReplacement(ncnl.toString());
					String newLocal = Matcher.quoteReplacement(getNameLow1ForLocal(ncnl));
					unitString = unitString.replaceAll(oldLocal, newLocal);
				}
			}
			submitUnits.add(unitString);
		}
		if(executeUnits.equals(submitUnits)) {
			return true;
		}
		return false;
	}
	private static boolean needChangeName(JimpleLocal pjl,JimpleLocal currentLocal) {
		//判断是否需要将当前的名称进行修改
		if(removeSpecialCharacters(pjl.toString()).equals(removeSpecialCharacters(currentLocal.toString()))) {
			return true;
		}
		return false;
	}
    public static String removeSpecialCharacters(String input) {
        // 使用正则表达式匹配并替换特殊字符
        String pattern = "[^a-zA-Z]";
        String replacement = "";
        String result = Pattern.compile(pattern).matcher(input).replaceAll(replacement);
        return result;
    }
	
	private static String getNameLow1ForLocal(JimpleLocal jl) {
		String localName =jl.toString();
	    Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(localName);

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            String numericSubstring = matcher.group();
            int numericValue = Integer.parseInt(numericSubstring);
            int newValue = numericValue - 1;
            String newNumericSubstring = String.valueOf(newValue);
            sb.append(localName, lastEnd, matcher.start());
            sb.append(newNumericSubstring);
            lastEnd = matcher.end();
        }
        sb.append(localName.substring(lastEnd));
		return sb.toString();
	}
	private static boolean localInTheStmt(JimpleLocal jl ,Stmt stmt) {
		for(ValueBox vb :stmt.getUseAndDefBoxes()) {
			if(vb.getValue() == jl) {
				return true;
			}
		}
		return false;
	}
	private static HashMap<Unit,String> getRealThisInvocString(SootMethod method){
		JimpleLocal executeThis = (JimpleLocal) method.retrieveActiveBody().getThisLocal();
		HashMap<Unit,String> thisInvocMap = new HashMap<>();
		method.retrieveActiveBody().getUnits().forEach((e)->{
			Stmt s1 = (Stmt)e;
			if(s1.containsInvokeExpr()&&useLocalInInvocStmt(executeThis, s1)) {
				String realMethodString = s1.getInvokeExpr().getMethod().getSignature();
				String originString = s1.toString();
				realMethodString = realMethodString.replaceAll("\\$", "\\\\\\$");
				String result = originString.replaceFirst("<.*?>", realMethodString);
				thisInvocMap.put(e, result);
			}
		});
		return thisInvocMap;
	}
	
	private static boolean useLocalInInvocStmt(Local l ,Stmt stmt) {
		for(ValueBox vb :stmt.getInvokeExpr().getUseBoxes()) {
			if (vb instanceof JimpleLocalBox) {
				if(vb.getValue() == l) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	private static JimpleLocal getReceiverLocal4InvocStmt(Stmt stmt) {
		for(ValueBox vb:stmt.getUseBoxes()) {
			if(vb instanceof JimpleLocalBox) {
				return (JimpleLocal) vb.getValue();
			}
		}
		return null;
	}
	
	private static boolean isFieldInvoc(Body currentBody ,Stmt stmt,JimpleLocal realExecutor,Stack<String> fieldSignature) {
		for(Unit u:getDefsStmt(realExecutor, stmt,currentBody)) {
			Stmt s = (Stmt)u;
			if(s.containsFieldRef()) {
				SootField sf = s.getFieldRef().getField();
				if(fieldSignature.contains(sf.getSignature())) return true;
				continue;
			}else if(s.containsInvokeExpr()) {
				//这种情况下，就是去找这个方法调用的return .
				SootMethod nsm = s.getInvokeExpr().getMethod();
				ReturnStmt returnStmt = getReturnStmt(nsm);
				JimpleLocal njl = (JimpleLocal) returnStmt.getOp();
				return isFieldInvoc(nsm.retrieveActiveBody(),returnStmt,njl,fieldSignature);
			}else if(isCastExpr(s)){
				 String typeName = getTypeName4Cast((AbstractDefinitionStmt) s);
				 SootClass sc = Scene.v().getSootClass(typeName);
				 if(sc.isInterface()&&allExecutorSubClasses.contains(sc)) {
					 JimpleLocal cal = getUseLocal4Cast((AbstractDefinitionStmt) s);
					 return isFieldInvoc(currentBody,s,cal,fieldSignature);
				 }
				 continue;
			}else {
				continue;
			}
		}
		return false;
	}
	
	private static JimpleLocal getUseLocal4Cast(AbstractDefinitionStmt stmt) {
		JCastExpr v =(JCastExpr)stmt.getRightOp();
		return (JimpleLocal) v.getOp();
	}
	private static String getTypeName4Cast(AbstractDefinitionStmt stmt) {
		JCastExpr v =(JCastExpr)stmt.getRightOp();
		return v.getCastType().toQuotedString();
	}
	
	public static boolean isCastExpr(Stmt stmt) {
		if(stmt instanceof AbstractDefinitionStmt) {
			AbstractDefinitionStmt jas = (AbstractDefinitionStmt) stmt;
			Value v =jas.getRightOp();
			if(v instanceof JCastExpr) {
				return true;
			}
		}
		return false;
	}
	public static List<Unit> getDefsStmt(JimpleLocal jl,Stmt stmt,Body body) {
		LocalDefs ld = G.v().soot_toolkits_scalar_LocalDefsFactory().newLocalDefs(body);
		return ld.getDefsOfAt(jl, stmt);
	}
	
	
	private static ReturnStmt getReturnStmt(SootMethod sm) {
		Body deleBody = sm.retrieveActiveBody();
		UnitGraph cfg = new BriefUnitGraph(deleBody);
		for(Unit u:cfg.getTails()) {
//			if(u.toString().contains("return")) {
//				return (Stmt)u;
//			}
			if(u instanceof ReturnStmt) {
				return (ReturnStmt)u;
			}
		}
		return null;
	}
	
	private static ClassInfo processSupClass(ClassInfo currentClassInfo, ClassInfo supClassInfo) {
		if(currentClassInfo == null) {
			currentClassInfo = new ClassInfo(supClassInfo);
			return currentClassInfo;
		}
		if(supClassInfo.declarField) {
			currentClassInfo.declarField = true;
			currentClassInfo.fieldSignatures.addAll(supClassInfo.fieldSignatures);
		}
		if(supClassInfo.delcarExecute) {
			currentClassInfo.delcarExecute = true;
			currentClassInfo.executeSignature = supClassInfo.executeSignature;
		}
		if(supClassInfo.declarSubmitR) {
			currentClassInfo.declarSubmitR = true;
			currentClassInfo.submitRSignature = supClassInfo.submitRSignature;
		}
		if(supClassInfo.declarSubmitC) {
			currentClassInfo.declarSubmitC = true;
			currentClassInfo.submitCSignature = supClassInfo.submitCSignature;
		}
		if(supClassInfo.declarSubmitRV) {
			currentClassInfo.declarSubmitRV = true;
			currentClassInfo.submitRVSignature = supClassInfo.submitRVSignature;
		}
		return currentClassInfo;
	}
	private static ClassInfo processSupInterfaceInfo(ClassInfo currentClassInfo, ClassInfo supInterfaceInfo) {
		//这里已经限制了当前处理的不是接口，因为接口不能实现接口。
		//如果两个及以上的接口同时声明了一个方法，那么有冲突的方法会遵守需要重新实现。
		//对多个接口的实现就是求异运算。都实现了，就相当于没实现。声明用或运算，只要有声明，说明这个类目前是声明这个方法或者字段的。
		//有一个实现了，另一个没实现，其实也相当于没实现，因为都是Executor接口或其子类，肯定声明了execute这个方法，所以，需要根据是否声明相同的方法，若声明了相同的方法，
		//则一定是null,两个都实现了有冲突，一个实现一个没实现，有冲突，只有两个都没实现，才不冲突。
		if(currentClassInfo == null) {
			currentClassInfo = new ClassInfo(supInterfaceInfo);
			return currentClassInfo;
		}
		//开始异或运算
		//接口肯定没有字段定义，所以直接赋值null。不操作也不会错。
//		currentClassInfo.fieldSignature = new HashSet<>();
		//判断是否同时声明了execute方法，并且还需要判断该Signature是否完全一致，实际上在一个方法中声明的，因为这决定了是否可以兼容。
		if(currentClassInfo.executeSignature != null&&supInterfaceInfo.executeSignature != null) {
			if(!currentClassInfo.executeSignature.equals(supInterfaceInfo.executeSignature)) {
				currentClassInfo.executeSignature = null;
			}
		}else {
			currentClassInfo.executeSignature = currentClassInfo.executeSignature == null ? supInterfaceInfo.executeSignature:currentClassInfo.executeSignature;
		}
		
		if(currentClassInfo.submitCSignature != null&&supInterfaceInfo.submitCSignature != null) {
			if(!currentClassInfo.submitCSignature.equals(supInterfaceInfo.submitCSignature)) {
				currentClassInfo.submitCSignature = null;
			}
		}else {
			currentClassInfo.submitCSignature = currentClassInfo.submitCSignature == null ? supInterfaceInfo.submitCSignature:currentClassInfo.submitCSignature;
		}

		if(currentClassInfo.submitRSignature != null&&supInterfaceInfo.submitRSignature != null) {
			if(!currentClassInfo.submitRSignature.equals(supInterfaceInfo.submitRSignature)) {
				currentClassInfo.submitRSignature = null;
			}
		}else {
			currentClassInfo.submitRSignature = currentClassInfo.submitRSignature == null ? supInterfaceInfo.submitRSignature:currentClassInfo.submitRSignature;
		}
		
		if(currentClassInfo.submitRVSignature != null&&supInterfaceInfo.submitRVSignature != null) {
			if(!currentClassInfo.submitRVSignature.equals(supInterfaceInfo.submitRVSignature)) {
				currentClassInfo.submitRVSignature = null;
			}
		}else {
			currentClassInfo.submitRVSignature = currentClassInfo.submitRVSignature == null ? supInterfaceInfo.submitRVSignature:currentClassInfo.submitRVSignature;
		}
		
		//开始或运算
		if(currentClassInfo.declarField||supInterfaceInfo.declarField) {
			currentClassInfo.declarField = true;
		}
		if(currentClassInfo.delcarExecute||supInterfaceInfo.delcarExecute) {
			currentClassInfo.delcarExecute = true;
		}
		if(currentClassInfo.declarSubmitR||supInterfaceInfo.declarSubmitR) {
			currentClassInfo.declarSubmitR = true;
		}
		if(currentClassInfo.declarSubmitC||supInterfaceInfo.declarSubmitC) {
			currentClassInfo.declarSubmitC = true;
		}
		if(currentClassInfo.declarSubmitRV||supInterfaceInfo.declarSubmitRV) {
			currentClassInfo.declarSubmitRV = true;
		}
		return currentClassInfo;
	}
	private static SootClass getDirectSupCommonClass4ExecutorFamily(SootClass sc){
		if(sc.isInterface()) {
			return null;
		}
		if(isExecutorSubClass(sc.getSuperclass())) return sc.getSuperclass();
		return null;
	}
	private static Set<SootClass> getDirectSupInterfaces4ExecutorFamily(SootClass sc){
		Set<SootClass> directSupInterfaces4ExecutorFamily = new HashSet<>();
		for(SootClass currentInterface : sc.getInterfaces()) {
			if(isExecutorSubClass(currentInterface)) {
				directSupInterfaces4ExecutorFamily.add(currentInterface);
			}
		}
		return directSupInterfaces4ExecutorFamily;
	}
	
	private static Set<SootClass> getDirectSupClasses4ExecutorFamily(SootClass sc){
		Set<SootClass> directSupClasses4ExecutorFamily = new HashSet<>();
		SootClass supClass = getDirectSupCommonClass4ExecutorFamily(sc);
		if(supClass !=null) directSupClasses4ExecutorFamily.add(getDirectSupCommonClass4ExecutorFamily(sc));
		directSupClasses4ExecutorFamily.addAll(getDirectSupInterfaces4ExecutorFamily(sc));
		return directSupClasses4ExecutorFamily;
	}
	
	
	public static boolean isExecutorSubClass(SootClass sc) {
		if(allExecutorSubClasses.contains(sc))	return true;
		return false;
	}
	//包含直接的子类和子接口
	public static Set<SootClass> getDirectSubClasses(SootClass sc){
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		Set<SootClass> alldirectSubClass = new HashSet<>();
		if(sc.isInterface()) {
			alldirectSubClass.addAll(hierarchy.getDirectSubinterfacesOf(sc));
			alldirectSubClass.addAll(hierarchy.getDirectImplementersOf(sc));
		}else {
			alldirectSubClass.addAll(hierarchy.getDirectSubclassesOf(sc));
		}
		return alldirectSubClass;
	}

	static class ClassInfo{
		// declar开头的字段，标记着当前类是否声明了字段或者方法，是指实际上是否声明了
		// 与父类有关的为_signature字段，保存着实际的定义
		boolean declarField;
		boolean delcarExecute;
		boolean declarSubmitR;
		boolean declarSubmitC;
		boolean declarSubmitRV;
		Stack<String> fieldSignatures;
		String executeSignature;
		String submitRSignature;
		String submitCSignature;
		String submitRVSignature;
		
		public ClassInfo() {
			declarField = false;
			delcarExecute = false;
			declarSubmitR = false;
			declarSubmitC = false;
			declarSubmitRV = false;
			fieldSignatures = new Stack<String>();
			executeSignature = null;
			submitRSignature = null;
			submitCSignature = null;
			submitRVSignature = null;
		}
		public ClassInfo(ClassInfo superClassInfo) {
			declarField = superClassInfo.declarField;
			delcarExecute = superClassInfo.delcarExecute;
			declarSubmitR = superClassInfo.declarSubmitR;
			declarSubmitC = superClassInfo.declarSubmitC;
			declarSubmitRV = superClassInfo.declarSubmitRV;
			fieldSignatures = superClassInfo.fieldSignatures.stream().collect(Collectors.toCollection(Stack::new));
			executeSignature = superClassInfo.executeSignature;
			submitRSignature = superClassInfo.submitRSignature;
			submitCSignature = superClassInfo.submitCSignature;
			submitRVSignature = superClassInfo.submitRVSignature;
		}
		public boolean hasAllSignature() {
			if( fieldSignatures.isEmpty()) {
				return false;
			}
			if(executeSignature == null || executeSignature.isEmpty()) {
				return false;
			}
			if(submitRSignature == null || submitRSignature.isEmpty()) {
				return false;
			}
			if(submitCSignature == null || submitCSignature.isEmpty()) {
				return false;
			}
			if(submitRVSignature == null || submitRVSignature.isEmpty()) {
				return false;
			}
			return true;
		}

	}
	/**
	 * 这个方法，将所有的封装类迭代判断，直到结果中不存在封装类。
	 * @param typeSetStrings
	 * @param wrapperClassesStrings
	 * @param ptset
	 * @return
	 */
	private static Set<String> processProxyClass(Set<String> typeSet, Set<String> wrapperClasses,DoublePointsToSet ptset) {
	    PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
	    Set<String> result = new HashSet<>();
	    for (String typeString : typeSet) {
	        if (wrapperClasses.contains(typeString)) {
	            SootClass wc = Scene.v().getSootClass(typeString);
	            String fieldSignature = getFieldSignatures4ProxyClass(wc);
	            SootField innerField = Scene.v().getField(fieldSignature);
	            DoublePointsToSet innerPtSet = (DoublePointsToSet) pta.reachingObjects(ptset,innerField);
	            Set<Type> realTypeSet = innerPtSet.possibleTypes();
	            
	            // 将可能的类型转换为字符串集合
	            Set<String> realTypeStrings = getStringInTypeSet(realTypeSet);
	            
	            // 递归处理嵌套的封装类
	            if(innerPtSet.pointsToSetEquals(ptset)) {
	            	continue;
	            }
	            Set<String> nestedResult = processProxyClass(realTypeStrings, wrapperClasses,innerPtSet);
	            for (String nestedType : nestedResult) {
	                if (!wrapperClasses.contains(nestedType)) {
	                    result.add(nestedType);
	                }
	            }
	        } else {
	            // 如果当前类型不是封装类，则直接添加到结果集
	            result.add(typeString);
	        }
	    }
	    
	    return result;
	}
	private static String getFieldSignatures4ProxyClass(SootClass sc) {
		ClassInfo ci =resultMap.get(sc);
		return ci.fieldSignatures.peek();
	}
	/**
	 * 是否可以安全的重构，就是判断调用提交异步任务方法的变量是否是安全提交的几种执行器的对象之一。.
	 *
	 * 	@param mInvocation 当前方法调用
	 *  @param invocStmt the invoc stmt
	 * 	@param refactorMode 如果是1，则为execute()，如果是2则为submitC,如果是3则为submitR，如果是4则为submitRV
	 * @return 1, 如果可以进行重构;0,不可重构;2,需要得到绑定并进行判断.
	 */
	public static boolean canRefactor(MethodInvocation mInvocation,Stmt invocStmt , int refactorMode) {
		if(mustDirtyClasses.isEmpty()) {
			AnalysisUtils.debugPrint("因无污染类,直接通过");
			return true;
		}
		if(invocStmt == null) return false;
		Set<String> completeSetTypeStrings = null;
		Set<String> wrapperClassesStrings = null;
		Set<SootClass> allDirtyClasses = null;
		if(refactorMode == 1){//execute()判断
			if(executeDirtyClasses.isEmpty()) {
				AnalysisUtils.debugPrint("因无execute污染类,直接通过");
				return true;
			}
			completeSetTypeStrings = getStringInSootClassSet(mayCompleteExecutorClasses);
			wrapperClassesStrings = null;
			allDirtyClasses = executeDirtyClasses;
		}else if(refactorMode == 2) {
			completeSetTypeStrings = getCompleteExecutorSubClassesName();
			wrapperClassesStrings = getStringInSootClassSet(proxySubmitCClass);
			allDirtyClasses = submitCDirtyClasses;
		}else if(refactorMode == 3) {
			completeSetTypeStrings = getCompleteExecutorSubClassesName();
			wrapperClassesStrings = getStringInSootClassSet(proxySubmitRClass);
			allDirtyClasses = submitRDirtyClasses;
		}else if(refactorMode == 4) {
			completeSetTypeStrings = getCompleteExecutorSubClassesName();
			wrapperClassesStrings = getStringInSootClassSet(proxySubmitRVClass);
			allDirtyClasses = submitRVDirtyClasses;
		}
		Set<String> typeSetStrings = new HashSet<>();
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
    			typeSetStrings = getStringInTypeSet(typeSet);
    			if(wrapperClassesStrings != null && !typeSetStrings.isEmpty()) {
    				boolean wrapFlag = false;
    				for(String classSiguture : typeSetStrings) {
    					if(wrapperClassesStrings.contains(classSiguture)) wrapFlag = true;
    				}
    				if(wrapFlag) {
        				System.out.println("进入封装类判断");
        				typeSetStrings = processProxyClass(typeSetStrings, wrapperClassesStrings,(DoublePointsToSet) ptset);
    				}
    			}
    		}	
    	}
        if(!typeSetStrings.isEmpty()) {
			if(completeSetTypeStrings.containsAll(typeSetStrings)) {
				//是安全重构的子集，就可以进行重构了。
				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据指向分析 可以重构,typeName为："+typeSetStrings);
				return true;
			}
			AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据指向分析 进行排除,typeName为："+typeSetStrings);
			return false;
		}else  {
			Future2Completable.debugUsePoint2num++;
			//说明没有被访问到，可以进行AST判断
			Expression exp = mInvocation.getExpression();
			String typeName = null;
			if (exp == null) {//对应this情况
				ASTNode aboutTypeDeclaration = (ASTNode) mInvocation;
				while(!(aboutTypeDeclaration instanceof TypeDeclaration)&&!(aboutTypeDeclaration instanceof AnonymousClassDeclaration)) {
					aboutTypeDeclaration = aboutTypeDeclaration.getParent();
				}
				if(aboutTypeDeclaration instanceof TypeDeclaration) {
					TypeDeclaration td = (TypeDeclaration)aboutTypeDeclaration;
					ITypeBinding tdBinding = td.resolveBinding();
					typeName = tdBinding.getQualifiedName();
					if(tdBinding.isNested()) {
						typeName = tdBinding.getBinaryName();
					}
				}else if(aboutTypeDeclaration instanceof AnonymousClassDeclaration) {
					AnonymousClassDeclaration acd = (AnonymousClassDeclaration)aboutTypeDeclaration;
					ITypeBinding tdBinding = acd.resolveBinding();
					typeName = tdBinding.getQualifiedName();
					if(tdBinding.isNested()) {
						typeName = tdBinding.getBinaryName();
					}
				}else {
					throw new RefutureException(mInvocation,"迭代,未得到类型定义或者匿名类定义");
				}
				typeName = typeName.replaceAll("<[^>]*>", "");
				if(completeSetTypeStrings.contains(typeName)) {
					AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据ASTtypeBinding 可以重构,typeName为："+typeName);
					return true;
				}else {
					AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据ASTtypeBinding 不可重构,typeName为："+typeName);
					return false;
				}
			}else {//对应存在接收器对象却无法得到对象类型的情况。
				typeName = AnalysisUtils.getTypeName4Exp(exp);
				SootClass sc = Scene.v().getSootClass(typeName);
				Hierarchy hierarchy = Scene.v().getActiveHierarchy();
				if(sc.isInterface()) {//获取绑定，如果是接口，就这么做
					List<SootClass> allImplementers = hierarchy.getImplementersOf(sc);
					for(SootClass implementer : allImplementers) {
						if(allDirtyClasses.contains(implementer)) {
							AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据ASTtypeBinding 不可重构,typeName为："+typeName);
							return false;
						}
					}
					AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据ASTtypeBinding 可以重构,typeName为："+typeName);
					return true;
				}else {//不是接口，则这么做：
					List<SootClass> allSubClasses = hierarchy.getSubclassesOfIncluding(sc);
					for(SootClass subClass : allSubClasses) {
						if(allDirtyClasses.contains(subClass)) {
							AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据ASTtypeBinding 不可重构,typeName为："+typeName);
							return false;
						}
					}
					AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactor]根据ASTtypeBinding ~~~可以重构~~~,typeName为："+typeName);
					return true;
				}
				
			}
		}
	}
	

	/**
	 * 判断参数的类型是否复合要求。.
	 * @param invocationNode 
	 * @param invocStmt the invoc stmt
	 * @return return 1代表Runnable,return 2代表callable，return 3 代表Runnable,value。
	 */
	public static int arguModel(MethodInvocation invocationNode) {
		String invocName = invocationNode.getName().toString();
		if(invocationNode.arguments().size() == 1) {
			Expression firstArgu = (Expression) invocationNode.arguments().get(0);
			String binaryName = AnalysisUtils.getTypeName4Exp(firstArgu);
			if(runnablesubClasses.contains(binaryName)) {
				if(invocName.equals("submit")) {
					AnalysisUtils.debugPrint("submit(Runnable)模式3");
					return 3;
				}else {
					//上下文已限制为submit/execute，所以这里为execute(runnable)
					AnalysisUtils.debugPrint("execute(runnable)模式1");
					return 1;
				}
				
			}else if(callableSubClasses.contains(binaryName)&&invocName.equals("submit")){
				AnalysisUtils.debugPrint("submit(Callable)模式2");
				return 2;
			}else {
				throw new RefutureException(invocationNode,"binaryName"+binaryName);
			}
		}
		else if(invocationNode.arguments().size() == 2 && invocName.equals("submit")) {
			Expression firstArgu = (Expression) invocationNode.arguments().get(0);
			String binaryName = AnalysisUtils.getTypeName4Exp(firstArgu);
			if(runnablesubClasses.contains(binaryName)) {
				AnalysisUtils.debugPrint("submit(Runnable,Value)模式4");
				return 4;
			}else {
				throw new RefutureException(invocationNode,"binaryName"+binaryName);
			}
		}
		return -1;
	}
	
	public static Set<String> getStringInTypeSet(Set<Type> typeSet){
		Set<String> rClassesStrings = new HashSet<>();
		for(Type type:typeSet) {
			rClassesStrings.add(type.toQuotedString());
		}
		return rClassesStrings;
	}
	public static Set<String> getStringInSootClassSet(Set<SootClass> sootClassSet){
		Set<String> rClassesStrings = new HashSet<>();
		for(SootClass SC:sootClassSet) {
			rClassesStrings.add(SC.getName());
		}
		return rClassesStrings;
	}
	public static Set<String> getCompleteExecutorSubClassesName(){
		return getStringInSootClassSet(mayCompleteExecutorSubClasses);
	}
	public static Set<String> getAllExecutorServiceSubClassesName(){
		return getStringInSootClassSet(allExecutorServiceSubClasses);
	}
	public static Set<String> getAllExecutorSubClassesName() {
		return getStringInSootClassSet(allExecutorSubClasses);
	}

	
}

