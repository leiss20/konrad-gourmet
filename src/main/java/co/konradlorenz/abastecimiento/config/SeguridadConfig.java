package co.konradlorenz.abastecimiento.config;
import co.konradlorenz.abastecimiento.repository.UsuarioRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
/** RNF-SEG-01: autenticación y autorización por roles; RNF-SEG-04: contraseñas con hash BCrypt. */
@Configuration @EnableWebSecurity @EnableMethodSecurity
public class SeguridadConfig {
  @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }
  @Bean UserDetailsService usuarios(UsuarioRepository r) {
    return email -> { var u = r.porEmail(email); if (u == null || !Boolean.TRUE.equals(u.get("activo"))) throw new UsernameNotFoundException(email);
      return User.withUsername(email).password((String) u.get("password_hash")).roles((String) u.get("rol")).build(); };
  }
  @Bean SecurityFilterChain cadena(HttpSecurity http) throws Exception {
    http.csrf(c -> { var h = new CsrfTokenRequestAttributeHandler(); h.setCsrfRequestAttributeName(null); c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(h); }) // simplificación del prototipo: activar CSRF en producción
      .authorizeHttpRequests(a -> a.requestMatchers("/login.html", "/api/tema").permitAll().anyRequest().authenticated())
      .formLogin(f -> f.loginPage("/login.html").loginProcessingUrl("/login").defaultSuccessUrl("/", true).failureUrl("/login.html?error"))
      .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login.html"))
      .exceptionHandling(e -> e.authenticationEntryPoint((q, s, x) -> { if (q.getRequestURI().startsWith("/api")) s.sendError(401); else s.sendRedirect("/login.html"); }));
    return http.build();
  }
  /** Crea los usuarios de demostración la primera vez (contraseña: Konrad2026). */
  @Bean ApplicationRunner sembrar(UsuarioRepository r, PasswordEncoder enc) {
    return a -> { if (r.contar() > 0) return; String h = enc.encode("Konrad2026");
      r.crear("Administrador", "admin@konrad.co", h, "ADMIN", null); r.crear("Jefe de cocina", "jefe@konrad.co", h, "JEFE_COCINA", 1L);
      r.crear("Mesero", "mesero@konrad.co", h, "MESERO", 1L); r.crear("Auxiliar de compras", "auxiliar@konrad.co", h, "AUXILIAR_COMPRAS", null);
      r.crear("Director de compras", "dircompras@konrad.co", h, "DIRECTOR_COMPRAS", null); r.crear("Director comercial", "dircomercial@konrad.co", h, "DIRECTOR_COMERCIAL", null); };
  }
}
