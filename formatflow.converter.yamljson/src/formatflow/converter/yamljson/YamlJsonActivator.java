
package formatflow.converter.yamljson;

import formatflow.api.FileConverter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class YamlJsonActivator implements BundleActivator {

    private ServiceRegistration<FileConverter> registration;

    @Override
    public void start(BundleContext context) throws Exception {

        FileConverter converter = new YamlJsonConverter();

        registration = context.registerService(
                FileConverter.class,
                converter,
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
