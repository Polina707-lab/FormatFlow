package formatflow.converter.jsoncsv;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import formatflow.api.FileConverter;

public class JsonCsvActivator implements BundleActivator {

    private ServiceRegistration<FileConverter> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        registration = context.registerService(
                FileConverter.class,
                new JsonCsvConverter(),
                null
        );
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}