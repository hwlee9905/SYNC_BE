package com.simple.book.domain.user.service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.simple.book.domain.alarm.service.AlarmUrlService;
import com.simple.book.domain.jwt.dto.AuthTokenDto;
import com.simple.book.domain.jwt.dto.CustomUserDetails;
import com.simple.book.domain.oauth2.CustomOAuth2User;
import com.simple.book.domain.user.dto.request.ModifyProfileImgRequestDto;
import com.simple.book.domain.user.dto.request.ModifyPwdRequestDto;
import com.simple.book.domain.user.dto.request.ModifyUserInfoRequestDto;
import com.simple.book.domain.user.dto.request.SignupRequestDto;
import com.simple.book.domain.user.dto.response.GetMyInfoResponseDto;
import com.simple.book.domain.user.dto.response.GetUserInfoResponseDto;
import com.simple.book.domain.user.entity.Authentication;
import com.simple.book.domain.user.entity.User;
import com.simple.book.domain.user.repository.AuthenticationRepository;
import com.simple.book.domain.user.repository.UserRepository;
import com.simple.book.domain.user.util.InfoSet;
import com.simple.book.domain.user.util.Role;
import com.simple.book.global.advice.ErrorCode;
import com.simple.book.global.advice.ResponseMessage;
import com.simple.book.global.config.ApplicationConfig;
import com.simple.book.global.exception.AuthenticationFailureException;
import com.simple.book.global.exception.IdenticalValuesCannotChangedException;
import com.simple.book.global.exception.ImageDirNotFoundException;
import com.simple.book.global.exception.ImageFileNotFoundException;
import com.simple.book.global.exception.UnknownException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService implements UserDetailsService {
	private final UserRepository userRepository;
	private final AuthenticationRepository authenticationRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final AlarmUrlService alarmUrlService;
	private final ApplicationConfig applicationConfig;

	@Transactional(rollbackFor = { Exception.class })
	public ResponseMessage remove(String userId) {
		try {
			Authentication authentication = authenticationRepository.findByUserId(userId);
			authenticationRepository.delete(authentication);
		} catch (Exception e) {
			throw new UnknownException(e.getMessage());
		}
		return ResponseMessage.builder().message("success").build();
	}

	// 회원가입
	@Transactional(rollbackFor = { Exception.class })
	public User signup(SignupRequestDto signupRequestDto) {
		boolean isSuccess;
		long id;
//		log.info("signup password : " + signupRequestDto.getPassword());
		Authentication authentication = Authentication.builder().userId(signupRequestDto.getUserId())
				.email(signupRequestDto.getEmail())
				.password(bCryptPasswordEncoder.encode(signupRequestDto.getPassword())).infoSet(InfoSet.DEFAULT)
				.build();
		try {
			authenticationRepository.saveAndFlush(authentication);
			isSuccess = true;
		} catch (DataIntegrityViolationException e) {
			throw new DataIntegrityViolationException("중복된 아이디입니다.", e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		User user = User.builder().username(signupRequestDto.getUsername()).role(Role.USER)
				.nickname(signupRequestDto.getNickname()).build();
		user.setAuthentication(authentication);
		authentication.setUser(user);
		try {
			id = userRepository.saveAndFlush(user).getId();
			isSuccess = true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (isSuccess) {
			alarmUrlService.createAlarmUrl(id);
		}

		return user;
	}

	// 로그인
	@Transactional(rollbackFor = { Exception.class })
	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		// DB에서 조회
		Authentication authentication = authenticationRepository.findByUserId(userId);
		if (authentication != null) {
			AuthTokenDto authTokenDto = AuthTokenDto.builder().infoSet(authentication.getInfoSet().toString())
					.name(authentication.getUser().getUsername()).username(authentication.getUserId())
					.password(authentication.getPassword()).role(authentication.getUser().getRole().toString()).build();

			// UserDetails에 담아서 return하면 AutneticationManager가 검증 함
			return new CustomUserDetails(authTokenDto);
		}
		throw new AuthenticationFailureException("아이디가 잘못되었습니다.", ErrorCode.USER_FAILED_AUTHENTICATION);
	}

	/**
	 * 내 정보 조회
	 * @return
	 */
	public ResponseMessage getMyInfo() {
		GetMyInfoResponseDto dto = new GetMyInfoResponseDto();
		try {
			String id = getCurrentUserId();
			User info = userRepository.findByAuthenticationUserId(id);
			String imgUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/user/img/").path(info.getProfileImg()).toUriString();
			
			dto.setUserId(id);	
			dto.setProfileImg(imgUrl);
			dto.setUsername(info.getUsername());
			dto.setNickname(info.getNickname());
			dto.setPosition(info.getPosition());
			dto.setIntroduction(info.getIntroduction());
		} catch (Exception e) {
			throw new UnknownException(e.getMessage());
		}
		return ResponseMessage.builder().value(dto).build();
	}
	
	/**
	 * 유저 정보 조회
	 * @param userId
	 * @return
	 */
	public ResponseMessage getUserInfo(long userId) {
		GetUserInfoResponseDto dto = new GetUserInfoResponseDto();
		try {
			Optional<User> info = userRepository.findById(userId);
			if (info.isPresent()) {
				User entity = info.get();
				String imgUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/user/img/").path(entity.getProfileImg()).toUriString();
				
				dto.setUserId(entity.getAuthentication().getUserId());
				dto.setProfileImg(imgUrl);
				dto.setUsername(entity.getUsername());
				dto.setNickname(entity.getNickname());
				dto.setPosition(entity.getPosition());
				dto.setIntroduction(entity.getIntroduction());
			}
		}catch (Exception e) {
			throw new UnknownException(e.getMessage());
		}
		return ResponseMessage.builder().value(dto).build();
	}

	/**
	 * 정보 변경
	 * 
	 * @param body
	 * @param userId
	 * @return
	 */
	@Transactional(rollbackFor = { Exception.class })
	public ResponseMessage modifyUserInfo(ModifyUserInfoRequestDto body, String userId) {
		User user = userRepository.findByAuthenticationUserId(userId);
		userInfoUpdate(body, user);
		return ResponseMessage.builder().message("수정 되었습니다.").build();
	}

	private void userInfoUpdate (ModifyUserInfoRequestDto body, User user) {
		String type = body.getType();
		String value = body.getValue();
		switch (type) {
		case "N":
			if (user.getNickname() != null && user.getNickname().equals(value)) {
				throw new IdenticalValuesCannotChangedException(value);
			}
			user.setNickname(value);
			break;
		case "P":
			if (user.getPosition() != null && user.getPosition().equals(value)) {
				throw new IdenticalValuesCannotChangedException(value);
			}
			user.setPosition(value);
			break;
		case "I":
			if (user.getIntroduction() != null && user.getIntroduction().equals(value)) {
				throw new IdenticalValuesCannotChangedException(value);
			}
			user.setIntroduction(value);
			break;
		default:
			throw new UnknownException(null);
		}
	}

	@Transactional(rollbackFor = { Exception.class })
	public ResponseMessage modifyProfileImg(ModifyProfileImgRequestDto body) {
		String imgPath = applicationConfig.getImagePath() + File.separator + "profile";
		String originName;
		try {
			originName = body.getProfileImg().getOriginalFilename();
		} catch (NullPointerException e) {
			throw new ImageFileNotFoundException(e.getMessage());
		}

		// 확장자 없을 경우 예외처리 해야함
		String extension = originName.substring(originName.lastIndexOf(".") + 1);
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		String saveName = uuid + "." + extension;
		String fileFullPath = Paths.get(imgPath, saveName).toString();

		File dir = new File(imgPath);
		if (!dir.exists()) {
			throw new ImageDirNotFoundException(dir.toString());
		}
		
		try {
			File uploadFile = new File(fileFullPath);
			body.getProfileImg().transferTo(uploadFile);
			User user = userRepository.findByAuthenticationUserId(getCurrentUserId());
			user.setProfileImg(saveName);
			
		} catch (Exception e) {
			throw new UnknownException(e.getMessage());
		}
		return ResponseMessage.builder().message("success").build();
	}

	/**
	 * 비밀번호 변경
	 * 
	 * @param body
	 * @param userDetails
	 * @return
	 */
	@Transactional(rollbackFor = { Exception.class })
	public ResponseMessage modifyPwd(ModifyPwdRequestDto body, UserDetails userDetails) {
		ResponseMessage result = null;
		String encodedPassword = userDetails.getPassword();
		boolean isCurrenPwdMatch = bCryptPasswordEncoder.matches(body.getCurrentPwd(), encodedPassword);
		if (isCurrenPwdMatch) {
			if (body.getNewPwd().equals(body.getCheckNewPwd())) {
				Authentication auth = authenticationRepository.findByUserId(userDetails.getUsername());
				auth.setPassword(bCryptPasswordEncoder.encode(body.getNewPwd()));
				result = ResponseMessage.builder().message("success").build();
			} else {
				result = ResponseMessage.builder().result(false).message("비밀번호가 일치 하지 않습니다.").build();
			}
		} else {
			result = ResponseMessage.builder().result(false).message("비밀번호를 확인 해 주세요.").build();
		}
		return result;
	}

	public String getCurrentUserId() {
		org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			if (authentication instanceof OAuth2AuthenticationToken) {
				CustomOAuth2User oauthToken = (CustomOAuth2User) authentication.getPrincipal();
				return oauthToken.getUsername(); // OAuth2로 인증된 경우 사용자 ID 추출
			} else if (authentication instanceof UsernamePasswordAuthenticationToken) {
				CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
				return customUserDetails.getUsername();
			}
		}
		return null; // 사용자가 인증되지 않았거나 인증 정보가 없는 경우
	}

}
