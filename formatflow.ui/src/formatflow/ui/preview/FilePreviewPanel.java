
package formatflow.ui.preview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Display;

public class FilePreviewPanel extends Composite {

    private final Text previewText;

    public FilePreviewPanel(Composite parent, String title) {
    	super(parent, SWT.NONE);

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 6;
        setLayout(layout);

        Label titleLabel = new Label(this, SWT.NONE);
        titleLabel.setText(title);

        previewText = new Text(this,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL
                        | SWT.H_SCROLL | SWT.READ_ONLY);
        previewText.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true));

        // Настраиваем моноширинный шрифт
        FontData[] fontData = previewText.getFont().getFontData();

        for (FontData data : fontData) {
        	data.setName(pickMonospaceFont(getDisplay()));
            data.setHeight(11);
        }

        Font previewFont = new Font(getDisplay(), fontData);
        previewText.setFont(previewFont);

        // Освобождаем шрифт при удалении текстового поля
        previewText.addDisposeListener(event -> previewFont.dispose());
        
        clearPreview();

    }
    
    private static String pickMonospaceFont(Display display) {
        for (String name : new String[] {"Menlo", "Monaco", "Consolas", "DejaVu Sans Mono"}) {
            if (display.getFontList(name, true).length > 0
                    || display.getFontList(name, false).length > 0) {
                return name;
            }
        }
        return "Monospaced";
    }

    // Отображение содержимого файла
    public void setPreview(String content) {
    	previewText.setForeground(null);   // обычный цвет темы
        previewText.setText(content != null ? content : "");
    }

    // Очистка предварительного просмотра
    public void clearPreview() {
    	previewText.setForeground(getDisplay().getSystemColor(
                SWT.COLOR_WIDGET_NORMAL_SHADOW));
        previewText.setText("File content preview...");
    }
}
