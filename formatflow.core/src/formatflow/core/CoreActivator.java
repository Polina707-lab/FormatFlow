package formatflow.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import formatflow.api.FileConverter;

/*
 * Создаёт ServiceTracker, указывая, какие сервисы нужно отслеживать.
	Определяет обработчики событий через интерфейс ServiceTrackerCustomizer.
	Управляет жизненным циклом трекера через open() и close().*/

public class CoreActivator implements BundleActivator {

    private ServiceTracker<FileConverter, FileConverter> tracker;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("FormatFlow Core started");

        tracker = new ServiceTracker<>(
                context,
                FileConverter.class,
                new ServiceTrackerCustomizer<FileConverter, FileConverter>() {

                    @Override
                    public FileConverter addingService(
                            ServiceReference<FileConverter> reference) {

                        FileConverter converter = context.getService(reference);

                        if (converter != null) {
                            System.out.println("Converter added: "
                                    + converter.getSourceFormat()
                                    + " -> "
                                    + converter.getTargetFormat());
                        }

                        return converter;
                    }

                    @Override
                    public void modifiedService(
                            ServiceReference<FileConverter> reference,
                            FileConverter converter) {

                        System.out.println("Converter service modified");
                    }

                    @Override
                    public void removedService(
                            ServiceReference<FileConverter> reference,
                            FileConverter converter) {

                        System.out.println("Converter removed: "
                                + converter.getSourceFormat()
                                + " -> "
                                + converter.getTargetFormat());

                        context.ungetService(reference);
                    }
                }
        );

        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (tracker != null) {
            tracker.close();
            tracker = null;
        }

        System.out.println("FormatFlow Core stopped");
    }
}