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
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;


public class NeedTestMethods {
	private static NeedTestMethods instance = null;
	private Set<String> refactoringMethods;
	private Set<String> allNeedTestMethods;
	
	private NeedTestMethods() {
		refactoringMethods = new HashSet<String>();
		allNeedTestMethods = new HashSet<String>();
	}
	
	private void prograteMethod() {
		System.out.println("开始推导");
		CallGraph cg = Scene.v().getCallGraph();
		ListIterator<String> refactoringMethodsIterator = new ArrayList<>(refactoringMethods).listIterator();
		while(refactoringMethodsIterator.hasNext()) {
			SootMethod currentMethod = Scene.v().getMethod(refactoringMethodsIterator.next());
			Iterator it = cg.edgesInto(currentMethod);
			while(it.hasNext()) {
				Edge e = (Edge)it.next();
				String newSootMethodSignature = e.getSrc().method().getSignature();
				if(allNeedTestMethods.add(newSootMethodSignature)) {
					refactoringMethodsIterator.add(newSootMethodSignature);
				}
			}
		}
		System.out.println("全部推导完毕！");
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

	public void addRefactoringMethods(String methodSignature) {
		this.refactoringMethods.add(methodSignature);
		this.allNeedTestMethods.add(methodSignature);
	}
	
	private Set<String> getAllNeedTestMethods() {
		prograteMethod();
		return allNeedTestMethods;
	}
	
	public void output2Txt() {
		String fileName = AnalysisUtils.eclipseProject.getName()+"methods.txt";
		String path = AnalysisUtils.eclipseProject.getLocation().toOSString();
		String filePath = path+File.separator+fileName;
		try {
            FileWriter writer = new FileWriter(filePath);
            // 遍历集合，将每个元素写入文件
            Set<String> allSigs = getAllNeedTestMethods();
            for (String str : allSigs) {
                writer.write(str + "\n"); // 每个元素单独为一行
            }
            writer.close();
            System.out.println("内容已写入到文件：" + filePath);
        } catch (IOException e) {
            System.out.println("写入文件时出现错误：" + e.getMessage());
        }
	}
	
}
