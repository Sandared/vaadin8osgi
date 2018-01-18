package org.osgi.enroute.example.vaadineight.integration.impl;

import java.util.Properties;

import org.osgi.enroute.example.vaadineight.integration.api.Application;
import org.osgi.framework.ServiceReference;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.ui.UI;



@SuppressWarnings("serial")
@VaadinServletConfiguration(productionMode = false, ui = UI.class)
public class VaadinOSGiServlet extends VaadinServlet {
	
	Application<?> application;
	ServiceReference<?> ref;

	public VaadinOSGiServlet(Application<?> application, ServiceReference<?> ref) {
		this.application = application;
		this.ref = ref;
	}

	@Override
	protected DeploymentConfiguration createDeploymentConfiguration(Properties initParameters) {
		initParameters.setProperty(SERVLET_PARAMETER_PRODUCTION_MODE, "false");
		initParameters.setProperty(SERVLET_PARAMETER_UI_PROVIDER, VaadinApplicationUIProvider.class.getName());
		
		return new DefaultDeploymentConfiguration(getClass(), initParameters);
	}

	class LocalVaadinServletService extends VaadinServletService {

		public LocalVaadinServletService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration)
				throws ServiceException {
			super(servlet, deploymentConfiguration);
		}

		Application<?> getApplication() {
			return application;
		}
	}

	@Override
	protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
			throws ServiceException {
		try {

			LocalVaadinServletService service = new LocalVaadinServletService(this, deploymentConfiguration);
			service.init();
			service.setClassLoader(new ClassLoader() {
				@Override
				public Class<?> loadClass(String name) throws ClassNotFoundException {
					// The Vaadin BootstrapHandler tries to get a WidgetsetInfo in its setupMainDiv method, 
					// which calls the getWidgetsetInfo method of UIProvider,
					// which in turn tries to load the class "AppWidgetset". 
					// According to the comments in UIProvider.findWidgetsetClass(), a ClassNotFoundException is a normal case, 
					// so I will just throw this Exception in my custom ClassLoader whenever it is asked for this class.
					if("AppWidgetset".equals(name))
						throw new ClassNotFoundException("This is an allowed return value for AppWidgetset");
					try {
						try {
							Class<?> loadClass = VaadinOSGiServlet.class.getClassLoader().loadClass(name);
							return loadClass;
						} catch (ClassNotFoundException e) {
							return ref.getBundle().loadClass(name);
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw e;
					}
				}
			});

			return service;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

}
