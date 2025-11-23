package artishok.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exhibition_hall_map")
@Getter
@Setter
public class ExhibitionHallMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_event_id", nullable = false)
    private ExhibitionEvent exhibitionEvent;

    @Column(name = "map_image_url")
    private String mapImageUrl;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "exhibitionHallMap", cascade = CascadeType.ALL)
    private List<ExhibitionStand> stands = new ArrayList<>();

}