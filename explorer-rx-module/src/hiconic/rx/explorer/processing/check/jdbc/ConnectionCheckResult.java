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

public class ConnectionCheckResult {

	private boolean connectionValid = false;
	private String details;
	private long elapsedTime = -1;
	
	
	public ConnectionCheckResult(boolean connectionValid, long elpasedTime) {
		this.connectionValid = connectionValid;
		this.elapsedTime = elpasedTime;
	}

	public ConnectionCheckResult(boolean connectionValid, long elapsedTime, String details) {
		this(connectionValid, elapsedTime);
		this.details = details;
	}

	
	public long getElapsedTime() {
		return elapsedTime;
	}
	
	public boolean isConnectionValid() {
		return connectionValid;
	}
	
	public String getDetails() {
		return details;
	}
	
	
}
