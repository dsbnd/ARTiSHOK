package artishok.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import artishok.entities.enums.ExhibitionStatus;

@Entity
@Table(name = "exhibition_event")
@Getter
@Setter
public class ExhibitionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExhibitionStatus status = ExhibitionStatus.DRAFT;

    @OneToMany(mappedBy = "exhibitionEvent", cascade = CascadeType.ALL)
    private List<ExhibitionHallMap> hallMaps = new ArrayList<>();


}