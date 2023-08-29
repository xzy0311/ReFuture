package refuture.refactoringwizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
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

import refuture.refactoring.RefutureRefactoring;

public class FutureTaskRefactoringWizardPage extends UserInputWizardPage {
	
	Button btnCheck;

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
		btnCheck.setText("refactoring get");
		GridData gdBtnCheck = new GridData();
		gdBtnCheck.horizontalSpan = 2;
		gdBtnCheck.horizontalAlignment = GridData.FILL;
		btnCheck.setLayoutData(gdBtnCheck);
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
	
	/**
	 * define the action listener
	 */
	private void defineListener(){
		RefutureRefactoring refactoring = (RefutureRefactoring) getRefactoring();
		
		
		btnCheck.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck.getEnabled()){
					refactoring.setRefactorPattern(2);
				}else{
					refactoring.setRefactorPattern(1);
				}
				
			}
			
		});
		
	}

}
