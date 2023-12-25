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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;

import refuture.refactoring.Future2Completable;
import refuture.refactoring.RefutureException;
import refuture.refactoring.RefutureRefactoring;
import refuture.sootUtil.Cancel;
import refuture.sootUtil.SootConfig;

public class FutureTaskRefactoringWizardPage extends UserInputWizardPage {
	
	Button btnCheck1;
	Button btnCheck2;
	Button btnCheck3;
	Button btnCheck4;
	Button btnCheck5;
	Text textField1;
	Text textField2;
	Text textField3;
	public FutureTaskRefactoringWizardPage(String name) {
		super(name);
	}

	@Override
	public void createControl(Composite parent) {
		// define UI
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout lay = new GridLayout();
		lay.numColumns = 1; // Set to 1 to have each button on a new line
		composite.setLayout(lay);

		// Button 1
		btnCheck1 = new Button(composite, SWT.CHECK);
		btnCheck1.setText("Find Thread Mode");
		GridData gdBtnCheck1 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		btnCheck1.setLayoutData(gdBtnCheck1);

		// Button 2
		btnCheck2 = new Button(composite, SWT.CHECK);
		btnCheck2.setText("Disable Cancel(true) Detect Mode");
		GridData gdBtnCheck2 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		btnCheck2.setLayoutData(gdBtnCheck2);
		
		// Button 3
		btnCheck3 = new Button(composite, SWT.CHECK);
		btnCheck3.setText("Extreme Speed Mode(Debug Use)");
		GridData gdBtnCheck3 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		btnCheck3.setLayoutData(gdBtnCheck3);
		
		// Button 4
		btnCheck4 = new Button(composite, SWT.CHECK);
		btnCheck4.setText("Fine Refactoring");
		GridData gdBtnCheck4 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		btnCheck4.setLayoutData(gdBtnCheck4);

		btnCheck5 = new Button(composite, SWT.CHECK);
		btnCheck5.setText("调试用，关闭DefRech");
		GridData gdBtnCheck5 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		btnCheck5.setLayoutData(gdBtnCheck5);

		Label label1 = new Label(composite, SWT.NONE);
		label1.setText("需定位的所在类:");
		textField1 = new Text(composite, SWT.BORDER);
 		textField1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

 		Label label2 = new Label(composite, SWT.NONE);
		label2.setText("需定位的所在方法:");
		textField2 = new Text(composite, SWT.BORDER);
		textField2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label label3 = new Label(composite, SWT.NONE);
		label3.setText("需定位的行号:");
		textField3 = new Text(composite, SWT.BORDER);
		textField3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		// Add listeners and other necessary code
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
					refactoring.setDisableCancelPattern(true);
				}else{
					refactoring.setDisableCancelPattern(false);
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
		
		btnCheck4.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck4.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck4.getEnabled()){
					Future2Completable.fineRefactoring = true;
				}else{
					Future2Completable.fineRefactoring = false;
				}
			}
		});
		
		btnCheck5.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck5.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck5.getEnabled()){
					Cancel.debug_UseDefRech = false;
				}else{
					Cancel.debug_UseDefRech = true;
				}
			}
		});
		
		textField1.addModifyListener(e -> {
		    String input = textField1.getText();
		    Future2Completable.debugClassName = input;
		});

		textField2.addModifyListener(e -> {
		    String input = textField2.getText();
		    Future2Completable.debugMethodName = input;
		});

		textField3.addModifyListener(e -> {
		    try {
		        int input = Integer.parseInt(textField3.getText());
		        Future2Completable.debugLineNumber = input;
		    } catch (NumberFormatException ex) {
		        throw new RefutureException(ex.getMessage());
		    }
		});
		
		
	}

}
