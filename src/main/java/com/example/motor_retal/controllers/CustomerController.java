package com.example.motor_retal.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.motor_retal.models.customers.Customer;
import com.example.motor_retal.repositories.CustomerRepository;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class CustomerController {
    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }
    
    @GetMapping("/customers")
    public String index(Model model) {
        model.addAttribute("customers", customerRepository.findAll());

        return "customers/index";
    }
    
    @GetMapping("/customers/add")
    public String create() {
        return "customers/create";
    }

    @PostMapping("/customers/add")
    public String store(
            @RequestParam String name,
            @RequestParam(required = false) String id_number,
            @RequestParam String phone,
            @RequestParam(required = false) String note,
            RedirectAttributes ra
    ) {
        String resolvedName = name == null ? "" : name.trim();
        String resolvedPhone = phone == null ? "" : phone.trim();
        String resolvedIdNumber = (id_number != null && !id_number.isBlank()) ? id_number.trim() : null;
        String resolvedNote = (note != null && !note.isBlank()) ? note.trim() : null;

        if (resolvedName.isBlank() || resolvedPhone.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Name and phone are required.");
            return "redirect:/customers/add";
        }

        Customer customer = new Customer();
        customer.setName(resolvedName);
        customer.setPhone(resolvedPhone);
        customer.setIdNumber(resolvedIdNumber);
        customer.setNote(resolvedNote);
        customer.setBlacklisted(false);

        try {
            customerRepository.save(customer);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not save customer. Please check your input and try again.");
            return "redirect:/customers/add";
        }

        ra.addFlashAttribute("successMessage", "Customer created successfully.");
        return "redirect:/customers";
    }

    @GetMapping("/customers/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        model.addAttribute("customer", customer);
        return "customers/edit";
    }

    @PostMapping("/customers/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String id_number,
            @RequestParam String phone,
            @RequestParam(required = false) String note,
            RedirectAttributes ra
    ) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String resolvedName = name == null ? "" : name.trim();
        String resolvedPhone = phone == null ? "" : phone.trim();
        String resolvedIdNumber = (id_number != null && !id_number.isBlank()) ? id_number.trim() : null;
        String resolvedNote = (note != null && !note.isBlank()) ? note.trim() : null;

        if (resolvedName.isBlank() || resolvedPhone.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Name and phone are required.");
            return "redirect:/customers/edit/" + id;
        }

        customer.setName(resolvedName);
        customer.setPhone(resolvedPhone);
        customer.setIdNumber(resolvedIdNumber);
        customer.setNote(resolvedNote);

        try {
            customerRepository.save(customer);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not update customer. Please check your input and try again.");
            return "redirect:/customers/edit/" + id;
        }

        ra.addFlashAttribute("successMessage", "Customer updated successfully.");
        return "redirect:/customers";
    }

    @GetMapping("/customers/delete/{id}")
    public String destroy(@PathVariable Long id, RedirectAttributes ra) {
        if (!customerRepository.existsById(id)) {
            ra.addFlashAttribute("errorMessage", "Customer not found.");
            return "redirect:/customers";
        }

        customerRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Customer deleted successfully.");
        return "redirect:/customers";
    }
    
}
