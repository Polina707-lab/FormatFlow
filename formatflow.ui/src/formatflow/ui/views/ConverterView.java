
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
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import formatflow.api.ConverterChangeListener;
import formatflow.api.FileConversionService;

public class ConverterView extends ViewPart {

    private Text sourceFileText;
    private Text outputFileText;

    private Combo sourceFormatCombo;
    private Combo targetFormatCombo;
    
    private ConverterChangeListener converterChangeListener;
    private Button convertButton;

    private Label statusLabel;
    private ServiceTracker<FileConversionService, FileConversionService> serviceTracker;

    @Override
    public void createPartControl(Composite parent) {
    	Bundle bundle = FrameworkUtil.getBundle(ConverterView.class);

    	System.out.println("UI Bundle: " + bundle);

    	if (bundle == null) {
    	    throw new IllegalStateException(
    	            "ConverterView is not loaded from an OSGi bundle"
    	    );
    	}

    	System.out.println("UI Bundle state: " + bundle.getState());

    	BundleContext context = bundle.getBundleContext();

    	System.out.println("UI BundleContext: " + context);

    	if (context == null) {
    	    throw new IllegalStateException(
    	            "FormatFlow UI BundleContext is unavailable"
    	    );
    	}
    	
    	converterChangeListener = () -> {
    	    scheduleFormatsUpdate();
    	};


serviceTracker = new ServiceTracker<>(
        context,
        FileConversionService.class,
        new ServiceTrackerCustomizer<FileConversionService, FileConversionService>() {

            @Override
            public FileConversionService addingService(
                    ServiceReference<FileConversionService> reference) {

                FileConversionService service =
                        context.getService(reference);

                if (service != null) {
                    service.addConverterChangeListener(
                            converterChangeListener
                    );
                }

                scheduleFormatsUpdate();

                return service;
            }

            @Override
            public void modifiedService(
                    ServiceReference<FileConversionService> reference,
                    FileConversionService service) {

                scheduleFormatsUpdate();
            }

            @Override
            public void removedService(
                    ServiceReference<FileConversionService> reference,
                    FileConversionService service) {

                service.removeConverterChangeListener(
                        converterChangeListener
                );

                context.ungetService(reference);

                scheduleFormatsUpdate();
            }
        }
);

serviceTracker.open();

    	
 
    	
    	
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

        sourceFormatCombo.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );
        
        sourceFormatCombo.addListener(SWT.Selection, event -> {
            updateTargetFormats();
        });

        Composite targetFormatColumn =
                new Composite(formatsRow, SWT.NONE);
        targetFormatColumn.setLayout(new GridLayout(1, false));
        targetFormatColumn.setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false)
        );

        createSectionLabel(targetFormatColumn, "Target format");

        targetFormatCombo =
                new Combo(targetFormatColumn, SWT.DROP_DOWN | SWT.READ_ONLY);
     
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
        convertButton = new Button(container, SWT.PUSH);
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
        
        updateSourceFormats();
       

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
        
        
        	File target = new File(outputPath);
        	
        	try {
        		if(source.getCanonicalFile().equals(target.getCanonicalFile())) {
        			MessageDialog.openError(
        					getSite(). getShell(),
        					"Invalid output file",
        					"Source and output files must be different"
        					);
        			return;
        		}
        		
        	}catch (IOException e){
        		MessageDialog.openError(
        				getSite().getShell(),
        				"File error",
        				e.getMessage()
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
        	    statusLabel.setText("Conversion service is unavailable");

        	    MessageDialog.openError(
        	            getSite().getShell(),
        	            "Service unavailable",
        	            "File conversion service is not available."
        	    );
        	    return;
        	}

        	try {
        	    service.convert(
        	            source,
        	            target,
        	            sourceFormat,
        	            targetFormat
        	    );

        	    statusLabel.setText("Conversion completed successfully");

        	    MessageDialog.openInformation(
        	            getSite().getShell(),
        	            "Success",
        	            "File converted successfully!"
        	    );

        	} catch (IOException | IllegalArgumentException e) {
        	    statusLabel.setText("Conversion failed");

        	    MessageDialog.openError(
        	            getSite().getShell(),
        	            "Conversion error",
        	            e.getMessage() != null ? e.getMessage() : e.toString()
        	    );
        	}

        
    }


@Override
public void dispose() {

    if (serviceTracker != null) {
        serviceTracker.close();
        serviceTracker = null;
    }

    converterChangeListener = null;

    super.dispose();
}


    
    @Override
    public void setFocus() {
        if (sourceFileText != null
                && !sourceFileText.isDisposed()) {
            sourceFileText.setFocus();
        }
    }
    
    




private void updateSourceFormats() {

    String previousSource = sourceFormatCombo.getText();

    FileConversionService service = serviceTracker.getService();

    if (service == null) {
        sourceFormatCombo.removeAll();
        targetFormatCombo.removeAll();
        statusLabel.setText("Conversion service is unavailable");
        updateConvertButtonState();
        return;
    }

    String[] formats = service.getSourceFormats()
            .toArray(new String[0]);

    sourceFormatCombo.setItems(formats);

    if (formats.length == 0) {
        targetFormatCombo.removeAll();
        statusLabel.setText("No converters available");
        updateConvertButtonState();
        return;
    }

    int previousIndex = sourceFormatCombo.indexOf(previousSource);

    if (previousIndex >= 0) {
        sourceFormatCombo.select(previousIndex);
    } else {
        sourceFormatCombo.select(0);
    }

    updateTargetFormats();

    statusLabel.setText("Ready to convert");
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

    int previousIndex = targetFormatCombo.indexOf(previousTarget);

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

        if (statusLabel == null || statusLabel.isDisposed()) {
            return;
        }

        updateSourceFormats();
    });
}



}
