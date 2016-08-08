package br.com.tribosapp.web.rest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codahale.metrics.annotation.Timed;

import br.com.tribosapp.domain.Picture;
import br.com.tribosapp.repository.PictureRepository;
import br.com.tribosapp.web.rest.util.HeaderUtil;

/**
 * REST controller for managing Picture.
 */
@RestController
@RequestMapping("/api")
public class PictureResource {

    private final Logger log = LoggerFactory.getLogger(PictureResource.class);

    @Inject
    private PictureRepository pictureRepository;

    /**
     * POST /pictures : Create a new picture.
     *
     * @param picture
     *            the picture to create
     * @return the ResponseEntity with status 201 (Created) and with body the
     *         new picture, or with status 400 (Bad Request) if the picture has
     *         already an ID
     * @throws URISyntaxException
     *             if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/pictures", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Picture> createPicture(@RequestParam("file") MultipartFile file,
            @RequestParam("file") Picture picture) throws URISyntaxException {
        log.debug("REST request to save Picture : {}", picture);
        if (picture.getId() != null) {
            return ResponseEntity.badRequest().headers(
                    HeaderUtil.createFailureAlert("picture", "idexists", "A new picture cannot already have an ID"))
                    .body(null);
        }
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("picture", "filenotfound", "The file has not been loaded"))
                    .body(null);
        }

        // Add automatic parameters
        picture.setCreatedAt(LocalDate.now());

        // store file in server
        try {
            FileUtils.writeByteArrayToFile(new File(Picture.generateFilename(file.getOriginalFilename())),
                    file.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("picture", "ioexception", e.getMessage())).body(null);
        }

        Picture result = pictureRepository.save(picture);
        return ResponseEntity.created(new URI("/api/pictures/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("picture", result.getId().toString())).body(result);
    }

    /**
     * PUT /pictures : Updates an existing picture.
     *
     * @param picture
     *            the picture to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated
     *         picture, or with status 400 (Bad Request) if the picture is not
     *         valid, or with status 500 (Internal Server Error) if the picture
     *         couldnt be updated
     * @throws URISyntaxException
     *             if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/pictures", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Picture> updatePicture(@RequestParam("file") MultipartFile file, @RequestBody Picture picture)
            throws URISyntaxException {
        log.debug("REST request to update Picture : {}", picture);
        if (picture.getId() == null) {
            return createPicture(file, picture);
        }
        Picture result = pictureRepository.save(picture);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert("picture", picture.getId().toString()))
                .body(result);
    }

    /**
     * GET /pictures : get all the pictures.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of pictures
     *         in body
     */
    @RequestMapping(value = "/pictures", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Picture> getAllPictures() {
        log.debug("REST request to get all Pictures");
        List<Picture> pictures = pictureRepository.findAll();
        return pictures;
    }

    /**
     * GET /pictures/:id : get the "id" picture.
     *
     * @param id
     *            the id of the picture to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the
     *         picture, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/pictures/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Picture> getPicture(@PathVariable Long id) {
        log.debug("REST request to get Picture : {}", id);
        Picture picture = pictureRepository.findOne(id);
        return Optional.ofNullable(picture).map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE /pictures/:id : delete the "id" picture.
     *
     * @param id
     *            the id of the picture to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/pictures/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deletePicture(@PathVariable Long id) {
        log.debug("REST request to delete Picture : {}", id);
        pictureRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("picture", id.toString())).build();
    }

}
