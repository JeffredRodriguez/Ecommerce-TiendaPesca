package com.tiendapesca.APItiendapesca.Entities;


import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "new_arrivals")

public class Newarrivals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private LocalDateTime added_at;
}
