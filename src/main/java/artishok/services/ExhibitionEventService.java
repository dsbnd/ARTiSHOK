package artishok.services;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import artishok.entities.ExhibitionEvent;
import artishok.repositories.ExhibitionEventRepository;

@Service
@Transactional
public class ExhibitionEventService {
    private final ExhibitionEventRepository exhibitionEventRepository;

    public ExhibitionEventService(ExhibitionEventRepository exhibitionEventRepository) {
        this.exhibitionEventRepository = exhibitionEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ExhibitionEvent> getAllExhibitionEvents() {
        return exhibitionEventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ExhibitionEvent> getExhibitionEventById(Long id) {
        return exhibitionEventRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ExhibitionEvent> getExhibitionEventByIdWithDetails(Long id) {
        // Если нужно загрузить связанные данные
        return exhibitionEventRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ExhibitionEvent> getExhibitionEventsByGalleryId(Long galleryId) {
        return exhibitionEventRepository.findByGalleryId(galleryId);
    }

    @Transactional
    public ExhibitionEvent saveExhibitionEvent(ExhibitionEvent exhibitionEvent) {
        return exhibitionEventRepository.save(exhibitionEvent);
    }

    @Transactional
    public void deleteExhibitionEvent(Long id) {
        exhibitionEventRepository.deleteById(id);
    }
}