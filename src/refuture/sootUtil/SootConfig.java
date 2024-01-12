package refuture.sootUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;

import refuture.refactoring.AnalysisUtils;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.options.Options;

public class SootConfig {
	public static boolean extremeSpeedModel;
	public static Date startTime;
	public static void sootConfigStaticInitial() {
		extremeSpeedModel = false;
	}
	
    public static void setupSoot(List<ICompilationUnit> allJavaFiles) {
    	startTime =new Date();
		System.out.println("The current start time is "+ startTime);
        soot.G.reset();
        BasicOptions();
        JBPhaseOptions();
        System.out.println("[setupSoot]:本次classPath："+Scene.v().getSootClassPath());
        Scene.v().loadNecessaryClasses();
        System.out.println("[setupSoot]:加载必要类完毕！");
		ExecutorSubclass.taskTypeAnalysis();
		ExecutorSubclass.executorSubClassAnalysis();
        ExecutorSubclass.threadPoolExecutorSubClassAnalysis();
        ExecutorSubclass.additionalExecutorServiceSubClassAnalysis();
		CollectionEntrypoint.entryPointInit(allJavaFiles);
        CGPhaseOptions();//启用Spark
        PackManager.v().runPacks();
        System.out.println("[setupSoot]:Soot配置完毕。");
        String filePath = "output.txt"; // 指定输出文件的路径
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(Scene.v().getEntryPoints().toString());
            fileWriter.close();
            System.out.println("字符串已成功输出到文件。");
        } catch (IOException e) {
            System.out.println("发生错误：" + e.getMessage());
        }
        Date currentTime = new Date();
        System.out.println("soot配置完毕的时间"+"The current start time is "+ currentTime+"已花费:"+((currentTime.getTime()-startTime.getTime())/1000)+"s");
    }

    /**
     * 基础配置
     */
    public static void BasicOptions(){
        // 将给定的类加载路径作为默认类加载路径
        Options.v().set_prepend_classpath(true);
        // 运行soot创建虚类，如果源码中找不到对应源码，soot 会自动创建一个虚拟的类来代替。
        Options.v().set_allow_phantom_refs(true);
        // 加入全局处理阶段
        Options.v().set_whole_program(true);
        // 行数表
        Options.v().set_keep_line_number(true);
        // Set output format for Soot
        Options.v().set_output_format(Options.output_format_none);
        // 添加jar包路径
        Options.v().set_process_jar_dir(getJarFolderPath());
        // 处理目录中所有的类
        Options.v().set_process_dir(AnalysisUtils.getSootClassPath());
    }

    /**
     * soot 的执行过程可以分为多个阶段（Phase），不同类型的Body，都有自己对应的处理阶段。
     * 像 JimpleBody 对应的处理阶段叫'jb'，
     * soot 提供了PhaseOptions，可以通过它来改变 soot 在该阶段的处理方式。
     */
    public static void JBPhaseOptions(){
        Options.v().setPhaseOption("jb", "use-original-names:true");
    }

    /**
     * Spark 是一个灵活的指针分析框架，同时支持创建Call Graph。
     * 选用指针分析算法创建Call Graph。
     * 除"cg.spark"之外，soot 还支持"cg.cha"（使用CHA算法创建Call Graph）、"cg.paddle"（paddle框架创建）、"CG" （分析整个源代码，包括JDK部分）
     */
    public static void CGPhaseOptions(){
    	 // 开启创建CG
        Options.v().setPhaseOption("cg.spark","enabled:true");
        if(!extremeSpeedModel) {
        	System.out.println("[CGPhaseOptions]:当前非快速模式");
        	//兼容多个main函数，并且不可抵达的方法也会进行分析。
        	Options.v().setPhaseOption("cg", "all-reachable:true");
        }else {
        	Options.v().setPhaseOption("cg", "all-reachable:true");
        	CHATransformer.v().transform();
        	System.out.println("cha CallGraph build success");
        	Options.v().setPhaseOption("cg", "all-reachable:false");
        	Scene.v().setEntryPoints(new ArrayList<SootMethod>(CollectionEntrypoint.getSetEntryPoint()));
        	System.out.println("设置入口点完成：");
        }
        // 一种复杂的分析方法，能够题升精度，同时会消耗大量时间。
        Options.v().setPhaseOption("cg.spark","on-fly-cg:true");
//        Options.v().setPhaseOption("cg.spark","apponly:true");
        
    }

    private static List<String> getJarFolderPath() {
    	String jarFolderPath = AnalysisUtils.getProjectPath()+File.separator+"lib";
    	File file = new File(jarFolderPath);
    	if(file.exists()) {
    		return Collections.singletonList(jarFolderPath);
    	}else {
        	return null;
    	}
    }
    
    
}