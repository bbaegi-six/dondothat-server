package org.bbagisix.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.bbagisix.chat.dto.ChatMessageDTO;
import org.bbagisix.chat.mapper.ChatMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
public class ChatServiceTest {

	@Mock
	private ChatMapper chatMapper;

	@InjectMocks
	private ChatService chatService;

	private ChatMessageDTO testMessage;
	private ChatMessageDTO savedMessage;

	@BeforeEach
	void setUp() {
		testMessage = ChatMessageDTO.builder()
			.challengeId(1L)
			.userId(100L)
			.message("안녕하세요!")
			.messageType("MESSAGE")
			.build();

		savedMessage = ChatMessageDTO.builder()
			.messageId(1L)
			.challengeId(1L)
			.userId(100L)
			.message("안녕하세요!")
			.messageType("MESSAGE")
			.sentAt(Timestamp.valueOf(LocalDateTime.now()))
			.userName("테스트유저1")
			.build();
	}

	@Test
	@DisplayName("채팅 메시지 저장 - 성공")
	void 메세지_저장_테스트() {
		// given
		given(chatMapper.insertMessage(any())).willReturn(1);
		given(chatMapper.selectMessageById(any())).willReturn(savedMessage);

		// when
		ChatMessageDTO result = chatService.saveMessage(testMessage);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getMessageId()).isEqualTo(1L);
		assertThat(result.getMessage()).isEqualTo("안녕하세요!");
		assertThat(result.getUserName()).isEqualTo("테스트유저1");
	}

	@Test
	@DisplayName("메시지 타입이 null인 경우 기본값 설정")
	void savedMessage_DefaultMessageType() {
		// given
		testMessage.setMessageType(null);
		given(chatMapper.insertMessage(any(ChatMessageDTO.class))).willReturn(1);
		given(chatMapper.selectMessageById(any())).willReturn(savedMessage);

		// when
		chatService.saveMessage(testMessage);

		// then
		verify(chatMapper).insertMessage(argThat(msg ->
			"MESSAGE".equals(msg.getMessageType())));
	}

}
