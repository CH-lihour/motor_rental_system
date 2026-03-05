package com.example.motor_retal.controllers;

import com.example.motor_retal.models.motorcycles.Motorcycle;
import com.example.motor_retal.models.motorcycles.MotorcycleStatus;
import com.example.motor_retal.models.motorcycles.MotorcycleType;
import com.example.motor_retal.repositories.MotorcycleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class MotorcycleController {
    private final MotorcycleRepository motorcycleRepository;

    public MotorcycleController(MotorcycleRepository motorcycleRepository) {
        this.motorcycleRepository = motorcycleRepository;
    }

    @GetMapping("/motorcycles")
    public String index(Model model) {
        model.addAttribute("motorcycles", motorcycleRepository.findAll());
        return "motorcycles/index";
    }

    @GetMapping("/motorcycles/add")
    public String create(Model model) {
        model.addAttribute("types", MotorcycleType.values());
        model.addAttribute("statuses", MotorcycleStatus.values());
        return "motorcycles/create";
    }

    @PostMapping("/motorcycles/add")
    public String store(
            @RequestParam String plate_number,
            @RequestParam String model,
            @RequestParam MotorcycleType type,
            @RequestParam(required = false) Integer engine_cc,
            @RequestParam BigDecimal daily_price,
            @RequestParam(defaultValue = "AVAILABLE") MotorcycleStatus status,
            @RequestParam(required = false) String note,
            RedirectAttributes ra
    ) {
        String plateNumber = plate_number == null ? "" : plate_number.trim();
        String modelName = model == null ? "" : model.trim();
        String resolvedNote = (note != null && !note.isBlank()) ? note.trim() : null;

        if (plateNumber.isBlank() || modelName.isBlank() || daily_price == null || daily_price.signum() <= 0) {
            ra.addFlashAttribute("errorMessage", "Plate number, model, and a positive daily price are required.");
            return "redirect:/motorcycles/add";
        }

        if (motorcycleRepository.existsByPlateNumberIgnoreCase(plateNumber)) {
            ra.addFlashAttribute("errorMessage", "Plate number already exists.");
            return "redirect:/motorcycles/add";
        }

        Motorcycle motorcycle = new Motorcycle();
        motorcycle.setPlateNumber(plateNumber);
        motorcycle.setModel(modelName);
        motorcycle.setType(type);
        motorcycle.setEngineCc(engine_cc);
        motorcycle.setDailyPrice(daily_price);
        motorcycle.setStatus(status);
        motorcycle.setNote(resolvedNote);

        try {
            motorcycleRepository.save(motorcycle);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not save motorcycle. Please try again.");
            return "redirect:/motorcycles/add";
        }

        ra.addFlashAttribute("successMessage", "Motorcycle created successfully.");
        return "redirect:/motorcycles";
    }

    @GetMapping("/motorcycles/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Motorcycle motorcycle = motorcycleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found"));
        model.addAttribute("motorcycle", motorcycle);
        model.addAttribute("types", MotorcycleType.values());
        model.addAttribute("statuses", MotorcycleStatus.values());
        return "motorcycles/edit";
    }

    @PostMapping("/motorcycles/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String plate_number,
            @RequestParam String model,
            @RequestParam MotorcycleType type,
            @RequestParam(required = false) Integer engine_cc,
            @RequestParam BigDecimal daily_price,
            @RequestParam(defaultValue = "AVAILABLE") MotorcycleStatus status,
            @RequestParam(required = false) String note,
            RedirectAttributes ra
    ) {
        Motorcycle motorcycle = motorcycleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Motorcycle not found"));

        String plateNumber = plate_number == null ? "" : plate_number.trim();
        String modelName = model == null ? "" : model.trim();
        String resolvedNote = (note != null && !note.isBlank()) ? note.trim() : null;

        if (plateNumber.isBlank() || modelName.isBlank() || daily_price == null || daily_price.signum() <= 0) {
            ra.addFlashAttribute("errorMessage", "Plate number, model, and a positive daily price are required.");
            return "redirect:/motorcycles/edit/" + id;
        }

        Optional<Motorcycle> existing = motorcycleRepository.findByPlateNumberIgnoreCase(plateNumber);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            ra.addFlashAttribute("errorMessage", "Plate number already exists.");
            return "redirect:/motorcycles/edit/" + id;
        }

        motorcycle.setPlateNumber(plateNumber);
        motorcycle.setModel(modelName);
        motorcycle.setType(type);
        motorcycle.setEngineCc(engine_cc);
        motorcycle.setDailyPrice(daily_price);
        motorcycle.setStatus(status);
        motorcycle.setNote(resolvedNote);

        try {
            motorcycleRepository.save(motorcycle);
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("errorMessage", "Could not update motorcycle. Please try again.");
            return "redirect:/motorcycles/edit/" + id;
        }

        ra.addFlashAttribute("successMessage", "Motorcycle updated successfully.");
        return "redirect:/motorcycles";
    }

    @GetMapping("/motorcycles/delete/{id}")
    public String destroy(@PathVariable Long id, RedirectAttributes ra) {
        if (!motorcycleRepository.existsById(id)) {
            ra.addFlashAttribute("errorMessage", "Motorcycle not found.");
            return "redirect:/motorcycles";
        }

        motorcycleRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Motorcycle deleted successfully.");
        return "redirect:/motorcycles";
    }
}
