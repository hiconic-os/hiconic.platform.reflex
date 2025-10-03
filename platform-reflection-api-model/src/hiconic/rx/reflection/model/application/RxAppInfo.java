// ============================================================================
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
package hiconic.rx.reflection.model.application;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.reflection.model.api.PlatformReflectionResponse;
import hiconic.rx.reflection.model.module.RxModuleInfo;
import hiconic.rx.reflection.model.module.RxTerminalInfo;
import hiconic.rx.reflection.model.streampipes.StreamPipesInfo;
import hiconic.rx.reflection.model.system.disk.FolderInfo;

public interface RxAppInfo extends PlatformReflectionResponse {

	EntityType<RxAppInfo> T = EntityTypes.T(RxAppInfo.class);

	String getApplicationName();
	void setApplicationName(String applicationName);

	String getApplicationId();
	void setApplicationId(String applicationId);

	RxTerminalInfo getTerminalInfo();
	void setTerminalInfo(RxTerminalInfo terminalInfo);

	List<RxModuleInfo> getModuleInfos();
	void setModuleInfos(List<RxModuleInfo> moduleInfos);

	StreamPipesInfo getStreamPipeInfo();
	void setStreamPipeInfo(StreamPipesInfo streamPipesInfo);

	FolderInfo getTempDirInfo();
	void setTempDirInfo(FolderInfo folderInfo);

}
