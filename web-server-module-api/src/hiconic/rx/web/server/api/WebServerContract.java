package hiconic.rx.web.server.api;

import hiconic.rx.module.api.wire.RxExportContract;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;

public interface WebServerContract extends RxExportContract {
	void addServlet(String name, String path, HttpServlet servlet);
	void addFilter(String name, Filter filter);
	String callerInfoFilterName();
	void addFilterServletNameMapping(String filterName, String mapping, DispatcherType dispatcherType);
	void addFilterMapping(String filterName, String mapping, DispatcherType dispatcherType);
}
