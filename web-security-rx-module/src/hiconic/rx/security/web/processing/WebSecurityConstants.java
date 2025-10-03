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
package hiconic.rx.security.web.processing;

public interface WebSecurityConstants {

	public static String REQUEST_PARAM_USER = "user";
	public static String REQUEST_PARAM_PASSWORD = "password";
	public static String REQUEST_PARAM_CONTINUE = "continue";
	public static String REQUEST_PARAM_SESSIONID = "sessionId";
	public static String REQUEST_PARAM_SESSIONUSERICONURL = "sessionUserIconUrl";
	public static String REQUEST_PARAM_MESSAGE = "message";
	public static String REQUEST_PARAM_MESSAGESTATUS = "messageStatus";
	public static String REQUEST_PARAM_STAYSIGNED = "staySigned";

	public static String REQUEST_VALUE_MESSAGESTATUS_OK = "OK";
	public static String REQUEST_VALUE_MESSAGESTATUS_ERROR = "ERROR";

	public static String COOKIE_PREFIX = "tf";
	public static String COOKIE_SESSIONID = COOKIE_PREFIX + REQUEST_PARAM_SESSIONID;
	public static String COOKIE_USER = COOKIE_PREFIX + REQUEST_PARAM_USER;

	public static String HEADER_PARAM_PREFIX = "gm";
	public static String HEADER_PARAM_SESSIONID = HEADER_PARAM_PREFIX + "-session-id";

	public final static String TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED = "TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED";

}
