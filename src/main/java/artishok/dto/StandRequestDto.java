package artishok.dto;

import artishok.entities.enums.StandStatus;
import artishok.entities.enums.StandType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandRequestDto {
    private Long exhibitionHallMapId;
    private String standNumber;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private StandType type;
    private StandStatus status = StandStatus.AVAILABLE;
	public Long getExhibitionHallMapId() {
		return exhibitionHallMapId;
	}
	public void setExhibitionHallMapId(Long exhibitionHallMapId) {
		this.exhibitionHallMapId = exhibitionHallMapId;
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