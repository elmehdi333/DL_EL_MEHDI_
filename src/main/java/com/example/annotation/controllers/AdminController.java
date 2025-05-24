package com.example.annotation.controllers;

import com.example.annotation.entities.Annotation;

import com.example.annotation.entities.Dataset;
import com.example.annotation.entities.Role;

import com.example.annotation.entities.User;
import com.example.annotation.repositories.AnnotationRepository;
import com.example.annotation.repositories.DatasetRepository;
import com.example.annotation.repositories.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageImpl; // Make sure you import this
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.pdf.parser.clipper.Paths;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private DatasetRepository datasetRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AnnotationRepository annotationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    




    @GetMapping("/datasets")
    public String listDatasets(Model model) {
        model.addAttribute("datasets", datasetRepository.findAll());
        return "admin/datasets";
    }

    @GetMapping("/datasets/create")
    public String createForm(Model model) {
        Dataset dataset = new Dataset();
        model.addAttribute("dataset", dataset);
        return "admin/createDataset";
    }
    @PostMapping("/datasets")
    @Transactional
    public String saveDataset(@ModelAttribute Dataset dataset,
                              @RequestParam(name = "file") MultipartFile file,
                              Authentication auth) throws IOException {

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        if (dataset.getDeadline() == null || !dataset.getDeadline().isAfter(LocalDate.now())) {
            return "redirect:/admin/datasets/create?error=invalid_deadline";
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File folder = new File(uploadDir);
        if (!folder.exists()) folder.mkdirs();

        String filePath = uploadDir + file.getOriginalFilename();
        file.transferTo(new File(filePath));

        dataset.setFilePath(filePath);
        dataset.setCreatedBy(user);
        Dataset savedDataset = datasetRepository.save(dataset);

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.lines().skip(1).forEach(line -> {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    Annotation annotation = new Annotation();
                    annotation.setText1(parts[0].trim());
                    annotation.setText2(parts[1].trim());
                    annotation.setStatus("unlabeled");
                    annotation.setProgressPercentage(0);
                    annotation.setAssignedTo(null);
                    annotation.setDataset(savedDataset);
                    annotationRepository.save(annotation);
                }
            });
        }

        return "redirect:/admin/datasets";
    }

    // ---------- EDIT DATASET ----------
//    @PostMapping("/datasets/{id}/edit")
//    @Transactional
//    public String updateDataset(@PathVariable Long id,
//                                @ModelAttribute Dataset dataset,
//                                @RequestParam(name = "file", required = false) MultipartFile file,
//                                RedirectAttributes redirectAttributes) throws IOException {
//
//        Dataset existing = datasetRepository.findById(id).orElseThrow();
//
//        if (dataset.getDeadline() == null || !dataset.getDeadline().isAfter(LocalDate.now())) {
//            redirectAttributes.addFlashAttribute("error", "Deadline must be after today.");
//            return "redirect:/admin/datasets/" + id + "/edit";
//        }
//
//        existing.setName(dataset.getName());
//        existing.setDescription(dataset.getDescription());
//        existing.setClasses(dataset.getClasses());
//        existing.setDeadline(dataset.getDeadline());
//
//        if (file != null && !file.isEmpty()) {
//            String uploadDir = System.getProperty("user.dir") + "/uploads/";
//            File folder = new File(uploadDir);
//            if (!folder.exists()) folder.mkdirs();
//
//            String filePath = uploadDir + file.getOriginalFilename();
//            file.transferTo(new File(filePath));
//
//            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//                reader.lines().skip(1).forEach(line -> {
//                    String[] parts = line.split(",");
//                    if (parts.length >= 2) {
//                        Annotation annotation = new Annotation();
//                        annotation.setText1(parts[0].trim());
//                        annotation.setText2(parts[1].trim());
//                        annotation.setStatus("unlabeled");
//                        annotation.setProgressPercentage(0);
//                        annotation.setAssignedTo(null);
//                        annotation.setDataset(existing);
//                        annotationRepository.save(annotation);
//                    }
//                });
//            }
//        }
//
//        datasetRepository.save(existing);
//        return "redirect:/admin/datasets";
//    }

    // ---------- DELETE DATASET ----------
    @GetMapping("/datasets/{id}/delete")
    @Transactional
    public String deleteDataset(@PathVariable Long id) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        annotationRepository.deleteAll(dataset.getAnnotations());
        datasetRepository.delete(dataset);
        return "redirect:/admin/datasets";
    }

