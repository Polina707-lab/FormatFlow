
package formatflow.ui.preview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FilePreviewPanel extends Composite {

    private final Text previewText;

    public FilePreviewPanel(Composite parent, String title) {
        super(parent, SWT.NONE);

        // Настраиваем расположение элементов
        setLayout(new GridLayout(1, false));

        // Заголовок панели
        Label titleLabel = new Label(this, SWT.NONE);
        titleLabel.setText(title);

        // Поле для отображения содержимого файла
        previewText = new Text(
                this,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL
                        | SWT.H_SCROLL | SWT.READ_ONLY
        );

        previewText.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true)
        );

        previewText.setText("No file selected");
    }

    // Отображение содержимого файла
    public void setPreview(String content) {
        previewText.setText(content != null ? content : "");
    }

    // Очистка предварительного просмотра
    public void clearPreview() {
        previewText.setText("No file selected");
    }
}
