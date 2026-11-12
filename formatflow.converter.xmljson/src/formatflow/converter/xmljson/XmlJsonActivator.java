
package formatflow.converter.xmljson;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import formatflow.api.FileConverter;

public class XmlJsonActivator implements BundleActivator {

    private ServiceRegistration<FileConverter> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        registration = context.registerService(
                FileConverter.class,
                new XmlJsonConverter(),
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
