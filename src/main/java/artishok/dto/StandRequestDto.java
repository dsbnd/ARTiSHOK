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
}