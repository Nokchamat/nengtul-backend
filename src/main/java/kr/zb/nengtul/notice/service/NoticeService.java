package kr.zb.nengtul.notice.service;

import static kr.zb.nengtul.global.exception.ErrorCode.NOT_FOUND_NOTICE;
import static kr.zb.nengtul.global.exception.ErrorCode.NOT_FOUND_USER;
import static kr.zb.nengtul.global.exception.ErrorCode.NO_PERMISSION;

import java.security.Principal;
import kr.zb.nengtul.global.exception.CustomException;
import kr.zb.nengtul.notice.domain.entity.Notice;
import kr.zb.nengtul.notice.domain.dto.NoticeDetailDto;
import kr.zb.nengtul.notice.domain.dto.NoticeListDto;
import kr.zb.nengtul.notice.domain.dto.NoticeReqDto;
import kr.zb.nengtul.notice.domain.repository.NoticeRepository;
import kr.zb.nengtul.user.domain.entity.User;
import kr.zb.nengtul.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

  private final UserRepository userRepository;
  private final NoticeRepository noticeRepository;

  @Transactional
  public void create(NoticeReqDto noticeReqDto, Principal principal) {
    User user = findUserByEmail(principal.getName());
    Notice notice = Notice.builder()
        .title(noticeReqDto.getTitle())
        .content(noticeReqDto.getContent())
        .noticeImg(noticeReqDto.getNoticeImg())
        .user(user)
        .viewCount(0L)
        .build();
    noticeRepository.save(notice);
  }

  @Transactional
  public void update(Long noticeId, NoticeReqDto noticeReqDto, Principal principal) {
    User user = findUserByEmail(principal.getName());
    Notice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_NOTICE));

    if (notice.getUser().equals(user)) {
      notice.setTitle(noticeReqDto.getTitle());
      notice.setContent(noticeReqDto.getContent());
      notice.setNoticeImg(noticeReqDto.getNoticeImg());

      noticeRepository.save(notice);
    } else {
      throw new CustomException(NO_PERMISSION);
    }
  }


  @Transactional
  public void delete(Long noticeId, Principal principal) {
    User user = findUserByEmail(principal.getName());
    Notice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_NOTICE));
    if (notice.getUser().equals(user)) {
      noticeRepository.deleteById(noticeId);
    } else {
      throw new CustomException(NO_PERMISSION);
    }
  }

  public Page<NoticeListDto> getList(Pageable pageable) {
    Page<Notice> noticeList = noticeRepository.findAll(pageable);
    return noticeList.map(NoticeListDto::buildNoticeListDto);
  }

  @Transactional
  public NoticeDetailDto getDetails(Long noticeId) {
    Notice notice = noticeRepository.findById(noticeId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_NOTICE));
    notice.setViewCount(notice.getViewCount() + 1);
    noticeRepository.saveAndFlush(notice);

    return NoticeDetailDto.buildNoticeDetailDto(notice);
  }

  public User findUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
  }
}