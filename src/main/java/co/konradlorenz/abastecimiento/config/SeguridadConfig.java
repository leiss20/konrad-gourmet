
package co.konradlorenz.abastecimiento.config;

import co.konradlorenz.abastecimiento.repository.UsuarioRepository;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.Map;

/**
 * Configuración centralizada de seguridad de Konrad Gourmet.
 *
 * RNF-SEG-01: autenticación y autorización por roles.
 * RNF-SEG-04: almacenamiento seguro de contraseñas mediante BCrypt.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    private static final String LOGIN_PAGE = "/login.html";
    private static final String LOGIN_PROCESSING_URL = "/login";
    private static final String LOGOUT_URL = "/logout";

    private static final String DEMO_PASSWORD = "Konrad2026";

    // --------------------------------------------------
    // CONFIGURACIÓN DE CONTRASEÑAS
    // --------------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --------------------------------------------------
    // AUTENTICACIÓN DE USUARIOS
    // --------------------------------------------------

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {

        return email -> cargarUsuario(email, usuarioRepository);
    }

    private UserDetails cargarUsuario(
            String email,
            UsuarioRepository usuarioRepository) {

        Map<String, Object> usuario = usuarioRepository.porEmail(email);

        if (usuario == null || !Boolean.TRUE.equals(usuario.get("activo"))) {
            throw new UsernameNotFoundException(
                "Usuario no encontrado o inactivo: " + email
            );
        }

        String passwordHash = (String) usuario.get("password_hash");
        String rol = (String) usuario.get("rol");

        if (passwordHash == null || rol == null || rol.isBlank()) {
            throw new UsernameNotFoundException(
                "El usuario no tiene credenciales o rol válido: " + email
            );
        }

        return User.withUsername(email)
                .password(passwordHash)
                .roles(rol)
                .build();
    }

    // --------------------------------------------------
    // CONFIGURACIÓN DE FILTROS DE SEGURIDAD
    // --------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        configurarCSRF(http);
        configurarAutorizacion(http);
        configurarLogin(http);
        configurarLogout(http);
        configurarManejoExcepciones(http);

        return http.build();
    }

    private void configurarCSRF(HttpSecurity http) throws Exception {

        CsrfTokenRequestAttributeHandler csrfHandler =
                new CsrfTokenRequestAttributeHandler();

        csrfHandler.setCsrfRequestAttributeName(null);

        http.csrf(csrf -> csrf
                .csrfTokenRepository(
                    CookieCsrfTokenRepository.withHttpOnlyFalse()
                )
                .csrfTokenRequestHandler(csrfHandler)
        );
    }

    private void configurarAutorizacion(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(LOGIN_PAGE, "/api/tema").permitAll()
                .anyRequest().authenticated()
        );
    }

    private void configurarLogin(HttpSecurity http) throws Exception {

        http.formLogin(login -> login
                .loginPage(LOGIN_PAGE)
                .loginProcessingUrl(LOGIN_PROCESSING_URL)
                .defaultSuccessUrl("/", true)
                .failureUrl(LOGIN_PAGE + "?error")
        );
    }

    private void configurarLogout(HttpSecurity http) throws Exception {

        http.logout(logout -> logout
                .logoutUrl(LOGOUT_URL)
                .logoutSuccessUrl(LOGIN_PAGE)
        );
    }

    private void configurarManejoExcepciones(HttpSecurity http)
            throws Exception {

        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {

                    if (request.getRequestURI().startsWith("/api")) {
                        response.sendError(401);
                    } else {
                        response.sendRedirect(LOGIN_PAGE);
                    }
                })
        );
    }

    // --------------------------------------------------
    // CREACIÓN DE USUARIOS DE DEMOSTRACIÓN
    // --------------------------------------------------

    @Bean
    public ApplicationRunner seedDemoUsers(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        return args -> crearUsuariosDemo(
                usuarioRepository,
                passwordEncoder
        );
    }

    private void crearUsuariosDemo(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        if (usuarioRepository.contar() > 0) {
            return;
        }

        String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);

        crearUsuario(usuarioRepository, "Administrador",
                "admin@konrad.co", passwordHash, "ADMIN", null);

        crearUsuario(usuarioRepository, "Jefe de cocina",
                "jefe@konrad.co", passwordHash, "JEFE_COCINA", 1L);

        crearUsuario(usuarioRepository, "Mesero",
                "mesero@konrad.co", passwordHash, "MESERO", 1L);

        crearUsuario(usuarioRepository, "Auxiliar de compras",
                "auxiliar@konrad.co", passwordHash, "AUXILIAR_COMPRAS", null);

        crearUsuario(usuarioRepository, "Director de compras",
                "dircompras@konrad.co", passwordHash, "DIRECTOR_COMPRAS", null);

        crearUsuario(usuarioRepository, "Director comercial",
                "dircomercial@konrad.co", passwordHash, "DIRECTOR_COMERCIAL", null);
    }

    private void crearUsuario(
            UsuarioRepository usuarioRepository,
            String nombre,
            String email,
            String passwordHash,
            String rol,
            Long superiorId) {

        usuarioRepository.crear(
                nombre,
                email,
                passwordHash,
                rol,
                superiorId
        );
    }
}
