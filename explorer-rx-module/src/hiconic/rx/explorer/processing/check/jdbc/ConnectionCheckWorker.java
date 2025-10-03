// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.explorer.processing.check.jdbc;

import java.sql.Connection;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import com.braintribe.logging.Logger;

public class ConnectionCheckWorker implements Callable<ConnectionCheckResult> {

	private static Logger logger = Logger.getLogger(ConnectionCheckWorker.class);
	
	private final DataSource dataSource;
	private final String name;

	public ConnectionCheckWorker(DataSource dataSource, String name) {
		this.dataSource = dataSource;
		this.name = name;
	}
	
	@Override
	public ConnectionCheckResult call() throws Exception {
		
		String errorMessage = null;
		boolean valid = false;
		long start = System.currentTimeMillis();
		try (Connection connection = dataSource.getConnection()) {
			valid = connection.isValid(15);
		} catch(Exception e) {
			valid = false;
			errorMessage = ExceptionUtil.getLastMessage(e);
			logger.error("Error while trying to verify validity of connection "+name, e);
		}
		long elapsedTime = System.currentTimeMillis()-start;
		
		return new ConnectionCheckResult(valid, elapsedTime, errorMessage);
	}

	
	

}
