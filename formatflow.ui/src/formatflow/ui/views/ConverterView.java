
package formatflow.ui.views;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.eclipse.swt.custom.SashForm;

import formatflow.api.FileConversionService;
import formatflow.ui.services.ConversionServiceTracker;
import formatflow.ui.validation.ConversionValidator;
import formatflow.ui.components.ConversionStatusPanel;
import formatflow.ui.components.ConversionStatusPanel.Status;

import java.nio.file.Path;

import formatflow.ui.preview.FilePreviewPanel;
import formatflow.ui.preview.FilePreviewService;
import formatflow.ui.util.FileFormatDetector;
import formatflow.ui.util.OutputPathGenerator;


public class ConverterView extends ViewPart {

    private Text sourceFileText;
    private Text outputFileText;

    private Combo sourceFormatCombo;
    private Combo targetFormatCombo;

    private Button convertButton;
    private ConversionStatusPanel statusPanel;

    private ConversionServiceTracker serviceTracker;
    private FilePreviewPanel sourcePreview;
    private FilePreviewPanel outputPreview;



    private final FilePreviewService previewService =
            new FilePreviewService();
   
    private boolean outputPathManuallyChanged = false;
    private boolean updatingOutputPath = false;

    @Override
    public void createPartControl(Composite parent) {

        // Создаём интерфейс
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true)
        );

        createHeader(container);
        createSourceFileSection(container);
        createFormatsSection(container);
        createOutputFileSection(container);
        createConvertSection(container);
        createPreviewSection(container);

        // Запускаем отслеживание OSGi-сервиса
        Bundle bundle = FrameworkUtil.getBundle(ConverterView.class);

        if (bundle == null || bundle.getBundleContext() == null) {
            throw new IllegalStateException(
                    "FormatFlow UI BundleContext is unavailable"
            );
        }

        BundleContext context = bundle.getBundleContext();

        serviceTracker = new ConversionServiceTracker(
                context,
                this::scheduleFormatsUpdate
        );

        serviceTracker.open();
        updateSourceFormats();
    }

    private void createHeader(Composite parent) {

        Label title = new Label(parent, SWT.NONE);
        title.setText("FormatFlow - File Converter");

        Label description = new Label(parent, SWT.NONE);
        description.setText("Convert files using available formats");
    }

    private void createSourceFileSection(Composite parent) {

        createSectionLabel(parent, "Source file");

        Composite row = createTwoColumnRow(parent);

        sourceFileText = new Text(row, SWT.BORDER);
        sourceFileText.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        Button browseButton = new Button(row, SWT.PUSH);
        browseButton.setText("Browse...");

        browseButton.addListener(SWT.Selection, event -> {
            FileDialog dialog = new FileDialog(
                    parent.getShell(), SWT.OPEN
            );
            

            dialog.setText("Select source file");

            String path = dialog.open();

            if (path != null) {
                sourceFileText.setText(path);
            }
        });
        
        sourceFileText.addListener(SWT.Modify, event -> {

            if (sourcePreview != null) {
                updatePreview(
                        sourceFileText.getText(),
                        sourcePreview
                );

                outputPreview.clearPreview();
            }

            if (serviceTracker != null) {
                detectSourceFormat();
            }

            updateOutputPath();
            resetConversionStatus();
        });
    }

    private void createFormatsSection(Composite parent) {

        Composite row = createTwoColumnRow(parent);

        Composite sourceColumn = new Composite(row, SWT.NONE);
        sourceColumn.setLayout(new GridLayout(1, false));
        sourceColumn.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        createSectionLabel(sourceColumn, "Source format");

        sourceFormatCombo = new Combo(
                sourceColumn, SWT.DROP_DOWN | SWT.READ_ONLY
        );

        sourceFormatCombo.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        sourceFormatCombo.addListener(SWT.Selection, event -> {
            updateTargetFormats();
            updateOutputPath();
            outputPreview.clearPreview();
            resetConversionStatus();
        });

        Composite targetColumn = new Composite(row, SWT.NONE);
        targetColumn.setLayout(new GridLayout(1, false));
        targetColumn.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        createSectionLabel(targetColumn, "Target format");

        targetFormatCombo = new Combo(
                targetColumn, SWT.DROP_DOWN | SWT.READ_ONLY
        );

        targetFormatCombo.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );
        
        targetFormatCombo.addListener(SWT.Selection, event -> {
            updateOutputPath();
            outputPreview.clearPreview();
            resetConversionStatus();
        });
    }

    private void createOutputFileSection(Composite parent) {

        createSectionLabel(parent, "Output file");

        Composite row = createTwoColumnRow(parent);

        outputFileText = new Text(row, SWT.BORDER);
        outputFileText.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        Button browseButton = new Button(row, SWT.PUSH);
        browseButton.setText("Browse...");

        browseButton.addListener(SWT.Selection, event -> {

            FileDialog dialog = new FileDialog(
                    parent.getShell(), SWT.SAVE
            );

            dialog.setText("Select output file");

            String format = targetFormatCombo.getText();

            if (format.isEmpty()) {
                dialog.setFileName("output");
            } else {
                dialog.setFileName("output." + format.toLowerCase());
            }

            dialog.setOverwrite(true);

            String path = dialog.open();

            if (path != null) {
                outputPathManuallyChanged = true;
                outputFileText.setText(path);
            }
        });
        
        outputFileText.addListener(SWT.Modify, event -> {

            if (!updatingOutputPath) {
                outputPathManuallyChanged = true;
            }

            if (outputPreview != null) {
                outputPreview.clearPreview();
            }

            resetConversionStatus();
        });
    }

    private void createConvertSection(Composite parent) {

        convertButton = new Button(parent, SWT.PUSH);
        convertButton.setText("Convert");

        GridData buttonData =
                new GridData(SWT.FILL, SWT.CENTER, true, false);
        buttonData.verticalIndent = 12;

        convertButton.setLayoutData(buttonData);

        convertButton.addListener(
                SWT.Selection,
                event -> handleConvert()
        );

        statusPanel = new ConversionStatusPanel(parent);

        GridData statusData = new GridData(
                SWT.FILL,
                SWT.CENTER,
                true,
                false
        );

        statusData.verticalIndent = 8;
        statusPanel.setLayoutData(statusData);
    }

    private void createSectionLabel(
            Composite parent,
            String text
    ) {
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

        try {
            ConversionValidator.validateFiles(
                    sourcePath, outputPath
            );
        } catch (IllegalArgumentException | IOException e) {
        	updateStatus(
        	        Status.ERROR,
        	        "Invalid file",
        	        e.getMessage()
        	);

            MessageDialog.openError(
                    getSite().getShell(),
                    "File validation error",
                    e.getMessage() != null ? e.getMessage() : e.toString()
            );
            return;
        }

        String sourceFormat = sourceFormatCombo.getText();
        String targetFormat = targetFormatCombo.getText();

        if (sourceFormat.isEmpty() || targetFormat.isEmpty()) {
            MessageDialog.openWarning(
                    getSite().getShell(),
                    "Missing format",
                    "Please select source and target formats."
            );
            return;
        }

        FileConversionService service = serviceTracker.getService();

        if (service == null) {
        	updateStatus(
        	        Status.ERROR,
        	        "Service unavailable",
        	        "Conversion service is not available"
        	);

            MessageDialog.openError(
                    getSite().getShell(),
                    "Service unavailable",
                    "File conversion service is not available."
            );
            return;
        }

        try {
            service.convert(
                    new File(sourcePath),
                    new File(outputPath),
                    sourceFormat,
                    targetFormat
            );
            
            updatePreview(outputPath, outputPreview);

            updateStatus(
                    Status.SUCCESS,
                    "Conversion completed successfully",
                    "Output file is ready"
            );

            MessageDialog.openInformation(
                    getSite().getShell(),
                    "Success",
                    "File converted successfully!"
            );

        } catch (IOException | IllegalArgumentException e) {
        	updateStatus(
        	        Status.ERROR,
        	        "Conversion failed",
        	        e.getMessage()
        	);
          
            outputPreview.clearPreview();

            MessageDialog.openError(
                    getSite().getShell(),
                    "Conversion error",
                    e.getMessage() != null ? e.getMessage() : e.toString()
            );
        }
    }

    private void updateSourceFormats() {

        String previousSource = sourceFormatCombo.getText();

        FileConversionService service = serviceTracker.getService();

        if (service == null) {
            sourceFormatCombo.removeAll();
            targetFormatCombo.removeAll();
            updateStatus(
                    Status.ERROR,
                    "Service unavailable",
                    "Waiting for conversion service"
            );
            updateConvertButtonState();
            return;
        }

        String[] formats = service.getSourceFormats()
                .toArray(new String[0]);

        sourceFormatCombo.setItems(formats);

        if (formats.length == 0) {
            targetFormatCombo.removeAll();
            updateStatus(
                    Status.READY,
                    "No converters available",
                    "Start a converter bundle to continue"
            );
            updateConvertButtonState();
            return;
        }

        int previousIndex =
                sourceFormatCombo.indexOf(previousSource);

        if (previousIndex >= 0) {
            sourceFormatCombo.select(previousIndex);
        } else {
            sourceFormatCombo.select(0);
        }

        detectSourceFormat();
        updateTargetFormats();
        updateOutputPath();

        updateStatus(
                Status.READY,
                "Ready to convert",
                "Select files and start conversion"
        );
        updateConvertButtonState();
    }

    private void updateTargetFormats() {

        String previousTarget = targetFormatCombo.getText();

        FileConversionService service = serviceTracker.getService();

        if (service == null) {
            targetFormatCombo.removeAll();
            updateConvertButtonState();
            return;
        }

        String sourceFormat = sourceFormatCombo.getText();

        String[] formats = service.getTargetFormats(sourceFormat)
                .toArray(new String[0]);

        targetFormatCombo.setItems(formats);

        int previousIndex =
                targetFormatCombo.indexOf(previousTarget);

        if (previousIndex >= 0) {
            targetFormatCombo.select(previousIndex);
        } else if (formats.length > 0) {
            targetFormatCombo.select(0);
        }

        updateConvertButtonState();
    }

    private void updateConvertButtonState() {

        if (convertButton == null || convertButton.isDisposed()) {
            return;
        }

        boolean formatsAvailable =
                !sourceFormatCombo.getText().isEmpty()
                && !targetFormatCombo.getText().isEmpty();

        boolean serviceAvailable =
                serviceTracker != null
                && serviceTracker.getService() != null;

        convertButton.setEnabled(
                formatsAvailable && serviceAvailable
        );
    }

    private void scheduleFormatsUpdate() {

        Display display = Display.getDefault();

        if (display.isDisposed()) {
            return;
        }

        display.asyncExec(() -> {
        	if (statusPanel == null || statusPanel.isDisposed()
        	        || serviceTracker == null) {
        	    return;
        	}

            updateSourceFormats();
        });
    }
    
  
