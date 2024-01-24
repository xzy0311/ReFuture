package refuture.refactoring;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import refuture.sootUtil.ExecutorSubclass;

/**
 * The Class AnalysisUtils. 提供分析方法的工具包类，它的方法都是静态的。
 */
public class AnalysisUtils {

	/** The projectpath. */
	private static List<String> PROJECTOUTPATH;

	/** The projectpath. */
	private static String PROJECTPATH;

	/** 输出调试信息标志 */
//	public static boolean debugFlag = false;
	
	public static List<CompilationUnit> allAST;

	//跳过一些方法
	public static List<String> skipMethodName = new ArrayList<String>();
	static {
//		skipMethodName.add("<com.hazelcast.jet.impl.processor.AsyncTransformUsingServiceBatchP_IntegrationTest: com.hazelcast.function.BiFunctionEx transformNotPartitionedFn(com.hazelcast.function.FunctionEx)>");
//		skipMethodName.add("<com.hazelcast.jet.impl.processor.AsyncTransformUsingServiceP_IntegrationTest: com.hazelcast.function.BiFunctionEx transformNotPartitionedFn(com.hazelcast.function.FunctionEx)>");
//		skipMethodName.add("<com.hazelcast.jet.impl.processor.AsyncTransformUsingServiceP_IntegrationTest: com.hazelcast.jet.function.TriFunction transformPartitionedFn(com.hazelcast.function.FunctionEx)>");
//		skipMethodName.add("org.elasticsearch.action.ActionFuture execute(org.elasticsearch.action.Action,java.lang.Object)");
//		skipMethodName.add("void execute(org.elasticsearch.action.Action,java.lang.Object,org.elasticsearch.action.ActionListener)");
//		skipMethodName.add("void shardExecute(org.elasticsearch.tasks.Task,java.lang.Object,org.elasticsearch.index.shard.ShardId,org.elasticsearch.action.ActionListener)");
	}
	
