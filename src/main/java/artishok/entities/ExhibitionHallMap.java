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

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "exhibition_event_id", nullable = false)
	private ExhibitionEvent exhibitionEvent;

	@Column(name = "map_image_url")
	private String mapImageUrl;

	// Обратная связь к стендам
	@OneToMany(mappedBy = "exhibitionHallMap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<ExhibitionStand> exhibitionStands = new ArrayList<>();

	// Геттеры и сеттеры
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExhibitionEvent getExhibitionEvent() {
		return exhibitionEvent;
	}

	public void setExhibitionEvent(ExhibitionEvent exhibitionEvent) {
		this.exhibitionEvent = exhibitionEvent;
	}

	public String getMapImageUrl() {
		return mapImageUrl;
	}

	public void setMapImageUrl(String mapImageUrl) {
		this.mapImageUrl = mapImageUrl;
	}

	public List<ExhibitionStand> getExhibitionStands() {
		return exhibitionStands;
	}

	public void setExhibitionStands(List<ExhibitionStand> exhibitionStands) {
		this.exhibitionStands = exhibitionStands;
	}

	// Метод для добавления стенда
	public void addExhibitionStand(ExhibitionStand stand) {
		exhibitionStands.add(stand);
		stand.setExhibitionHallMap(this);
	}

	// Метод для удаления стенда
	public void removeExhibitionStand(ExhibitionStand stand) {
		exhibitionStands.remove(stand);
		stand.setExhibitionHallMap(null);
	}
}