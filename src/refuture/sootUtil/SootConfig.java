package refuture.sootUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;

import refuture.refactoring.AnalysisUtils;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;

public class SootConfig {
	public static boolean extremeSpeedModel;
	
	public static List<String> sourceClassPath;
	
	public static List<String> libClassPath;
	
	public static void sootConfigStaticInitial() {
		extremeSpeedModel = false;
		sourceClassPath = null;
		libClassPath = null;
	}
	
    public static void setupSoot() {
    	try {
			processClassPath();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        soot.G.reset();
        BasicOptions();
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
        // 处理目录中所有的类
        Options.v().set_process_dir(sourceClassPath);
        AnalysisUtils.debugPrint("[setupSoot]:本次process_dir："+Scene.v().getSootClassPath());
        // 添加jar包路径
        for(String libPath :libClassPath) {
        	Scene.v().extendSootClassPath(libPath);
        }
        AnalysisUtils.debugPrint("[setupSoot]:本次SootClassPath："+Scene.v().getSootClassPath());
        Scene.v().loadNecessaryClasses();
        AnalysisUtils.debugPrint("[setupSoot]:加载必要类完毕！");
    }


    /**
     * Spark 是一个灵活的指针分析框架，同时支持创建Call Graph。
     * 选用指针分析算法创建Call Graph。
     */
    public static void CGPhaseOptions(){
    	 // 开启创建CG
    	Options.v().setPhaseOption("cg", "all-reachable:true");
        Options.v().setPhaseOption("cg.spark","enabled:true");
        Options.v().setPhaseOption("cg.spark","on-fly-cg:true");
        if(extremeSpeedModel) {
        	System.out.println("[CGPhaseOptions]:当前为快速模式");
        	Options.v().setPhaseOption("cg.spark","apponly:true");
        }
    }
    
    private static void processClassPath() throws JavaModelException {
    	IProject project = AnalysisUtils.eclipseProject;
    	String projectPath = project.getFullPath().toOSString();
    	String locationPath = project.getLocation().toOSString();
    	Set<String> sourceClassPath =new HashSet<>();
    	Set<String> libClassPath = new HashSet<>();
    	Set<String> libProjectPath = new HashSet<>();
		PerProjectInfo ppi = JavaModelManager.getJavaModelManager().getPerProjectInfoCheckExistence(project);
		String javaFlag = File.separator+"jre"+File.separator+"lib"+File.separator;
		for(IClasspathEntry cp : ppi.rawClasspath) {
			if(cp.getContentKind() == 1) {
				IPath ip = cp.getOutputLocation();
				if(ip != null&& !ip.isEmpty()) {
					sourceClassPath.add(ip.toOSString().replaceFirst(projectPath, locationPath));
				}else if(cp.getEntryKind() == IClasspathEntry.CPE_PROJECT){
					libProjectPath.add(cp.getPath().toOSString());
				}
			}else if(cp.getContentKind() ==2 ) {
				IPath ip = cp.getPath();
				if(ip != null&& !ip.isEmpty()) {
					String libPathString = ip.toOSString();
					if(!libPathString.contains(javaFlag)) {
			            File file = new File(libPathString);
			            if (!file.exists()) {
			               continue;
			            }
						libClassPath.add(libPathString);
					}
				}
			}
		}
		for(IClasspathEntry cp : ppi.resolvedClasspath) {
			if(cp.getContentKind() == 1) {
				IPath ip = cp.getOutputLocation();
				if(ip != null&& !ip.isEmpty()) {
					sourceClassPath.add(ip.toOSString().replaceFirst(projectPath, locationPath));
				}else if(cp.getEntryKind() == IClasspathEntry.CPE_PROJECT){
					libProjectPath.add(cp.getPath().toOSString());
				}
			}else if(cp.getContentKind() ==2 ) {
				IPath ip = cp.getPath();
				if(ip != null&& !ip.isEmpty()) {
					String libPathString = ip.toOSString();
					if(!libPathString.contains(javaFlag)) {
			            File file = new File(libPathString);
			            if (!file.exists()) {
			               continue;
			            }
						libClassPath.add(libPathString);
					}
				}
			}
		}
		for(String libProject: libProjectPath) {
			for(String classPath :sourceClassPath) {
				String newLibProjectPath = classPath.replaceFirst(projectPath, libProject);
		        File file = new File(newLibProjectPath);
		        if (file.exists()) {
		            libClassPath.add(newLibProjectPath);
		        }
			}
		}
		
		SootConfig.sourceClassPath = new ArrayList<String>(sourceClassPath);
		SootConfig.libClassPath = new ArrayList<String>(libClassPath);
    }

    
}