package formatflow.ui.components;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
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

    private static final int ICON_SIZE = 16;

    private static final RGB SUCCESS_RGB = new RGB(46, 160, 67);
    private static final RGB ERROR_RGB = new RGB(218, 54, 51);

    private final LocalResourceManager resources;
    private final Label iconLabel;
    private final Label messageLabel;

    private Image currentIcon;

    public ConversionStatusPanel(Composite parent) {
        super(parent, SWT.NONE);

        resources = new LocalResourceManager(
                JFaceResources.getResources(), this);

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 4;
        layout.horizontalSpacing = 8;
        setLayout(layout);

        iconLabel = new Label(this, SWT.NONE);

        GridData iconData = new GridData(
                SWT.CENTER, SWT.CENTER, false, false
        );

        iconData.verticalIndent = 2;

        iconLabel.setLayoutData(iconData);

        messageLabel = new Label(this, SWT.NONE);
        messageLabel.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Иконка создаётся вручную, поэтому освобождаем её сами
        addDisposeListener(e -> disposeCurrentIcon());

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

        Color color = colorFor(status);

        Image newIcon = createIcon(status, color);
        iconLabel.setImage(newIcon);
        disposeCurrentIcon();
        currentIcon = newIcon;

        messageLabel.setText(message != null ? message : "");

        // Ready/Success: приглушённый серый текст, как на макете.
        // Error: цвет ошибки, чтобы бросалось в глаза.
        messageLabel.setForeground(
                status == Status.ERROR ? color : getGrayColor());

        // Подробности не занимают место, а показываются во всплывающей подсказке
        String tooltip = (details != null && !details.isBlank())
                ? details : null;
        messageLabel.setToolTipText(tooltip);
        iconLabel.setToolTipText(tooltip);

        layout(true, true);
    }

    private Color colorFor(Status status) {
        return switch (status) {
            case SUCCESS -> resources.create(
                    ColorDescriptor.createFrom(SUCCESS_RGB));
            case ERROR -> resources.create(
                    ColorDescriptor.createFrom(ERROR_RGB));
            default -> getGrayColor();
        };
    }

    private Color getGrayColor() {
        return getDisplay().getSystemColor(
                SWT.COLOR_WIDGET_NORMAL_SHADOW);
    }

    private Image createIcon(Status status, Color color) {

        Image image = new Image(getDisplay(), ICON_SIZE, ICON_SIZE);

        GC gc = new GC(image);
        try {
            // Фон иконки совпадает с фоном панели
            gc.setBackground(getBackground());
            gc.fillRectangle(0, 0, ICON_SIZE, ICON_SIZE);

            gc.setAdvanced(true);
            gc.setAntialias(SWT.ON);
            gc.setForeground(color);
            gc.setLineWidth(1);
            gc.drawOval(1, 1, ICON_SIZE - 3, ICON_SIZE - 3);

            gc.setLineWidth(2);

            if (status == Status.ERROR) {
                gc.drawLine(5, 5, 10, 10);
                gc.drawLine(10, 5, 5, 10);
            } else {
                gc.drawPolyline(new int[] { 4, 8, 7, 11, 12, 5 });
            }
        } finally {
            gc.dispose();
        }

        return image;
    }

    private void disposeCurrentIcon() {
        if (currentIcon != null && !currentIcon.isDisposed()) {
            currentIcon.dispose();
        }
        currentIcon = null;
    }
}