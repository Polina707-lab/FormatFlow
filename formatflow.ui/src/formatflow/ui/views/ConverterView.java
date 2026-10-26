
package formatflow.ui.views;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class ConverterView extends ViewPart {

    private Text sourceFileText;
    private Text outputFileText;

    private Combo sourceFormatCombo;
    private Combo targetFormatCombo;

    private Label statusLabel;

    @Override
    public void createPartControl(Composite parent) {

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        container.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true)
        );

        // Header
        Label title = new Label(container, SWT.NONE);
        title.setText("FormatFlow - File Converter");

        Label description = new Label(container, SWT.NONE);
        description.setText("Convert files using available formats");

        // Source file
        createSectionLabel(container, "Source file");

        Composite sourceRow = createTwoColumnRow(container);

        sourceFileText = new Text(sourceRow, SWT.BORDER);
        sourceFileText.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        Button sourceBrowseButton =
                new Button(sourceRow, SWT.PUSH);
        sourceBrowseButton.setText("Browse...");

        sourceBrowseButton.addListener(SWT.Selection, event -> {
            FileDialog dialog =
                    new FileDialog(parent.getShell(), SWT.OPEN);

            dialog.setText("Select source file");

            String path = dialog.open();

            if (path != null) {
                sourceFileText.setText(path);
            }
        });

        // Formats
        Composite formatsRow = createTwoColumnRow(container);

        Composite sourceFormatColumn =
                new Composite(formatsRow, SWT.NONE);
        sourceFormatColumn.setLayout(new GridLayout(1, false));
        sourceFormatColumn.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        createSectionLabel(sourceFormatColumn, "Source format");

        sourceFormatCombo =
                new Combo(sourceFormatColumn, SWT.DROP_DOWN | SWT.READ_ONLY);
        sourceFormatCombo.setItems("CSV");
        sourceFormatCombo.select(0);
        sourceFormatCombo.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        Composite targetFormatColumn =
                new Composite(formatsRow, SWT.NONE);
        targetFormatColumn.setLayout(new GridLayout(1, false));
        targetFormatColumn.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        createSectionLabel(targetFormatColumn, "Target format");

        targetFormatCombo =
                new Combo(targetFormatColumn, SWT.DROP_DOWN | SWT.READ_ONLY);
        targetFormatCombo.setItems("JSON");
        targetFormatCombo.select(0);
        targetFormatCombo.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        // Output file
        createSectionLabel(container, "Output file");

        Composite outputRow = createTwoColumnRow(container);

        outputFileText = new Text(outputRow, SWT.BORDER);
        outputFileText.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        Button outputBrowseButton =
                new Button(outputRow, SWT.PUSH);
        outputBrowseButton.setText("Browse...");

        outputBrowseButton.addListener(SWT.Selection, event -> {
            FileDialog dialog =
                    new FileDialog(parent.getShell(), SWT.SAVE);

            dialog.setText("Select output file");
            dialog.setFileName("output.json");
            dialog.setOverwrite(true);

            String path = dialog.open();

            if (path != null) {
                outputFileText.setText(path);
            }
        });

        // Convert button
        Button convertButton = new Button(container, SWT.PUSH);
        convertButton.setText("Convert");

        GridData buttonData =
                new GridData(SWT.FILL, SWT.CENTER, true, false);
        buttonData.verticalIndent = 12;
        convertButton.setLayoutData(buttonData);

        convertButton.addListener(SWT.Selection, event -> {
            handleConvert();
        });

        // Status
        statusLabel = new Label(container, SWT.WRAP);
        statusLabel.setText("Ready to convert");
        statusLabel.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );
    }

    private void createSectionLabel(
            Composite parent, String text) {

        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
    }

    private Composite createTwoColumnRow(Composite parent) {

        Composite row = new Composite(parent, SWT.NONE);
        row.setLayout(new GridLayout(2, false));
        row.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        return row;
    }

    private void handleConvert() {

        String sourcePath = sourceFileText.getText().trim();
        String outputPath = outputFileText.getText().trim();

        if (sourcePath.isEmpty() || outputPath.isEmpty()) {
            MessageDialog.openWarning(
                    getSite().getShell(),
                    "Missing file",
                    "Please select source and output files."
            );
            return;
        }

        File source = new File(sourcePath);

        if (!source.isFile()) {
            MessageDialog.openError(
                    getSite().getShell(),
                    "Invalid source file",
                    "The selected source file does not exist."
            );
            return;
        }

        if (new File(outputPath).equals(source)) {
            MessageDialog.openError(
                    getSite().getShell(),
                    "Invalid output file",
                    "Source and output files must be different."
            );
            return;
        }

        String sourceFormat = sourceFormatCombo.getText();
        String targetFormat = targetFormatCombo.getText();

        statusLabel.setText(
                "Ready: " + sourceFormat + " -> " + targetFormat
        );

        MessageDialog.openInformation(
                getSite().getShell(),
                "FormatFlow",
                "The interface is ready.\n"
                + "Next step: connect ConversionService."
        );
    }

    @Override
    public void setFocus() {
        if (sourceFileText != null
                && !sourceFileText.isDisposed()) {
            sourceFileText.setFocus();
        }
    }
}
