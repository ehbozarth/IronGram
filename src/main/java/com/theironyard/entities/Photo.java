package com.theironyard.entities;

import javax.persistence.*;

/**
 * Created by earlbozarth on 11/17/15.
 */

@Entity(name="photos")
public class Photo {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    public int id;

    @ManyToOne
    public User sender;

    @ManyToOne
    public User receiver;

    @Column(nullable = false)
    public String filename;

}
