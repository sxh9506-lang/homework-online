package com.homework.online.service;

import com.homework.online.common.ApiException;
import com.homework.online.config.JwtService;
import com.homework.online.domain.ClassMember;
import com.homework.online.domain.Role;
import com.homework.online.domain.SchoolClass;
import com.homework.online.domain.User;
import com.homework.online.dto.AuthDtos;
import com.homework.online.repo.ClassMemberRepository;
import com.homework.online.repo.SchoolClassRepository;
import com.homework.online.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Mapper mapper;

    public AuthService(UserRepository userRepository,
                       SchoolClassRepository schoolClassRepository,
                       ClassMemberRepository memberRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       Mapper mapper) {
        this.userRepository = userRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String username = request.username == null ? "" : request.username.trim();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.badRequest("用户名或密码不正确"));
        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw ApiException.badRequest("用户名或密码不正确");
        }
        return new AuthDtos.AuthResponse(jwtService.generate(user.getId()), mapper.toUserDto(user));
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String username = request.username.trim();
        if (userRepository.existsByUsername(username)) {
            throw ApiException.badRequest("该用户名已被注册");
        }
        SchoolClass schoolClass = null;
        if (request.classCode != null && !request.classCode.isBlank()) {
            schoolClass = schoolClassRepository.findByCode(request.classCode.trim().toUpperCase())
                    .orElseThrow(() -> ApiException.badRequest("班级邀请码不存在"));
        }
        User user = new User(username, passwordEncoder.encode(request.password), request.name.trim(), Role.STUDENT);
        user.setStudentNo(request.studentNo == null || request.studentNo.isBlank() ? null : request.studentNo.trim());
        userRepository.save(user);
        if (schoolClass != null) {
            memberRepository.save(new ClassMember(schoolClass, user));
        }
        return new AuthDtos.AuthResponse(jwtService.generate(user.getId()), mapper.toUserDto(user));
    }

    public User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }

    public AuthDtos.UserDto me(Long userId) {
        return mapper.toUserDto(requireUser(userId));
    }

    @Transactional
    public AuthDtos.UserDto updateProfile(Long userId, AuthDtos.ProfileRequest request) {
        User user = requireUser(userId);
        if (request.name != null && !request.name.isBlank()) {
            user.setName(request.name.trim());
        }
        if (request.email != null) {
            user.setEmail(request.email.isBlank() ? null : request.email.trim());
        }
        return mapper.toUserDto(userRepository.save(user));
    }
}
