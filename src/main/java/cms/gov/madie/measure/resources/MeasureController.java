package cms.gov.madie.measure.resources;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cms.gov.madie.measure.models.Measure;
import cms.gov.madie.measure.repositories.MeasureRepository;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MeasureController {

  @Autowired private final MeasureRepository repository;

  @GetMapping("/measures")
  public ResponseEntity<List<Measure>> getMeasures() {
    List<Measure> measures = repository.findAll();
    return ResponseEntity.ok(measures);
  }

  @GetMapping("/measures/{id}")
  public ResponseEntity<Measure> getMeasure(@PathVariable("id") String id) {
    Optional<Measure> measure = repository.findById(id);
    return measure
        .map(ResponseEntity::ok)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping("/measure")
  public ResponseEntity<Measure> addMeasure(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure) {

    checkDuplicateCqlLibraryName(measure.getCqlLibraryName());

    // Clear ID so that the unique GUID from MongoDB will be applied
    measure.setId(null);
    Measure savedMeasure = repository.save(measure);

    return ResponseEntity.status(HttpStatus.CREATED).body(savedMeasure);
  }

  @PutMapping("/measure")
  public ResponseEntity<String> updateMeasure(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure) {
    ResponseEntity<String> response = ResponseEntity.badRequest().body("Measure does not exist.");

    if (measure.getId() != null) {
      Optional<Measure> persistedMeasure = repository.findById(measure.getId());
      if (persistedMeasure.isPresent()) {
        if (isCqlLibraryNameChanged(measure, persistedMeasure)) {
          checkDuplicateCqlLibraryName(measure.getCqlLibraryName());
        }
        repository.save(measure);
        response = ResponseEntity.ok().body("Measure updated successfully.");
      }
    }
    return response;
  }

  private boolean isCqlLibraryNameChanged(Measure measure, Optional<Measure> persistedMeasure) {
    return !Objects.equals(persistedMeasure.get().getCqlLibraryName(), measure.getCqlLibraryName());
  }

  private void checkDuplicateCqlLibraryName(String cqlLibraryName) {
    if (StringUtils.isNotEmpty(cqlLibraryName)
        && repository.findByCqlLibraryName(cqlLibraryName).isPresent()) {
      throw new DuplicateKeyException(
          "cqlLibraryName", "CQL library with given name already exists");
    }
  }
}
