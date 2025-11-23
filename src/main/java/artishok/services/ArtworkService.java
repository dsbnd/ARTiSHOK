package artishok.services;

import java.util.List;

import org.springframework.stereotype.Service;


import artishok.entities.Artwork;
import artishok.repositories.ArtworkRepository;


@Service
public class ArtworkService {
	private final ArtworkRepository artworkRepository;
	
	ArtworkService(ArtworkRepository artworkRepository){
		this.artworkRepository=artworkRepository;
	}
	public List<Artwork> getAllArtworks() {
		return artworkRepository.findAll();
	}
}