	/**
	 * Collect from select,并得到项目的路径。
	 * @param project the project
	 * @return 传入的对象中包含的java文件列表。
	 *8.3 projectNest need to implement 
	 *
	 */
	public static List<ICompilationUnit> collectFromSelect(IJavaProject project) {
		List<ICompilationUnit> allJavaFiles = new ArrayList<ICompilationUnit>();
		
		// 得到输出的class在的文件夹，方便后继使用soot分析。
		try {
			String projectoutpath = project.getOutputLocation().toOSString();
			PROJECTPATH = project.getProject().getLocation().toOSString();
	        int secondSlashIndex = projectoutpath.indexOf('/', projectoutpath.indexOf('/') + 1);
	        projectoutpath = projectoutpath.substring(secondSlashIndex);
			PROJECTOUTPATH = new ArrayList<String>();
			PROJECTOUTPATH.add(PROJECTPATH + projectoutpath);
//			PROJECTOUTPATH.add(PROJECTPATH + projectoutpath +"/1");
		} catch (JavaModelException ex) {
			System.out.println(ex);
		}
		// 得到选中的元素中的JAVA项目。
		try {
			/*
			 * ********这里有一些配置，需要手动更改。************
			 */
			// 1.1 测试标志，是否将test-classes替换classes从而得到测试代码生成的class文件路径。适合JGroups flume xml项目。
			boolean testFlag = true;
			if (testFlag) {
				String projectOutPath = PROJECTOUTPATH.get(0);
				String projectTestOutPath = projectOutPath.replace("classes", "test-classes");
				PROJECTOUTPATH.add(projectTestOutPath);
				testFlag = false;
			}
			//1.2 手动添加测试类class文件路径
			// 1.2.1cassandra使用
//			String projectTestOutPath = PROJECTPATH+File.separator+"build"+File.separator+"test"+File.separator+"classes";
			// 1.2.2 elasticSearch 使用
//			String projectTestOutPath = PROJECTPATH+File.separator+"build-eclipse"+File.separator+"2";
			// 1.2.3 traific use
//			String projectTestOutPath = PROJECTPATH+File.separator+"target"+File.separator+"bin-test";
			// 1.2.4 dropward use
//			String projectTestOutPath = "/home/xzy/runtime-workspace/dropwizard-1.3.16/dropwizard-logging/target/classes";
			// 1.2.5 mockito use
//			String projectTestOutPath = "/home/xzy/runtime-workspace/mockito-3.3.0/bin/test";
			//1.3 上面开启,此项必须开启
//			PROJECTOUTPATH.add(projectTestOutPath);
			for (IJavaElement element : project.getChildren()) {
			//2 对源码包的过滤选项。
				//2.1jGroups，cassandra, lucene-solr 使用
//				boolean javaFolder = element.toString().startsWith("src")&&!element.getElementName().equals("resources")||element.toString().startsWith("test");
//				boolean javaFolder = (element.toString().startsWith("src")&&!element.getElementName().equals("resources"))||element.toString().startsWith("target");// xml,flume,jenkins
//				boolean javaFolder = element.getElementName().equals("java")||element.getElementName().equals("test")||element.getElementName().equals("classes");// tomcat
//				boolean javaFolder = element.toString().startsWith("src");// Jailer   SPECjbb
				boolean javaFolder = element.getElementName().equals("java");// signalserver、hadoop zookeeper syncope elaticSearch tika brooklyn使用。
//				boolean javaFolder = element.getElementName().equals("java")||element.getElementName().equals("gen-java");
				if (javaFolder) {// 找到包，给AST使用
					IPackageFragmentRoot packageRoot = (IPackageFragmentRoot) element;
					for (IJavaElement ele : packageRoot.getChildren()) {
						if (ele instanceof IPackageFragment) {
							IPackageFragment packageFragment = (IPackageFragment) ele;
							// 一个CompilationUnit代表一个java文件。
							for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
								allJavaFiles.add(unit);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return allJavaFiles;
	}

	/**
	 * 得到 node所属的方法的Soot中名称，在方法体外和构造函数，则返回“void {@code<init>}()”,否则返回方法签名.
	 * 方法的返回值若是引用类型，需要写全名。
	 * @param node node必须保证，是类里面的语句节点，否则陷入无限循环。
	 * @return the method name
	 */
	public static String getMethodNameNArgusofSoot(ASTNode node) {
		String methodSootName = "void <init>()";
		while (!(node instanceof TypeDeclaration)) {
			if (node instanceof MethodDeclaration) {
				MethodDeclaration mdNode = (MethodDeclaration) node;
				IMethodBinding imb = mdNode.resolveBinding();
				Type type = mdNode.getReturnType2();
				String methodReturnTypeName;
				if (type == null) {// 构造函数为null,但是需要判断是否有参数
					if (getmethodParameters(mdNode).isEmpty()) {
						break;
					} else {
						methodSootName = "void <init>(" + getmethodParameters(mdNode) + ")";
						break;
					}
				}
				if (type.resolveBinding().isTypeVariable()) {
					methodReturnTypeName = "java.lang.Object";
				} else {
					methodReturnTypeName = imb.getReturnType().getQualifiedName().toString();
					if(methodReturnTypeName.contains(".")) {
						methodReturnTypeName = imb.getReturnType().getBinaryName().toString();
					}
				}
				String methodSimpleName = mdNode.getName().toString();
				String methodParameters = getmethodParameters(mdNode);
				methodSootName = methodReturnTypeName + " " + methodSimpleName + "(" + methodParameters + ")";
				break;
			}else if(node instanceof Initializer){
				if(Modifier.isStatic(((Initializer) node).getModifiers())) {
					methodSootName = "void <clinit>()";
				}
				break;//在初始化块中，不是静态的，直接退出循环就行。
			}else if(node instanceof FieldDeclaration) {
				if(Modifier.isStatic(((FieldDeclaration) node).getModifiers())) {
					methodSootName = "void <clinit>()";
				}
				break;//在字段中，不是静态的，直接退出循环就行。
			}
			node = node.getParent();
		}
		if(node instanceof MethodDeclaration) {//节点不在初始化块和字段声明中。肯定在方法定义中。
			if (methodSootName == "void <init>()") {
				TypeDeclaration td = getTypeDeclaration4node(node);
				if(td == null) {throw new NullPointerException();}
				ITypeBinding typeBinding = td.resolveBinding();
				if(typeBinding.isNested()&&!Modifier.isStatic(td.getModifiers())) {//innerClass & not static,it jimple class contructor method's parameters not empty,就算它的代码中是空参数的构造函数。
					String bn = typeBinding.getBinaryName();
					int lastIndex = bn.lastIndexOf('$');
					if(lastIndex != -1) {
						bn = bn.substring(0,lastIndex);
					}else {
						throw new IllegalStateException("error");
					}
					methodSootName = "void <init>("+bn+")";
				}
			}
			if (methodSootName != "void <init>()"&&countGreaterThanOneLessThanSign(methodSootName)>1) {
//				System.out.println("[AnalysisUtils.getSootMethodName处理前]"+methodSootName);
				// 去除额外的<>和因此产生的空格。
				String result = methodSootName.replaceAll("<[^<>]*>", "");
				do {
					methodSootName = result;
					result = methodSootName.replaceAll("<[^<>]*>", "");
				} while (!result.equals(methodSootName));
				methodSootName = methodSootName.replaceAll("\\s{2,}", " ");
//				System.out.println("[AnalysisUtils.getSootMethodName处理前]"+methodSootName);
			}
			
		}


		// 0829 增加得到名称中的''符号的逻辑.暂时使用,弄完这个项目,就注释掉
//		methodSootName = methodSootName.replace(".to.",".'to'.");
		
		return methodSootName;

	}
	
	public static String getSimpleMethodNameofSoot(ASTNode node) {
		String methodSootName = "<init>";
		while (!(node instanceof TypeDeclaration)) {
			if (node instanceof MethodDeclaration) {
				MethodDeclaration mdNode = (MethodDeclaration) node;
				Type type = mdNode.getReturnType2();
				if (type == null) {// 构造函数为null
					break;
				}
				String methodSimpleName = mdNode.getName().toString();
				methodSootName = methodSimpleName;
				break;
			}else if(node instanceof Initializer){
				if(Modifier.isStatic(((Initializer) node).getModifiers())) {
					methodSootName = "<clinit>";
				}
				break;//在初始化块中，不是静态的，直接退出循环就行。
			}else if(node instanceof FieldDeclaration) {
				if(Modifier.isStatic(((FieldDeclaration) node).getModifiers())) {
					methodSootName = "<clinit>";
				}
				break;//在字段中，不是静态的，直接退出循环就行。
			}
			node = node.getParent();
		}
		return methodSootName;

	}

	/**
	 * 通过MethodDeclaration,得到参数的类型全名。 需要开启绑定。
	 * @param mdNode the md node
	 * @return the method parameters
	 */
	private static String getmethodParameters(MethodDeclaration mdNode) {
		List<ASTNode> parameterList = mdNode.parameters();
		String parameterString = new String();
		if (parameterList.isEmpty()) {
			return "";
		} else {
			for (ASTNode astnode : parameterList) {
				SingleVariableDeclaration param = (SingleVariableDeclaration) astnode;
				ITypeBinding typeBinding = param.getType().resolveBinding();
				
				String typefullName;
				if (typeBinding.isTypeVariable()) {//是类型变量，也就是T等。
					typefullName = "java.lang.Object";
				} else if (typeBinding.isPrimitive() || typeBinding.isArray()) {
					typefullName = typeBinding.getQualifiedName();
				} else {
					typefullName = typeBinding.getBinaryName();
				}
				if(param.isVarargs()) {
					typefullName = typefullName+"[]";
				}
				if (parameterString.isEmpty()) {
					parameterString = typefullName;
				} else {
					parameterString = parameterString + "," + typefullName;
				}
			}
			return parameterString;
		}
	}

	/**
	 * 得到节点所属的方法块的方法定义节点, 可能返回null;
	 *
	 * @param node the node
	 * @return the method declaration 4 node
	 */
	public static MethodDeclaration getMethodDeclaration4node(ASTNode node) {
		while (!(node instanceof TypeDeclaration)) {
			if (node instanceof MethodDeclaration) {
				break;
			}
			node = node.getParent();
		}
		if(node instanceof MethodDeclaration) {
			return (MethodDeclaration) node;
		}else {//node instanceof TypeDeclaration
			//if InvocationNode not in MethodDeclaration,it may in Initial Block (static or not) .if in static block it will in staic void <clinit>() in soot ,if in block ,it will in public void <init>().
			//so ,i just return null;
			return null;
		}
	}

	/**
	 * 得到节点所属的类定义节点，这个是离节点最近的那个类定义节点。可能返回null.
	 *
	 * @param node the node
	 * @return the Type declaration 4 node
	 */
	public static TypeDeclaration getTypeDeclaration4node(ASTNode node) {

		while (!(node instanceof TypeDeclaration)) {
			node = node.getParent();
			if (node == null) {
				return null;
			}
		}
		return (TypeDeclaration) node;
	}
	
	/**
	 * Checks if is declaration or assignment.
	 *
	 * @param invocationNode the invocation node
	 * @return true, if is declar or assign
	 */
	public static boolean isDeclarOrAssign(MethodInvocation invocationNode) {
		ASTNode parentNode = invocationNode.getParent();

		if (parentNode instanceof Assignment || parentNode instanceof VariableDeclarationStatement) {
			return true;
		}
		return false;
	}

	public static String getProjectPath() {
		return PROJECTPATH;
	}

	public static List<String> getSootClassPath() {
		return PROJECTOUTPATH;
	}

	public static void throwNull() {
		throw new NullPointerException();
	}

	/**
	 * 进行一个判断，使用AST相关特性：
	 * 1.判断去除this，super等特殊变量作为receiverObject的情况。我不会找到super,因为是superMethodInvocation
	 * 2.不是污染的执行器子类。
	 * 3.10.17日,为了方便统计相关信息,这里不再卡污染类.只卡this和压根不是ExecutorService子类.
	 */
//	public static boolean receiverObjectIsComplete(MethodInvocation invocationNode) {
//		List<Expression> arguExps =  invocationNode.arguments();
//		if(arguExps.size() == 0) {return false;}
//		boolean isTask = false;
//		for(Expression arguExp : arguExps) {
//			String arguTypeName = getTypeName4Exp(arguExp);
//			if(arguTypeName == null) {
//				throw new RefutureException(invocationNode,"得不到类型绑定");
//			}else if(arguTypeName == "java.lang.Object") {throw new RefutureException(invocationNode,"得到了object");}
//			if(ExecutorSubclass.callableSubClasses.contains(arguTypeName)|| ExecutorSubclass.runnablesubClasses.contains(arguTypeName)) {
//				isTask = true;
//			}
//		}
//		if(isTask) {Future2Completable.maybeRefactoringNode++;}else {return false;}
//		
//		//到这里,invocation是submit/execute(task...);
//		Expression exp = invocationNode.getExpression();
//		Set <String> allSubNames = ExecutorSubclass.getAllExecutorSubClassesName();
//		Set <String> allSubServiceNames = ExecutorSubclass.getAllExecutorServiceSubClassesName();
//		if(exp==null){
//			debugPrint("[AnalysisUtils.receiverObjectIsComplete]receiverObject为this，继续重构。");
//			// 判断invocationNode所在类是否是子类，若是子类，则任务提交点+1.
//			ASTNode aboutTypeDeclaration = (ASTNode) invocationNode;
//			while(!(aboutTypeDeclaration instanceof TypeDeclaration)&&!(aboutTypeDeclaration instanceof AnonymousClassDeclaration)) {
//				aboutTypeDeclaration = aboutTypeDeclaration.getParent();
//			}
//			String typeName = null;
//			if(aboutTypeDeclaration instanceof TypeDeclaration) {
//				TypeDeclaration td = (TypeDeclaration)aboutTypeDeclaration;
//				ITypeBinding tdBinding = td.resolveBinding();
//				typeName = tdBinding.getQualifiedName();
//				if(tdBinding.isNested()) {
//					typeName = tdBinding.getBinaryName();
//				}
//			}else if(aboutTypeDeclaration instanceof AnonymousClassDeclaration) {
//				AnonymousClassDeclaration acd = (AnonymousClassDeclaration)aboutTypeDeclaration;
//				ITypeBinding tdBinding = acd.resolveBinding();
//				typeName = tdBinding.getQualifiedName();
//				if(tdBinding.isNested()) {
//					typeName = tdBinding.getBinaryName();
//				}
//			}else {
//				throw new RefutureException(invocationNode,"迭代,未得到类型定义或者匿名类定义");
//			}
//			typeName = typeName.replaceAll("<[^>]*>", "");
//			if(invocationNode.getName().toString().equals("execute")&&allSubNames.contains(typeName)) {
//				Future2Completable.canRefactoringNode++;
//				return true;
//			}else if(invocationNode.getName().toString().equals("submit")&&allSubServiceNames.contains(typeName)) {
//				Future2Completable.canRefactoringNode++;
//				return true;
//			}else if(typeName == "java.lang.Object") {
//				throw new RefutureException(invocationNode,"typeBinding为Object,精度不够");
//			}else {
//				Future2Completable.useNotExecutorSubClass++;
//				return false;
//			}
//		}
//		String typeName = getTypeName4Exp(exp);
//		if(typeName==null){
////			Future2Completable.canRefactoringNode++;
//			throw new RefutureException(invocationNode,"typeBinding为null，应该是eclipse环境下的源码没有调试好，这里卡");
////			debugPrint("[AnalysisUtils.receiverObjectIsComplete]typeBinding为null，应该是eclipse环境下的源码没有调试好，这里不卡");
////			return true;
//		}
//		
//		if(invocationNode.getName().toString().equals("execute")&&(allSubNames.contains(typeName))) {
//			Future2Completable.canRefactoringNode++;
//			debugPrint("[AnalysisUtils.receiverObjectIsComplete]初步ast判定可以,这里不卡");
//			return true;
//		}
//		else if(invocationNode.getName().toString().equals("submit")&&(allSubServiceNames.contains(typeName))) {
//			Future2Completable.canRefactoringNode++;
//			debugPrint("[AnalysisUtils.receiverObjectIsComplete]ast判定可以,这里不卡");
//			return true;
//		}else if(typeName == "java.lang.Object") {
//			throw new RefutureException(invocationNode,"typeBinding为Object,精度不够");
//		}
//		debugPrint("[AnalysisUtils.receiverObjectIsComplete]ast判定这个调用对象的类不是子类,这个对象的类型名为:"+typeName);
//		Future2Completable.useNotExecutorSubClass++;
//		return false;
//	}
//	
	
	
	public static void debugPrint(String message) {
		if(Future2Completable.debugFlag == true) {
			System.out.println(message);
		}
	}
	public static String invocNodeInfo(Expression expNode) {
		StringBuilder message = new StringBuilder();
    	message.append("所在类为"+getTypeDeclaration4node(expNode).getName()+";");
    	message.append("所在方法为"+getMethodNameNArgusofSoot(expNode)+";");
    	CompilationUnit astUnit = (CompilationUnit)expNode.getRoot();
    	message.append("行号为"+astUnit.getColumnNumber(expNode.getStartPosition())+";");
    	return message.toString();
	}
	
    public static int countGreaterThanOneLessThanSign(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '<') {
                count++;
            }
        }
        return count;
    }

	public static Block getBlockNode(ASTNode getNode) {
		while(true) {
			if(getNode instanceof TypeDeclaration) {
				TypeDeclaration type = (TypeDeclaration)getNode;
				if(type.resolveBinding().isTopLevel() ) {
					break;
				}
			}
			if(getNode == null) {return null;}
			if(getNode instanceof Block) {
				Block block = (Block)getNode;
				return block;
			}
			getNode = getNode.getParent();
		}
		return null;
	}
	
	public static String getTypeName4Exp(Expression exp) {
		ITypeBinding typeBinding = exp.resolveTypeBinding();
		if(typeBinding == null) {
			return null;
		}
		String typeName = typeBinding.getQualifiedName();
		if(typeBinding.isNested()) {
			typeName = typeBinding.getBinaryName();
		}else if(typeBinding.isAnonymous()) {
			ITypeBinding superTypeBinding = typeBinding.getSuperclass();
			if(superTypeBinding.isNested()) {
				typeName = superTypeBinding.getBinaryName();
			}else {
				typeName = superTypeBinding.getQualifiedName();
			}
		}
		if (typeName == null) {
			return null;
		}
		typeName = typeName.replaceAll("<[^>]*>", "");
		return typeName;
	}

}
