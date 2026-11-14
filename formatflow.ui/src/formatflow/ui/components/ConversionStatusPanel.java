
package formatflow.ui.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ConversionStatusPanel extends Composite {

    public enum Status {
        READY,
        SUCCESS,
        ERROR
    }

    private final Label iconLabel;
    private final Label messageLabel;

    public ConversionStatusPanel(Composite parent) {
        super(parent, SWT.NONE);

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 4;
        layout.horizontalSpacing = 8;

        setLayout(layout);

        iconLabel = new Label(this, SWT.NONE);
        iconLabel.setLayoutData(
                new GridData(SWT.CENTER, SWT.CENTER, false, false)
        );

        messageLabel = new Label(this, SWT.NONE);
        messageLabel.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        setStatus(Status.READY, "Ready to convert", null);
    }


public void setStatus(
        Status status,
        String message,
        String details
) {
    if (isDisposed()) {
        return;
    }

    String symbol;

    switch (status) {
        case SUCCESS -> symbol = "✓";
        case ERROR -> symbol = "✕";
        default -> symbol = "○";
    }

    iconLabel.setText(symbol);
    messageLabel.setText(message != null ? message : "");

    layout(true, true);
}

}