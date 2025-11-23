package artishok.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import artishok.entities.enums.GalleryStatus;

@Entity
@Table(name = "gallery")
@Getter
@Setter
public class Gallery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    @Column(nullable = false)
    private String address;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "logo_url")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GalleryStatus status = GalleryStatus.PENDING;

    @Column(name = "admin_comment")
    private String adminComment;

    @OneToMany(mappedBy = "gallery", cascade = CascadeType.ALL)
    private List<GalleryOwnership> ownerships = new ArrayList<>();

    @OneToMany(mappedBy = "gallery", cascade = CascadeType.ALL)
    private List<ExhibitionEvent> exhibitions = new ArrayList<>();
}
