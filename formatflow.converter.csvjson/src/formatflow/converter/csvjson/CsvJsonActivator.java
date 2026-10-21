package formatflow.converter.csvjson;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import formatflow.api.FileConverter;
import org.osgi.framework.ServiceRegistration;

public class CsvJsonActivator implements BundleActivator{
private ServiceRegistration<FileConverter> registration;
	@Override
	public void start(BundleContext context) throws Exception {

		//добавим регистрацию нашего сервиса
		registration  = context.registerService(
				FileConverter.class,
				new CsvJsonConverter(),
				null
				);

		System.out.println("CSV to JSON converter registered");

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if(registration != null) {
			registration.unregister();
			registration = null;
		}
				System.out.println("Csv to Json converter stopped");


	}

}
