package kr.zb.nengtul.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import kr.zb.nengtul.global.exception.CustomException;
import kr.zb.nengtul.user.domain.dto.UserFindEmailReqDto;
import kr.zb.nengtul.user.domain.dto.UserJoinDto;
import kr.zb.nengtul.user.domain.dto.UserPasswordChangeDto;
import kr.zb.nengtul.user.domain.dto.UserUpdateDto;
import kr.zb.nengtul.user.domain.entity.User;
import kr.zb.nengtul.user.domain.repository.UserRepository;
import kr.zb.nengtul.user.mailgun.client.MailgunClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import s3bucket.service.AmazonS3Service;

@DisplayName("회원 테스트")
class UserServiceTest {

  private UserService userService;
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private MailgunClient mailgunClient;

  @BeforeEach
  void setUp() {
    // Mock 객체 초기화
    userRepository = mock(UserRepository.class);
    AmazonS3Service amazonS3Service = mock(AmazonS3Service.class);
    mailgunClient = mock(MailgunClient.class);
    userService = mock(UserService.class);
    passwordEncoder = mock(PasswordEncoder.class);

    userService = new UserService(
        userRepository, passwordEncoder, mailgunClient, amazonS3Service);
  }

  @Test
  @DisplayName("회원 가입 성공")
  void joinUser_SUCCESS() {
    // given
    UserJoinDto userJoinDto = UserJoinDto.builder()
        .name("이름")
        .nickname("닉네임")
        .email("aa@aa.aa")
        .phoneNumber("010-1234-1234")
        .address("주소1")
        .addressDetail("주소2")
        .password("password")
        .build();

    String encodedPassword = "encodedPassword"; // 가상의 인코딩된 비밀번호를 생성
    when(passwordEncoder.encode(userJoinDto.getPassword())).thenReturn(encodedPassword);

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

    when(userRepository.existsByEmail(userJoinDto.getEmail())).thenReturn(false);
    when(userRepository.existsByNickname(userJoinDto.getNickname())).thenReturn(false);
    when(userRepository.existsByPhoneNumber(userJoinDto.getPhoneNumber())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(user);

    // when
    userService.joinUser(userJoinDto);

    // then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User capturedUser = userCaptor.getValue();
    assertEquals(userJoinDto.getName(), capturedUser.getName());
    assertEquals(userJoinDto.getNickname(), capturedUser.getNickname());
    assertEquals(encodedPassword, capturedUser.getPassword());
    assertEquals(userJoinDto.getPhoneNumber(), capturedUser.getPhoneNumber());
    assertEquals(userJoinDto.getEmail(), capturedUser.getEmail());
    assertEquals(userJoinDto.getAddress(), capturedUser.getAddress());
    assertEquals(userJoinDto.getAddressDetail(), capturedUser.getAddressDetail());
    assertNull(capturedUser.getProfileImageUrl());
    assertFalse(capturedUser.isEmailVerifiedYn());
  }

  @Test
  @DisplayName("회원 가입 실패 - 중복된 이메일")
  void joinUser_FAIL_DuplicateEmail() {
    // given
    UserJoinDto userJoinDto = UserJoinDto.builder()
        .name("이름")
        .nickname("닉네임")
        .email("existing_email@domain.com") // 이미 존재하는 이메일 주소로 설정
        .phoneNumber("010-1234-1234")
        .address("주소1")
        .addressDetail("주소2")
        .password("password")
        .build();

    when(userRepository.existsByEmail(userJoinDto.getEmail())).thenReturn(true);

    // when, then
    assertThrows(CustomException.class, () -> userService.joinUser(userJoinDto));
  }

  @Test
  @DisplayName("회원 가입 실패 - 중복된 닉네임")
  void joinUser_FAIL_DuplicateNickname() {
    // given
    UserJoinDto userJoinDto = UserJoinDto.builder()
        .name("이름")
        .nickname("existing_nickname") // 이미 존재하는 닉네임으로 설정
        .email("aa@aa.aa")
        .phoneNumber("010-1234-1234")
        .address("주소1")
        .addressDetail("주소2")
        .password("password")
        .build();

    when(userRepository.existsByNickname(userJoinDto.getNickname())).thenReturn(true);

    // when, then
    assertThrows(CustomException.class, () -> userService.joinUser(userJoinDto));
  }

  @Test
  @DisplayName("회원 가입 실패 - 중복된 전화번호")
  void joinUser_FAIL_DuplicatePhoneNumber() {
    // given
    UserJoinDto userJoinDto = UserJoinDto.builder()
        .name("이름")
        .nickname("닉네임")
        .email("aa@aa.aa")
        .phoneNumber("010-9999-9999") // 이미 존재하는 전화번호로 설정
        .address("주소1")
        .addressDetail("주소2")
        .password("password")
        .build();

    when(userRepository.existsByPhoneNumber(userJoinDto.getPhoneNumber())).thenReturn(true);

    // when, then
    assertThrows(CustomException.class, () -> userService.joinUser(userJoinDto));
  }

  @Test
  @DisplayName("이메일 인증 성공")
  void verifyEmail_SUCCESS() {
    // given
    String email = "aa@aa.aa";
    String code = "verificationCode";
    User user = User.builder()
        .email(email)
        .build();
    user.setVerificationCode(code);
    user.setVerifyExpiredAt(LocalDateTime.now().plusDays(1));

    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // when
    userService.verifyEmail(email, code);

    // then
    assertTrue(user.isEmailVerifiedYn());
  }

  @Test
  @DisplayName("회원 탈퇴 성공")
  void quitUser_SUCCESS() {
    // given
    String email = "aa@aa.aa";
    User user = User.builder()
        .email(email)
        .build();

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

    // when
    userService.quitUser(principal);

    // then
    verify(userRepository).deleteById(user.getId());
  }

  @Test
  @DisplayName("회원 탈퇴 실패 - 사용자를 찾을 수 없음")
  void quitUser_FAIL_USER_NOT_FOUND() {
    // given
    String email = "aa@aa.aa";

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // when + then
    assertThrows(CustomException.class, () -> userService.quitUser(principal));
  }

  @Test
  @DisplayName("회원 정보 수정 성공")
  void updateUser_SUCCESS() {
    // given
    String email = "aa@aa.aa";
    User user = User.builder()
        .email(email)
        .nickname("이름")
        .phoneNumber("010-1111-1111")
        .address("주소")
        .addressDetail("주소1")
        .build();

    UserUpdateDto userUpdateDto = UserUpdateDto.builder()
        .nickname("새이름")
        .phoneNumber("010-2222-2222")
        .address("새주소")
        .addressDetail("새주소1")
        .build();

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(userRepository.existsByNickname(anyString())).thenReturn(false);

    // when
    userService.updateUser(principal, userUpdateDto, null);

    // then
    verify(userRepository).save(user);
    assertEquals("새이름", user.getNickname());
    assertEquals("010-2222-2222", user.getPhoneNumber());
    assertEquals("새주소", user.getAddress());
    assertEquals("새주소1", user.getAddressDetail());
  }

  @Test
  @DisplayName("회원 정보 수정 실패 - 중복된 닉네임")
  void updateUser_FAIL_DUPLICATE_NICKNAME() {
    // given
    String email = "aa@aa.aa";
    User user = User.builder()
        .email(email)
        .nickname("이름")
        .phoneNumber("010-1111-1111")
        .build();

    UserUpdateDto userUpdateDto = UserUpdateDto.builder()
        .nickname("새이름")
        .phoneNumber("010-2222-2222")
        .build();

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(userRepository.existsByNickname("새이름")).thenReturn(true); // 닉네임이 이미 존재하는 경우

    // when + then
    assertThrows(CustomException.class,
        () -> userService.updateUser(principal, userUpdateDto, null));
  }

  @Test
  @DisplayName("이메일 찾기 성공")
  void findEmail_SUCCESS() {
    // given
    String name = "이름";
    String phoneNumber = "010-1234-1234";
    String email = "aa@aa.aa";
    UserFindEmailReqDto userFindEmailReqDto = UserFindEmailReqDto.builder()
        .name(name)
        .phoneNumber(phoneNumber)
        .build();

    User user = User.builder()
        .name(name)
        .phoneNumber(phoneNumber)
        .email(email)
        .build();

    when(userRepository.findByNameAndPhoneNumber(name, phoneNumber)).thenReturn(Optional.of(user));

    // when
    User foundUser = userService.findEmail(userFindEmailReqDto);

    // then
    assertEquals(email, foundUser.getEmail());
  }

  @Test
  @DisplayName("이메일 찾기 실패 - 유저 없음")
  void findEmail_FAIL_NOT_FOUND_USER() {
    // given
    String name = "이름";
    String phoneNumber = "010-1234-1234";
    UserFindEmailReqDto userFindEmailReqDto = UserFindEmailReqDto.builder()
        .name(name)
        .phoneNumber(phoneNumber)
        .build();

    when(userRepository.findByNameAndPhoneNumber(name, phoneNumber)).thenReturn(Optional.empty());

    // when + then
    assertThrows(CustomException.class, () -> userService.findEmail(userFindEmailReqDto));
  }

  @Test
  @DisplayName("비밀번호 변경 성공")
  void changePassword_SUCCESS() {
    // given
    String email = "aa@aa.aa";
    String newPassword = "비번";
    UserPasswordChangeDto userPasswordChangeDto = UserPasswordChangeDto.builder()
        .password(newPassword)
        .build();

    User user = User.builder()
        .email(email)
        .password("옛날비번")
        .build();
    Principal principal = mock(Principal.class);

    when(principal.getName()).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode(anyString())).thenReturn("새비번");

    // when
    userService.changePassword(principal, userPasswordChangeDto);

    // then
    verify(userRepository).save(user);
    assertEquals("새비번", user.getPassword());
  }

  @Test
  @DisplayName("비밀번호 변경 실패 - 유저 없음")
  void changePassword_FAIL_NOT_FOUND_USER() {
    // given
    String email = "aa@aa.aa";
    String newPassword = "newPassword";
    UserPasswordChangeDto userPasswordChangeDto = UserPasswordChangeDto.builder()
        .password(newPassword)
        .build();
    Principal principal = mock(Principal.class);

    when(principal.getName()).thenReturn(email);
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // when + then
    assertThrows(CustomException.class,
        () -> userService.changePassword(principal, userPasswordChangeDto));
  }

}