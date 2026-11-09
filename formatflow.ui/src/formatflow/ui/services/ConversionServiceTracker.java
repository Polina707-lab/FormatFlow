
package formatflow.ui.services;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import formatflow.api.ConverterChangeListener;
import formatflow.api.FileConversionService;

public class ConversionServiceTracker {

    private final BundleContext context;
    private final Runnable onChange;
    private final ConverterChangeListener changeListener;

    private ServiceTracker<FileConversionService, FileConversionService> tracker;

    public ConversionServiceTracker(
            BundleContext context,
            Runnable onChange
    ) {
        this.context = context;
        this.onChange = onChange;
        this.changeListener = () -> this.onChange.run();
    }

    public void open() {
        if (tracker != null) {
            return;
        }

        tracker = new ServiceTracker<>(
                context,
                FileConversionService.class,
                new ServiceTrackerCustomizer<FileConversionService, FileConversionService>() {

                    @Override
                    public FileConversionService addingService(
                            ServiceReference<FileConversionService> reference
                    ) {
                        FileConversionService service =
                                context.getService(reference);

                        if (service != null) {
                            service.addConverterChangeListener(changeListener);
                        }

                        onChange.run();
                        return service;
                    }

                    @Override
                    public void modifiedService(
                            ServiceReference<FileConversionService> reference,
                            FileConversionService service
                    ) {
                    	onChange.run();
                    }

                    @Override
                    public void removedService(
                            ServiceReference<FileConversionService> reference,
                            FileConversionService service
                    ) {
                        if (service != null) {
                            service.removeConverterChangeListener(changeListener);
                        }

                        context.ungetService(reference);
                        onChange.run();
                    }
                }
        );

        tracker.open();
    }

    public FileConversionService getService() {
        return tracker != null ? tracker.getService() : null;
    }

    public void close() {
        if (tracker != null) {
            tracker.close();
            tracker = null;
        }
    }
}
