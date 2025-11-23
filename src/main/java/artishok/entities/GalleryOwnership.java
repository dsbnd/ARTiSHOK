package artishok.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "gallery_ownership")
@Data
public class GalleryOwnership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gallery_id", nullable = false)
    private Gallery gallery;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Column(name= "is_primary", nullable=false)
    private Boolean isPrimary = false;
    @Column(name= "verification_code", nullable=false)
    private Integer verificationCode;
    @Column(name = "code_expiry")
    private LocalDateTime codeExpiry;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
