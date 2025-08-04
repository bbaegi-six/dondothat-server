package org.bbagisix.chat.dto;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
public class UserChallengeInfoDTO {

	private Long userChallengeId;
	private Long userId;
	private Long challengeId;
	private String challengeName;
	private String status;        // ongoing, completed, failed
	private LocalDateTime startDate;    // 메시지 이력 조회 시작점
	private LocalDateTime endDate;        // 챌린지 종료일 (참고용)

	/**
	 * 채팅방 접근 가능한지 확인
	 * ongoing 상태일 때만 채팅방 접근 가능
	 */
	public boolean canAccessChatRoom() {
		return "ongoing".equals(status);
	}

	/**
	 * 챌린지가 종료되었는지 확인
	 * completed 또는 failed 상태일 때 종료
	 */
	public boolean isFinished() {
		return "completed".equals(status) || "failed".equals(status);
	}
}
