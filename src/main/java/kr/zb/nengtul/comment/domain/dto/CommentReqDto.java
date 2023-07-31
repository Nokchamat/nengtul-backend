package kr.zb.nengtul.comment.domain.dto;

import static kr.zb.nengtul.global.exception.ErrorCode.CONTENT_NOT_NULL_MESSAGE;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReqDto {
  @NotEmpty(message = CONTENT_NOT_NULL_MESSAGE)
  private String content;
}
