package formatflow.core;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.ArrayList;
import java.util.List;


import formatflow.api.FileConverter;

public class ConverterRegistry {
	
	private final BundleContext context;
	private ServiceTracker<FileConverter, FileConverter> tracker;
	
	public ConverterRegistry(BundleContext context) {
		//инъекция зависимостей через конструктор
		this.context = context;
	}
	
    public void start() throws Exception {

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
    
    public List<FileConverter>  getConverters(){
    	if(tracker == null) {
    		return List.of();
    	}
    	//возвращает снимок отслеживаемых сервисов в виде SortedMap, где ключи — ServiceReference, 
    	// а значения — объекты FileConverter.
    	//тут получаем список доступных сервисов на момент вызова
    	return new ArrayList<>(tracker.getTracked().values());
    	
    
    }

 
    public void stop() throws Exception {
        if (tracker != null) {
            tracker.close();
            tracker = null;
        }
    }
	

}
