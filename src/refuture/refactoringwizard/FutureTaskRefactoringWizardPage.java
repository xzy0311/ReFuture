package refuture.refactoringwizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import refuture.refactoring.RefutureRefactoring;
import refuture.sootUtil.SootConfig;

public class FutureTaskRefactoringWizardPage extends UserInputWizardPage {
	
	Button btnCheck1;
	Button btnCheck2;
	Button btnCheck3;
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
		btnCheck1 = new Button(composite, SWT.CHECK);
		btnCheck1.setText("Find Thread Mode");
		GridData gdBtnCheck = new GridData();
		gdBtnCheck.horizontalSpan = 2;
		gdBtnCheck.horizontalAlignment = GridData.FILL;
		btnCheck1.setLayoutData(gdBtnCheck);
		btnCheck2 = new Button(composite, SWT.CHECK);
		btnCheck2.setText("Cancel Detect Mode");
		GridData gdBtnCheck2 = new GridData();
		gdBtnCheck2.horizontalAlignment = GridData.FILL;
		btnCheck2.setLayoutData(gdBtnCheck2);
		
		btnCheck3 = new Button(composite, SWT.CHECK);
		btnCheck3.setText("Extreme Speed Mode");
		GridData gdBtnCheck3 = new GridData();
		gdBtnCheck3.horizontalAlignment = GridData.FILL;
		btnCheck3.setLayoutData(gdBtnCheck3);
		
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
		btnCheck1.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck1.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck1.getEnabled()){
					refactoring.setRefactorPattern(2);
				}else{
					refactoring.setRefactorPattern(1);
				}
			}
		});
		btnCheck2.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck2.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck2.getEnabled()){
					refactoring.setCancelPattern(true);
				}else{
					refactoring.setCancelPattern(false);
				}
			}
		});
		btnCheck3.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck3.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck3.getEnabled()){
					SootConfig.extremeSpeedModel = true;
				}else{
					SootConfig.extremeSpeedModel = false;
				}
			}
		});
	}

}
