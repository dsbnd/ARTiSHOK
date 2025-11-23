package artishok.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import artishok.entities.enums.StandStatus;
import artishok.entities.enums.StandType;

@Entity
@Table(name = "exhibition_stand")
@Getter
@Setter
public class ExhibitionStand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_hall_map_id", nullable = false)
    private ExhibitionHallMap exhibitionHallMap;

    @Column(name = "stand_number", nullable = false)
    private String standNumber;

    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StandType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StandStatus status = StandStatus.AVAILABLE;

    @OneToMany(mappedBy = "exhibitionStand")
    private List<Booking> bookings = new ArrayList<>();

}