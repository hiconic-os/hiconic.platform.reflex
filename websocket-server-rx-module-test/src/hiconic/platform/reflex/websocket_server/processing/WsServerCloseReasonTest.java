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
package hiconic.platform.reflex.websocket_server.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import jakarta.websocket.CloseReason.CloseCodes;

public class WsServerCloseReasonTest {

	@Test
	public void preservesShortReasons() {
		assertThat(WsServer.closeReason(CloseCodes.CANNOT_ACCEPT, "invalid session").getReasonPhrase()).isEqualTo("invalid session");
	}

	@Test
	public void truncatesLongReasonsAtUtf8Boundary() {
		String reason = "Session not found: " + "ä".repeat(100);
		String truncated = WsServer.closeReason(CloseCodes.CANNOT_ACCEPT, reason).getReasonPhrase();

		assertThat(truncated.getBytes(StandardCharsets.UTF_8)).hasSizeLessThanOrEqualTo(WsServer.MAX_CLOSE_REASON_BYTES);
		assertThat(reason).startsWith(truncated);
		assertThat(truncated).doesNotContain("�");
	}
}
