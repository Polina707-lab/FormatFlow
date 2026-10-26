package formatflow.core;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import formatflow.api.FileConversionService;

public class CoreActivator implements BundleActivator {

    private ConverterRegistry registry;
    private ConversionService conversionService;
    private ServiceRegistration<FileConversionService> serviceRegistration;
    

    @Override
    public void start(BundleContext context) throws Exception {

        System.out.println("FormatFlow Core started");

        registry = new ConverterRegistry(context);
        registry.start();

        conversionService = new ConversionService(registry);
        serviceRegistration = context.registerService(
        			FileConversionService.class,
        			conversionService,
        			null
        		);
        		

        System.out.println("Available converters:");

        registry.getConverters().forEach(converter ->
                System.out.println(
                        converter.getSourceFormat()
                                + " -> "
                                + converter.getTargetFormat()
                )
        );
        
        
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }

        conversionService = null;

        if (registry != null) {
            registry.stop();
            registry = null;
        }

        System.out.println("FormatFlow Core stopped");
    }
}