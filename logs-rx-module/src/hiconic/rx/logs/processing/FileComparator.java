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
import java.util.Comparator;
import java.util.Date;

public class FileComparator implements Comparator<File> {
	@Override
	public int compare(File file1, File file2) {
		int res = new Date(file2.lastModified()).compareTo(new Date(file1.lastModified()));
		if (res == 0) {
			return file1.compareTo(file2);
		}

		return res;
	}
}
