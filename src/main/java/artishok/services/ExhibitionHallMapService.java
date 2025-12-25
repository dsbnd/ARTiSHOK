package artishok.services;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import artishok.entities.ExhibitionEvent;
import artishok.entities.ExhibitionHallMap;
import artishok.repositories.ExhibitionHallMapRepository;
import artishok.repositories.ExhibitionEventRepository;

@Service
@Transactional
public class ExhibitionHallMapService {
    private final ExhibitionEventRepository exhibitionEventRepository;
    private final ExhibitionHallMapRepository exhibitionHallMapRepository;

    public ExhibitionHallMapService(ExhibitionHallMapRepository exhibitionHallMapRepository,
                                    ExhibitionEventRepository exhibitionEventRepository) {
        this.exhibitionHallMapRepository = exhibitionHallMapRepository;
        this.exhibitionEventRepository = exhibitionEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ExhibitionHallMap> getAllExhibitionHallMaps() {
        return exhibitionHallMapRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ExhibitionHallMap> getExhibitionHallMapById(Long id) {
        return exhibitionHallMapRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ExhibitionHallMap> getExhibitionHallMapByIdWithStands(Long id) {
        return exhibitionHallMapRepository.findByIdWithStands(id);
    }

    @Transactional(readOnly = true)
    public List<ExhibitionHallMap> getExhibitionHallMapsByEventId(Long eventId) {
        // Используем оптимизированный запрос
        return exhibitionHallMapRepository.findByExhibitionEventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<ExhibitionHallMap> getExhibitionHallMapsByEventIdWithStands(Long eventId) {
        return exhibitionHallMapRepository.findByExhibitionEventIdWithStands(eventId);
    }

    @Transactional
    public ExhibitionHallMap saveExhibitionHallMap(ExhibitionHallMap exhibitionHallMap) {
        return exhibitionHallMapRepository.save(exhibitionHallMap);
    }

    @Transactional
    public void deleteExhibitionHallMap(Long id) {
        exhibitionHallMapRepository.deleteById(id);
    }

    @Transactional
    public ExhibitionHallMap saveWithEvent(ExhibitionHallMap map, Long eventId) {
        ExhibitionEvent event = exhibitionEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие не найдено с ID: " + eventId));
        map.setExhibitionEvent(event);
        return exhibitionHallMapRepository.save(map);
    }
}