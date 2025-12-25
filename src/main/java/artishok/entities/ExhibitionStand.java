package artishok.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
	@JsonIgnoreProperties({"exhibitionStands"})
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

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StandType type;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StandStatus status = StandStatus.AVAILABLE;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ExhibitionHallMap getExhibitionHallMap() {
		return exhibitionHallMap;
	}

	public void setExhibitionHallMap(ExhibitionHallMap exhibitionHallMap) {
		this.exhibitionHallMap = exhibitionHallMap;
	}

	public String getStandNumber() {
		return standNumber;
	}

	public void setStandNumber(String standNumber) {
		this.standNumber = standNumber;
	}

	public Integer getPositionX() {
		return positionX;
	}

	public void setPositionX(Integer positionX) {
		this.positionX = positionX;
	}

	public Integer getPositionY() {
		return positionY;
	}

	public void setPositionY(Integer positionY) {
		this.positionY = positionY;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public StandType getType() {
		return type;
	}

	public void setType(StandType type) {
		this.type = type;
	}

	public StandStatus getStatus() {
		return status;
	}

	public void setStatus(StandStatus status) {
		this.status = status;
	}



}