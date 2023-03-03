package refuture.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FutureTaskRefactoringWizardPage extends UserInputWizardPage {
	
	Button btnCheck;
	Label labName;
	Text txtTimeOut;

	public FutureTaskRefactoringWizardPage(String name) {
		super(name);
	}

	@Override
	public void createControl(Composite parent) {
		// define UI
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout lay = new GridLayout();
		lay.numColumns = 2;
		composite.setLayout(lay);
		btnCheck = new Button(composite, SWT.CHECK);
		btnCheck.setText("Add timeout parameter");
		GridData gdBtnCheck = new GridData();
		gdBtnCheck.horizontalSpan = 2;
		gdBtnCheck.horizontalAlignment = GridData.FILL;
		btnCheck.setLayoutData(gdBtnCheck);
		labName = new Label(composite, SWT.WRAP);
		labName.setText("TimeOut:");
		GridData gdLabName = new GridData();
		gdLabName.horizontalAlignment = GridData.BEGINNING;
		gdLabName.grabExcessHorizontalSpace = true;
		labName.setLayoutData(gdLabName);
		txtTimeOut = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gdTxtTimeOut = new GridData();
		gdTxtTimeOut.horizontalAlignment = GridData.END;
		gdLabName.grabExcessHorizontalSpace = true;
		txtTimeOut.setLayoutData(gdTxtTimeOut);
		txtTimeOut.setText("500");
		// init status
		labName.setEnabled(false);
		txtTimeOut.setEnabled(false);
		// add listener
		defineListener();
		// 将 composite 纳入框架的控制
		setControl(composite);
		Dialog.applyDialogFont(composite);
//		notifyStatus(true, "refactoring finished");
	}
	
	private void notifyStatus(boolean valid, String message) { 
		 // 设置错误信息
		 setErrorMessage(message); 
		 // 设置页面完成状态
		 setPageComplete(valid); 
	 }
	
//	private void setRefactoring(boolean selection, String text) { 
//	    AnnotationRefactoring refactoring = (AnnotationRefactoring) getRefactoring();
//	    refactoring.setNeedTimeout(true); 
//	    if(selection) { 
//	    	refactoring.setTimeout(txtTimeOut.getText());
////	        refactoring.setTimeout(Integer.valueOf(txtTimeOut.getText()).intValue());
//	    } 
//	}
	
	/**
	 * define the action listener
	 */
	private void defineListener(){
		FutureTaskRefactoring refactoring = (FutureTaskRefactoring) getRefactoring();
		
		
		btnCheck.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck.getEnabled()){
					System.out.println(btnCheck.getEnabled());
					refactoring.needTimeout = true;
					txtTimeOut.setEnabled(true);
				}else{
					refactoring.needTimeout = false;
					txtTimeOut.setEnabled(false);
				}
				
			}
			
		});
		
		txtTimeOut.addModifyListener(new ModifyListener(){

			@Override
			public void modifyText(ModifyEvent arg0) {
				refactoring.timeoutValue = txtTimeOut.getText();
			}
			
		});
	}

}
