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
import org.eclipse.jdt.core.dom.Assignment;
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
import soot.Scene;
import soot.SootClass;

// TODO: Auto-generated Javadoc
/**
 * The Class AnalysisUtils. 提供分析方法的工具包类，它的方法都是静态的。
 */
public class AnalysisUtils {

	/** The projectpath. */
	private static List<String> PROJECTOUTPATH;

	/** The projectpath. */
	private static String PROJECTPATH;

	/** 输出调试信息标志 */
	private static boolean debugFlag = true;

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
			int lastIndex = PROJECTPATH.lastIndexOf(File.separator);
			String RUNTIMEPATH = PROJECTPATH.substring(0, lastIndex);
			PROJECTOUTPATH = new ArrayList<String>();
			PROJECTOUTPATH.add(RUNTIMEPATH + projectoutpath);
		} catch (JavaModelException ex) {
			System.out.println(ex);
		}
		// 得到选中的元素中的JAVA项目。
		try {
			/*
			 * ********这里有一些配置，需要手动更改。************
			 */
			// 1.1 测试标志，是否将test-classes替换classes从而得到测试代码生成的class文件路径。适合JGroups flume 项目。
			boolean testFlag = true;
			if (testFlag) {
				System.out.println("PROJECTOUTPATH IS :"+PROJECTOUTPATH);
				String projectOutPath = PROJECTOUTPATH.get(0);
				String projectTestOutPath = projectOutPath.replace("classes", "test-classes");
				PROJECTOUTPATH.add(projectTestOutPath);
				testFlag = false;
			}
			
			//1.2 手动添加测试类class文件路径
			// 1.2.1cassandra使用
//			String projectTestOutPath = PROJECTPATH+File.separator+"build"+File.separator+"test"+File.separator+"classes";
			// 1.2.2hadoop zookeeper  use
//			String projectTestOutPath = PROJECTPATH+File.separator+"target"+File.separator+"test-classes";
//			PROJECTOUTPATH.add(projectTestOutPath);
			for (IJavaElement element : project.getChildren()) {
			//2 对源码包的过滤选项。
				//2.1jGroups，cassandra, lucene-solr 使用
//				boolean javaFolder = element.toString().startsWith("src")&&!element.getElementName().equals("resources")||element.toString().startsWith("test");
				boolean javaFolder = element.toString().startsWith("src")&&!element.getElementName().equals("resources")||element.toString().startsWith("target");//flume
//				boolean javaFolder = element.toString().startsWith("java");//其他
//				boolean javaFolder = element.getElementName().equals("java");// signalserver、tomcat、hadoop zookeeper使用。
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
	 * 
	 * @param node node必须保证，是类里面的语句节点，否则陷入无限循环。
	 * @return the method name
	 */
	public static String getSootMethodName(ASTNode node) {
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
				break;
			}else if(node instanceof FieldDeclaration) {
				if(Modifier.isStatic(((FieldDeclaration) node).getModifiers())) {
					methodSootName = "void <clinit>()";
				}
				break;
			}
			node = node.getParent();
		}
		if(node instanceof MethodDeclaration) {
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
			}else {
				TypeDeclaration td = getTypeDeclaration4node(node);
				ITypeBinding typeBinding = td.resolveBinding();
				if(typeBinding.isNested()&&!Modifier.isStatic(td.getModifiers())) {//innerClass & not static,it jimple class contructor method's parameters not empty
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
		}


		return methodSootName;

	}

	/**
	 * 通过MethodDeclaration,得到参数的类型全名。 需要开启绑定。
	 *
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
				if (typeBinding.isTypeVariable()) {
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
	 * 得到节点所属的方法块的方法定义节点, 
	 *
	 * @param node the node
	 * @return the method declaration 4 node
	 */
	public static MethodDeclaration getMethodDeclaration4node(ASTNode node) {
		int lineNumberPosition = node.getStartPosition();
		while (!(node instanceof TypeDeclaration)) {
			if (node instanceof MethodDeclaration) {
				break;
			}
			node = node.getParent();
			if (node == node.getParent()) {
				System.out.println("@error[AnalysisUtils.getMethodDeclaration4node]：有问题"+node);
				throw new ExceptionInInitializerError("[AnalysisUtils.getMethodDeclaration4node]：有问题"+node);
			}
		}
		if(node instanceof MethodDeclaration) {
			return (MethodDeclaration) node;
		}else {
//			CompilationUnit cu = (CompilationUnit)node.getRoot();
//			throw new NullPointerException("[AnalysisUtils.getMethodDeclaration4node]空方法定义,属于类："
//			+getTypeDeclaration4node(node).resolveBinding().getQualifiedName()+"行号："+cu.getLineNumber(lineNumberPosition));
			//if InvocationNode not in MethodDeclaration,it may in Initial Block (static or not) .if in static block it will in staic void <clinit>() in soot ,if in block ,it will in public void <init>().
			//so ,i just return null;
			return null;
			
			
		}
	}

	/**
	 * 得到节点所属的类定义节点
	 *
	 * @param node the node
	 * @return the Type declaration 4 node
	 */
	public static TypeDeclaration getTypeDeclaration4node(ASTNode node) {

		while (!(node instanceof TypeDeclaration)) {
			node = node.getParent();
			if (node == node.getParent()) {
				System.out.println("@error[AnalysisUtils.getTypeDeclaration4node]：有问题"+node);
				throw new ExceptionInInitializerError("[AnalysisUtils.getTypeDeclaration4node]：有问题"+node);
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
	 */
	public static boolean receiverObjectIsComplete(MethodInvocation invocationNode) {
		Expression exp = invocationNode.getExpression();
		if(exp==null){
			debugPrint("[AnalysisUtils.receiverObjectIsComplete]receiverObject为this，进行排除。");
			return false;
		}
		ITypeBinding typeBinding = exp.resolveTypeBinding();
		if(typeBinding==null){
			debugPrint("[AnalysisUtils.receiverObjectIsComplete]typeBinding为null，这里不卡");
			return true;
		}
		String typeName = typeBinding.getQualifiedName();
		if(typeBinding.isNested()) {
			typeName = typeBinding.getBinaryName();
		}
		SootClass sc = Scene.v().getSootClass(typeName);
		Set<SootClass> dirtyclasses = ExecutorSubclass.getallDirtyExecutorSubClass();
		if(dirtyclasses.contains(sc)) {
			debugPrint("[AnalysisUtils.receiverObjectIsComplete]根据ASTtypeBinding 属于污染类，进行排除");
			return false;
		}
		return true;
	}
	public static void debugPrint(String message) {
		if(debugFlag == true) {
			System.out.println(message);
		}
	}
	public static int lineNumberPrint(MethodInvocation invocationNode) {
		CompilationUnit astUnit = (CompilationUnit)invocationNode.getRoot();
		return astUnit.getColumnNumber(invocationNode.getStartPosition());
	}
	
	private static List<IJavaProject> FindAllProjects(IJavaProject ijp) throws JavaModelException{
		List<IJavaProject> javaList = new ArrayList();
		for(IJavaElement ije :ijp.getChildren()) {
			if(ije instanceof IJavaProject) {
				javaList.add((IJavaProject)ije);
			}
		}
		return javaList;
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
}
