
package formatflow.ui.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
    private final Label detailsLabel;

    public ConversionStatusPanel(Composite parent) {
        super(parent, SWT.BORDER);

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 12;
        layout.marginHeight = 10;
        layout.horizontalSpacing = 10;

        setLayout(layout);

        // Значок состояния
        iconLabel = new Label(this, SWT.NONE);
        iconLabel.setLayoutData(
                new GridData(SWT.CENTER, SWT.CENTER, false, false)
        );

        // Контейнер для текста
        Composite textContainer = new Composite(this, SWT.NONE);

        GridLayout textLayout = new GridLayout(1, false);
        textLayout.marginWidth = 0;
        textLayout.marginHeight = 0;

        textContainer.setLayout(textLayout);
        textContainer.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        messageLabel = new Label(textContainer, SWT.NONE);
        messageLabel.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        detailsLabel = new Label(textContainer, SWT.WRAP);
        detailsLabel.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        setStatus(
                Status.READY,
                "Ready to convert",
                "Select files and start conversion"
        );
    }

    public void setStatus(
            Status status,
            String message,
            String details
    ) {

        if (isDisposed()) {
            return;
        }

        int iconId;

        switch (status) {
            case SUCCESS -> iconId = SWT.ICON_INFORMATION;
            case ERROR -> iconId = SWT.ICON_ERROR;
            default -> iconId = SWT.ICON_INFORMATION;
        }

        Image image = getDisplay().getSystemImage(iconId);
        iconLabel.setImage(image);

        messageLabel.setText(message != null ? message : "");
        detailsLabel.setText(details != null ? details : "");

        layout(true, true);
    }
}
