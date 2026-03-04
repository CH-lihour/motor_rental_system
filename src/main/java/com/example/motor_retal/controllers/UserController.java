package com.example.motor_retal.controllers;

import com.example.motor_retal.models.users.Role;
import com.example.motor_retal.models.users.User;
import com.example.motor_retal.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public String index(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users/index";
    }

    @GetMapping("/users/add")
    public String create() {
        return "users/create";
    }

    @PostMapping("/users/add")
    public String store(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String email,
            @RequestParam Role role,
            @RequestParam(defaultValue = "false") Boolean active,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes ra
    ) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = (email == null || email.isBlank()) ? null : email.trim();

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            addFormDataForCreate(ra, normalizedUsername, normalizedEmail, role, active);
            ra.addFlashAttribute("errorMessage", "Username already exists.");
            return "redirect:/users/add";
        }

        if (normalizedEmail != null && userRepository.findByEmail(normalizedEmail).isPresent()) {
            addFormDataForCreate(ra, normalizedUsername, normalizedEmail, role, active);
            ra.addFlashAttribute("errorMessage", "Email already exists.");
            return "redirect:/users/add";
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(normalizedEmail);
        user.setRole(role);
        user.setActive(active);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = saveImage(imageFile);
            user.setImage(imagePath);
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            addFormDataForCreate(ra, normalizedUsername, normalizedEmail, role, active);
            ra.addFlashAttribute("errorMessage", "Username or email already exists.");
            return "redirect:/users/add";
        }

        ra.addFlashAttribute("successMessage", "User created successfully.");
        return "redirect:/users";
    }

    private String saveImage(MultipartFile imageFile) {
        try {
            Path uploadDir = Paths.get("src/main/resources/static/uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String originalName = imageFile.getOriginalFilename();
            String extension = "";
            if (originalName != null) {
                int dotIndex = originalName.lastIndexOf('.');
                if (dotIndex >= 0) {
                    extension = originalName.substring(dotIndex);
                }
            }

            String fileName = UUID.randomUUID() + extension;
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image file", e);
        }
    }

    @GetMapping("/users/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
            model.addAttribute("user", user);

        return "users/edit";
    }

    @PostMapping("/users/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String email,
            @RequestParam Role role,
            @RequestParam(defaultValue = "false") Boolean active,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes ra
    ) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = (email == null || email.isBlank()) ? null : email.trim();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<User> existingByUsername = userRepository.findByUsername(normalizedUsername);
        if (existingByUsername.isPresent() && !existingByUsername.get().getId().equals(id)) {
            addFormDataForEdit(ra, normalizedUsername, normalizedEmail, role, active);
            ra.addFlashAttribute("errorMessage", "Username already exists.");
            return "redirect:/users/edit/" + id;
        }

        if (normalizedEmail != null) {
            Optional<User> existingByEmail = userRepository.findByEmail(normalizedEmail);
            if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(id)) {
                addFormDataForEdit(ra, normalizedUsername, normalizedEmail, role, active);
                ra.addFlashAttribute("errorMessage", "Email already exists.");
                return "redirect:/users/edit/" + id;
            }
        }

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setRole(role);
        user.setActive(active);

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            user.setImage(saveImage(imageFile));
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            addFormDataForEdit(ra, normalizedUsername, normalizedEmail, role, active);
            ra.addFlashAttribute("errorMessage", "Username or email already exists.");
            return "redirect:/users/edit/" + id;
        }

        ra.addFlashAttribute("successMessage", "User updated successfully.");
        return "redirect:/users";
    }

    @GetMapping("/users/delete/{id}")
    public String destroy(@PathVariable Long id, Model model, RedirectAttributes ra){
        if (!userRepository.existsById(id)) {
            ra.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/users";
        }

        userRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "User deleted successfully.");
        return "redirect:/users";
    }

    private void addFormDataForCreate(RedirectAttributes ra, String username, String email, Role role, Boolean active) {
        ra.addFlashAttribute("formUsername", username);
        ra.addFlashAttribute("formEmail", email);
        ra.addFlashAttribute("formRole", role);
        ra.addFlashAttribute("formActive", Boolean.TRUE.equals(active));
    }

    private void addFormDataForEdit(RedirectAttributes ra, String username, String email, Role role, Boolean active) {
        ra.addFlashAttribute("formUsername", username);
        ra.addFlashAttribute("formEmail", email);
        ra.addFlashAttribute("formRole", role);
        ra.addFlashAttribute("formActive", Boolean.TRUE.equals(active));
    }
}
