package artishok.dto;

import artishok.entities.ExhibitionStand;
import artishok.entities.enums.StandStatus;
import artishok.entities.enums.StandType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StandResponseDto {
    private Long id;
    private Long exhibitionHallMapId;
    private String standNumber;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    private StandType type;
    private StandStatus status;

    public StandResponseDto(ExhibitionStand stand) {
        this.id = stand.getId();
        this.exhibitionHallMapId = stand.getExhibitionHallMap() != null ? stand.getExhibitionHallMap().getId() : null;
        this.standNumber = stand.getStandNumber();
        this.positionX = stand.getPositionX();
        this.positionY = stand.getPositionY();
        this.width = stand.getWidth();
        this.height = stand.getHeight();
        this.type = stand.getType();
        this.status = stand.getStatus();
    }
}