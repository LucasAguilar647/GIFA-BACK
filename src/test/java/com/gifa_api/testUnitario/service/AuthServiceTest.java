package com.gifa_api.testUnitario.service;

import com.gifa_api.config.jwt.JwtService;
import com.gifa_api.dto.login.*;
import com.gifa_api.exception.NotFoundException;
import com.gifa_api.exception.RegisterException;
import com.gifa_api.model.Usuario;
import com.gifa_api.repository.IUsuarioRepository;
import com.gifa_api.service.IChoferService;
import com.gifa_api.service.impl.AuthServiceImpl;
import com.gifa_api.utils.enums.EstadoUsuario;
import com.gifa_api.utils.enums.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {
// completar tests relacionados al chofer
    @Mock
    private IUsuarioRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private IChoferService choferService;

    @InjectMocks
    private AuthServiceImpl authService;

    private final String password = "1234";
    private final String encodedPassword = "encodedPassword";

    private final String mockedJwtToken = "mockedJwtToken";

    private final String adminUsername = "admin";
    private final String supervisorUsername = "supervisor";
    private final String operadorUsername = "operador";
    private final String gerenteUsername = "gerente";
    private final String choferUsername = "chofer";

    private UpdateRequestDTO updateUsuarioDTO;
    private  Usuario usuario;
    private  Usuario chofer;
    private List<Usuario> usuarios;
    
    @BeforeEach
     void setUp(){
        updateUsuarioDTO = UpdateRequestDTO.builder()
                .username("usuario")
                .password("password")
                .build();
        chofer = Usuario.builder()

                .id(1)
                .rol(Rol.CHOFER)
                .estadoUsuario(EstadoUsuario.HABILITADO)
                .build();
        usuario = Usuario.builder()
                .id(2)
                .estadoUsuario(EstadoUsuario.INHABILITADO)
                .contrasena("asdsa")
                .rol(Rol.ADMINISTRADOR)
                .build();
        usuarios = List.of(usuario,chofer);
    }

   @Test
   @DisplayName("habilitar usuario que no existe lanza NotFoundException")
   void habilitarUsuarioInvalido(){
       when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
       assertThrows(NotFoundException.class,() -> authService.habilitar(anyInt()));

       verify(userRepository,never()).save(any(Usuario.class));
   }

    @Test
    @DisplayName("inhabilitar usuario que no existe lanza NotFoundException")
    void inhabilitarUsuarioInvalido(){
        when(userRepository.findById(usuario.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,() -> authService.inhabilitar(usuario.getId()));

        verify(userRepository,never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("actualizar usuario que no existe lanza NotFoundException")
    void actualizarUsuarioInvalidoInvalido(){
        when(userRepository.findById(usuario.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,() -> authService.update(usuario.getId(),updateUsuarioDTO));

        verify(userRepository,never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("loguearse con nombre de usuario que no existe lanza NotFoundException")
    void testLoginUsuarioNoExiste() {
        String usuarioInexistente = "unknown";
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(usuarioInexistente);
        loginRequest.setPassword(password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Simular autenticación NO exitosa
        when(userRepository.findByUsuario(usuarioInexistente)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.login(loginRequest));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsuario(usuarioInexistente);
    }

    @Test
    @DisplayName("Se registra usuario existente, lanza RegisterException")
    void testRegisterUsuarioYaExiste() {
        String usuarioAdmin = adminUsername;
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(usuarioAdmin, password, Rol.ADMINISTRADOR.toString());

        when(userRepository.existsByUsuario(usuarioAdmin)).thenReturn(true);

        assertThrows(RegisterException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Se habilita usuario exitosamente")
    void habilitarUsuario(){
        when(userRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        authService.habilitar(usuario.getId());

        assertEquals(usuario.getEstadoUsuario(),EstadoUsuario.HABILITADO);
        verify(userRepository,times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Se inhabilita usuario exitosamente")
    void inhabilitarUsuario(){
        usuario.setEstadoUsuario(EstadoUsuario.HABILITADO);
        when(userRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        authService.inhabilitar(usuario.getId());

        assertEquals(usuario.getEstadoUsuario(),EstadoUsuario.INHABILITADO);
        verify(userRepository,times(1)).save(any(Usuario.class));
        verify(choferService,never()).inhabilitarUsuarioChofer(usuario.getId());
    }

    @Test
    @DisplayName("Se inhabilita chofer exitosamente")
    void inhabilitar_cambiaEstadoDelUsuarioYchoferAinhabilitado(){
        chofer.setEstadoUsuario(EstadoUsuario.HABILITADO);
        when(userRepository.findById(chofer.getId())).thenReturn(Optional.of(chofer));
        authService.inhabilitar(chofer.getId());

        assertEquals(chofer.getEstadoUsuario(),EstadoUsuario.INHABILITADO);
        verify(userRepository,times(1)).save(any(Usuario.class));
        verify(choferService,times(1)).inhabilitarUsuarioChofer(chofer.getId());

    }

    @Test
    @DisplayName("Se ve todos los usuarios correctamente")
    void getAll(){
        when(userRepository.findAll()).thenReturn(usuarios);
        List<GetUserDTO> usuariosRegistrados = authService.getAll();

        assertEquals(usuariosRegistrados.size(),usuarios.size());
        assertEquals(usuariosRegistrados.get(0).getId(),usuarios.get(0).getId());
        assertEquals(usuariosRegistrados.get(1).getId(),usuarios.get(1).getId());
    }



    @Test
    @DisplayName("Se actualiza usuario correctamente")
    void actualizarUsuario(){
        when(userRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(updateUsuarioDTO.getPassword())).thenReturn(updateUsuarioDTO.getPassword());
        authService.update(usuario.getId(),updateUsuarioDTO);

        assertEquals(updateUsuarioDTO.getUsername(),usuario.getUsername());
        assertEquals(updateUsuarioDTO.getPassword(),usuario.getPassword());
        verify(userRepository,times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Administrador se logue exitosamente")
    void loginAdmin() {
        loginTestHelper(adminUsername, Rol.ADMINISTRADOR);
    }

    @Test
    @DisplayName("Supervisor se logue exitosamente")
    void testLogin_Success_Supervisor() {
        loginTestHelper(supervisorUsername, Rol.SUPERVISOR);
    }

    @Test
    @DisplayName("Operador se logue exitosamente")
    void testLogin_Success_Operador() {
        loginTestHelper(operadorUsername, Rol.OPERADOR);
    }

    @Test
    @DisplayName("Gerente se logue exitosamente")
    void testLogin_Success_Gerente() {
        loginTestHelper(gerenteUsername, Rol.GERENTE);
    }

    @Test
    @DisplayName("Administrador se registra exitosamente")
    void testRegister_Success_Admin() {
        registerTestHelper(adminUsername, Rol.ADMINISTRADOR);
    }

    @Test
    @DisplayName("Supervisor se registra exitosamente")
    void testRegister_Success_Supervisor() {
        registerTestHelper(supervisorUsername, Rol.SUPERVISOR);
    }

    @Test
    @DisplayName("Operador se registra exitosamente")
    void testRegister_Success_Operador() {
        registerTestHelper(operadorUsername, Rol.OPERADOR);
    }

    @Test
    @DisplayName("Gerente se registra exitosamente")
    void testRegister_Success_Gerente() {
        registerTestHelper(gerenteUsername, Rol.GERENTE);
    }

    private void loginTestHelper(String username, Rol role) {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        Usuario user = new Usuario();
        user.setUsuario(username);
        user.setContrasena(encodedPassword);
        user.setRol(role);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // Simular autenticación exitosa
        when(userRepository.findByUsuario(username)).thenReturn(Optional.of(user));
        when(jwtService.getToken(user)).thenReturn(mockedJwtToken);

        LoginResponseDTO response = authService.login(loginRequest);

        assertEquals(username, response.getUsername());
        assertEquals(mockedJwtToken, response.getToken());
        assertEquals(role, response.getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsuario(username);
        verify(jwtService).getToken(user);
    }

    private void registerTestHelper(String username, Rol role) {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setRole(role.name());
        Usuario user = new Usuario();
        user.setUsuario(username);
        user.setContrasena(password);
        user.setRol(role);

        when(userRepository.existsByUsuario(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtService.getToken(any(Usuario.class))).thenReturn(mockedJwtToken);


        LoginResponseDTO response = authService.register(registerRequest);

        assertEquals(username, response.getUsername());
        assertEquals(mockedJwtToken, response.getToken());
        assertEquals(role, response.getRole());
        verify(userRepository).save(any(Usuario.class));
    }
}