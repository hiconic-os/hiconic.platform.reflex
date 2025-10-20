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
package hiconic.rx.logs.processing;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;

public class DateFileFilter implements FileFilter {
	private String pattern;
	private Date from;
	private Date to;

	public DateFileFilter(Date from, Date to, String pattern) {
		super();

		this.pattern = pattern;
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean accept(File checkFile) {
		if (checkFile.isFile() == false) {
			return false;
		}
		if (pattern != null && checkFile.getName().matches(pattern) == false) {
			return false;
		}

		if (from != null || to != null) {
			Date date = new Date(checkFile.lastModified());

			if (from != null && from.compareTo(date) > 0) {
				return false;
			}
			if (to != null && to.compareTo(date) < 0) {
				return false;
			}
		}

		return true;
	}
}
