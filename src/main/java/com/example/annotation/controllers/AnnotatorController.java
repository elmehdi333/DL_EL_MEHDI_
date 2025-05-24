package com.example.annotation.controllers;

import com.example.annotation.entities.Annotation;
import com.example.annotation.entities.Role;
import com.example.annotation.entities.User;
import com.example.annotation.repositories.AnnotationRepository;
import com.example.annotation.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/annotator")
public class AnnotatorController {

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('annotator')")
    public String viewTasks(Authentication auth,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String progressFilter,
                            @RequestParam(required = false) String search,
                            @RequestParam(defaultValue = "asc") String sort,
                            Model model) {

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Annotation> all = annotationRepository.findByAssignedTo(user);

        if (progressFilter != null) {
            all = all.stream().filter(a -> {
                int p = a.getProgressPercentage();
                return switch (progressFilter) {
                    case "0-50" -> p <= 50;
                    case "51-99" -> p > 50 && p < 100;
                    case "100" -> p == 100;
                    default -> true;
                };
            }).collect(Collectors.toList());
        }

        final String searchTerm = (search == null || "null".equalsIgnoreCase(search)) ? null : search;
        if (searchTerm != null && !searchTerm.isBlank()) {
            all = all.stream().filter(a ->
                    (a.getText1() != null && a.getText1().toLowerCase().contains(searchTerm.toLowerCase())) ||
                    (a.getText2() != null && a.getText2().toLowerCase().contains(searchTerm.toLowerCase()))
            ).collect(Collectors.toList());
        }

        Comparator<Annotation> comparator = Comparator.comparingInt(Annotation::getProgressPercentage);
        if ("desc".equalsIgnoreCase(sort)) {
            comparator = comparator.reversed();
        }
        all.sort(comparator);

        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        page = Math.max(0, Math.min(page, totalPages - 1));
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<Annotation> pageContent = all.subList(from, to);
        Page<Annotation> annotationPage = new PageImpl<>(pageContent, PageRequest.of(page, size), total);

        long completedCount = all.stream().filter(a -> "labeled".equalsIgnoreCase(a.getStatus())).count();

        Map<LocalDate, Long> completionsPerDay = all.stream()
                .filter(a -> "labeled".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getUpdatedAt() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getUpdatedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        List<String> lineLabels = new ArrayList<>();
        List<Long> lineData = new ArrayList<>();
        LocalDate min = completionsPerDay.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate max = completionsPerDay.keySet().stream().max(LocalDate::compareTo).orElse(LocalDate.now());

        for (LocalDate date = min; !date.isAfter(max); date = date.plusDays(1)) {
            lineLabels.add(date.toString());
            lineData.add(completionsPerDay.getOrDefault(date, 0L));
        }

        model.addAttribute("tasks", annotationPage.getContent());
        model.addAttribute("annotationsPage", annotationPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("progressFilter", progressFilter);
        model.addAttribute("search", searchTerm);
        model.addAttribute("sort", sort);
        model.addAttribute("totalCount", total);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("lineLabels", lineLabels);
        model.addAttribute("lineData", lineData);

        return "annotator/taskList";
    }

    @GetMapping("/taskList")
    public String showTaskList(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<Annotation> tasks = annotationRepository.findByAssignedTo(user);
        model.addAttribute("tasks", tasks);
        return "annotator/taskList";
    }

    @GetMapping("/tasks/{id}")
    public String workOnTask(@PathVariable Long id, Authentication auth, Model model) {
        Annotation annotation = annotationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotation not found"));

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (annotation.getDataset() != null &&
                annotation.getDataset().getDeadline() != null &&
                annotation.getDataset().getDeadline().isBefore(LocalDate.now())) {
            model.addAttribute("deadlinePassed", true);
        }

        List<Annotation> all = annotationRepository.findByAssignedTo(user)
                .stream()
                .sorted(Comparator.comparing(Annotation::getId))
                .toList();

        int index = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }

        Long previousId = (index > 0) ? all.get(index - 1).getId() : null;
        Long nextId = (index < all.size() - 1) ? all.get(index + 1).getId() : null;

        model.addAttribute("annotation", annotation);
        model.addAttribute("previousId", previousId);
        model.addAttribute("nextId", nextId);
        return "annotator/workOnTask";
    }

//    @PostMapping("/tasks/{id}")
//    public String submitAnnotation(@PathVariable Long id,
//                                   @RequestParam String value,
//                                   @RequestParam String status,
//                                   @RequestParam(required = false) Integer progressPercentage,
//                                   @RequestParam(required = false) Boolean similar,
//                                   Authentication auth) {
//        Annotation annotation = annotationRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotation not found"));
//
//        User user = userRepository.findByUsername(auth.getName())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
//
//        if (annotation.getDataset() != null &&
//            annotation.getDataset().getDeadline() != null &&
//            annotation.getDataset().getDeadline().isBefore(LocalDate.now()) &&
//            user.getRole() != Role.admin) {
//            return "redirect:/annotator/tasks?error=deadline";
//        }
//
//        if (annotation.getProgressPercentage() == 100 && user.getRole() != Role.admin) {
//            return "redirect:/annotator/tasks";
//        }
//
//        if ("labeled".equalsIgnoreCase(status)) {
//            progressPercentage = 100;
//        } else if ("unlabeled".equalsIgnoreCase(status)) {
//            progressPercentage = 0;
//        }
//
//        annotation.setAnnotationValue(value);
//        annotation.setStatus(status);
//        annotation.setSimilar(similar);
//
//        if ("in-progress".equalsIgnoreCase(status)) {
//            annotation.setProgressPercentage(progressPercentage != null ? progressPercentage : 0);
//        } else {
//            annotation.setProgressPercentage(progressPercentage);
//        }
//
//        annotation.setUpdatedAt(LocalDateTime.now());
//        annotationRepository.save(annotation);
//
//        List<Annotation> all = annotationRepository.findByAssignedTo(user)
//                .stream()
//                .sorted(Comparator.comparing(Annotation::getId))
//                .toList();
//
//        int index = -1;
//        for (int i = 0; i < all.size(); i++) {
//            if (all.get(i).getId().equals(id)) {
//                index = i;
//                break;
//            }
//        }
//
//        if (index < all.size() - 1) {
//            Long nextId = all.get(index + 1).getId();
//            return "redirect:/annotator/tasks/" + nextId;
//        }
//
//        return "redirect:/annotator/tasks";
//    }
//    @PostMapping("/tasks/{id}")
//    public String submitAnnotation(@PathVariable Long id,
//                                   @RequestParam String value,
//                                   @RequestParam String status,
//                                   @RequestParam(required = false) Integer progressPercentage,
//                                   @RequestParam(required = false) Boolean similar,
//                                   Authentication auth) {
//        Annotation annotation = annotationRepository.findById(id)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotation not found"));
//
//        User user = userRepository.findByUsername(auth.getName())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
//
//        if (annotation.getDataset() != null &&
//            annotation.getDataset().getDeadline() != null &&
//            annotation.getDataset().getDeadline().isBefore(LocalDate.now()) &&
//            user.getRole() != Role.admin) {
//            return "redirect:/annotator/tasks?error=deadline";
//        }
//
//        // Block editing labeled tasks unless admin
//        if (annotation.getProgressPercentage() == 100 && user.getRole() != Role.admin) {
//            return "redirect:/annotator/tasks";
//        }
//
//        // ✅ Validation: must select similarity for 'labeled'
//        if ("labeled".equalsIgnoreCase(status) && similar == null) {
//            return "redirect:/annotator/tasks/" + id + "?error=similarity_required";
//        }
//
//        // Set progress
//        if ("labeled".equalsIgnoreCase(status)) {
//            progressPercentage = 100;
//        } else if ("unlabeled".equalsIgnoreCase(status)) {
//            progressPercentage = 0;
//        }
//
//        annotation.setAnnotationValue(value);
//        annotation.setStatus(status);
//        annotation.setSimilar(similar);
//        annotation.setProgressPercentage(progressPercentage != null ? progressPercentage : 0);
//        annotation.setUpdatedAt(LocalDateTime.now());
//
//        annotationRepository.save(annotation);
//
//        // Redirect to next task
//        List<Annotation> all = annotationRepository.findByAssignedTo(user)
//                .stream()
//                .sorted(Comparator.comparing(Annotation::getId))
//                .toList();
//
//        int index = -1;
//        for (int i = 0; i < all.size(); i++) {
//            if (all.get(i).getId().equals(id)) {
//                index = i;
//                break;
//            }
//        }
//
//        if (index < all.size() - 1) {
//            Long nextId = all.get(index + 1).getId();
//            return "redirect:/annotator/tasks/" + nextId;
//        }
//
//        return "redirect:/annotator/tasks";
//    }
    @PostMapping("/tasks/{id}")
    public String submitAnnotation(@PathVariable Long id,
                                   @RequestParam String value,
                                   @RequestParam String status,
                                   @RequestParam(required = false) Integer progressPercentage,
                                   @RequestParam(required = false) Boolean similar,
                                   Authentication auth) {

        Annotation annotation = annotationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annotation not found"));

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // ⛔ Block if deadline has passed and user is not admin
        if (annotation.getDataset() != null &&
            annotation.getDataset().getDeadline() != null &&
            !annotation.getDataset().getDeadline().isAfter(LocalDate.now()) &&
            user.getRole() != Role.admin) {
            return "redirect:/annotator/tasks?error=deadline";
        }

        // ⛔ Block editing completed tasks for non-admins
        if (annotation.getProgressPercentage() == 100 && user.getRole() != Role.admin) {
            return "redirect:/annotator/tasks";
        }

        // ⛔ Require similarity answer before marking as labeled
        if ("labeled".equalsIgnoreCase(status) && similar == null) {
            return "redirect:/annotator/tasks/" + id + "?error=similarity_required";
        }

        // ✅ Handle progress setting
        if ("labeled".equalsIgnoreCase(status)) {
            progressPercentage = 100;
        } else if ("unlabeled".equalsIgnoreCase(status)) {
            progressPercentage = 0;
        }

        // ✅ Update annotation entity
        annotation.setAnnotationValue(value);
        annotation.setStatus(status);
        annotation.setSimilar(similar);
        annotation.setProgressPercentage(progressPercentage != null ? progressPercentage : 0);
        annotation.setUpdatedAt(LocalDateTime.now());

        annotationRepository.save(annotation);

        // ✅ Navigate to next task if available
        List<Annotation> all = annotationRepository.findByAssignedTo(user)
                .stream()
                .sorted(Comparator.comparing(Annotation::getId))
                .toList();

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) {
                if (i < all.size() - 1) {
                    return "redirect:/annotator/tasks/" + all.get(i + 1).getId();
                }
                break;
            }
        }

        return "redirect:/annotator/tasks";
    }

}
