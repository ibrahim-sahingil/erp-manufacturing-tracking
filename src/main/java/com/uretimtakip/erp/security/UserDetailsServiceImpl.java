package com.uretimtakip.erp.security;

import com.uretimtakip.erp.user.User;
import com.uretimtakip.erp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security'nin login sirasinda kullanicinin DB'den nasil yukleyecegini soyler.
 *
 * AuthenticationManager bunu cagirir:
 *   loadUserByUsername("admin") -> DB'den User'i bul, UserDetails'e cevir.
 *
 * Sonra Spring Security verilen sifreyi BCrypt ile hash'leyip
 * DB'deki hash ile karsilastirir.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Kullanici bulunamadi: " + username
                ));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // Permissions'lari authority olarak ekle
        if (user.getPermissions() != null) {
            user.getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p))
            );
        }

        // Role'u ROLE_ prefix ile ekle
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority(
                    "ROLE_" + user.getRole().toUpperCase()
            ));
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(authorities)
                .disabled(user.getIsActive() != null && !user.getIsActive())
                .build();
    }
}