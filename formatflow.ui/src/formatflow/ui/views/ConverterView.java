package formatflow.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class ConverterView extends ViewPart {

    @Override
    public void createPartControl(Composite parent) {

        Composite container = new Composite(parent, SWT.NONE);

        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true)
        );

        Label title = new Label(container, SWT.NONE);
        title.setText("FormatFlow - File Converter");

        Button convertButton = new Button(container, SWT.PUSH);
        convertButton.setText("Convert");
    }

    @Override
    public void setFocus() {

    }
}