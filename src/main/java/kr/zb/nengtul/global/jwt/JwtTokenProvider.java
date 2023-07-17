package kr.zb.nengtul.global.jwt;

//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jws;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Date;

import java.util.Optional;
import kr.zb.nengtul.global.entity.RoleType;
import kr.zb.nengtul.global.jwt.service.CustomUserDetailService;
import kr.zb.nengtul.user.entity.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@RequiredArgsConstructor
@Component
@Getter
@Slf4j
public class JwtTokenProvider {

  //환경변수에 키값 지정
  @Value("${spring.jwt.secret-key}")
  private String secretKey;

  @Value("${spring.jwt.access.expiration}")
  private Long accessTokenExpirationPeriod;

  @Value("${spring.jwt.refresh.expiration}")
  private Long refreshTokenExpirationPeriod;

  @Value("${spring.jwt.access.header}")
  private String accessHeader;

  @Value("${spring.jwt.refresh.header}")
  private String refreshHeader;
  /**
   * JWT의 Subject와 Claim으로 email 사용 -> 클레임의 name을 "email"으로 설정 JWT의 헤더에 들어오는 값 : 'Authorization(Key)
   * = Bearer {토큰} (Value)' 형식
   */
  private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
  private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
  private static final String EMAIL_CLAIM = "email";
  private static final String BEARER = "Bearer ";

  private final UserRepository userRepository;

  /**
   * AccessToken 생성 메소드
   */
  public String createAccessToken(String email) {
    Date now = new Date();
    return JWT.create() // JWT 토큰을 생성하는 빌더 반환
        .withSubject(ACCESS_TOKEN_SUBJECT) // JWT의 Subject 지정 -> AccessToken이므로 AccessToken
        .withExpiresAt(new Date(now.getTime() + accessTokenExpirationPeriod)) // 토큰 만료 시간 설정

        //클레임으로는 저희는 email 하나만 사용합니다.
        //추가적으로 식별자나, 이름 등의 정보를 더 추가하셔도 됩니다.
        //추가하실 경우 .withClaim(클래임 이름, 클래임 값) 으로 설정해주시면 됩니다
        .withClaim(EMAIL_CLAIM, email)
        .sign(Algorithm.HMAC512(
            secretKey)); // HMAC512 알고리즘 사용, application-jwt.yml에서 지정한 secret 키로 암호화
  }

  /**
   * RefreshToken 생성 RefreshToken은 Claim에 email도 넣지 않으므로 withClaim() X
   */
  public String createRefreshToken() {
    Date now = new Date();
    return JWT.create()
        .withSubject(REFRESH_TOKEN_SUBJECT)
        .withExpiresAt(new Date(now.getTime() + refreshTokenExpirationPeriod))
        .sign(Algorithm.HMAC512(secretKey));
  }

  /**
   * AccessToken 헤더에 실어서 보내기
   */
  public void sendAccessToken(HttpServletResponse response, String accessToken) {
    response.setStatus(HttpServletResponse.SC_OK);

    response.setHeader(accessHeader, accessToken);
    log.info("재발급된 Access Token : {}", accessToken);
  }

  /**
   * AccessToken + RefreshToken 헤더에 실어서 보내기
   */
  public void sendAccessAndRefreshToken(HttpServletResponse response, String accessToken,
      String refreshToken) {
    response.setStatus(HttpServletResponse.SC_OK);

    setAccessTokenHeader(response, accessToken);
    setRefreshTokenHeader(response, refreshToken);
    log.info("Access Token, Refresh Token 헤더 설정 완료");
  }

