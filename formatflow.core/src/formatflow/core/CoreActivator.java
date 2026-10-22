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

 private ConverterRegistry registry;
 

    @Override
    public void start(BundleContext context) throws Exception {
    	
        System.out.println("FormatFlow Core started");

        registry = new ConverterRegistry(context);
        registry.start();
        
        System.out.println("Available converters:");

        registry.getConverters().forEach(converter  ->
        		System.out.println(
        				converter.getSourceFormat() + " -> " + converter.getTargetFormat()
        				)
        );
       
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    			if(registry != null) {
    				registry.stop();
    				registry = null;
  
    				
        }

        System.out.println("FormatFlow Core stopped");
    }
}