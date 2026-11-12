
package formatflow.converter.jsonxml;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import formatflow.api.FileConverter;

public class JsonXmlActivator implements BundleActivator {

    private ServiceRegistration<FileConverter> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        registration = context.registerService(
                FileConverter.class,
                new JsonXmlConverter(),
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
