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
		System.out.println("全部推导完毕！");
		for(SootMethod sm:allNeedTestMethods) {
			//1.public 
			if(sm.isPublic()) {
				//2.匿名类
				String sig = sm.getSignature();
				SootClass sc = sm.getDeclaringClass();
				String className =sig.split(":")[0];
				if(sc.isConcrete()&&!className.contains("$")) {
					randoopUseSigs.add(cover2RandoopSig(sig));
				}
			}
		}
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
	
	private Set<String> getAllNeedTestMethods() {
		prograteMethod();
		return randoopUseSigs;
	}
	
	public void output2Txt() {
		String fileName = AnalysisUtils.eclipseProject.getName()+"methods.txt";
		String path = AnalysisUtils.eclipseProject.getLocation().toOSString();
		String filePath = path+File.separator+fileName;
		try {
            FileWriter writer = new FileWriter(filePath);
            // 遍历集合，将每个元素写入文件
            Set<String> allSigs = getAllNeedTestMethods();
            for (String sig : allSigs) {
                writer.write(sig + "\n"); // 每个元素单独为一行
            }
            writer.close();
            System.out.println("内容已写入到文件：" + filePath);
        } catch (IOException e) {
            System.out.println("写入文件时出现错误：" + e.getMessage());
        }
	}
	
    private static String cover2RandoopSig(String sig) {
        int indexA = sig.indexOf(":");
        String classInfo = sig.substring(1, indexA).trim();
        String methodInfo = sig.substring(indexA + 2, sig.length() - 1).trim();
        String methodAndParams = methodInfo.substring(methodInfo.indexOf(" ") + 1);
        return classInfo + "." + methodAndParams;
    }
	
}
