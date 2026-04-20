package leonil.sulude.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a user in the authentication system.
 * Implements Spring Security's UserDetails to integrate with the security framework.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    /** Unique identifier for the user   */
    @Id
    @GeneratedValue
    private UUID id;

    /** User's full name (for display purposes)*/
    @Column(nullable = false)
    private String name;


    /** Unique email used for login */
    @Column(nullable = false, unique = true)
    private String email;

    /** Hashed password (BCrypt) */
    @Column(nullable = false)
    private String password;

    /** User role (e.g. USER, PROVIDER, ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** If false, account is expired */
    private boolean accountNonExpired = true;

    /** If false, account is locked */
    private boolean accountNonLocked = true;

    /** If false, password has expired */
    private boolean credentialsNonExpired = true;

    /** If false, account is disabled */
    private boolean enabled = true;


    /**
     * Returns the authorities granted to the user.
     *
     * Spring Security uses this to evaluate access control
     * (e.g. @PreAuthorize, route protection).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Returns the username used for authentication.
     *
     * In this system, the email acts as the username.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the account is not expired.
     */
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    /**
     * Indicates whether the account is not locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    /**
     * Indicates whether the credentials are not expired.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    /**
     * Indicates whether the user is enabled.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
