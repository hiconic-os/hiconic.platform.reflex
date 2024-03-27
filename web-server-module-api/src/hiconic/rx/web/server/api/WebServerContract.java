package hiconic.rx.web.server.api;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;

import hiconic.rx.module.api.wire.RxExportContract;

public interface WebServerContract extends RxExportContract {
	void addServlet(String name, String path, HttpServlet servlet);
	void addFilter(String name, Filter filter);
	String callerInfoFilterName();
	void addFilterServletNameMapping(String filterName, String mapping, DispatcherType dispatcherType);
	void addFilterMapping(String filterName, String mapping, DispatcherType dispatcherType);
}
