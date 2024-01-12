package refuture.sootUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import refuture.astvisitor.InstanceofVisiter;
import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.Future2Completable;
import refuture.refactoring.RefutureException;
import soot.Hierarchy;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.Type;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.internal.JimpleLocalBox;
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
	
	public static Set<JimpleLocal> useInstanceofExecutorLocal;
	
	/** 包含我手动添加的jdk中自带的执行器类型,以及完全没有重写关键方法子类. */
	private static Set<SootClass>mayCompleteExecutorSubClasses;//存入可能可以重构的类型以及包装类。
	
	private static Set<SootClass>wrapperClass;
	
	/** The all dirty classes. */
	private static Set<SootClass>allDirtyClasses;
	
	/** The all appendent classes. */
	private static Set<SootClass>allAdditionalClasses;
	
	/** 包括子接口和所有实现类限定名. */
	public static Set<String> callableSubClasses;
	
	/** 包括子接口和所有实现类限定名. */
	public static Set<String> runnablesubClasses;
	/**
	 * Inits the static field.
	 *
	 * @return true, if successful
	 */
	public static boolean initStaticField() {
		mayCompleteExecutorSubClasses = new HashSet<SootClass>();
		wrapperClass = new HashSet<SootClass>();
		allDirtyClasses = new HashSet<SootClass>();
		allAdditionalClasses = new HashSet<SootClass>();
		allExecutorServiceSubClasses= new HashSet<SootClass>();
		allExecutorSubClasses = new HashSet<SootClass>();
		useInstanceofExecutorLocal = new HashSet<JimpleLocal>();
		callableSubClasses = new HashSet<String>();
		runnablesubClasses = new HashSet<String>();
		return true;
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
		
		AnalysisUtils.debugPrint("CallableSubClasses:"+callableSubClasses.toString());
		AnalysisUtils.debugPrint("RunnableSubClasses:"+runnablesubClasses.toString());
	}

	public static void executorSubClassAnalysis() {
		SootClass executorClass = Scene.v().getSootClass("java.util.concurrent.Executor");
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		allExecutorSubClasses.addAll(hierarchy.getImplementersOf(executorClass));
		allExecutorSubClasses.addAll(hierarchy.getSubinterfacesOfIncluding(executorClass));
		for(CompilationUnit astUnit : AnalysisUtils.allAST) {
			InstanceofVisiter insOf = new InstanceofVisiter();
			astUnit.accept(insOf);
			List<InstanceofExpression> insOfNodes = insOf.getResult();
			for(InstanceofExpression insOfNode:insOfNodes) {
				String qName = insOfNode.getRightOperand().resolveBinding().getQualifiedName();
				if(runnablesubClasses.contains(qName)&&!qName.equals("java.lang.Runnable")) {
					Stmt stmt = AdaptAst.getJimpleInvocStmt(insOfNode);
					List<ValueBox> boxes = stmt.getUseBoxes();
					for(ValueBox box : boxes) {
						if(box instanceof ImmediateBox) {
							JimpleLocal local = (JimpleLocal)box.getValue();
							useInstanceofExecutorLocal.add(local);
							if(SootConfig.extremeSpeedModel) {
								CollectionEntrypoint.entryPointSet.add(AdaptAst.getSootMethod4invocNode(insOfNode));
							}
						}
					}
				}
			}
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
		
//		mayCompleteExecutorSubClasses.add(executorServiceClass);//不是安全的。
//		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedScheduledExecutorService"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.AbstractExecutorService"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.ThreadPoolExecutor"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.Executors$FinalizableDelegatedExecutorService"));
//		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedExecutorService"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.ScheduledThreadPoolExecutor"));
		mayCompleteExecutorSubClasses.add(Scene.v().getSootClass("java.util.concurrent.ForkJoinPool"));
		wrapperClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedScheduledExecutorService"));
		wrapperClass.add(Scene.v().getSootClass("java.util.concurrent.Executors$DelegatedExecutorService"));
		
		Hierarchy hierarchy = Scene.v().getActiveHierarchy();
		List<SootClass> serviceSubImplementers = hierarchy.getImplementersOf(executorServiceClass);
		allExecutorServiceSubClasses.addAll(serviceSubImplementers);
		List<SootClass> serviceSubInterfaces = hierarchy.getSubinterfacesOfIncluding(executorServiceClass);
		allExecutorServiceSubClasses.addAll(serviceSubInterfaces);
		
		for(SootClass tPESubClass : serviceSubImplementers) {
			if(mayCompleteExecutorSubClasses.contains(tPESubClass)||allDirtyClasses.contains(tPESubClass)||wrapperClass.contains(tPESubClass)) {
				continue;
			}
			//判断是否是dirtyClass
			boolean flag1 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.util.concurrent.Callable)");
			boolean flag2 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable,java.lang.Object)");
			boolean flag3 = tPESubClass.declaresMethod("java.util.concurrent.Future submit(java.lang.Runnable)");
			boolean flag4 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.util.concurrent.Callable)");
			boolean flag5 = tPESubClass.declaresMethod("java.util.concurrent.RunnableFuture newTaskFor(java.lang.Runnable,java.lang.Object)");
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
		AnalysisUtils.debugPrint("mayCompleteExecutorSubClasses:"+mayCompleteExecutorSubClasses.toString());
		AnalysisUtils.debugPrint("");
		AnalysisUtils.debugPrint("allDirtyClasses:"+allDirtyClasses.toString());
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
		if(isSubmit) {
			if(allDirtyClasses.isEmpty()) {
				return true;
			}
			Set<String> completeSetTypeStrings = getCompleteExecutorSubClassesName();
			Set<String> wrapperClassesStrings = getwrapperClassesName();
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
	    			for (Type obj : typeSet) {
	    				typeSetStrings.add(obj.toString()); // 将每个对象转换为字符串类型并添加到 Set<String> 中
	    			}
	    			if(wrapperClassesStrings != null && wrapperClassesStrings.containsAll(typeSetStrings)) {
	    				Set<String> tempStrings = new HashSet<>(typeSetStrings);
	    				typeSetStrings.clear();
	    				for(String typeName: tempStrings) {
	    					SootClass wc = Scene.v().getSootClass(typeName);
	    					SootField innerE = wc.getFields().getFirst();
	    					PointsToSet leftESet = pa.reachingObjects(ptset,innerE);
	    					Set<Type> realTypeSet = leftESet.possibleTypes();
	    					for (Type obj : realTypeSet) {
	    	    				typeSetStrings.add(obj.toString()); // 将每个对象转换为字符串类型并添加到 Set<String> 中
	    	    			}
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
			
		}else {//execute()判断
			if(useInstanceof(invocStmt)) {
				return false;
			}else {
				return true;
			}
		}
		
	}
	
	/**
	 * 判断参数的类型是否复合要求。.
	 * @param invocationNode 
	 *
	 * @param invocStmt the invoc stmt
	 * @param argType   为1,代表是callable;为2,代表Runnable;为3,代表FutureTask;为4,代表两个参数。
	 * @return true, if successful
	 */
//	public static boolean canRefactorArgu(MethodInvocation invocationNode, Stmt invocStmt,int argType) {
//		/*这里已经限制了调用的方法是submit或者execute，所以第一个参数一定是：callable、Runnable或者，FutureTask。
//		 * 我只分析invocStmt第一个参数，根据argType进行判断，为1,则判断是否是callable的子类，为2或者3,则判断是否是FutureTask,
//		 * 若不是，再判断是否是Runnable。lambda表达式也可以正常的分析，因为在Jinple中，lambda表达式会首先由一个Local变量指向它代表的对象。
//		 */
//		InvokeExpr ivcExp = invocStmt.getInvokeExpr();
//		List<Value> lv =ivcExp.getArgs();
//		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
//		if(invocationNode.arguments().size() == 0) {
//			AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:未传入实参，排除");
//			return false;
//		}
//		if(invocationNode.arguments().size() == 2) {
//			AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:注意，这个可能是两个参数的");
//			if(argType == 4) {
//				return true;
//			}
//			return false;
//		}
//		if(lv.get(0)instanceof Local) {
//			Local la1 = (Local) lv.get(0);
//			PointsToSet ptset = pa.reachingObjects(la1);
//			Set<Type> typeSet = ptset.possibleTypes();
//			Hierarchy hierarchy = Scene.v().getActiveHierarchy();
//			SootClass callable = Scene.v().getSootClass("java.util.concurrent.Callable");
//			List<SootClass> callableImplementers =hierarchy.getImplementersOf(callable);
//			SootClass futureTask = Scene.v().getSootClass("java.util.concurrent.FutureTask");
//			SootClass runnable = Scene.v().getSootClass("java.lang.Runnable");
//			List<SootClass> runnableImplementers =hierarchy.getImplementersOf(runnable);
//			switch (argType) {
//			case 1://是否是Callable的子类.
//				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]进入callable子类判断");
//				if(typeSet.isEmpty()) {
//					//利用ASTbinding 获得类型
//					Expression exp = (Expression) invocationNode.arguments().get(0);
//    				String typeName = AnalysisUtils.getTypeName4Exp(exp);
//    				if(typeName == null) {
//    					throw new RefutureException(invocationNode);
//    				}
//    				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]程序中没有访问到,进一步判断,类型为："+typeName);
//    				if(callable.getName().equals(typeName)) {
//    					AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]根据ASTtypeBinding 可以重构");
//						return true;
//    				}
//    				for(SootClass callableImplementer:callableImplementers) {
//    					if(callableImplementer.getName().equals(typeName)) {
//    						AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]根据ASTtypeBinding 可以重构");
//    						return true;
//    					}
//    				}
//    				
//    				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]根据AST不是callable的子类,排除,得到的类型名为"+ typeName);
//				}else {
//					for(Type type:typeSet) {
//						SootClass sc = Scene.v().getSootClass(type.getEscapedName());
//						if(sc.isPhantom()) {
//							AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:传入的实参类型无法获得SootClass，排除");
//							return false;
//						}
//						if(callableImplementers.contains(sc)) {
//							return true;
//						}else {
//							AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:不是callable的子类，排除");
//						}
//					}
//				}
//				break;
//			case 2:		
//				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]进入runnable子类判断");
//				if(typeSet.isEmpty()) {
//					//利用ASTbinding 获得类型
//					Expression exp = (Expression) invocationNode.arguments().get(0);
//    				String typeName = AnalysisUtils.getTypeName4Exp(exp);
//    				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]程序中没有访问到,进一步判断,类型为："+typeName);
//    				if(runnable.getName().equals(typeName)) {
//    					AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]根据ASTtypeBinding 可以重构");
//						return true;
//    				}
//    				for(SootClass runnableImplementer:runnableImplementers) {
//    					if(runnableImplementer.getName().equals(typeName)) {
//    						AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]根据ASTtypeBinding 可以重构");
//    						return true;
//    					}
//    				}
//    				AnalysisUtils.debugPrint("[ExecutorSubClass.canRefactorArgu]根据AST不是runnable的子类,排除,得到的类型名为"+ typeName);
//				}else {
//					for(Type type:typeSet) {
//						SootClass sc = Scene.v().getSootClass(type.getEscapedName());
//						if(sc.isPhantom()) {
//							AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:传入的实参类型无法获得SootClass，排除");
//							return false;
//						}
//						if(runnableImplementers.contains(sc)) {
//							return true;
//						}else{
//							AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:不是Runnable的子类，排除");
//						}
//					}
//				}
//				break;
//			case 3:		
//				for(Type type:typeSet) {
//					SootClass sc = Scene.v().getSootClass(type.getEscapedName());
//					if(sc.isPhantom()) {
//						AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:传入的实参类型无法获得SootClass，排除");
//						return false;
//					}
//					if(hierarchy.isClassSuperclassOfIncluding(futureTask, sc)) {
//						return true;
//					}
//				}
//				break;
//			case 4:		
//				if(lv.size()==2) {
//				return true;
//			}
//				break;
//			default:
//				throw new IllegalArgumentException("Invalid number");
//		}
//			AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:哎，排除");
//			return false;
//		}
//		AnalysisUtils.debugPrint("[ExecutorSubclass.canRefactorArgu]:不是local变量，排除");
//		return false;
//	}
//	
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


	public static boolean useInstanceof(Stmt invocStmt) {
		PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
		InvokeExpr ivcExp = invocStmt.getInvokeExpr();
		List<Value> lv =ivcExp.getArgs();
		if(lv.get(0)instanceof Local) {
			Local la1 = (Local) lv.get(0);
			PointsToSet ptset = pa.reachingObjects(la1);
			for(JimpleLocal ioLocal:useInstanceofExecutorLocal) {
				if(ptset.hasNonEmptyIntersection(pa.reachingObjects(ioLocal))) {
					Future2Completable.useInstanceof++;
					AnalysisUtils.debugPrint("因使用 instanceof而被排除");
					return true;
				}
			}
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
	
	public static Set<String> getwrapperClassesName(){
		Set<String> wrapperClassesStrings = new HashSet<>();
		for(SootClass completeSC:wrapperClass) {
			wrapperClassesStrings.add(completeSC.getName());
		}
		return wrapperClassesStrings;
	}

	public static Set<String> getAllDirtyExecutorSubClassName(){
		Set<String> allDirtyClassesStrings = new HashSet<>();
		for(SootClass allDirtyClass:allDirtyClasses) {
			allDirtyClassesStrings.add(allDirtyClass.getName());
		}
		return allDirtyClassesStrings;
	}
	
	public static Set<String> getAllExecutorServiceSubClassesName(){
		Set<String> allExecutorSubClassesStrings = new HashSet<>();
		for(SootClass allExecutorSubClass:allExecutorServiceSubClasses) {
			allExecutorSubClassesStrings.add(allExecutorSubClass.getName());
		}
		return allExecutorSubClassesStrings;
	}
	public static Set<String> getAllExecutorSubClassesName() {
		Set<String> allExecutorSubClassesStrings = new HashSet<>();
		for(SootClass allExecutorSubClass:allExecutorSubClasses) {
			allExecutorSubClassesStrings.add(allExecutorSubClass.getName());
		}
		return allExecutorSubClassesStrings;
	}

	
}

