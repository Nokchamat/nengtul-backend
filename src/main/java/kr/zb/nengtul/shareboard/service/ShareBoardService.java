package kr.zb.nengtul.shareboard.service;

import static kr.zb.nengtul.global.exception.ErrorCode.NOT_FOUND_SHARE_BOARD;
import static kr.zb.nengtul.global.exception.ErrorCode.NOT_VERIFY_EMAIL;
import static kr.zb.nengtul.global.exception.ErrorCode.NO_PERMISSION;

import java.security.Principal;
import java.util.List;
import kr.zb.nengtul.global.exception.CustomException;
import kr.zb.nengtul.shareboard.domain.dto.ShareBoardDto;
import kr.zb.nengtul.shareboard.domain.entity.ShareBoard;
import kr.zb.nengtul.shareboard.domain.repository.ShareBoardRepository;
import kr.zb.nengtul.user.domain.entity.User;
import kr.zb.nengtul.user.domain.repository.UserRepository;
import kr.zb.nengtul.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import s3bucket.service.AmazonS3Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareBoardService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final ShareBoardRepository shareBoardRepository;
  private final AmazonS3Service amazonS3Service;

  @Transactional
  public void createShareBoard(ShareBoardDto shareBoardDto, Principal principal,
      MultipartFile image) {
    User user = userService.findUserByEmail(principal.getName());
    if (!user.isEmailVerifiedYn()) {
      throw new CustomException(NOT_VERIFY_EMAIL);
    }

    ShareBoard shareBoard = ShareBoard.builder()
        .user(user)
        .title(shareBoardDto.getTitle())
        .price(shareBoardDto.getPrice())
        .content(shareBoardDto.getContent())
        .place(shareBoardDto.getPlace())
        .lat(shareBoardDto.getLat())
        .lon(shareBoardDto.getLon())
        .closed(false)
        .build();
    shareBoardRepository.save(shareBoard);
    shareBoard.setShareImg(amazonS3Service.uploadFileForShareBoard(image, shareBoard.getId()));
    user.setPointAddShardBoard(user.getPoint());
    userRepository.saveAndFlush(user);
  }

  @Transactional
  public void updateShareBoard(Long id, ShareBoardDto shareBoardDto, Principal principal,
      MultipartFile image) {
    ShareBoard shareBoard = shareBoardRepository.findById(id)
        .orElseThrow(() -> new CustomException(NOT_FOUND_SHARE_BOARD));
    User user = userService.findUserByEmail(principal.getName());
    if (!shareBoard.getUser().equals(user)){
      throw new CustomException(NO_PERMISSION);
    }
    if (image != null) {
      if (user.getProfileImageUrl() != null) {
        // 이미지가 있을 경우 이미지 업데이트
        amazonS3Service.updateFile(image, shareBoard.getShareImg());
      } else {
        // 이미지가 없을 경우 새 이미지 업로드
        user.setProfileImageUrl(amazonS3Service.uploadFileForShareBoard(image, shareBoard.getId()));
      }
    }
    shareBoard.setTitle(shareBoardDto.getTitle());
    shareBoard.setContent(shareBoardDto.getContent());
    shareBoard.setPlace(shareBoardDto.getPlace());
    shareBoard.setPrice(shareBoardDto.getPrice());
    shareBoard.setLat(shareBoardDto.getLat());
    shareBoard.setLon(shareBoardDto.getLon());
    shareBoardRepository.save(shareBoard);
  }

  @Transactional
  public void deleteShareBoard(Long id, Principal principal) {
    ShareBoard shareBoard = shareBoardRepository.findById(id)
        .orElseThrow(() -> new CustomException(NOT_FOUND_SHARE_BOARD));
    User user = userService.findUserByEmail(principal.getName());
    if (shareBoard.getUser() != user) {
      throw new CustomException(NO_PERMISSION);
    }
    shareBoardRepository.delete(shareBoard);
  }

  @Transactional
  public List<ShareBoard> getShareBoardList(double lat, double lon, double range, Boolean closed) {
    List<ShareBoard> shareBoardList;
    if (closed == null) {
      shareBoardList = shareBoardRepository.findByLatBetweenAndLonBetween(
          lat - range, lat + range, lon - range, lon + range);

    } else {
      shareBoardList = shareBoardRepository.findByLatBetweenAndLonBetweenAndClosed(
          lat - range, lat + range, lon - range, lon + range, closed);

    }
    return shareBoardList;
  }

  public List<ShareBoard> getMyShareBoard(Principal principal) {
    User user = userService.findUserByEmail(principal.getName());
    return shareBoardRepository.findAllByUser(user);
  }
}