//    @PostMapping("/datasets")
//    @Transactional
//    public String saveDataset(@ModelAttribute Dataset dataset,
//                              @RequestParam(name = "file") MultipartFile file,
//                              Authentication auth) throws IOException {
//
//        String username = auth.getName();
//        User user = userRepository.findByUsername(username).orElseThrow();
//
//        // ✅ Block if deadline is null or before tomorrow
//        if (dataset.getDeadline() == null || !dataset.getDeadline().isAfter(LocalDate.now())) {
//            return "redirect:/admin/datasets/create?error=invalid_deadline";
//        }
//
//        String uploadDir = System.getProperty("user.dir") + "/uploads/";
//        File folder = new File(uploadDir);
//        if (!folder.exists()) folder.mkdirs();
//
//        String filePath = uploadDir + file.getOriginalFilename();
//        file.transferTo(new File(filePath));
//
//        dataset.setFilePath(filePath);
//        dataset.setCreatedBy(user);
//
//        Dataset savedDataset = datasetRepository.save(dataset);
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//            reader.lines().skip(1).forEach(line -> {
//                String[] parts = line.split(",");
//                if (parts.length >= 2) {
//                    Annotation annotation = new Annotation();
//                    annotation.setText1(parts[0].trim());
//                    annotation.setText2(parts[1].trim());
//                    annotation.setStatus("unlabeled");
//                    annotation.setProgressPercentage(0);
//                    annotation.setAssignedTo(null); // No auto-assign
//                    annotation.setDataset(savedDataset);
//                    annotationRepository.save(annotation);
//                }
//            });
//        }
//
//        return "redirect:/admin/datasets";
//    }
//
    @GetMapping("/datasets/{id}/edit")
    public String editDatasetForm(@PathVariable Long id, Model model) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        model.addAttribute("dataset", dataset);
        return "admin/editDataset";
    }
    @PostMapping("/datasets/{id}/edit")
    @Transactional
    public String updateDataset(@PathVariable Long id,
                                @ModelAttribute Dataset dataset,
                                @RequestParam(name = "file", required = false) MultipartFile file,
                                RedirectAttributes redirectAttributes) throws IOException {

        Dataset existing = datasetRepository.findById(id).orElseThrow();

        // ✅ Validate deadline: must be after today
        if (dataset.getDeadline() == null || !dataset.getDeadline().isAfter(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "Deadline must be after today.");
            return "redirect:/admin/datasets/" + id + "/edit";
        }

        existing.setName(dataset.getName());
        existing.setDescription(dataset.getDescription());
        existing.setClasses(dataset.getClasses());
        existing.setDeadline(dataset.getDeadline());

        if (file != null && !file.isEmpty()) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File folder = new File(uploadDir);
            if (!folder.exists()) folder.mkdirs();

            String filePath = uploadDir + file.getOriginalFilename();
            file.transferTo(new File(filePath));

            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                reader.lines().skip(1).forEach(line -> {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        Annotation annotation = new Annotation();
                        annotation.setText1(parts[0].trim());
                        annotation.setText2(parts[1].trim());
                        annotation.setStatus("unlabeled");
                        annotation.setDataset(existing);
                        annotation.setAssignedTo(null);
                        annotationRepository.save(annotation);
                    }
                });
            }
        }

        datasetRepository.save(existing);
        return "redirect:/admin/datasets";
    }


    @PostMapping("/datasets/{id}/assign-manual")
    @Transactional
    public String assignManual(@PathVariable Long id,
                               @RequestParam List<Long> annotationIds,
                               @RequestParam List<Long> assignedUserIds) {

        for (int i = 0; i < annotationIds.size(); i++) {
            Long annotationId = annotationIds.get(i);
            Long userId = assignedUserIds.get(i);
            Optional<Annotation> optionalAnnotation = annotationRepository.findById(annotationId);
            Optional<User> optionalUser = userRepository.findById(userId);

            if (optionalAnnotation.isPresent() && optionalUser.isPresent()) {
                Annotation annotation = optionalAnnotation.get();
                annotation.setAssignedTo(optionalUser.get());
                annotationRepository.save(annotation);
            }
        }

        return "redirect:/admin/datasets/" + id;
    }

    @GetMapping("/datasets/{id}/assign-manual")
    public String manualAssignPage(@PathVariable Long id, Model model) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        List<User> annotators = userRepository.findByRole(Role.annotator);
        List<Annotation> unassignedAnnotations = annotationRepository.findByDatasetAndAssignedToIsNull(dataset);

        model.addAttribute("dataset", dataset);
        model.addAttribute("unassignedAnnotations", unassignedAnnotations);
        model.addAttribute("annotators", annotators);
        return "admin/manualAssign";
    }




    
    @GetMapping("/datasets/{id}")
    public String viewDetails(@PathVariable Long id,
                              @RequestParam(name = "tab", defaultValue = "unlabeled") String tab,
                              @RequestParam(name = "page", defaultValue = "0") String rawPage,
                              @RequestParam(name = "size", defaultValue = "5") String rawSize,
                              @RequestParam(required = false) String progressFilter,
                              @RequestParam(required = false) String statusFilter,
                              @RequestParam(required = false) String search,
                              Model model) throws JsonProcessingException {

        int page = 0;
        int size = 5;

        try {
            page = Integer.parseInt(rawPage.trim());
        } catch (NumberFormatException ignored) {}

        try {
            size = Integer.parseInt(rawSize.trim());
        } catch (NumberFormatException ignored) {}

        final String searchTerm = (search == null || "null".equalsIgnoreCase(search.trim()) || search.trim().isEmpty())
                                    ? null : search.trim();

        Dataset dataset = datasetRepository.findById(id).orElseThrow();

        List<Annotation> all = annotationRepository.findByDataset(dataset).stream()
                .filter(a -> a.getStatus() != null && a.getStatus().equalsIgnoreCase(tab))
                .toList();

        if (statusFilter != null && !statusFilter.isBlank()) {
            all = all.stream().filter(a -> a.getStatus().equalsIgnoreCase(statusFilter)).toList();
        }

        if (progressFilter != null) {
            all = all.stream().filter(a -> {
                int p = a.getProgressPercentage() != null ? a.getProgressPercentage() : 0;
                return switch (progressFilter) {
                    case "0-50" -> p <= 50;
                    case "51-99" -> p > 50 && p < 100;
                    case "100" -> p == 100;
                    default -> true;
                };
            }).toList();
        }

        if (searchTerm != null) {
            all = all.stream().filter(a ->
                (a.getText1() != null && a.getText1().toLowerCase().contains(searchTerm.toLowerCase())) ||
                (a.getText2() != null && a.getText2().toLowerCase().contains(searchTerm.toLowerCase()))
            ).toList();
        }

        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<Annotation> pageContent = all.subList(from, to);
        Page<Annotation> annotationPage = new PageImpl<>(pageContent, PageRequest.of(page, size), total);

        Map<String, Double> avgProgressMap = annotationRepository.findByDataset(dataset).stream()
        	    .filter(a -> a.getAssignedTo() != null)
        	    .filter(a -> a.getAssignedTo().getUsername() != null && !a.getAssignedTo().getUsername().isBlank())
        	    .filter(a -> a.getProgressPercentage() != null)
        	    .collect(Collectors.groupingBy(
        	        a -> a.getAssignedTo().getUsername(),
        	        Collectors.averagingInt(Annotation::getProgressPercentage)
        	    ));

        	String avgProgressJson = new ObjectMapper().writeValueAsString(avgProgressMap);
        	model.addAttribute("avgProgressJson", avgProgressJson);


        model.addAttribute("dataset", dataset);
        model.addAttribute("annotators", userRepository.findByRole(Role.annotator));
        model.addAttribute("annotations", annotationPage.getContent());
        model.addAttribute("annotationsPage", annotationPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", annotationPage.getTotalPages());
        model.addAttribute("activeTab", tab);
        model.addAttribute("progressFilter", progressFilter);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("search", searchTerm);
        model.addAttribute("avgProgressJson", avgProgressJson);

        return "admin/viewDataset";
    }
    private int parseIntOrDefault(String raw, int defaultValue) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @PostMapping("/datasets/{id}/assign")
    public String assignAnnotators(@PathVariable(name = "id") Long id,
                                   @RequestParam(name = "annotatorIds") List<Long> annotatorIds) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        if (dataset.getAnnotators() == null)
            dataset.setAnnotators(new HashSet<>());

        annotatorIds.forEach(uid ->
                userRepository.findById(uid).ifPresent(dataset.getAnnotators()::add));
        datasetRepository.save(dataset);
        return "redirect:/admin/datasets/" + id;
    }

    @GetMapping("/datasets/{id}/unassign/{userId}")
    public String unassignAnnotator(@PathVariable(name = "id") Long id,
                                    @PathVariable(name = "userId") Long userId) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        dataset.getAnnotators().removeIf(u -> u.getId().equals(userId));
        datasetRepository.save(dataset);
        return "redirect:/admin/datasets/" + id;
    }

    @GetMapping("/annotations")
    public String allAnnotations(Model model,
                                 @RequestParam(name = "page", defaultValue = "0") int page,
                                 @RequestParam(name = "keyword", defaultValue = "") String keyword) {

        List<Annotation> all = annotationRepository.findAll();
        if (!keyword.isBlank()) {
            all = all.stream()
                    .filter(a -> a.getText1().contains(keyword) || a.getText2().contains(keyword))
                    .collect(Collectors.toList());
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) all.size() / pageSize);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        List<Annotation> pageItems = all.subList(fromIndex, toIndex);

        model.addAttribute("annotations", pageItems);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        return "admin/allAnnotations";
    }

    @PostMapping("/annotations/{id}/status")
    public String updateAnnotationStatus(@PathVariable(name = "id") Long id,
                                         @RequestParam(name = "status") String status) {
        Annotation annotation = annotationRepository.findById(id).orElseThrow();
        annotation.setStatus(status);
        annotationRepository.save(annotation);
        return "redirect:/admin/annotations";
    }

    @GetMapping("/annotations/{id}/edit")
    public String editAnnotationForm(@PathVariable(name = "id") Long id, Model model) {
        Annotation annotation = annotationRepository.findById(id).orElseThrow();
        model.addAttribute("annotation", annotation);
        model.addAttribute("users", userRepository.findByRole(Role.annotator));
        return "admin/editAnnotation";
    }


    @PostMapping("/datasets/{id}/update-annotations")
    @Transactional
    public String updateAnnotations(@PathVariable Long id,
                                    @RequestParam(name = "annotationIds", required = false) List<Long> annotationIds,
                                    @RequestParam(name = "statuses", required = false) List<String> statuses,
                                    @RequestParam(name = "assignedUserIds", required = false) List<Long> assignedUserIds,
                                    @RequestParam String currentTab) {

        if (annotationIds != null && statuses != null && assignedUserIds != null) {
            for (int i = 0; i < annotationIds.size(); i++) {
                Annotation annotation = annotationRepository.findById(annotationIds.get(i)).orElseThrow();
                annotation.setStatus(statuses.get(i));
                Long userId = assignedUserIds.get(i);
                annotation.setAssignedTo(userId != null && userId > 0
                    ? userRepository.findById(userId).orElse(null)
                    : null);
                annotationRepository.save(annotation); // ✅ must be saved
            }
        }

        return "redirect:/admin/datasets/" + id + "?tab=" + currentTab;
    }



    @GetMapping("/datasets/{id}/confirm-delete")
    public String confirmDeleteDataset(@PathVariable Long id, Model model) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        model.addAttribute("dataset", dataset);
        return "admin/confirmDeleteDataset";
    }