private void createPreviewSection(Composite parent) {

    SashForm previewContainer = new SashForm(
            parent,
            SWT.HORIZONTAL
    );

    GridData layoutData = new GridData(
            SWT.FILL,
            SWT.FILL,
            true,
            true
    );

    layoutData.heightHint = 250;
    layoutData.minimumHeight = 150;

    previewContainer.setLayoutData(layoutData);

    sourcePreview = new FilePreviewPanel(
            previewContainer,
            "Source preview"
    );

    outputPreview = new FilePreviewPanel(
            previewContainer,
            "Output preview"
    );

    previewContainer.setWeights(50, 50);
}

    
    private void updatePreview(
            String filePath,
            FilePreviewPanel panel
    ) {
        if (filePath == null || filePath.isBlank()) {
            panel.clearPreview();
            return;
        }

        try {
            String content = previewService.readPreview(
                    Path.of(filePath.trim())
            );

            panel.setPreview(content);

        } catch (IOException | IllegalArgumentException e) {
            panel.setPreview(
                    "Preview unavailable: "
                            + (e.getMessage() != null
                            ? e.getMessage() : e.toString())
            );
        }
    }
    
    private void detectSourceFormat() {

        String filePath = sourceFileText.getText().trim();

        String detectedFormat =
                FileFormatDetector.detectFormat(filePath);

        if (detectedFormat.isEmpty()) {
            return;
        }

        int index = sourceFormatCombo.indexOf(detectedFormat);

        if (index >= 0) {
            sourceFormatCombo.select(index);
            updateTargetFormats();
        }
    }
    
    private void updateOutputPath() {

        if (outputPathManuallyChanged) {
            return;
        }

        String generatedPath = OutputPathGenerator.generate(
                sourceFileText.getText(),
                targetFormatCombo.getText()
        );

        updatingOutputPath = true;

        try {
            outputFileText.setText(generatedPath);
        } finally {
            updatingOutputPath = false;
        }
    }
    
    private void updateStatus(
            Status status,
            String message,
            String details
    ) {

        if (statusPanel == null || statusPanel.isDisposed()) {
            return;
        }

        statusPanel.setStatus(status, message, details);
    }
    
    private void resetConversionStatus() {

        if (statusPanel == null || statusPanel.isDisposed()) {
            return;
        }

        updateStatus(
                Status.READY,
                "Ready to convert",
                "Select files and start conversion"
        );
    }

    @Override
    public void setFocus() {
        if (sourceFileText != null
                && !sourceFileText.isDisposed()) {
            sourceFileText.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (serviceTracker != null) {
            serviceTracker.close();
            serviceTracker = null;
        }

        super.dispose();
    }
}
