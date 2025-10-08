package com.metalworkshop.PatientService.controller;

import com.metalworkshop.PatientService.dto.PatientRequestDto;
import com.metalworkshop.PatientService.dto.PatientResponseDto;
import com.metalworkshop.PatientService.dto.validators.UpdatePatientValidationGroup;
import com.metalworkshop.PatientService.repository.PatientRepository;
import com.metalworkshop.PatientService.service.PatientService;
import jakarta.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    @Autowired
    public PatientController(PatientService patientService,
                             PatientRepository patientRepository) {
        this.patientService = patientService;
    }

    @GetMapping()
    public ResponseEntity<List<PatientResponseDto>> findAll() {

        return ResponseEntity.ok().body(patientService.findAll());
    }

    @GetMapping("/{name}")
    public ResponseEntity<PatientResponseDto> findByName(@PathVariable("name") String name) {

        return ResponseEntity.of(patientService.findByName(name));
    }

    @PostMapping
    public ResponseEntity<PatientResponseDto> createPatient(
            @Validated(Default.class) @RequestBody PatientRequestDto requestDto) {

        return ResponseEntity.ok().body(patientService.createPatient(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDto> updatePatient(
            @PathVariable("id") UUID id,
            @Validated(UpdatePatientValidationGroup.class) @RequestBody PatientRequestDto requestDto) {
        PatientResponseDto patientResponseDto = patientService.updatePatient(id, requestDto);

        return ResponseEntity.ok().body(patientResponseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePatientById(@PathVariable("id") UUID id) {
        patientService.deleteByUUID(id);

        return ResponseEntity.ok().build();
    }
}
