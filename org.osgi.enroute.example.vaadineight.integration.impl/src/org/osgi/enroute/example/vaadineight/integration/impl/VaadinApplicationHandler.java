package org.osgi.enroute.example.vaadineight.integration.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.enroute.example.vaadineight.integration.api.Application;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import com.vaadin.server.Constants;


@Component(name = "osgi.enroute.examples.vaadin.provider")
public class VaadinApplicationHandler {
	
	final ConcurrentHashMap<String, ServiceRegistration<Servlet>> servlets = new ConcurrentHashMap<>();
	private HttpService http;

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addZApplication(Application<?> a, Map<String, Object> map, ServiceReference<?> ref) throws ServletException, NamespaceException {
		try {
			BundleContext context = ref.getBundle().getBundleContext();
			Dictionary<String, Object> servletProps = new Hashtable<>();
			// must be present
			String urlPattern = getVaadinUrlPattern((String) map.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN));
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, urlPattern);
			// hardcoded, but the official Vaadin solution is even uglier
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX + Constants.PARAMETER_VAADIN_RESOURCES, "/vaadin-8.2.1");
			
			Object asyncSupported = map.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);
			if(asyncSupported != null)
				servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, (Boolean) asyncSupported);
			
			ServiceRegistration<Servlet> app = context.registerService(Servlet.class, new VaadinOSGiServlet(a, ref), servletProps);
			servlets.put(urlPattern, app);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void removeZApplication(Application<?> a, Map<String, Object> map) {
		String urlPattern = (String) map.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
		ServiceRegistration<Servlet> app = servlets.remove(urlPattern);
		if (app != null) {
			try {
                app.unregister();
            } catch (IllegalStateException ise) {
                // This service may have already been unregistered
                // automatically by the OSGi framework if the
                // application bundle is being stopped. This is
                // obviously not a problem for us.
            }
		}
	}
	
	// Vaadin sends xhr requests for anything to .../yourPath/UIDL/?...
	// so if the user defined a path like .../test this will become .../test/* to answer those requests
	private String getVaadinUrlPattern(String urlPattern) {
		if(urlPattern.endsWith("/"))
			return urlPattern + "*";
		if(urlPattern.endsWith("/*"))
			return urlPattern;
		if(!urlPattern.endsWith("/") && !urlPattern.endsWith("/*"))
			return urlPattern + "/*";
		throw new IllegalArgumentException();
	}
}
