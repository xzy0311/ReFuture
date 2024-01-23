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

	public static void sootConfigStaticInitial() {
		extremeSpeedModel = false;
	}
	
    public static void setupSoot() {
        soot.G.reset();
        BasicOptions();
        System.out.println("[setupSoot]:本次classPath："+Scene.v().getSootClassPath());
        Scene.v().loadNecessaryClasses();
        System.out.println("[setupSoot]:加载必要类完毕！");
        CGPhaseOptions();//启用Spark
        PackManager.v().runPacks();
        System.out.println("[setupSoot]:Soot包运行完毕。");
    }

    /**
     * 基础配置
     */
    public static void BasicOptions(){
        // 将给定的类加载路径作为默认类加载路径，如果手动设置的话，可以根据不同的待重构项目编译等级来确定jdk具体路径。
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
     * Spark 是一个灵活的指针分析框架，同时支持创建Call Graph。
     * 选用指针分析算法创建Call Graph。
     * 除"cg.spark"之外，soot 还支持"cg.cha"（使用CHA算法创建Call Graph）、"cg.paddle"（paddle框架创建）、"CG" （分析整个源代码，包括JDK部分）
     */
    public static void CGPhaseOptions(){
    	 // 开启创建CG
        Options.v().setPhaseOption("cg.spark","enabled:true");
    	Options.v().setPhaseOption("cg", "all-reachable:true");
        // 一种复杂的分析方法，能够题升精度，同时会消耗大量时间。
        Options.v().setPhaseOption("cg.spark","on-fly-cg:true");
        if(extremeSpeedModel) {
        	System.out.println("[CGPhaseOptions]:当前为快速模式");
        	Options.v().setPhaseOption("cg.spark","apponly:true");
        }
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