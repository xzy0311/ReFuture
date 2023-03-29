package refuture.sootUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public abstract class SootConfig {

//    private final static String EXAMPLE_ClASS_PATH = System.getProperty("user.dir") + File.separator + "target" + File.separator + "classes";
//	private final static String EXAMPLE_ClASS_PATH = System.getProperty("user.dir");
    private static List<String> excludePackage;

    public static void setupSoot(String classPath) {

//    	System.out.println("【sootdebug】当前处理的类路径"+"EXAMPLE_ClASS_PATH");
        soot.G.reset();

        BasicOptions(classPath);
        

//        JBPhaseOptions();

//        CGPhaseOptions();



        Scene.v().loadNecessaryClasses();

//        System.out.println("[xzydebug]");

//        SootClass sootClass = Scene.v().getSootClass(className);
//
//        if(setMain)
//        {
//          Scene.v().setMainClass(sootClass);
//        }
//
//        sootClass.setApplicationClass();
//        PackManager.v().runPacks();

    }

    /**
     * 基础配置
     */
    public static void BasicOptions(String classPath){

        // 设置 soot 类加载路径
        Options.v().set_soot_classpath(classPath);
        // 将给定的类加载路径作为默认类加载路径
        Options.v().set_prepend_classpath(true);
        // 运行soot创建虚类，如果源码中找不到对应源码，soot 会自动创建一个虚拟的类来代替。
        Options.v().set_allow_phantom_refs(false);
        // 加入全局处理阶段
        Options.v().set_whole_program(true);
        // 行数表
        Options.v().set_keep_line_number(true);
        // 输入文件时，使用的编译码类型
        Options.v().set_output_format(Options.output_format_jimple);
        // 处理目录中所有的类
        Options.v().set_process_dir(Collections.singletonList(classPath));
        // 详细地打印处理过程的信息
        Options.v().set_verbose(true);
        // 去除某些类
        Options.v().set_exclude(AddExcludePackage());

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
        // 同 BasicOptions 中的 verbose
        Options.v().setPhaseOption("cg.spark","verbose:true");
        // 一种复杂的分析方法，能够题升精度，同时会消耗大量时间。
        Options.v().setPhaseOption("cg.spark","on-fly-cg:true");

    }

    public static List<String> AddExcludePackage(){

        if (excludePackage == null){
            excludePackage = new ArrayList<>();
        }

        excludePackage.add("java.");
        excludePackage.add("javax.");
        excludePackage.add("sun.");
        excludePackage.add("sunw.");
        excludePackage.add("com.sun.");
        excludePackage.add("com.ibm.");

        return excludePackage;
    }
    
    
}