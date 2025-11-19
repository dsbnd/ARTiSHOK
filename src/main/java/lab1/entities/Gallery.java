package lab1.entities;


import jakarta.persistence.*;
import lab1.entities.enums.GalleryStatus;

@Entity
@Table(name = "gallery")
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

//
}
