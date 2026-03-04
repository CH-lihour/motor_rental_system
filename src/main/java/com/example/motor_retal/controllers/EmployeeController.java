package com.example.motor_retal.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.motor_retal.models.employees.Employee;
import com.example.motor_retal.repositories.EmployeeRepository;
import com.example.motor_retal.models.employees.Position;
import com.example.motor_retal.models.employees.Status;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class EmployeeController {
    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository){
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/employees")
    public String index(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        return "employees/index";
    }

    @GetMapping("/employees/add")
    public String create(Model model) {
        if (!model.containsAttribute("formEmployeeCode")) {
            model.addAttribute("formEmployeeCode", generateNextEmployeeCode());
        }
        return "employees/create";
    }

    @PostMapping("/employees/add")
    public String store(
            @RequestParam String fullname,
            @RequestParam(required = false) String phone,
            @RequestParam Position position,
            @RequestParam(defaultValue = "false") Boolean active,
            RedirectAttributes ra
    ) {
        String fullName = fullname == null ? "" : fullname.trim();
        String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;
        String code = generateNextEmployeeCode();

        if (fullName.isBlank()) {
            addFormDataForCreate(ra, fullName, resolvedPhone, position, active, code);
            ra.addFlashAttribute("errorMessage", "Full name is required.");
            return "redirect:/employees/add";
        }

        // Handle concurrent requests that may generate the same next code.
        int maxRetries = 5;
        while (employeeRepository.findByEmployeeCode(code).isPresent() && maxRetries-- > 0) {
            code = incrementEmployeeCode(code);
        }

        if (employeeRepository.findByEmployeeCode(code).isPresent()) {
            addFormDataForCreate(ra, fullName, resolvedPhone, position, active, code);
            ra.addFlashAttribute("errorMessage", "Could not generate a unique employee code. Please try again.");
            return "redirect:/employees/add";
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setFullName(fullName);
        employee.setPhone(resolvedPhone);
        employee.setPosition(position);
        employee.setStatus(Boolean.TRUE.equals(active) ? Status.ACTIVE : Status.INACTIVE);

        try {
            employeeRepository.save(employee);
        } catch (DataIntegrityViolationException e) {
            addFormDataForCreate(ra, fullName, resolvedPhone, position, active, generateNextEmployeeCode());
            ra.addFlashAttribute("errorMessage", "Employee code already exists.");
            return "redirect:/employees/add";
        }

        ra.addFlashAttribute("successMessage", "Employee created successfully.");
        return "redirect:/employees";
    }

    private void addFormDataForCreate(RedirectAttributes ra, String fullName, String phone, Position position, Boolean active, String code) {
        ra.addFlashAttribute("formFullName", fullName);
        ra.addFlashAttribute("formPhone", phone);
        ra.addFlashAttribute("formPosition", position);
        ra.addFlashAttribute("formActive", Boolean.TRUE.equals(active));
        ra.addFlashAttribute("formEmployeeCode", code);
    }

    private String generateNextEmployeeCode() {
        Optional<Employee> latest = employeeRepository.findTopByOrderByIdDesc();
        if (latest.isEmpty() || latest.get().getEmployeeCode() == null || latest.get().getEmployeeCode().isBlank()) {
            return "EMP001";
        }
        return incrementEmployeeCode(latest.get().getEmployeeCode().trim());
    }

    private String incrementEmployeeCode(String code) {
        Matcher matcher = Pattern.compile("^(.*?)(\\d+)$").matcher(code);
        if (!matcher.matches()) {
            return "EMP001";
        }

        String prefix = matcher.group(1);
        String numberPart = matcher.group(2);
        int width = numberPart.length();
        int next = Integer.parseInt(numberPart) + 1;
        return prefix + String.format("%0" + width + "d", next);
    }

    @GetMapping("/employees/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Employee emp = employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee not found"));
            model.addAttribute("emp", emp);
        return "employees/edit";
    }

    @PostMapping("/employees/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String fullname,
            @RequestParam(required = false) String phone,
            @RequestParam Position position,
            @RequestParam(defaultValue = "false") Boolean active,
            RedirectAttributes ra
    ) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String fullName = fullname == null ? "" : fullname.trim();
        String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;

        if (fullName.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Full name is required.");
            return "redirect:/employees/edit/" + id;
        }

        emp.setFullName(fullName);
        emp.setPhone(resolvedPhone);
        emp.setPosition(position);
        emp.setStatus(Boolean.TRUE.equals(active) ? Status.ACTIVE : Status.INACTIVE);

        employeeRepository.save(emp);
        ra.addFlashAttribute("successMessage", "Employee updated successfully.");
        return "redirect:/employees";
    }

    @GetMapping("/employees/delete/{id}")
    public String destroy(@PathVariable Long id, Model model, RedirectAttributes ra){
        if (!employeeRepository.existsById(id)) {
            ra.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/employees";
        }

        employeeRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Employee deleted successfully.");
        return "redirect:/employees";
    }
    
}
