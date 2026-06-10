package com.example.auth.controller;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * 사용자의 프로필 이미지를 서버에 업로드하고 정보에 반영합니다.
     * @param file 업로드할 이미지 파일
     * @param userDetails 사용자 인증 정보
     * @return 업로드된 이미지의 저장 경로 URL
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String filePath = fileStorageService.storeFile(file);
        user.setProfileImageUrl(filePath);
        user.setActiveProfileType("CUSTOM");
        userRepository.save(user);

        return ResponseEntity.ok(filePath);
    }

    /**
     * 사용자의 프로필 설정(예: 기본 이미지 선택, 커스텀 이미지 선택 등)을 업데이트합니다.
     * @param settings 변경할 설정값들
     * @param userDetails 사용자 인증 정보
     * @return 상태코드 200 OK
     */
    @PatchMapping("/settings")
    public ResponseEntity<Void> updateProfileSettings(
            @RequestBody Map<String, String> settings,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String type = settings.get("activeProfileType"); // DEFAULT_1, DEFAULT_2, CUSTOM 등
        if (type != null) {
            user.setActiveProfileType(type);
            userRepository.save(user);
        }

        return ResponseEntity.ok().build();
    }
}
