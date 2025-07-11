package com.redis.fulltextsearchandautocomplete.domain;

import com.redis.om.spring.annotations.*;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document
public class Movie {

    @Id
    private String id;

    @Searchable
    @AutoComplete
    private String title;

    @Indexed(sortable = true)
    private int year;

    @Indexed
    private List<String> cast;

    @Indexed
    private List<String> genres;

    private String href;

    @Searchable
    @AutoCompletePayload("title")
    private String extract;

    private String thumbnail;
    private int thumbnailWidth;
    private int thumbnailHeight;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<String> getCast() {
        return cast;
    }

    public void setCast(List<String> cast) {
        this.cast = cast;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getExtract() {
        return extract;
    }

    public void setExtract(String extract) {
        this.extract = extract;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public void setThumbnailWidth(int thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
    }

    public int getThumbnailHeight() {
        return thumbnailHeight;
    }

    public void setThumbnailHeight(int thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
    }
}