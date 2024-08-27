package refuture.sootUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.RefutureException;
import soot.ArrayType;
import soot.Hierarchy;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;


public class NeedTestMethods {
	private static NeedTestMethods instance = null;
	private Set<SootMethod> refactoringMethods;
	private Set<String> randoopUseSigs;
	
	private NeedTestMethods() {
		refactoringMethods = new HashSet<SootMethod>();
		randoopUseSigs = new HashSet<String>();
	}
	
	private void prograteMethod() {
		System.out.println("开始推导");
		CallGraph cg = Scene.v().getCallGraph();
		Set<SootMethod> allNeedTestMethods = new HashSet<SootMethod>(refactoringMethods);
		ListIterator<SootMethod> refactoringMethodsIterator = new ArrayList<>(refactoringMethods).listIterator();
		while(refactoringMethodsIterator.hasNext()) {
			SootMethod currentMethod =refactoringMethodsIterator.next();
			Iterator it = cg.edgesInto(currentMethod);
			while(it.hasNext()) {
				Edge e = (Edge)it.next();
				SootMethod newSootMethod = e.getSrc().method();
				if(allNeedTestMethods.add(newSootMethod)) {
					refactoringMethodsIterator.add(newSootMethod);
				}
			}
		}
		//以上得到所有的想要生成测试的方法
		//在这里做一个工作列表算法。推导出必须要加入的构造方法。
		Set<SootMethod> workSet = canGenTestMethods(allNeedTestMethods);//用于保存最终结果和判断是否已经处理过的工具。
		ListIterator<SootMethod> workListIterator = new ArrayList<>(workSet).listIterator();
		Hierarchy hi = Scene.v().getActiveHierarchy();
		while(workListIterator.hasNext()) {
			SootMethod currentMethod =workListIterator.next();
			if(!currentMethod.isStatic()&&!currentMethod.isConstructor()) {//所在类和参数都推断。这里只写所在类
				SootClass currentClass= currentMethod.getDeclaringClass();
				currentClass.getMethods().forEach((method)->{
					if(method.isConstructor()&&method.isPublic()) {
						if(workSet.add(method)) {
							workListIterator.add(method);
						}
					}
				});
			}
			//这里写参数推断
			currentMethod.getParameterTypes().forEach((arguType)->{
				if(arguType instanceof ArrayType) {
					ArrayType tempType = (ArrayType)arguType;
					arguType = tempType.baseType;
				}
				if(!(arguType instanceof PrimType)) {
					if(arguType instanceof RefType) {
						RefType refType = (RefType)arguType;
						SootClass refClass = refType.getSootClass();
						if(refClass.isConcrete()) {
							refClass.getMethods().forEach((method)->{
								if(method.isConstructor()&&method.isPublic()) {
									if(workSet.add(method)) {
										workListIterator.add(method);
									}
								}
							});
						}else if(refClass.isInterface()){
							hi.getImplementersOf(refClass).forEach((im)->{
								if(im.isConcrete()&&!isAnomyClass(im)) {
									im.getMethods().forEach((method)->{
										if(method.isConstructor()&&method.isPublic()) {
											if(workSet.add(method)) {
												workListIterator.add(method);
											}
										}
									});
								}
							});
						}else {//对应abstract类情况
							hi.getSubclassesOf(refClass).forEach((sc)->{
								if(sc.isConcrete()&&!isAnomyClass(sc)) {
									sc.getMethods().forEach((method)->{
										if(method.isConstructor()&&method.isPublic()) {
											if(workSet.add(method)) {
												workListIterator.add(method);
											}
										}
									});
								}
							});
						}
					}else {
						throw new RefutureException(currentMethod, "参数不是引用类型，查看原因");
					}
				}
			});
		}
		workSet.forEach((e)->{
			randoopUseSigs.add(cover2RandoopSig(e.getSignature()));
		});
		System.out.println("全部推导完毕！");
	}
	
	private Set<SootMethod> canGenTestMethods(Set<SootMethod> ssm) {
		Set<SootMethod> rssm = new HashSet<>();
		for(SootMethod sm:ssm) {
			if(sm.isPublic()&&!sm.isJavaLibraryMethod()) {
				SootClass sc = sm.getDeclaringClass();
				if(sc.isPublic()&&!isAnomyClass(sc)) {
					if(sm.isStatic()) {
						rssm.add(sm);
					}else {
						if(sc.isConcrete()) {
							rssm.add(sm);
						}
					}
				}
			}
		}
		return rssm;
	}
	private boolean isAnomyClass(SootClass sc) {
		String className = sc.getName();
		return className.contains("$");
	}

	public static NeedTestMethods getInstance() {
		if (instance == null) {
			instance = new NeedTestMethods();
		}
		return instance;
	}
	
	public static void reset() {
		instance = null;
	}

	public void addRefactoringMethods(SootMethod sm) {
		this.refactoringMethods.add(sm);
	}
	
	public void output2Txt() {
		String fileName = AnalysisUtils.eclipseProject.getName()+"-methods.txt";
		String path = AnalysisUtils.eclipseProject.getLocation().toOSString();
		String filePath = path+File.separator+fileName;
		try {
            FileWriter writer = new FileWriter(filePath);
            // 遍历集合，将每个元素写入文件
            prograteMethod();
            Set<String> allSigs = this.randoopUseSigs;
            for (String sig : allSigs) {
                writer.write(sig + "\n"); // 每个元素单独为一行
            }
            writer.close();
            System.out.println("内容已写入到文件：" + filePath);
        } catch (IOException e) {
            System.out.println("写入文件时出现错误：" + e.getMessage());
        }
	}
	
    private static String cover2RandoopSig(String sig) {//优化构造函数
        int indexA = sig.indexOf(":");
        String classInfo = sig.substring(1, indexA).trim();
        String methodInfo = sig.substring(indexA + 2, sig.length() - 1).trim();
        String methodAndParams = methodInfo.substring(methodInfo.indexOf(" ") + 1);
        if(methodAndParams.contains("<init>")) {
        	methodAndParams = methodAndParams.replace("<init>", "");
        	return classInfo+methodAndParams;
        }
        return classInfo + "." + methodAndParams;
    }
	
}