//    @GetMapping("/datasets/{id}/delete")
//    public String deleteDataset(@PathVariable Long id) {
//        Dataset dataset = datasetRepository.findById(id).orElseThrow();
//        annotationRepository.deleteAll(dataset.getAnnotations()); // clean up annotations
//        datasetRepository.deleteById(id);
//        return "redirect:/admin/datasets";
//    }
    @GetMapping("/datasets/{id}/manual-assign")
    public String showManualAssignForm(@PathVariable Long id, Model model) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        List<Annotation> annotations = annotationRepository.findByDataset(dataset);
        List<User> annotators = userRepository.findByRole(Role.annotator);

        model.addAttribute("dataset", dataset);
        model.addAttribute("annotations", annotations);
        model.addAttribute("annotators", annotators);
        return "admin/manualAssign";
    }

    @PostMapping("/datasets/{id}/manual-assign")
    public String handleManualAssignment(@PathVariable Long id,
                                         @RequestParam Map<String, String> params) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        List<Annotation> annotations = annotationRepository.findByDataset(dataset);

        for (Annotation annotation : annotations) {
            String key = "assign_" + annotation.getId();
            if (params.containsKey(key)) {
                Long annotatorId = Long.parseLong(params.get(key));
                userRepository.findById(annotatorId).ifPresent(annotation::setAssignedTo);
            }
        }

        annotationRepository.saveAll(annotations);
        return "redirect:/admin/datasets/" + id;
    }



    @GetMapping("/annotations/export")
    public void exportAnnotations(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"annotations.csv\"");
        PrintWriter writer = response.getWriter();
        writer.println("Dataset,Text1,Text2,AssignedTo,Status");

        for (Annotation a : annotationRepository.findAll()) {
            writer.println(String.format("%s,%s,%s,%s,%s",
                    a.getDataset().getName(),
                    a.getText1(),
                    a.getText2(),
                    a.getAssignedTo() != null ? a.getAssignedTo().getUsername() : "",
                    a.getStatus()));
        }

        writer.flush();
        writer.close();
    }


    @GetMapping("/datasets/{id}/unassigned")
    public String viewUnassignedAnnotations(@PathVariable Long id,
                                            @RequestParam(defaultValue = "") String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            Model model) {
        Dataset dataset = datasetRepository.findById(id).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        List<Annotation> all = annotationRepository.findByDatasetAndAssignedToIsNull(dataset);

        if (!keyword.isBlank()) {
            all = all.stream().filter(a ->
                a.getText1().contains(keyword) || a.getText2().contains(keyword)
            ).toList();
        }

        int total = all.size();
        int from = page * size;
        int to = Math.min(from + size, total);
        List<Annotation> pageItems = all.subList(from, to);

        model.addAttribute("dataset", dataset);
        model.addAttribute("annotations", pageItems);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (int)Math.ceil((double)total / size));
        model.addAttribute("keyword", keyword);
        return "admin/unassignedAnnotations";
    } 

    @PostMapping("/datasets/preview")
    public String handlePreviewUpload(@RequestParam("previewFile") MultipartFile previewFile,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) throws IOException {
        // Validate file is not empty
        if (previewFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No file selected.");
            return "redirect:/admin/datasets/create";
        }

        List<List<String>> allLines = new ArrayList<>();

        // Read uploaded CSV file line by line
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(previewFile.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(Arrays.asList(line.split(",")));
            }
        }

        if (allLines.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Uploaded file is empty.");
            return "redirect:/admin/datasets/create";
        }

        // Save parsed lines in session for paging
        session.setAttribute("previewLines", allLines);
        return "redirect:/admin/datasets/preview?page=0";
    }
    @GetMapping("/datasets/preview")
    public String renderPreviewPage(@RequestParam(defaultValue = "0") int page,
                                    HttpSession session,
                                    Model model) {
        List<List<String>> allLines = (List<List<String>>) session.getAttribute("previewLines");

        if (allLines == null || allLines.isEmpty()) {
            model.addAttribute("header", List.of());
            model.addAttribute("rows", List.of());
            model.addAttribute("error", "No preview data found.");
            return "admin/createDataset";
        }

        int pageSize = 10;
        int totalRows = allLines.size() - 1; // exclude header
        int totalPages = (int) Math.ceil((double) totalRows / pageSize);

        // Clamp page number within bounds
        page = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = 1 + page * pageSize; // skip header
        int toIndex = Math.min(fromIndex + pageSize, allLines.size());

        List<String> header = allLines.get(0);
        List<List<String>> rows = fromIndex < toIndex ? allLines.subList(fromIndex, toIndex) : List.of();

        model.addAttribute("dataset", new Dataset()); // Keeps the form working
        model.addAttribute("header", header);
        model.addAttribute("rows", rows);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "admin/createDataset";
    }



}
