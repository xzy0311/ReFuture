package refuture.refactoring;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactRepositoryRef;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.embedder.MavenExecutionContext;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


public class ExportReferencedLibraries {
    public static void export(IJavaProject javaProject) {
        try {
            // 获取类路径条目（Referenced Libraries）
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            IProject project = javaProject.getProject();
            for(IClasspathEntry entry:entries) {
            	System.out.println(entry);
            }
            // 指定导出目录
            String exportPath = javaProject.getPath().toOSString()+File.separator+"refuture-lib";
            System.out.println("debug:"+exportPath);
            File exportDir = new File(exportPath);
            // 遍历类路径条目
            for (IClasspathEntry entry : entries) {
                // 仅处理 JAR 类型的条目
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    // 获取 JAR 文件的完整路径
                	System.out.println(entry.getPath());
                    IResource resource = project.findMember(entry.getPath());
                    String jarPath = resource.getRawLocation().toOSString();
                    
                    // 拷贝 JAR 文件到导出目录
                    File jarFile = new File(jarPath);
                    File exportFile = new File(exportDir, jarFile.getName());
                    FileUtils.copyFile(jarFile, exportFile);
                }else if(entry.getPath().toString().startsWith("org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER")) {
                	processM2e(javaProject);
                }
                
            }
            
            System.out.println("导出完成！");
        } catch (CoreException | IOException e) {
            e.printStackTrace();
        }
    }
    public static void processM2e(IJavaProject javaProject) {
//    	IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();
//    	IMavenProjectFacade mavenProjectFacade = mavenProjectRegistry.create(javaProject.getProject(), null);
//    	MavenProject mavenProject = mavenProjectFacade.getMavenProject();
//    	try {
//			List<String> dependencies = mavenProject.getCompileClasspathElements();
//			
//		} catch (DependencyResolutionRequiredException e) {
//			e.printStackTrace();
//		}
//    	AnalysisUtils.throwNull();
    }
    
    
}