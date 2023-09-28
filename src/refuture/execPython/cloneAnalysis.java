package refuture.execPython;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;

public class cloneAnalysis {
	public static void main(String[] args) {
		URL url= cloneAnalysis.class.getProtectionDomain().getCodeSource().getLocation();
		
//		String projectPath = System.getProperty("user.dir");
		String projectPath = url.getPath();
		String parsePath = projectPath+"semantic-token-extract"+File.separator+"java"+File.separator+"parse.py";
		String detectPath = projectPath+"clone-detect"+File.separator+"clonedetector";
		String tokenDirPath = projectPath+"tokens";
		String reportDirPath = projectPath+"report";
		String inputDir = projectPath+"inputDir";
        try {  
//        	extract semantic tokens
        	deleteDirectory(tokenDirPath);
        	createDirectory(tokenDirPath);
        	deleteDirectory(reportDirPath);
            createDirectory(reportDirPath);
            Process p1 = Runtime.getRuntime().exec("python3 "+parsePath+" -i " + inputDir + " -o "+tokenDirPath+" -m common");  
            p1.waitFor();
//          clone detection
            Process p2 = Runtime.getRuntime().exec(detectPath+" -i "+tokenDirPath+" -o . -t 0.6 -m common -l java");
            p2.waitFor();
            Process p3 = Runtime.getRuntime().exec("mv bigcloneeval.pair report.log "+reportDirPath);
//          collect results
            Process process = Runtime.getRuntime().exec("find "+reportDirPath+" -type f -name *.pair");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String catCommand = "cat " + line;
                Process catProcess = Runtime.getRuntime().exec(catCommand);
                BufferedReader catReader = new BufferedReader(new InputStreamReader(catProcess.getInputStream()));
                String catLine;
                while ((catLine = catReader.readLine()) != null) {
                   System.out.println(catLine);
                }
                catReader.close();
                catProcess.waitFor();
            }
            //改造计划，利用无向图表示，一个数值对是一个节点，它与与它相似的节点相连。如果其中有一个节点是
            
            deleteDirectory(tokenDirPath);
            deleteDirectory(reportDirPath);
            reader.close();
            process.waitFor();
        } catch (Exception e) {  
            e.printStackTrace();  
        } 
	}
    private static void deleteDirectory(String directory) throws IOException, InterruptedException {
        String command = String.format("rm -rf %s", directory);
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
    }

    private static void createDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

}
