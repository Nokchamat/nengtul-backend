package kr.zb.nengtul.user.service;

import static kr.zb.nengtul.global.exception.ErrorCode.ALREADY_EXIST_EMAIL;
import static kr.zb.nengtul.global.exception.ErrorCode.ALREADY_VERIFIED;
import static kr.zb.nengtul.global.exception.ErrorCode.EXPIRED_CODE;
import static kr.zb.nengtul.global.exception.ErrorCode.NOT_FOUND_USER;
import static kr.zb.nengtul.global.exception.ErrorCode.SHORT_PASSWORD;
import static kr.zb.nengtul.global.exception.ErrorCode.WRONG_VERIFY_CODE;

import java.security.Principal;
import java.time.LocalDateTime;
import kr.zb.nengtul.global.exception.CustomException;
import kr.zb.nengtul.user.entity.domain.User;
import kr.zb.nengtul.user.entity.dto.UserDetailDto;
import kr.zb.nengtul.user.entity.dto.UserFindEmailReqDto;
import kr.zb.nengtul.user.entity.dto.UserFindEmailResDto;
import kr.zb.nengtul.user.entity.dto.UserFindPasswordDto;
import kr.zb.nengtul.user.entity.dto.UserJoinDto;
import kr.zb.nengtul.user.entity.dto.UserUpdateDto;
import kr.zb.nengtul.user.entity.repository.UserRepository;
import kr.zb.nengtul.user.mailgun.client.MailgunClient;
import kr.zb.nengtul.user.mailgun.client.mailgun.SendMailForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final MailgunClient mailgunClient;

  //회원가입 및 이메일 인증 발송
  @Transactional
  public void join(UserJoinDto userJoinDto) {
    // 이메일 중복 확인
    if (userRepository.existsByEmail(userJoinDto.getEmail())) {
      throw new CustomException(ALREADY_EXIST_EMAIL);
    } else if (userJoinDto.getPassword() == null || userJoinDto.getPassword().length() < 8) {
      throw new CustomException(SHORT_PASSWORD);
    }

    User user = User.builder()
        .name(userJoinDto.getName())
        .nickname(userJoinDto.getNickname())
        .password(passwordEncoder.encode(userJoinDto.getPassword()))
        .phoneNumber(userJoinDto.getPhoneNumber())
        .email(userJoinDto.getEmail())
        .address(userJoinDto.getAddress())
        .addressDetail(userJoinDto.getAddressDetail())
        .profileImageUrl(null)
        .build();
    userRepository.save(user);

    verifyEmailForm(user, userJoinDto.getEmail(), userJoinDto.getName());
  }

  //이메일 인증
  @Transactional
  public void verify(String email, String code) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
    if (user.isEmailVerifiedYn()) {
      throw new CustomException(ALREADY_VERIFIED);
    } else if (!user.getVerificationCode().equals(code)) {
      throw new CustomException(WRONG_VERIFY_CODE);
    } else if (user.getVerifyExpiredAt().isBefore(LocalDateTime.now())) {
      throw new CustomException(EXPIRED_CODE);
    }
    user.setEmailVerifiedYn(true);
  }

  //유저 상세보기
  public UserDetailDto getUserDetail(Principal principal) {
    User user = userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    return UserDetailDto.buildUserDetailDto(user);
  }

  //회원 탈퇴
  @Transactional
  public void quit(Principal principal) {
    System.out.println(principal.getName());
    User user = userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
    userRepository.deleteById(user.getId());
  }

  //회원 정보 수정
  @Transactional
  public String update(Principal principal, UserUpdateDto userUpdateDto) {
    User user = userRepository.findByEmail(principal.getName())
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    if (userUpdateDto.getPassword() == null || userUpdateDto.getPassword().length() < 8) {
      throw new CustomException(SHORT_PASSWORD);
    }

    String updateProfileImageUrl =
        userUpdateDto.getProfileImageUrl() == null ? "" : userUpdateDto.getProfileImageUrl();

    user.setNickname(userUpdateDto.getNickname());
    user.setPassword(passwordEncoder.encode(userUpdateDto.getPassword()));
    user.setPhoneNumber(userUpdateDto.getPhoneNumber());
    user.setAddress(userUpdateDto.getAddress());
    user.setAddressDetail(userUpdateDto.getAddressDetail());
    user.setProfileImageUrl(updateProfileImageUrl);

    userRepository.save(user);
    return user.getEmail();
  }

  //가입한 이메일 찾기(아이디 찾기)
  public UserFindEmailResDto findEmail(UserFindEmailReqDto userFindEmailReqDto) {
    User user = userRepository.findByName(userFindEmailReqDto.getName())
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
    if (!user.getPhoneNumber().equals(userFindEmailReqDto.getPhoneNumber())) {
      throw new CustomException(NOT_FOUND_USER);
    }
    return UserFindEmailResDto.buildUserFindEmailResDto(user.getEmail());
  }

  //임시 비밀번호 발급(비밀번호 찾기)
  @Transactional
  public void getNewPassword(UserFindPasswordDto userFindPasswordDto) {
    //이메일 전송 (이메일, 이름, 휴대폰 번호 부여받음)
    User user = userRepository.findByEmailAndNameAndPhoneNumber(userFindPasswordDto.getEmail(),
            userFindPasswordDto.getName(), userFindPasswordDto.getPhoneNumber())
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    String code = RandomStringUtils.random(10, true, true);

    //임시비밀번호 발급 및 이메일 전송 + db에 임시 비밀번호로 저장
    SendMailForm sendMailForm = SendMailForm.builder()
        .from("lvet0330@gmail.com")
        .to(user.getEmail())
        .subject("냉털 임시 비밀번호 발급 이메일")
        .text(getPasswordEmailBody(user.getName(), code))
        .build();

    mailgunClient.sendEmail(sendMailForm);
    user.setPassword(passwordEncoder.encode(code));
    userRepository.save(user);
  }

  //인증코드 및 시간 설정
  @Transactional
  public void changeCustomerValidateEmail(Long customerId, String verificationCode) {
    User user = userRepository.findById(customerId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    user.setVerifyExpiredAt(LocalDateTime.now().plusDays(1));
    user.setVerificationCode(verificationCode);
    userRepository.save(user);
  }

  //인증코드 및 시간 재설정
  public void resetVerify(Long userId) {
    //유저 정보페이지에서 가져오기때문에 바로 get
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
    verifyEmailForm(user, user.getEmail(), user.getName());
  }

  private void verifyEmailForm(User user, String email, String name) {
    String code = RandomStringUtils.random(10, true, true);

    SendMailForm sendMailForm = SendMailForm.builder()
        .from("lvet0330@gmail.com")
        .to(email)
        .subject("냉털 계정 인증용 이메일")
        .text(getVerificationEmailBody(email, name, code))
        .build();

    mailgunClient.sendEmail(sendMailForm);
    changeCustomerValidateEmail(user.getId(), code);
  }

  //인증용 이메일
  private String getVerificationEmailBody(String email, String name, String code) {
    //TODO : HTML email 폼 적용 예정
    return "안녕하세요 " + name + "! 링크를 통해 이메일 인증을 진행해주세요. \n\n"
        //.append("http://localhost:8080/v1/nengtul/user/verify?email=") //로컬
        + "http://43.200.162.72:8080/v1/nengtul/user/verify?email=" //배포
        + email
        + "&code="
        + code;
  }

  //비밀번호 찾기용 Email
  private String getPasswordEmailBody(String name, String code) {
    //TODO : HTML email 폼 적용 예정
    return "안녕하세요 " + name
        + " 님! 이메일에 작성된 임시 비밀번호를 통해 로그인해주세요. \n\n"
        + "임시 비밀번호를 통해 로그인 후 비밀번호를 꼭 변경해주세요. \n\n"
        + "임시 비밀번호 : "
        + code;
  }
}