  /**
   * 헤더에서 RefreshToken 추출 토큰 형식 : Bearer XXX에서 Bearer를 제외하고 순수 토큰만 가져오기 위해서 헤더를 가져온 후 "Bearer"를
   * 삭제(""로 replace)
   */
  public Optional<String> extractRefreshToken(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader(refreshHeader))
        .filter(refreshToken -> refreshToken.startsWith(BEARER))
        .map(refreshToken -> refreshToken.replace(BEARER, ""));
  }

  /**
   * 헤더에서 AccessToken 추출 토큰 형식 : Bearer XXX에서 Bearer를 제외하고 순수 토큰만 가져오기 위해서 헤더를 가져온 후 "Bearer"를
   * 삭제(""로 replace)
   */
  public Optional<String> extractAccessToken(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader(accessHeader))
        .filter(refreshToken -> refreshToken.startsWith(BEARER))
        .map(refreshToken -> refreshToken.replace(BEARER, ""));
  }

  /**
   * AccessToken에서 Email 추출 추출 전에 JWT.require()로 검증기 생성 verify로 AceessToken 검증 후 유효하다면 getClaim()으로
   * 이메일 추출 유효하지 않다면 빈 Optional 객체 반환
   */
  public Optional<String> extractEmail(String accessToken) {
    try {
      // 토큰 유효성 검사하는 데에 사용할 알고리즘이 있는 JWT verifier builder 반환
      return Optional.ofNullable(JWT.require(Algorithm.HMAC512(secretKey))
          .build() // 반환된 빌더로 JWT verifier 생성
          .verify(accessToken) // accessToken을 검증하고 유효하지 않다면 예외 발생
          .getClaim(EMAIL_CLAIM) // claim(Emial) 가져오기
          .asString());
    } catch (Exception e) {
      log.error("액세스 토큰이 유효하지 않습니다.");
      return Optional.empty();
    }
  }

  /**
   * AccessToken 헤더 설정
   */
  public void setAccessTokenHeader(HttpServletResponse response, String accessToken) {
    response.setHeader(accessHeader, accessToken);
  }

  /**
   * RefreshToken 헤더 설정
   */
  public void setRefreshTokenHeader(HttpServletResponse response, String refreshToken) {
    response.setHeader(refreshHeader, refreshToken);
  }

  /**
   * RefreshToken DB 저장(업데이트)
   */
  public void updateRefreshToken(String email, String refreshToken) {
    userRepository.findByEmail(email)
        .ifPresentOrElse(
            user -> user.updateRefreshToken(refreshToken),
            () -> new Exception("일치하는 회원이 없습니다.")
        );
  }

  public boolean isTokenValid(String token) {
    try {
      JWT.require(Algorithm.HMAC512(secretKey)).build().verify(token);
      return true;
    } catch (Exception e) {
      log.error("유효하지 않은 토큰입니다. {}", e.getMessage());
      return false;
    }
  }
}
//  private final CustomUserDetailService userDetailsService;
//
//  // 객체 초기화, secretKey를 Base64로 인코딩
//  @PostConstruct
//  protected void init() {
//    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
//  }
//
//  // JWT 토큰 생성
//  public String createToken(String userPk, RoleType roles) {
//    Claims claims = Jwts.claims().setSubject(userPk);
//    claims.put("roles", roles); // 정보는 key / value 쌍으로 저장
//    Date now = new Date();
//    return Jwts.builder()
//        .setClaims(claims) // 정보 저장
//        .setIssuedAt(now) // 토큰 발행 시간 정보
//        .setExpiration(new Date(now.getTime() + tokenValidTime)) // 토큰 유효기간
//        .signWith(SignatureAlgorithm.HS512, secretKey)  // 사용할 암호화 알고리즘
//        // signature 에 들어갈 secret값 세팅
//        .compact();
//  }
//
//  // JWT 토큰에서 인증 정보 조회
//  public Authentication getAuthentication(String token) {
//    UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserPk(token));
//    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
//  }
//
//  // 토큰에서 회원 정보 추출
//  public String getUserPk(String token) {
//    return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
//  }
//
//  // Request의 Header에서 token 값을 가져옵니다. "Authorization" : "Bearer +TOKEN값'
//  public String resolveToken(HttpServletRequest request) {
//    String token = request.getHeader((TOKEN_HEADER));
//
//    if (!ObjectUtils.isEmpty(token) && token.startsWith(TOKEN_PREFIX)) { //토큰형태 포함
//      return token.substring(TOKEN_PREFIX.length()); //실제토큰 부위
//    }
//
//    return null;
//  }
//
//  // 토큰의 유효성 + 만료일자 확인
//  public boolean validateToken(String jwtToken) {
//    try {
//      Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(jwtToken);
//      return !claims.getBody().getExpiration().before(new Date());
//    } catch (Exception e) {
//      return false;
//    }
//  }
//  public void setAccessTokenHeader(HttpServletResponse response, String accessToken) {
//    response.setHeader(TOKEN_HEADER, accessToken);
//  }
//  public void sendAccessToken(HttpServletResponse response, String accessToken) {
//    response.setStatus(HttpServletResponse.SC_OK);
//
//    response.setHeader(TOKEN_HEADER, accessToken);
//    log.info("재발급된 Access Token : {}", accessToken);
//  }
//  public void sendAccessAndRefreshToken(HttpServletResponse response, String accessToken) {
//    response.setStatus(HttpServletResponse.SC_OK);
//
//    setAccessTokenHeader(response, accessToken);
//    log.info("Access Token 헤더 설정 완료");
//  }
//}